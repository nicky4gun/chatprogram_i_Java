package org.example.server;

import org.example.protocol.Message;
import org.example.protocol.MessageParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChatClientHandler implements Runnable {
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
                if (rawClientMessage.isBlank()) {
                    continue;
                }

                System.out.println("[Client] " + rawClientMessage);

                Message clientMessage;
                try {
                    clientMessage = messageParser.parseClientMessage(rawClientMessage);
                } catch (IllegalArgumentException e) {
                    sendError("Fejl i beskedformat: " + e.getMessage(), "");
                    continue;
                }

                try {
                    handleClientMessage(clientMessage);
                } catch (Exception e) {
                    sendError("Fejl ved behandling af besked: " + (e.getMessage() == null ? "" : e.getMessage()), "");
                }
            }
        } catch (IOException e) {
            System.out.println("Client handler error: " + e.getMessage());
        } finally {
            disconnect();
            ClientRegistry.unregister(this);
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
            case "LOGIN" -> login(clientMessage.getPayload());
            case "JOIN_ROOM" -> joinRoom(clientMessage.getTarget());
            case "LEAVE_ROOM" -> leaveRoom(clientMessage.getTarget());
            case "TEXT" -> sendTextToRoom(clientMessage.getTarget(), clientMessage.getPayload());
            case "PRIVATE" -> sendPrivateMessage(clientMessage.getTarget(), clientMessage.getPayload());
            case "HISTORY" -> showHistory(clientMessage.getTarget());
            default -> sendError("Ukendt kommando: " + clientMessage.getType(), clientMessage.getTarget());
        }
    }

    public void login(String requestedUsername) {
        if (requestedUsername == null || requestedUsername.isBlank()) {
            sendError("Brugernavn mangler", "");
            return;
        }

        String normalizedUsername = requestedUsername.trim();
        if (!ClientRegistry.registerUsername(this, normalizedUsername)) {
            sendError("Brugernavnet er allerede i brug", "");
            return;
        }

        this.username = normalizedUsername;
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
            showHistory(normalizedRoomName);
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
        chatRoomManager.addRoomMessage(normalizedRoomName, roomMessage);
        broadcastToRoom(normalizedRoomName, roomMessage);
    }

    private void showHistory(String roomName) {
        if (requireLogin()) {
            return;
        }

        String resolvedRoomName = resolveHistoryRoomName(roomName);
        if (resolvedRoomName == null) {
            return;
        }

        if (!chatRoomManager.isUserInRoom(resolvedRoomName, username)) {
            sendError("Du er ikke medlem af rummet", resolvedRoomName);
            return;
        }

        List<Message> history = chatRoomManager.getRoomHistory(resolvedRoomName);
        sendMessageToClient(new Message(Instant.now(), "OK", "Server", resolvedRoomName,
                "Historik for rummet " + resolvedRoomName + ":"));

        if (history.isEmpty()) {
            sendMessageToClient(new Message(Instant.now(), "OK", "Server", resolvedRoomName,
                    "Der er ingen historik i rummet endnu"));
            return;
        }

        for (Message historicalMessage : history) {
            sendMessageToClient(new Message(Instant.now(), "TEXT", historicalMessage.getSender(),
                    historicalMessage.getTarget(), historicalMessage.getPayload()));
        }
    }

    private String resolveHistoryRoomName(String roomName) {
        if (roomName != null && !roomName.isBlank()) {
            return roomName.trim();
        }

        if (joinedRooms.isEmpty()) {
            sendError("Du er ikke medlem af noget rum", "");
            return null;
        }

        if (joinedRooms.size() == 1) {
            return joinedRooms.iterator().next();
        }

        sendError("Angiv et rum-navn, du deltager i flere rum", "");
        return null;
    }

    public void sendPrivateMessage(String recipientUsername, String payload) {
        if (requireLogin()) {
            return;
        }

        if (recipientUsername == null || recipientUsername.isBlank()) {
            sendError("Modtager mangler", "");
            return;
        }

        String normalizedRecipient = recipientUsername.trim();
        ChatClientHandler recipient = ClientRegistry.getClient(normalizedRecipient);
        if (recipient == null) {
            sendError("Brugeren findes ikke", normalizedRecipient);
            return;
        }

        Message privateMessage = new Message(Instant.now(), "PRIVATE", username, normalizedRecipient, payload);
        recipient.sendMessageToClient(privateMessage);
        sendMessageToClient(new Message(Instant.now(), "OK", "Server", normalizedRecipient, "Besked sendt"));
    }

    private void broadcastToRoom(String roomName, Message roomMessage) {
        for (ChatClientHandler client : ClientRegistry.getAllClients()) {
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
            ClientRegistry.unregister(this);
        }
    }

    private void sendMessageToClient(Message message) {
        String formattedMessage = messageParser.formatServerMessage(message);
        System.out.println("[Server] " + formattedMessage);
        out.println(formattedMessage);
    }

    public String getUsername() {
        return username;
    }
}