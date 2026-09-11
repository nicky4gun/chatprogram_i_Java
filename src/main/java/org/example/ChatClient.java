package org.example;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ChatClient {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 5001;


    public static void main(String[] args) {
        try (Socket socket = new Socket(DEFAULT_HOST, DEFAULT_PORT);
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
             Scanner scanner = new Scanner(System.in)) {
            ServerListener serverListener = new ServerListener(socket);
            serverListener.start();

            System.out.println("Connected to server. Type messages and press Enter. Type 'quit' to exit.");
            while (true) {
                String message = scanner.nextLine();
                if (message == null) break;
                if ("quit".equalsIgnoreCase(message.trim())) {
                    System.out.println("Shutting down client...");
                    break;
                }
                out.println(message);
                System.out.println("Client sent: " + message);
            }

            // graceful shutdown
            try {
                socket.close();
            } catch (Exception ignored) {
            }
        } catch (Exception e) {
            System.out.println("Client error: " + e.getMessage());
        }
    }
}
