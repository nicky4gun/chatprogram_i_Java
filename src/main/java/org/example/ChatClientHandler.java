package org.example;

import java.net.Socket;

public class ChatClientHandler implements Runnable {
    private final Socket clientSocket;

    public ChatClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        // Handle client communication here
    }
}
