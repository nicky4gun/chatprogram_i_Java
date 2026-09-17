package org.example.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ChatServer {

    private static final int PORT = 5001;
    private static final int MAX_USERS_ALLOWED = 3;

    public static void main(String[] args) {
        ChatRoomManager chatRoomManager = new ChatRoomManager();
        ExecutorService executorService = Executors.newFixedThreadPool(MAX_USERS_ALLOWED);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Chat server started on port " + PORT + "...");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutdown requested, stopping executor...");
                executorService.shutdown();
                try {
                    if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                        executorService.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executorService.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }));

            while (!serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("New client connected: " + clientSocket.getRemoteSocketAddress());

                    try {
                        ChatClientHandler clientHandler = new ChatClientHandler(clientSocket, chatRoomManager);
                        executorService.execute(clientHandler);
                    } catch (IOException e) {
                        System.out.println("Failed to initialize handlers: " + e.getMessage());
                        try { clientSocket.close(); } catch (IOException ignored) {}
                    }
                } catch (IOException e) {
                    System.out.println("Accept failed: " + e.getMessage());
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        } finally {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
