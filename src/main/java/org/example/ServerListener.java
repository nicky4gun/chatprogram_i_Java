package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ServerListener implements Runnable {
    private final Socket socket;
    private Thread listenerThread;

    public ServerListener(Socket socket) {
        this.socket = socket;
    }

    public void start() {
        listenerThread = new Thread(this, "server-listener-" + socket.getPort());
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String incomingText;
            while ((incomingText = in.readLine()) != null) {
                System.out.println(incomingText);
            }
            // normal EOF (peer closed output)
            System.out.println("ServerListener: connection closed by remote");
        } catch (IOException e) {
            // If socket was closed locally as part of graceful shutdown, avoid noisy error log
            String msg = e.getMessage() == null ? "" : e.getMessage();
            boolean socketClosed = socket.isClosed() || msg.contains("Socket closed") || msg.contains("Connection reset");
            if (socketClosed) {
                System.out.println("ServerListener: socket closed");
            } else {
                sendErrorToServer("ServerListener error: " + e.getMessage());
            }
        }
    }

    private void sendErrorToServer(String errorMessage) {
        System.out.println(errorMessage);
    }
}
