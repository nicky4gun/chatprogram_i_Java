package org.example.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.example.protocol.Message;
import org.junit.jupiter.api.Test;

class ChatRoomManagerTest {

    private final ChatRoomManager chatRoomManager = new ChatRoomManager();

    @Test
    void joinAndLeaveRoomWorks() {
        assertTrue(chatRoomManager.joinRoom("general", "alice"));
        assertTrue(chatRoomManager.isUserInRoom("general", "alice"));

        assertTrue(chatRoomManager.leaveRoom("general", "alice"));
        assertFalse(chatRoomManager.isUserInRoom("general", "alice"));
    }

    @Test
    void invalidRoomOrUsernameIsRejected() {
        assertFalse(chatRoomManager.joinRoom("general", "   "));
        assertFalse(chatRoomManager.leaveRoom("general", "   "));
        assertFalse(chatRoomManager.isUserInRoom("general", "   "));
    }

    @Test
    void roomHistoryIsStoredInOrder() {
        chatRoomManager.addRoomMessage("general", new Message(Instant.parse("2024-01-01T12:00:00Z"), "TEXT", "alice", "general", "first"));
        chatRoomManager.addRoomMessage("general", new Message(Instant.parse("2024-01-01T12:00:01Z"), "TEXT", "bob", "general", "second"));

        assertEquals(2, chatRoomManager.getRoomHistory("general").size());
        assertEquals("first", chatRoomManager.getRoomHistory("general").get(0).getPayload());
        assertEquals("second", chatRoomManager.getRoomHistory("general").get(1).getPayload());
    }

    @Test
    void removeUserFromAllRoomsRemovesUserAcrossRooms() {
        chatRoomManager.joinRoom("general", "alice");
        chatRoomManager.joinRoom("support", "alice");

        assertTrue(chatRoomManager.removeUserFromAllRooms("alice"));
        assertFalse(chatRoomManager.isUserInRoom("general", "alice"));
        assertFalse(chatRoomManager.isUserInRoom("support", "alice"));
    }
}
