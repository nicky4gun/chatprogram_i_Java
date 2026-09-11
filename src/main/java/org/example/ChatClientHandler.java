package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.Instant;

public class ChatClientHandler implements Runnable {
    private final Socket clientSocket;
    private final MessageParser messageParser;
    private String username;

    public ChatClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.messageParser = new MessageParser();
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String rawClientMessage;
            while ((rawClientMessage = in.readLine()) != null) {
                Message clientMessage = messageParser.parseClientMessage(rawClientMessage);
                Message serverMessage;

                if ("LOGIN".equalsIgnoreCase(clientMessage.getType())) {
                    this.username = clientMessage.getPayload();
                    serverMessage = new Message(
                            Instant.now(),
                            "OK",
                            "Server",
                            this.username,
                            "Login godkendt"
                    );
                } else {
                    serverMessage = new Message(
                            Instant.now(),
                            clientMessage.getType(),
                            username != null ? username : "Server",
                            clientMessage.getTarget(),
                            clientMessage.getPayload()
                    );
                }

                out.println(messageParser.formatServerMessage(serverMessage));
            }
        } catch (IOException e) {
            System.out.println("Client handler error: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.out.println("Client socket close error: " + e.getMessage());
            }
        }
    }
}
