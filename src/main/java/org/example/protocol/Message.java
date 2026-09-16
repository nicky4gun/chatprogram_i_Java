package org.example.protocol;

import java.time.Instant;

public class Message {
    private final Instant timestamp;
    private final String type;
    private final String sender;
    private final String target;
    private final String payload;

    public Message(Instant timestamp, String type, String sender, String target, String payload) {
        this.timestamp = timestamp;
        this.type = type;
        this.sender = sender;
        this.target = target;
        this.payload = payload;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getType() {
        return type;
    }

    public String getSender() {
        return sender;
    }

    public String getTarget() {
        return target;
    }

    public String getPayload() {
        return payload;
    }
}
