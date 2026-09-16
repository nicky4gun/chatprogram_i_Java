package org.example.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClientRegistryTest {

    @BeforeEach
    void setUp() {
        ClientRegistry.clear();
    }

    @AfterEach
    void tearDown() {
        ClientRegistry.clear();
    }

    @Test
    void registerUsernamePreventsDuplicateNames() throws IOException {
        ChatClientHandler first = createHandler();
        ChatClientHandler second = createHandler();

        assertTrue(ClientRegistry.registerUsername(first, "alice"));
        assertFalse(ClientRegistry.registerUsername(second, "alice"));
        assertEquals(first, ClientRegistry.getClient("alice"));
        assertEquals(1, ClientRegistry.getAllClients().size());
    }

    @Test
    void unregisterRemovesUserFromRegistry() throws IOException {
        ChatClientHandler client = createHandler();

        client.login("bob");
        assertEquals(client, ClientRegistry.getClient("bob"));

        ClientRegistry.unregister(client);
        assertEquals(null, ClientRegistry.getClient("bob"));
    }

    private static ChatClientHandler createHandler() throws IOException {
        ServerSocket serverSocket = new ServerSocket(0);
        Socket clientSocket = new Socket("localhost", serverSocket.getLocalPort());
        Socket acceptedSocket = serverSocket.accept();
        serverSocket.close();
        return new ChatClientHandler(acceptedSocket, new ChatRoomManager());
    }
}
