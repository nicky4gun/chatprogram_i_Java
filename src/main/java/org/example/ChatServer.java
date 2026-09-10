package org.example;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {

    private static final int PORT = 5001;
    private static final int MAX_USERS_ALLOWED = 3;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            ExecutorService executorService = Executors.newFixedThreadPool(MAX_USERS_ALLOWED);

            System.out.println("Chat server started on port " + PORT + "...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getRemoteSocketAddress());

                ChatClientHandler clientHandler = new ChatClientHandler(clientSocket);
                executorService.execute(clientHandler);
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }
}
