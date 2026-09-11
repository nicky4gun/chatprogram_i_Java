package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.Instant;

public class ChatClientHandler implements Runnable {
    private final Socket clientSocket;
    private final PrintWriter out;
    private final MessageParser messageParser;
    private String username;

    public ChatClientHandler(Socket clientSocket) throws IOException {
        this.clientSocket = clientSocket;
        this.out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);
        this.messageParser = new MessageParser();
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String rawClientMessage;
            while ((rawClientMessage = in.readLine()) != null) {
                Message clientMessage = messageParser.parseClientMessage(rawClientMessage);
                Message serverMessage = processIncomingMessage(clientMessage);

                out.println(messageParser.formatServerMessage(serverMessage));
            }
        } catch (IOException e) {
            System.out.println("Client handler error: " + e.getMessage());
        } finally {
            ClientRegistry.unregister(this);
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.out.println("Client socket close error: " + e.getMessage());
            }
        }
    }

    Message processIncomingMessage(Message clientMessage) {
        if (clientMessage == null) {
            throw new IllegalArgumentException("Client message kan ikke være null");
        }

        if ("LOGIN".equalsIgnoreCase(clientMessage.getType())) {
            String requestedUsername = clientMessage.getPayload();
            if (requestedUsername == null || requestedUsername.trim().isEmpty()) {
                return createErrorMessage("CLIENT", "Brugernavn må ikke være tomt");
            }

            String normalizedUsername = requestedUsername.trim();
            if (!ClientRegistry.registerUsername(this, normalizedUsername)) {
                return createErrorMessage(normalizedUsername, "Brugernavnet er allerede i brug");
            }

            this.username = normalizedUsername;
            return new Message(
                    Instant.now(),
                    "OK",
                    "Server",
                    normalizedUsername,
                    "Login godkendt"
            );
        }

        if (this.username == null) {
            return createErrorMessage("CLIENT", "Du skal vælge et brugernavn først");
        }

        return new Message(
                Instant.now(),
                clientMessage.getType(),
                this.username,
                clientMessage.getTarget(),
                clientMessage.getPayload()
        );
    }

    // kept for compatibility (not used server-side now)
    public void handleIncomingText(String text) {
        out.println(text);
        System.out.println("Forwarded to client handler: " + text);
    }

    public void sendErrorToServer(String errorMessage) {
        out.println(errorMessage);
        System.out.println("Sent error to server: " + errorMessage);
    }

    public String getUsername() {
        return username;
    }

    public void close() {
        try {
            clientSocket.close();
        } catch (IOException e) {
            System.out.println("Client socket close error: " + e.getMessage());
        }
    }

    private Message createErrorMessage(String target, String message) {
        return new Message(
                Instant.now(),
                "ERROR",
                "Server",
                target,
                message
        );
    }
}