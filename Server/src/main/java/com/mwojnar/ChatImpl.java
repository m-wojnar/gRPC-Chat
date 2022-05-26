package com.mwojnar;

import com.google.protobuf.ByteString;
import com.mwojnar.gen.*;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

public class ChatImpl extends ChatGrpc.ChatImplBase {
    private final Logger logger;
    private final Connection connection;

    public ChatImpl(Logger logger, Connection connection) {
        this.logger = logger;
        this.connection = connection;
    }

    @Override
    public void join(UserInfo request, StreamObserver<JoinResponse> responseObserver) {
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

        List<Message> missingMessages;
        try {
            missingMessages = getMissingMessages(request.getGroupId(), ackId);
        } catch (SQLException | IOException e) {
            logger.warning("Couldn't find missing messages for user " + request.getUserId() + ".");
            missingMessages = new LinkedList<>();
        }

        var response = JoinResponse.newBuilder().addAllMessages(missingMessages).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();

        logger.info("User " + request.getUserId() + " joined successfully.");
    }

    private void createGroupIfNotExists(long groupId) throws SQLException {
        var statement = connection.createStatement();
        var resultSet = statement.executeQuery("SELECT * FROM groups WHERE groups.group_id == " + groupId);

        if (!resultSet.next()) {
            statement.executeUpdate(String.format("INSERT INTO groups VALUES (%d, DEFAULT)", groupId));
            logger.info("Created group " + groupId);
        }
    }

    private long insertOrUpdateUser(long userId, long groupId, Long ackId) throws SQLException {
        var statement = connection.createStatement();

        if (ackId == null) {
            var resultSet = statement.executeQuery("SELECT MAX(message_id) FROM messages WHERE group_id == " + groupId);
            if (resultSet.next()) {
                ackId = resultSet.getLong("message_id");
            } else {
                throw new SQLException();
            }
        }

        statement.executeUpdate(String.format("INSERT INTO users VALUES(%d, %d, %d) ON DUPLICATE KEY UPDATE", userId, groupId, ackId));
        return ackId;
    }

    private List<Message> getMissingMessages(long groupId, long ackId) throws SQLException, IOException {
        var statement = connection.createStatement();
        var resultsSet = statement.executeQuery(String.format("SELECT * FROM messages WHERE group_id == %d AND ack_id >= %d", groupId, ackId));
        var list = new LinkedList<Message>();

        while (resultsSet.next()) {
            var message = Message.newBuilder()
                    .setId(resultsSet.getLong("message_id"))
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
    public StreamObserver<Message> sendMessage(StreamObserver<Message> responseObserver) {
        return super.sendMessage(responseObserver);
    }
}
