package org.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatClient {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 5001;


    public static void main(String[] args) {
        MessageParser parser = new MessageParser();
        
        try (BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));
             Socket socket = new Socket(DEFAULT_HOST, DEFAULT_PORT);
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("Connected to chat server at " + DEFAULT_HOST + ":" + DEFAULT_PORT);
            ServerListener serverListener = new ServerListener(socket);
            serverListener.start();

            System.out.print("Indtast brugernavn: ");
            String username = keyboard.readLine();
            if (username == null || username.trim().isEmpty()) {
                System.out.println("Brugernavn må ikke være tomt.");
                return;
            }

            Message loginMessage = new Message(null, "LOGIN", null, "Server", username.trim());
            out.println(parser.formatClientMessage(loginMessage));

            String loginReply = in.readLine();
            Message loginResponse = parser.parseServerMessage(loginReply);
            System.out.println("Client received: " + loginResponse);

            if ("ERROR".equalsIgnoreCase(loginResponse.getType())) {
                System.out.println("Login fejlede. Prøv et andet brugernavn.");
                return;
            }

            System.out.print("Indtast TYPE: ");
            String type = keyboard.readLine();
            System.out.print("Indtast TARGET: ");
            String target = keyboard.readLine();
            System.out.print("Indtast PAYLOAD: ");
            String payload = keyboard.readLine();

            Message clientMessage = new Message(null, type, null, target, payload);
            String wireMessage = parser.formatClientMessage(clientMessage);
            out.println(wireMessage);

            String serverWireMessage = in.readLine();
            Message serverMessage = parser.parseServerMessage(serverWireMessage);

            System.out.println("Client sent: " + wireMessage);
            System.out.println("Client received: " + serverMessage);
        } catch (Exception e) {
            System.out.println("Client error: " + e.getMessage());
        }
    }
}
