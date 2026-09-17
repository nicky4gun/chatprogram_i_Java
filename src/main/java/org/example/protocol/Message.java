package org.example.protocol;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class Message {
    private static final DateTimeFormatter DISPLAY_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

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

    public String getFormattedTimestamp() {
        if (timestamp == null) {
            return "";
        }
        return DISPLAY_TIMESTAMP_FORMATTER.format(timestamp);
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
