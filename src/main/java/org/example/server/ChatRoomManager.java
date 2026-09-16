package org.example.server;

import org.example.protocol.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChatRoomManager {
    private final Map<String, Set<String>> rooms = new HashMap<>();
    private final Map<String, List<Message>> roomHistory = new HashMap<>();

    public boolean joinRoom(String roomName, String username) {
        String normalizedRoomName = normalizeRoomName(roomName);
        String normalizedUsername = normalizeUsername(username);
        if (normalizedRoomName.isEmpty() || normalizedUsername.isEmpty()) {
            return false;
        }

        rooms.putIfAbsent(normalizedRoomName, new HashSet<>());
        return rooms.get(normalizedRoomName).add(normalizedUsername);
    }

    public boolean leaveRoom(String roomName, String username) {
        String normalizedRoomName = normalizeRoomName(roomName);
        String normalizedUsername = normalizeUsername(username);
        if (normalizedRoomName.isEmpty() || normalizedUsername.isEmpty()) {
            return false;
        }

        Set<String> members = rooms.get(normalizedRoomName);
        if (members == null) {
            return false;
        }

        boolean removed = members.remove(normalizedUsername);
        if (members.isEmpty()) {
            rooms.remove(normalizedRoomName);
        }
        return removed;
    }

    public boolean isUserInRoom(String roomName, String username) {
        String normalizedRoomName = normalizeRoomName(roomName);
        String normalizedUsername = normalizeUsername(username);
        if (normalizedRoomName.isEmpty() || normalizedUsername.isEmpty()) {
            return false;
        }

        Set<String> members = rooms.get(normalizedRoomName);
        return members != null && members.contains(normalizedUsername);
    }

    public boolean addRoomMessage(String roomName, Message roomMessage) {
        String normalizedRoomName = normalizeRoomName(roomName);
        if (normalizedRoomName.isEmpty() || roomMessage == null || roomMessage.getType() == null) {
            return false;
        }

        if (!"TEXT".equalsIgnoreCase(roomMessage.getType())) {
            return false;
        }

        roomHistory.computeIfAbsent(normalizedRoomName, key -> new ArrayList<>()).add(roomMessage);
        return true;
    }

    public List<Message> getRoomHistory(String roomName) {
        String normalizedRoomName = normalizeRoomName(roomName);
        if (normalizedRoomName.isEmpty()) {
            return Collections.emptyList();
        }
        return roomHistory.getOrDefault(normalizedRoomName, Collections.emptyList());
    }

    public boolean removeUserFromAllRooms(String username) {
        String normalizedUsername = normalizeUsername(username);
        if (normalizedUsername.isEmpty()) {
            return false;
        }

        boolean removed = false;
        for (Map.Entry<String, Set<String>> entry : new HashMap<>(rooms).entrySet()) {
            Set<String> members = entry.getValue();
            if (members.remove(normalizedUsername)) {
                removed = true;
            }
            if (members.isEmpty()) {
                rooms.remove(entry.getKey());
            }
        }

        return removed;
    }

    private String normalizeRoomName(String roomName) {
        if (roomName == null) {
            return "";
        }
        return roomName.trim().toLowerCase();
    }

    private String normalizeUsername(String username) {
        if (username == null) {
            return "";
        }
        return username.trim();
    }
}
