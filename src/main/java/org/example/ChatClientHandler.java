package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatClientHandler implements Runnable {
    private final Socket clientSocket;
    private final PrintWriter out;

    public ChatClientHandler(Socket clientSocket) throws IOException {
        this.clientSocket = clientSocket;
        this.out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("Received from client: " + line);
                // Echo back to client so client-side ServerListener can see it
                out.println("Server echo: " + line);
            }
        } catch (IOException e) {
            System.out.println("Client handler error: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException ignored) {
            }
        }
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
}