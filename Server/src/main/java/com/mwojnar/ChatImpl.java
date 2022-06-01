package com.mwojnar;

import com.google.protobuf.ByteString;
import com.mwojnar.gen.*;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Logger;

public class ChatImpl extends ChatGrpc.ChatImplBase {
    private final Logger logger;
    private final Connection connection;
    private final Map<Long, StreamObserver<Message>> observers;
    private final Map<Long, Long> groups;

    public ChatImpl(Logger logger, Connection connection) {
        this.logger = logger;
        this.connection = connection;
        this.observers = new HashMap<>();
        this.groups = new HashMap<>();
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

        logger.info("User " + request.getUserId() + " joined group " + request.getGroupId() + " successfully.");
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private synchronized void createGroupIfNotExists(long groupId) throws SQLException {
        var statement = connection.createStatement();
        var resultSet = statement.executeQuery("SELECT * FROM groups WHERE groups.group_id == " + groupId);

        if (!resultSet.next()) {
            statement.executeUpdate("INSERT INTO groups VALUES (" + groupId + ")");
            logger.info("Created group " + groupId + ".");
        }
    }

    private synchronized long insertOrUpdateUser(long userId, long groupId, Long ackId) throws SQLException {
        var statement = connection.createStatement();

        if (ackId == null) {
            var resultSet = statement.executeQuery("SELECT COALESCE(MAX(message_id), 0) FROM messages WHERE group_id == " + groupId);
            ackId = resultSet.getLong(1);
        }

        statement.executeUpdate("INSERT OR REPLACE INTO users VALUES(" + userId + "," + groupId + "," + ackId + ")");
        return ackId;
    }

    private synchronized long getLastTime(long userId) throws SQLException {
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
                    first = false;
                    try {
                        init(message.getUserId(), responseObserver);

                        observers.put(userId, responseObserver);
                        groups.put(userId, groupId);

                        logger.info("Initialized user " + message.getUserId());
                    } catch (SQLException | IOException e) {
                        logger.warning("Couldn't init connection to user " + message.getUserId() + ".");
                        removeUser(message.getUserId());
                        responseObserver.onError(new StatusRuntimeException(Status.ABORTED.withDescription("Couldn't init user.")));
                    }
                    return;
                }

                long newMessageId;
                try {
                    newMessageId = saveMessage(message);
                } catch (SQLException e) {
                    logger.warning("Couldn't save new message from user " + message.getUserId() + ".");
                    removeUser(message.getUserId());
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

                Map<Long, StreamObserver<Message>> observersCopy;
                Map<Long, Long> groupsCopy;
                synchronized (this) {
                    observersCopy = new HashMap<>(observers);
                    groupsCopy = new HashMap<>(groups);
                }

                var toRemove = new LinkedList<Long>();

                for (var entry : observersCopy.entrySet()) {
                    if (groupsCopy.get(entry.getKey()) == groupId && entry.getKey() != message.getUserId()) {
                        try {
                            entry.getValue().onNext(response);
                        } catch (StatusRuntimeException | IllegalStateException e) {
                            logger.warning("Couldn't deliver message to user " + entry.getKey() + ".");
                            toRemove.add(entry.getKey());
                        }
                    }
                }

                toRemove.forEach(this::removeUser);

                logger.info("Received message " + newMessageId + " from user " + userId + " and sent to other users.");
            }

            private void init(long userId, StreamObserver<Message> responseObserver) throws SQLException, IOException {
                ResultSet resultSet;

                synchronized (this) {
                    var statement = connection.createStatement();
                    resultSet = statement.executeQuery("SELECT group_id, ack_id FROM users WHERE user_id == " + userId);
                }

                this.userId = userId;
                this.groupId = resultSet.getLong(1);
                var ackId = resultSet.getLong(2);

                sendMissingMessages(ackId, responseObserver);
            }

            private synchronized void sendMissingMessages(long ackId, StreamObserver<Message> responseObserver) throws SQLException {
                var statement = connection.createStatement();
                var ackResultSet = statement.executeQuery("SELECT COALESCE(MAX(message_id), 0) FROM messages WHERE group_id == " + groupId);
                var serverAckId = ackResultSet.getLong(1);

                var resultsSet = statement.executeQuery(
                        "SELECT * FROM messages WHERE group_id == " + groupId + " AND message_id > " + ackId + " AND user_id != " + userId + " ORDER BY time"
                );

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
                    var media = resultsSet.getBytes("media");
                    if (!resultsSet.wasNull()) {
                        message.setMime(mime);
                        message.setMedia(ByteString.copyFrom(media));
                    }

                    responseObserver.onNext(message.build());
                }
            }

            private synchronized long saveMessage(Message message) throws SQLException {
                var statement = connection.createStatement();
                statement.executeUpdate("UPDATE users SET ack_id = " + message.getAckId() + " WHERE user_id == " + userId);
                removeOldMessages(groupId);

                var preparedStatement = connection.prepareStatement("INSERT INTO messages VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
                preparedStatement.setNull(1, Types.NULL);
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

                var rowsNum = preparedStatement.executeUpdate();
                var generatedKeys = statement.getGeneratedKeys();

                if (rowsNum == 0 || !generatedKeys.next()) {
                    throw new SQLException();
                }

                return generatedKeys.getLong(1);
            }

            private synchronized void removeOldMessages(long groupId) throws SQLException {
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
                removeUser(userId);

                try {
                    removeOldMessages(groupId);
                } catch (SQLException e) {
                    logger.warning("Couldn't remove old messages from group " + groupId + ".");
                }

                responseObserver.onCompleted();
                logger.info("User " + userId + " disconnected.");
            }

            private synchronized void removeUser(long userId) {
                if (observers.containsKey(userId)) {
                    return;
                }

                observers.remove(userId);
                groups.remove(userId);

                try {
                    var statement = connection.createStatement();
                    statement.executeUpdate("DELETE FROM users WHERE user_id == " + userId);
                } catch (SQLException e) {
                    logger.warning("Couldn't remove user " + userId + " from database.");
                }
            }
        };
    }
}