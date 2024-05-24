package com.redhash.redislite;

import lombok.extern.slf4j.Slf4j;

import java.net.ServerSocket;

@Slf4j
public class Server {
    public static final int PORT = 6379;
    private static final InMemoryStorage STORAGE = new InMemoryStorage();

    public static void main(String[] args) {
        log.info("Starting RedisLite server...");

        try (ServerSocket server = new ServerSocket(PORT)) {
            log.info("Server started on port " + PORT);
            while (true) {
                new ClientHandler(server.accept(), STORAGE).start();
            }
        } catch (Exception e) {
            log.error("Error starting server", e);
        }
    }
}
