package org.example.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChatClientHandlerTest {

    @BeforeEach
    void setUp() {
        ClientRegistry.clear();
    }

    @AfterEach
    void tearDown() {
        ClientRegistry.clear();
    }

    @Test
    void loginAndJoinRoomWorks() throws Exception {
        TestConnection connection = createConnection();
        ChatClientHandler handler = new ChatClientHandler(connection.acceptedSocket, new ChatRoomManager());

        handler.login("alice");
        invokePrivateVoid(handler, "joinRoom", "general");

        assertEquals("alice", handler.getUsername());
        assertEquals(handler, ClientRegistry.getClient("alice"));

        String loginResponse = connection.clientReader.readLine();
        String joinResponse = connection.clientReader.readLine();
        assertTrue(loginResponse.contains("Login godkendt"));
        assertTrue(joinResponse.contains("Du deltager nu i rummet general"));

        connection.close();
    }

    @Test
    void broadcastAndPrivateMessagesReachRightClients() throws Exception {
        TestConnection aliceConnection = createConnection();
        TestConnection bobConnection = createConnection();
        ChatClientHandler aliceHandler = new ChatClientHandler(aliceConnection.acceptedSocket, new ChatRoomManager());
        ChatClientHandler bobHandler = new ChatClientHandler(bobConnection.acceptedSocket, new ChatRoomManager());

        aliceHandler.login("alice");
        bobHandler.login("bob");
        invokePrivateVoid(aliceHandler, "joinRoom", "general");
        invokePrivateVoid(bobHandler, "joinRoom", "general");

        aliceConnection.clientReader.readLine();
        bobConnection.clientReader.readLine();
        aliceConnection.clientReader.readLine();
        bobConnection.clientReader.readLine();

        invokePrivateVoid(aliceHandler, "sendTextToRoom", "general", "hello");

        String roomMessageForAlice = aliceConnection.clientReader.readLine();
        String roomMessageForBob = bobConnection.clientReader.readLine();
        assertTrue(roomMessageForAlice.contains("hello"));
        assertTrue(roomMessageForBob.contains("hello"));

        aliceHandler.sendPrivateMessage("bob", "secret");
        String privateAck = aliceConnection.clientReader.readLine();
        String privateMessage = bobConnection.clientReader.readLine();
        assertTrue(privateAck.contains("Besked sendt"));
        assertTrue(privateMessage.contains("secret"));

        aliceConnection.close();
        bobConnection.close();
    }

    @Test
    void threeClientsCanJoinSameRoomAndBroadcast() throws Exception {
        TestConnection aliceConnection = createConnection();
        TestConnection bobConnection = createConnection();
        TestConnection charlieConnection = createConnection();

        ChatClientHandler aliceHandler = new ChatClientHandler(aliceConnection.acceptedSocket, new ChatRoomManager());
        ChatClientHandler bobHandler = new ChatClientHandler(bobConnection.acceptedSocket, new ChatRoomManager());
        ChatClientHandler charlieHandler = new ChatClientHandler(charlieConnection.acceptedSocket, new ChatRoomManager());

        aliceHandler.login("alice");
        bobHandler.login("bob");
        charlieHandler.login("charlie");
        invokePrivateVoid(aliceHandler, "joinRoom", "general");
        invokePrivateVoid(bobHandler, "joinRoom", "general");
        invokePrivateVoid(charlieHandler, "joinRoom", "general");

        assertTrue(aliceConnection.clientReader.readLine().contains("Login godkendt"));
        assertTrue(bobConnection.clientReader.readLine().contains("Login godkendt"));
        assertTrue(charlieConnection.clientReader.readLine().contains("Login godkendt"));
        assertTrue(aliceConnection.clientReader.readLine().contains("Du deltager nu i rummet general"));
        assertTrue(bobConnection.clientReader.readLine().contains("Du deltager nu i rummet general"));
        assertTrue(charlieConnection.clientReader.readLine().contains("Du deltager nu i rummet general"));

        invokePrivateVoid(aliceHandler, "sendTextToRoom", "general", "hello everyone");

        String aliceBroadcast = aliceConnection.clientReader.readLine();
        String bobBroadcast = bobConnection.clientReader.readLine();
        String charlieBroadcast = charlieConnection.clientReader.readLine();

        assertTrue(aliceBroadcast.contains("hello everyone"));
        assertTrue(bobBroadcast.contains("hello everyone"));
        assertTrue(charlieBroadcast.contains("hello everyone"));

        aliceConnection.close();
        bobConnection.close();
        charlieConnection.close();
    }

    @Test
    void duplicateUsernameIsRejected() throws Exception {
        TestConnection firstConnection = createConnection();
        TestConnection secondConnection = createConnection();

        ChatClientHandler first = new ChatClientHandler(firstConnection.acceptedSocket, new ChatRoomManager());
        ChatClientHandler second = new ChatClientHandler(secondConnection.acceptedSocket, new ChatRoomManager());

        first.login("alice");
        second.login("alice");

        firstConnection.clientReader.readLine();
        secondConnection.clientReader.readLine();

        assertEquals(first, ClientRegistry.getClient("alice"));
        assertTrue(second.getUsername() == null || second.getUsername().isBlank());

        firstConnection.close();
        secondConnection.close();
    }

    @Test
    void malformedMessageAndUnexpectedDisconnectDoNotCrashTheServer() throws Exception {
        TestConnection connection = createConnection();
        ChatClientHandler handler = new ChatClientHandler(connection.acceptedSocket, new ChatRoomManager());

        Thread thread = new Thread(handler);
        thread.start();

        connection.clientWriter.println("BADLINE");
        String errorResponse = connection.clientReader.readLine();
        assertTrue(errorResponse.contains("ERROR"));

        connection.clientWriter.println("LOGIN||alice");
        String loginResponse = connection.clientReader.readLine();
        assertTrue(loginResponse.contains("Login godkendt"));

        connection.clientSocket.close();
        thread.join(1000);

        assertTrue(ClientRegistry.getClient("alice") == null);
        connection.close();
    }

    private static TestConnection createConnection() throws IOException {
        ServerSocket serverSocket = new ServerSocket(0);
        Socket clientSocket = new Socket("localhost", serverSocket.getLocalPort());
        Socket acceptedSocket = serverSocket.accept();
        serverSocket.close();

        return new TestConnection(clientSocket, acceptedSocket);
    }

    private static void invokePrivateVoid(Object target, String methodName, Object... args) throws Exception {
        Class<?>[] parameterTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            parameterTypes[i] = args[i].getClass();
        }
        Method method = target.getClass().getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        method.invoke(target, args);
    }

    private static final class TestConnection implements AutoCloseable {
        private final Socket clientSocket;
        private final Socket acceptedSocket;
        private final BufferedReader clientReader;
        private final PrintWriter clientWriter;

        private TestConnection(Socket clientSocket, Socket acceptedSocket) throws IOException {
            this.clientSocket = clientSocket;
            this.acceptedSocket = acceptedSocket;
            this.clientReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            this.clientWriter = new PrintWriter(clientSocket.getOutputStream(), true);
        }

        @Override
        public void close() throws IOException {
            clientWriter.close();
            clientReader.close();
            clientSocket.close();
            acceptedSocket.close();
        }
    }
}
