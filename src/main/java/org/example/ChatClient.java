package org.example;

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

        Thread messagePrinter = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
               try {
                   String rawMessage = incomingMessages.take();
                   if (rawMessage == null || rawMessage.isBlank()) {
                       continue;
                   }

                   try {
                       Message serverMessage = parser.parseServerMessage(rawMessage);
                       printIncomingMessage(serverMessage);
                   } catch (IllegalArgumentException e) {
                       System.out.println("Server: " + rawMessage);
                   }
               } catch (InterruptedException e) {
                   Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "message-printer");
        messagePrinter.setDaemon(true);

        try (BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));
             Socket socket = new Socket(DEFAULT_HOST, DEFAULT_PORT);
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)) {

            System.out.println("Connected to chat server at " + DEFAULT_HOST + ":" + DEFAULT_PORT);
            ServerListener serverListener = new ServerListener(socket, incomingMessages);
            serverListener.start();
            messagePrinter.start();

            while (true) {
               System.out.print(System.lineSeparator() + "Indtast TYPE (eller EXIT for at lukke): ");
               System.out.flush();
               String type = keyboard.readLine();
               if (type == null) {
                   break;
               }
               if ("EXIT".equalsIgnoreCase(type.trim())) {
                   break;
               }

               System.out.print("Indtast TARGET: ");
               System.out.flush();
               String target = keyboard.readLine();
               System.out.print("Indtast PAYLOAD: ");
               System.out.flush();
               String payload = keyboard.readLine();

               if (target == null || payload == null) {
                   break;
               }

               Message clientMessage = new Message(null, type, null, target, payload);
               String wireMessage = parser.formatClientMessage(clientMessage);
               out.println(wireMessage);
               System.out.println("Sendt: " + wireMessage);
               System.out.flush();
            }
        } catch (Exception e) {
            System.out.println("Client error: " + e.getMessage());
        }
    }

    private static void printIncomingMessage(Message serverMessage) {
        if ("PRIVATE".equalsIgnoreCase(serverMessage.getType())) {
            System.out.println("[PRIVATE] " + serverMessage.getSender() + " -> " + serverMessage.getTarget()
                   + ": " + serverMessage.getPayload());
            return;
        }

        System.out.println("Server response: " + serverMessage.getType()
               + " | " + serverMessage.getSender()
               + " | " + serverMessage.getTarget()
               + " | " + serverMessage.getPayload());
    }
}
