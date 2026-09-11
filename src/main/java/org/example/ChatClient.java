package org.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

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
        MessageParser parser = new MessageParser();
        
        try (BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));
             Socket socket = new Socket(DEFAULT_HOST, DEFAULT_PORT);
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            
            System.out.print("Indtast TYPE: ");
            String type = keyboard.readLine();
            System.out.print("Indtast TARGET: ");
            String target = keyboard.readLine();
            System.out.print("Indtast PAYLOAD: ");
            String payload = keyboard.readLine();

            Message clientMessage = new Message(null, type, null, target, payload);
            String wireMessage = parser.formatClientMessage(clientMessage);
            out.println(wireMessage);

            String serverWireMessage = in.readLine();
            Message serverMessage = parser.parseServerMessage(serverWireMessage);

            System.out.println("Client sent: " + wireMessage);
            System.out.println("Client received: " + serverMessage);
        } catch (Exception e) {
            System.out.println("Client error: " + e.getMessage());
        }
    }
}
