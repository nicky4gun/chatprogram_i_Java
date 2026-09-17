package org.example.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class MessageParserTest {

    private final MessageParser parser = new MessageParser();

    @Test
    void parseClientMessageParsesValidFormat() {
        Message message = parser.parseClientMessage("LOGIN|general|alice");

        assertEquals("LOGIN", message.getType());
        assertEquals("general", message.getTarget());
        assertEquals("alice", message.getPayload());
    }

    @Test
    void parseServerMessageParsesValidFormat() {
        Instant timestamp = Instant.parse("2024-01-01T12:00:00Z");
        String rawMessage = timestamp + "|TEXT|bob|general|hello";

        Message message = parser.parseServerMessage(rawMessage);

        assertEquals(timestamp, message.getTimestamp());
        assertEquals("TEXT", message.getType());
        assertEquals("bob", message.getSender());
        assertEquals("general", message.getTarget());
        assertEquals("hello", message.getPayload());
    }

    @Test
    void formatCommandsCreateExpectedProtocolStrings() {
        Message clientMessage = new Message(null, "TEXT", null, "general", "hello");
        Message serverMessage = new Message(Instant.parse("2024-01-01T12:00:00Z"), "TEXT", "alice", "general", "hello");

        assertEquals("TEXT|general|hello", parser.formatClientMessage(clientMessage));
        assertEquals("2024-01-01T12:00:00Z|TEXT|alice|general|hello", parser.formatServerMessage(serverMessage));
    }

    @Test
    void historyCommandParsesWithEmptyPayload() {
        Message message = parser.parseClientMessage("HISTORY|general|");

        assertEquals("HISTORY", message.getType());
        assertEquals("general", message.getTarget());
        assertEquals("", message.getPayload());
    }

    @Test
    void malformedInputThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> parser.parseClientMessage("BAD"));
        assertThrows(IllegalArgumentException.class, () -> parser.parseServerMessage("BAD"));
    }
}
