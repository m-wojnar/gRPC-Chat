package com.mwojnar;

import com.google.protobuf.ByteString;
import com.mwojnar.gen.*;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
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

        if (!missingMessages.isEmpty()) {
            missingMessages.forEach(responseObserver::onNext);
            logger.info(String.format("%d missing messages send to user %d.", missingMessages.size(), request.getUserId()));
        } else {
            var response = Message.newBuilder().setAckId(ackId).build();
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
            var resultSet = statement.executeQuery("SELECT MAX(message_id) FROM messages WHERE group_id == " + groupId);
            ackId = resultSet.getLong("message_id");
            if (resultSet.wasNull()) {
                throw new SQLException();
            }
        }

        statement.executeUpdate(String.format("INSERT INTO users VALUES(%d, %d, %d) ON DUPLICATE KEY UPDATE", userId, groupId, ackId));
        return ackId;
    }

    private List<Message> getMissingMessages(long groupId, long ackId) throws SQLException, IOException {
        var statement = connection.createStatement();
        var resultsSet = statement.executeQuery(String.format("SELECT * FROM messages WHERE group_id == %d AND message_id > %d", groupId, ackId));
        var list = new LinkedList<Message>();

        var ackResult = statement.executeQuery("SELECT MAX(message_id) FROM messages WHERE group_id == " + groupId);
        var serverAckId = ackResult.getLong("message_id");

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

                var response = Message.newBuilder()
                        .setId(newMessageId)
                        .setAckId(newMessageId)
                        .setUserId(userId)
                        .setPriority(message.getPriority())
                        .setText(message.getText())
                        .setTime(message.getTime());

                if (message.hasReplyId()) {
                    response.setReplyId(message.getReplyId());
                }

                if (message.hasMedia()) {
                    response.setMedia(message.getMedia());
                    response.setMime(message.getMime());
                }

                observers.entrySet()
                        .stream()
                        .filter(entry -> entry.getKey() != message.getUserId())
                        .forEach(entry -> entry.getValue().onNext(response.build()));
            }

            private long saveMessage(Message message) throws SQLException {
                var statement = connection.createStatement();
                var groupResult = statement.executeQuery("SELECT group_id FROM users WHERE user_id == " + message.getUserId());
                groupId = groupResult.getLong("group_id");

                if (groupResult.wasNull()) {
                    throw new SQLException();
                }

                var idResult = statement.executeQuery("SELECT MAX(message_id) FROM messages WHERE group_id == " + groupId);
                var newMessageId = idResult.getLong("message_id") + 1;

                statement.executeUpdate(String.format("UPDATE users SET ack_id = %d WHERE user_id == %d", message.getAckId(), userId));
                removeOldMessages(groupId);

                var preparedStatement = connection.prepareStatement(
                        String.format("INSERT INTO messages VALUES (%d, %s, %d, %d, %d, %s, %d, ?, %s)",
                                newMessageId,
                                message.hasReplyId() ? message.getReplyId() : "NULL",
                                userId,
                                groupId,
                                message.getPriorityValue(),
                                message.getText(),
                                message.getTime(),
                                message.hasMime() ? message.getMime() : "NULL"
                        ));

                if (message.hasMedia()) {
                    preparedStatement.setBlob(1, message.getMedia().newInput());
                }

                preparedStatement.executeUpdate();
                return newMessageId;
            }

            private void removeOldMessages(long groupId) throws SQLException {
                var statement = connection.createStatement();
                var resultSet = statement.executeQuery("SELECT MIN(ack_id) FROM users WHERE group_id == " + groupId);
                var ackId = resultSet.getLong("ack_id");
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

                try {
                    observers.remove(userId);
                    removeUser(userId);
                } catch (SQLException e) {
                    logger.warning("Couldn't remove user " + userId + ".");
                }

                try {
                    removeOldMessages(groupId);
                } catch (SQLException e) {
                    logger.warning("Couldn't remove old messages from group " + groupId + ".");
                }
            }

            private void removeUser(long userId) throws SQLException {
                var statement = connection.createStatement();
                statement.executeUpdate("DELETE FROM users WHERE user_id == " + userId);
            }
        };
    }
}