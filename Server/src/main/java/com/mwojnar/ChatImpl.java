package com.mwojnar;

import com.google.protobuf.ByteString;
import com.mwojnar.gen.*;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class ChatImpl extends ChatGrpc.ChatImplBase {
    private final Logger logger;
    private final Connection connection;
    private final Map<Long, StreamObserver<Message>> observers;

    public ChatImpl(Logger logger, Connection connection) {
        this.logger = logger;
        this.connection = connection;
        this.observers = new HashMap<>();
    }

    @Override
    public void join(UserInfo request, StreamObserver<Message> responseObserver) {
        if (!request.hasUserId() || !request.hasGroupId()) {
            logger.warning("Join request didn't provide userId or groupId.");
            responseObserver.onError(new StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription("Join request must provide userId and groupId.")));
            return;
        }

        try {
            createGroupIfNotExists(request.getGroupId());
        } catch (SQLException e) {
            logger.warning("Couldn't find or create group => user " + request.getUserId() + " join rejected.");
            responseObserver.onError(new StatusRuntimeException(Status.ABORTED.withDescription("Couldn't find or create group.")));
            return;
        }

        long ackId;
        try {
            ackId = insertOrUpdateUser(request.getUserId(), request.getGroupId(), request.hasAckId() ? request.getAckId() : null);
            logger.info("User " + request.getUserId() + " joined successfully.");
        } catch (SQLException e) {
            logger.warning("Couldn't insert or update user => user " + request.getUserId() + " join rejected.");
            responseObserver.onError(new StatusRuntimeException(Status.ABORTED.withDescription("Couldn't insert or update user.")));
            return;
        }

        List<Message> missingMessages;
        try {
            missingMessages = getMissingMessages(request.getGroupId(), ackId);
        } catch (SQLException | IOException e) {
            logger.warning("Couldn't find missing messages for user " + request.getUserId() + ".");
            missingMessages = new LinkedList<>();
        }

        long lastTime;
        try {
            lastTime = getLastTime(request.getUserId());
        } catch (SQLException e) {
            logger.warning("Couldn't find last messages for user " + request.getUserId() + ".");
            lastTime = System.currentTimeMillis() / 1000;
        }

        if (!missingMessages.isEmpty()) {
            missingMessages.forEach(responseObserver::onNext);
            logger.info(String.format("%d missing messages send to user %d.", missingMessages.size(), request.getUserId()));
        } else {
            var response = Message.newBuilder()
                    .setTime(lastTime)
                    .setAckId(ackId)
                    .build();
            responseObserver.onNext(response);
        }

        observers.put(request.getUserId(), responseObserver);
    }

    private void createGroupIfNotExists(long groupId) throws SQLException {
        var statement = connection.createStatement();
        var resultSet = statement.executeQuery("SELECT * FROM groups WHERE groups.group_id == " + groupId);

        if (!resultSet.next()) {
            statement.executeUpdate("INSERT INTO groups VALUES (" + groupId + ")");
            logger.info("Created group " + groupId + ".");
        }
    }

    private long insertOrUpdateUser(long userId, long groupId, Long ackId) throws SQLException {
        var statement = connection.createStatement();

        if (ackId == null) {
            var resultSet = statement.executeQuery("SELECT COALESCE(MAX(message_id), 0) FROM messages WHERE group_id == " + groupId);
            ackId = resultSet.getLong(1);
        }

        statement.executeUpdate(String.format("INSERT OR REPLACE INTO users VALUES(%d, %d, %d)", userId, groupId, ackId));
        return ackId;
    }

    private List<Message> getMissingMessages(long groupId, long ackId) throws SQLException, IOException {
        var statement = connection.createStatement();
        var ackResult = statement.executeQuery("SELECT COALESCE(MAX(message_id), 0) FROM messages WHERE group_id == " + groupId);
        var serverAckId = ackResult.getLong(1);

        var resultsSet = statement.executeQuery(String.format("SELECT * FROM messages WHERE group_id == %d AND message_id > %d", groupId, ackId));
        var list = new LinkedList<Message>();

        while (resultsSet.next()) {
            var message = Message.newBuilder()
                    .setId(resultsSet.getLong("message_id"))
                    .setAckId(serverAckId)
                    .setUserId(resultsSet.getLong("user_id"))
                    .setPriority(Priority.forNumber(resultsSet.getInt("priority")))
                    .setText(resultsSet.getString("text"))
                    .setTime(resultsSet.getLong("time"));

            var replyId = resultsSet.getLong("reply_id");
            if (!resultsSet.wasNull()) {
                message.setReplyId(replyId);
            }

            var mime = resultsSet.getString("mime");
            var media = resultsSet.getBlob("media");
            if (!resultsSet.wasNull()) {
                message.setMime(mime);
                message.setMedia(ByteString.readFrom(media.getBinaryStream()));
            }

            list.add(message.build());
        }

        return list;
    }

    private long getLastTime(long userId) throws SQLException {
        var statement = connection.createStatement();
        var resultSet = statement.executeQuery("SELECT MAX(time) FROM messages WHERE user_id == " + userId);
        return resultSet.getLong(1);
    }

    @Override
    public StreamObserver<Message> sendMessage(StreamObserver<Empty> responseObserver) {
        return new StreamObserver<>() {
            private Long userId = null;
            private Long groupId = null;

            @Override
            public void onNext(Message message) {
                if (!message.hasUserId()) {
                    logger.warning("Message didn't provide userId.");
                    return;
                }

                long newMessageId;
                try {
                    userId = userId == null ? message.getUserId() : userId;
                    newMessageId = saveMessage(message);
                } catch (SQLException e) {
                    logger.warning("Couldn't save new message from user " + message.getUserId() + ".");
                    return;
                }

                var responseBuilder = Message.newBuilder()
                        .setId(newMessageId)
                        .setAckId(newMessageId)
                        .setUserId(userId)
                        .setPriority(message.getPriority())
                        .setText(message.getText())
                        .setTime(message.getTime());

                if (message.hasReplyId()) {
                    responseBuilder.setReplyId(message.getReplyId());
                }

                if (message.hasMedia()) {
                    responseBuilder.setMedia(message.getMedia());
                    responseBuilder.setMime(message.getMime());
                }

                var response = responseBuilder.build();

                for (var entry : observers.entrySet()) {
                    if (entry.getKey() != message.getUserId()) {
                        try {
                            entry.getValue().onNext(response);
                        } catch (StatusRuntimeException e) {
                            logger.warning("Couldn't deliver message to user " + entry.getKey() + " => removing user.");
                            observers.remove(entry.getKey());
                        }
                    }
                }

                logger.info(String.format("Received message %d from user %d and sent to other users.", newMessageId, message.getUserId()));
            }

            private long saveMessage(Message message) throws SQLException {
                var statement = connection.createStatement();
                var groupResult = statement.executeQuery("SELECT group_id FROM users WHERE user_id == " + message.getUserId());
                groupId = groupResult.getLong(1);

                if (groupResult.wasNull()) {
                    throw new SQLException();
                }

                var idResult = statement.executeQuery("SELECT COALESCE(MAX(message_id), 0) FROM messages WHERE group_id == " + groupId);
                var newMessageId = idResult.getLong(1) + 1;

                statement.executeUpdate(String.format("UPDATE users SET ack_id = %d WHERE user_id == %d", message.getAckId(), userId));
                removeOldMessages(groupId);

                var preparedStatement = connection.prepareStatement("INSERT INTO messages VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
                preparedStatement.setLong(1, newMessageId);
                preparedStatement.setLong(3, userId);
                preparedStatement.setLong(4, groupId);
                preparedStatement.setInt(5, message.getPriorityValue());
                preparedStatement.setString(6, message.getText());
                preparedStatement.setLong(7, message.getTime());

                if (message.hasReplyId()) {
                    preparedStatement.setLong(2, message.getReplyId());
                }
                else {
                    preparedStatement.setNull(2, Types.NULL);
                }

                if (message.hasMedia() && message.hasMime()) {
                    preparedStatement.setBytes(8, message.getMedia().toByteArray());
                    preparedStatement.setString(9, message.getMime());
                }
                else {
                    preparedStatement.setNull(8, Types.NULL);
                    preparedStatement.setNull(9, Types.NULL);
                }

                preparedStatement.executeUpdate();
                return newMessageId;
            }

            private void removeOldMessages(long groupId) throws SQLException {
                var statement = connection.createStatement();
                var resultSet = statement.executeQuery("SELECT MIN(ack_id) FROM users WHERE group_id == " + groupId);
                var ackId = resultSet.getLong(1);
                statement.executeUpdate(String.format("DELETE FROM messages WHERE group_id == %d AND message_id <= %d", groupId, ackId));
            }

            @Override
            public void onError(Throwable throwable) {
                logger.warning(throwable.getMessage());
            }

            @Override
            public void onCompleted() {
                if (userId == null || groupId == null) {
                    return;
                }

                observers.get(userId).onCompleted();
                observers.remove(userId);

                try {
                    removeUser(userId);
                } catch (SQLException e) {
                    logger.warning("Couldn't remove user " + userId + ".");
                }

                try {
                    removeOldMessages(groupId);
                } catch (SQLException e) {
                    logger.warning("Couldn't remove old messages from group " + groupId + ".");
                }

                logger.info("User " + userId + " disconnected.");
            }

            private void removeUser(long userId) throws SQLException {
                var statement = connection.createStatement();
                statement.executeUpdate("DELETE FROM users WHERE user_id == " + userId);
            }
        };
    }
}