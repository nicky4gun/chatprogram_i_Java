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

            System.out.println("Connected to chat server at " + DEFAULT_HOST + ":" + DEFAULT_PORT);
            ServerListener serverListener = new ServerListener(socket, incomingMessages);
            serverListener.start();
             messagePrinter.start();

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

        System.out.println("\nServer: " + serverMessage.getType() + "|" + serverMessage.getSender() + "|" + serverMessage.getTarget()
                + "|" + serverMessage.getPayload());
    }
}
