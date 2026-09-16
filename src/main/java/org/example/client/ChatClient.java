package org.example.client;

import org.example.protocol.Message;
import org.example.protocol.MessageParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ChatClient {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 5001;

    public static void main(String[] args) {
        MessageParser parser = new MessageParser();
        BlockingQueue<String> incomingMessages = new LinkedBlockingQueue<>();

        Thread messagePrinter = createMessagePrinter(incomingMessages, parser);

        try (BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));
             Socket socket = new Socket(DEFAULT_HOST, DEFAULT_PORT);
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)) {

            System.out.println("Connected to server at " + DEFAULT_HOST + ":" + DEFAULT_PORT);

            ServerListener serverListener = new ServerListener(socket, incomingMessages);
            serverListener.start();
            messagePrinter.start();

            printChatMenu();

            while (true) {
                System.out.print(System.lineSeparator() + "Indtast kommando (eller EXIT for at lukke): ");
                String request = keyboard.readLine();

                if (request == null) {
                    break;
                }

                if ("EXIT".equalsIgnoreCase(request.trim())) {
                    break;
                }

                writer.println(request);
                System.out.println("Client: " + request);
            }
        } catch (Exception e) {
            System.out.println("Client error: " + e.getMessage());
        }
    }

    private static Thread createMessagePrinter(BlockingQueue<String> incomingMessages, MessageParser parser) {
        Thread messagePrinter = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String rawMessage = incomingMessages.take();
                    if (rawMessage.isBlank()) {
                        continue;
                    }

                    Message serverMessage = parser.parseServerMessage(rawMessage);
                    printIncomingMessage(serverMessage);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "message-printer");
        messagePrinter.setDaemon(true);
        return messagePrinter;
    }

    private static void printIncomingMessage(Message serverMessage) {
        if ("PRIVATE".equalsIgnoreCase(serverMessage.getType())) {
            System.out.println("[PRIVATE] " + serverMessage.getSender() + " -> " + serverMessage.getTarget()
                    + ": " + serverMessage.getPayload());
            return;
        }

        if ("TEXT".equalsIgnoreCase(serverMessage.getType())) {
            System.out.println("[" + serverMessage.getTarget() + "] " + serverMessage.getSender()
                    + ": " + serverMessage.getPayload());
            return;
        }

        if ("OK".equalsIgnoreCase(serverMessage.getType())) {
            if (serverMessage.getPayload() != null && serverMessage.getPayload().startsWith("Historik for rummet ")) {
                System.out.println("\n[HISTORY " + serverMessage.getTarget() + "] " + serverMessage.getPayload());
                return;
            }
            System.out.println("\n[OK] " + serverMessage.getPayload());
            return;
        }

        if ("ERROR".equalsIgnoreCase(serverMessage.getType())) {
            System.out.println("\n[ERROR] " + serverMessage.getPayload());
            return;
        }

        System.out.println("\nServer: " + serverMessage.getTimestamp()
                + "|" + serverMessage.getType()
                + "|" + serverMessage.getSender()
                + "|" + serverMessage.getTarget()
                + "|" + serverMessage.getPayload());
    }

    private static void printChatMenu() {
        System.out.println("""
                Available commands:
                1. Login: LOGIN||<username>
                2. Join room: JOIN_ROOM|<room_name>|
                3. Leave room: LEAVE_ROOM|<room_name>|
                4. Send text: TEXT|<room_name>|<message>
                5. History: HISTORY|<room_name>| or HISTORY|| (current room if unique)
                6. Private message: PRIVATE|<recipient>|<message>
                7. Exit: EXIT
                """);
    }
}
