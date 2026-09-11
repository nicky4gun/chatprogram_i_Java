package org.example;

import java.time.Instant;

public class MessageParser {

    public Message parseClientMessage(String message) {
        String[] fields = message.split("\\|", 3);
        if (fields.length != 3) {
            throw new IllegalArgumentException("Client message must be in format TYPE|TARGET|PAYLOAD");
        }

        String type = fields[0];
        String target = fields[1];
        String payload = fields[2];

        return new Message(null, type, null, target, payload);
    }

    public Message parseServerMessage(String message) {
        String[] fields = message.split("\\|", 5);
        if (fields.length != 5) {
            throw new IllegalArgumentException("Server message must be in format TIMESTAMP|TYPE|SENDER|TARGET|PAYLOAD");
        }

        Instant timestamp = Instant.parse(fields[0]);
        String type = fields[1];
        String sender = fields[2];
        String target = fields[3];
        String payload = fields[4];

        return new Message(timestamp, type, sender, target, payload);
    }

    public String formatClientMessage(Message message) {
        if (message.getType() == null || message.getTarget() == null || message.getPayload() == null) {
            throw new IllegalArgumentException("Client message requires type, target and payload");
        }

        return message.getType() + "|" + message.getTarget() + "|" + message.getPayload();
    }

    public String formatServerMessage(Message message) {
        if (message.getTimestamp() == null
                || message.getType() == null
                || message.getSender() == null
                || message.getTarget() == null
                || message.getPayload() == null) {
            throw new IllegalArgumentException("Server message requires timestamp, type, sender, target and payload");
        }

        return message.getTimestamp() + "|"
                + message.getType() + "|"
                + message.getSender() + "|"
                + message.getTarget() + "|"
                + message.getPayload();
    }
}
