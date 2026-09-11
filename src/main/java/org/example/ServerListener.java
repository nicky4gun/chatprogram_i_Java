package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

public class ServerListener implements Runnable {
    private final Socket socket;
    private final BlockingQueue<String> incomingMessages;
    private Thread listenerThread;

    public ServerListener(Socket socket, BlockingQueue<String> incomingMessages) {
        this.socket = socket;
        this.incomingMessages = incomingMessages;
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
                incomingMessages.put(incomingText);
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            String msg = e.getMessage() == null ? "" : e.getMessage();
            boolean socketClosed = socket.isClosed() || msg.contains("Socket closed") || msg.contains("Connection reset");
            if (!socketClosed) {
                try {
                    incomingMessages.put("ERROR|SERVER|" + msg);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
