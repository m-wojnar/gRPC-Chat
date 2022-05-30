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
import java.util.*;
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
    public void join(UserInfo request, StreamObserver<ServerInfo> responseObserver) {
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
        } catch (SQLException e) {
            logger.warning("Couldn't insert or update user => user " + request.getUserId() + " join rejected.");
            responseObserver.onError(new StatusRuntimeException(Status.ABORTED.withDescription("Couldn't insert or update user.")));
            return;
        }

        long lastTime;
        try {
            lastTime = getLastTime(request.getUserId());
        } catch (SQLException e) {
            lastTime = System.currentTimeMillis() / 1000;
        }

        var response = ServerInfo.newBuilder()
                .setTime(lastTime)
                .setClientAckId(ackId)
                .build();

        logger.info("User " + request.getUserId() + " joined successfully.");
        responseObserver.onNext(response);
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

        statement.executeUpdate("INSERT OR REPLACE INTO users VALUES(" + userId + "," + groupId + "," + ackId + ")");
        return ackId;
    }

    private long getLastTime(long userId) throws SQLException {
        var statement = connection.createStatement();
        var resultSet = statement.executeQuery("SELECT MAX(time) FROM messages WHERE user_id == " + userId);
        return resultSet.getLong(1);
    }

    @Override
    public StreamObserver<Message> messagesStream(StreamObserver<Message> responseObserver) {
        return new StreamObserver<>() {
            private long userId;
            private long groupId;
            private boolean first = true;

            @Override
            public void onNext(Message message) {
                if (first) {
                    try {
                        init(message.getUserId(), responseObserver);
                    } catch (SQLException | IOException e) {
                        logger.warning("Couldn't init user connection => user " + message.getUserId() + " init rejected.");
                        responseObserver.onError(new StatusRuntimeException(Status.ABORTED.withDescription("Couldn't init user.")));
                        return;
                    }
                }

                long newMessageId;
                try {
                    newMessageId = saveMessage(message);
                } catch (SQLException e) {
                    logger.warning("Couldn't save new message from user " + message.getUserId() + ".");
                    responseObserver.onError(new StatusRuntimeException(Status.ABORTED.withDescription("Couldn't save new message.")));
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
                            entry.getValue().onError(new StatusRuntimeException(Status.ABORTED.withDescription("Couldn't deliver new message.")));
                            removeUser(entry.getKey());
                        }
                    }
                }

                logger.info("Received message " + newMessageId + " from user " + userId + " and sent to other users.");
            }

            private void init(long userId, StreamObserver<Message> responseObserver) throws SQLException, IOException {
                this.first = false;
                this.userId = userId;

                var statement = connection.createStatement();
                var resultSet = statement.executeQuery("SELECT group_id, ack_id FROM users WHERE user_id == " + userId);

                this.groupId = resultSet.getLong(1);
                sendMissingMessages(this.groupId, resultSet.getLong(2), responseObserver);
            }

            private void sendMissingMessages(long groupId, long ackId, StreamObserver<Message> responseObserver) throws SQLException, IOException {
                var statement = connection.createStatement();
                var ackResultSet = statement.executeQuery("SELECT COALESCE(MAX(message_id), 0) FROM messages WHERE group_id == " + groupId);
                var serverAckId = ackResultSet.getLong(1);

                var resultsSet = statement.executeQuery("SELECT * FROM messages WHERE group_id == " + groupId + " AND message_id > " + ackId);
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

                    responseObserver.onNext(message.build());
                }
            }

            private long saveMessage(Message message) throws SQLException {
                var statement = connection.createStatement();
                var idResult = statement.executeQuery("SELECT COALESCE(MAX(message_id), 0) FROM messages WHERE group_id == " + groupId);
                var newMessageId = idResult.getLong(1) + 1;

                statement.executeUpdate("UPDATE users SET ack_id = " + message.getAckId() + " WHERE user_id == " + userId);
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
                } else {
                    preparedStatement.setNull(2, Types.NULL);
                }

                if (message.hasMedia() && message.hasMime()) {
                    preparedStatement.setBytes(8, message.getMedia().toByteArray());
                    preparedStatement.setString(9, message.getMime());
                } else {
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
                statement.executeUpdate("DELETE FROM messages WHERE group_id == " + groupId + " AND message_id <= " + ackId);
            }

            @Override
            public void onError(Throwable throwable) {
                logger.warning(throwable.getMessage());
            }

            @Override
            public void onCompleted() {
                observers.get(userId).onCompleted();
                removeUser(userId);

                try {
                    removeOldMessages(groupId);
                } catch (SQLException e) {
                    logger.warning("Couldn't remove old messages from group " + groupId + ".");
                }
            }

            private void removeUser(long userId) {
                try {
                    var statement = connection.createStatement();
                    statement.executeUpdate("DELETE FROM users WHERE user_id == " + userId);
                } catch (SQLException e) {
                    logger.warning("Couldn't remove user " + userId + " from database.");
                }

                observers.remove(userId);
                logger.info("User " + userId + " disconnected.");
            }
        };
    }
}