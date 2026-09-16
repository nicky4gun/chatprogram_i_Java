package org.example.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void removeUserFromAllRoomsRemovesUserAcrossRooms() {
        chatRoomManager.joinRoom("general", "alice");
        chatRoomManager.joinRoom("support", "alice");

        assertTrue(chatRoomManager.removeUserFromAllRooms("alice"));
        assertFalse(chatRoomManager.isUserInRoom("general", "alice"));
        assertFalse(chatRoomManager.isUserInRoom("support", "alice"));
    }
}
