package com.mwojnar;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.logging.Logger;


public class ChatServer {
    public static final int DEFAULT_PORT = 50051;
    public static final int DEFAULT_THREADS_NUM = 16;
    public static final String CONNECTION_STRING = "jdbc:sqlite:resources/chat.db";

    public static void main(String[] args) throws IOException, InterruptedException, SQLException {
        final var server = new ChatServer();
        server.start();
        server.blockUntilShutdown();
    }

    private final Logger logger;
    private final Server server;
    private final int port;
    private final Connection connection;

    public ChatServer(int port, int nThreads) throws SQLException {
        logger = Logger.getLogger(ChatServer.class.getName());

        try {
            connection = DriverManager.getConnection(CONNECTION_STRING);
            logger.info("Server connected to database.");
        } catch (SQLException e) {
            logger.warning("Server couldn't connect to database.");
            throw e;
        }

        try {
            createTablesIfNotExist();
        } catch (SQLException e) {
            logger.warning("Server couldn't create database tables.");
            throw e;
        }

        this.port = port;
        server = ServerBuilder
                .forPort(port)
                .executor(Executors.newFixedThreadPool(nThreads))
                .addService(new ChatImpl(logger, connection))
                .build();
    }

    public ChatServer(int port) throws SQLException {
        this(port, DEFAULT_THREADS_NUM);
    }

    public ChatServer() throws SQLException {
        this(DEFAULT_PORT);
    }

    private void createTablesIfNotExist() throws SQLException {
        var statement = connection.createStatement();
        statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS groups (
                        group_id    INTEGER PRIMARY KEY
                    );
                """);
        statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS users (
                        user_id     INTEGER PRIMARY KEY,
                        group_id    INTEGER REFERENCES groups (group_id) ON DELETE CASCADE ON UPDATE CASCADE NOT NULL,
                        ack_id      INTEGER
                    );
                """);
        statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS messages (
                        message_id  INTEGER PRIMARY KEY AUTOINCREMENT,
                        reply_id    INTEGER,
                        user_id     INTEGER REFERENCES users (user_id) ON DELETE CASCADE ON UPDATE CASCADE NOT NULL,
                        group_id    INTEGER REFERENCES groups (group_id) ON DELETE CASCADE ON UPDATE CASCADE NOT NULL,
                        priority    INTEGER NOT NULL,
                        text        TEXT NOT NULL,
                        time        INTEGER NOT NULL,
                        media       BLOB,
                        mime        TEXT
                    );
                """);

        logger.info("Created missing tables.");
    }

    private void start() throws IOException {
        server.start();
        logger.info("Server started, listening on " + port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                stop();
            } catch (SQLException e) {
                logger.warning("Server couldn't close database connection.");
                System.out.println(e.getMessage());
            }
        }));
    }

    private void stop() throws SQLException {
        if (connection != null) {
            connection.close();
            logger.info("Database connection closed.");
        }

        if (server != null) {
            logger.info("Shutting down server.");
            server.shutdown();
            logger.info("Server shut down.");
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }
}
