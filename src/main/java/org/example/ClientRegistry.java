package org.example;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;

public class ClientRegistry {
    private static final Map<String, ChatClientHandler> ACTIVE_USERS = new ConcurrentHashMap<>();

    private ClientRegistry() {
    }

    public static boolean registerUsername(ChatClientHandler clientHandler, String username) {
        String normalizedUsername = normalizeUsername(username);
        if (clientHandler == null) {
            throw new IllegalArgumentException("Client handler kan ikke være null");
        }

        synchronized (ACTIVE_USERS) {
            ChatClientHandler existingClient = ACTIVE_USERS.get(normalizedUsername);
            if (existingClient != null && existingClient != clientHandler) {
                return false;
            }

            String previousUsername = clientHandler.getUsername();
            if (previousUsername != null && !previousUsername.equals(normalizedUsername)) {
                ACTIVE_USERS.remove(previousUsername);
            }

            ACTIVE_USERS.put(normalizedUsername, clientHandler);
            return true;
        }
    }

    public static void unregister(ChatClientHandler clientHandler) {
        if (clientHandler == null) {
            return;
        }

        synchronized (ACTIVE_USERS) {
            String username = clientHandler.getUsername();
            if (username != null) {
                ACTIVE_USERS.remove(username);
            }
        }
    }

    public static boolean isUsernameTaken(String username) {
        String normalizedUsername = normalizeUsername(username);
        synchronized (ACTIVE_USERS) {
            return normalizedUsername != null && ACTIVE_USERS.containsKey(normalizedUsername);
        }
    }

    public static ChatClientHandler getClient(String username) {
        String normalizedUsername = normalizeUsername(username);
        synchronized (ACTIVE_USERS) {
            return normalizedUsername == null ? null : ACTIVE_USERS.get(normalizedUsername);
        }
    }

    public static Collection<ChatClientHandler> getAllClients() {
        synchronized (ACTIVE_USERS) {
            return new ArrayList<>(ACTIVE_USERS.values());
        }
    }

    public static void clear() {
        synchronized (ACTIVE_USERS) {
            ACTIVE_USERS.clear();
        }
    }

    private static String normalizeUsername(String username) {
        if (username == null) {
            return null;
        }

        String trimmed = username.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
