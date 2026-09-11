package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ChatClientHandler implements Runnable {
    private static final ConcurrentMap<String, ChatClientHandler> connectedClients = new ConcurrentHashMap<>();

    private final Socket clientSocket;
    private final ChatRoomManager chatRoomManager;
    private final PrintWriter out;
    private final MessageParser messageParser;
    private final Set<String> joinedRooms = ConcurrentHashMap.newKeySet();
    private String username;

    public ChatClientHandler(Socket clientSocket, ChatRoomManager chatRoomManager) throws IOException {
        this.clientSocket = clientSocket;
        this.chatRoomManager = chatRoomManager;
        this.out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);
        this.messageParser = new MessageParser();
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String rawClientMessage;
            while ((rawClientMessage = in.readLine()) != null) {
                Message clientMessage = messageParser.parseClientMessage(rawClientMessage);
                handleClientMessage(clientMessage);
            }
        } catch (IOException e) {
            System.out.println("Client handler error: " + e.getMessage());
        } finally {
            disconnect();
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.out.println("Client socket close error: " + e.getMessage());
            }
        }
    }

    private void handleClientMessage(Message clientMessage) {
        if (clientMessage == null || clientMessage.getType() == null) {
            sendError("Ukendt kommando", "");
            return;
        }

        switch (clientMessage.getType().toUpperCase()) {
            case "LOGIN":
                login(clientMessage.getPayload());
                break;
            case "JOIN_ROOM":
                joinRoom(clientMessage.getTarget());
                break;
            case "LEAVE_ROOM":
                leaveRoom(clientMessage.getTarget());
                break;
            case "TEXT":
                sendTextToRoom(clientMessage.getTarget(), clientMessage.getPayload());
                break;
            default:
                sendError("Ukendt kommando: " + clientMessage.getType(), clientMessage.getTarget());
                break;
        }
    }

    private void login(String requestedUsername) {
        if (requestedUsername == null || requestedUsername.isBlank()) {
            sendError("Brugernavn mangler", "");
            return;
        }

        this.username = requestedUsername.trim();
        connectedClients.put(this.username, this);
        sendMessageToClient(new Message(Instant.now(), "OK", "Server", this.username, "Login godkendt"));
    }

    private void joinRoom(String roomName) {
        if (requireLogin()) {
            return;
        }

        if (requireRoomName(roomName)) {
            return;
        }

        String normalizedRoomName = roomName.trim();
        if (chatRoomManager.joinRoom(normalizedRoomName, username)) {
            joinedRooms.add(normalizedRoomName);
            sendMessageToClient(new Message(Instant.now(), "OK", "Server", normalizedRoomName,
                    "Du deltager nu i rummet " + normalizedRoomName));
            return;
        }

        if (isInRoom(normalizedRoomName)) {
            sendError("Du deltager allerede i rummet", normalizedRoomName);
            return;
        }

        sendError("Kunne ikke tilslutte til rummet", normalizedRoomName);
    }

    private void leaveRoom(String roomName) {
        if (requireLogin()) {
            return;
        }

        if (requireRoomName(roomName)) {
            return;
        }

        String normalizedRoomName = roomName.trim();
        if (chatRoomManager.leaveRoom(normalizedRoomName, username)) {
            joinedRooms.remove(normalizedRoomName);
            sendMessageToClient(new Message(Instant.now(), "OK", "Server", normalizedRoomName,
                    "Du har forladt rummet " + normalizedRoomName));
            return;
        }

        sendError("Du er ikke medlem af rummet", normalizedRoomName);
    }

    private void sendTextToRoom(String roomName, String payload) {
        if (requireLogin()) {
            return;
        }

        if (requireRoomName(roomName)) {
            return;
        }

        String normalizedRoomName = roomName.trim();
        if (!chatRoomManager.isUserInRoom(normalizedRoomName, username)) {
            sendError("Du er ikke medlem af rummet", normalizedRoomName);
            return;
        }

        Message roomMessage = new Message(Instant.now(), "TEXT", username, normalizedRoomName, payload);
        broadcastToRoom(normalizedRoomName, roomMessage);
    }

    private void broadcastToRoom(String roomName, Message roomMessage) {
        for (ChatClientHandler client : connectedClients.values()) {
            if (client != null && client.isInRoom(roomName)) {
                client.sendMessageToClient(roomMessage);
            }
        }
    }

    private boolean requireLogin() {
        if (username == null) {
            sendError("Du skal logge ind først", "");
            return true;
        }
        return false;
    }

    private boolean requireRoomName(String roomName) {
        if (roomName == null || roomName.isBlank()) {
            sendError("Rum-navn mangler", username);
            return true;
        }
        return false;
    }

    private void sendError(String errorMessage, String target) {
        sendMessageToClient(new Message(Instant.now(), "ERROR", "Server", target, errorMessage));
    }

    private boolean isInRoom(String roomName) {
        return roomName != null && joinedRooms.contains(roomName.trim());
    }

    private void disconnect() {
        if (username != null) {
            chatRoomManager.removeUserFromAllRooms(username);
            connectedClients.remove(username, this);
        }
    }

    private void sendMessageToClient(Message message) {
        out.println(messageParser.formatServerMessage(message));
    }
}