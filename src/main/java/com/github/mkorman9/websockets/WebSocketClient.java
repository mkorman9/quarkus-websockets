package com.github.mkorman9.websockets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.mkorman9.websockets.packets.ServerPacket;
import com.github.mkorman9.websockets.packets.ServerPacketType;
import jakarta.websocket.Session;
import lombok.Builder;

import java.io.IOException;

@Builder
public record WebSocketClient(
    Session session,
    String username
) {
    private static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    public static void send(Session session, ServerPacketType type, Object data) {
        var packet = ServerPacket.builder()
            .type(type)
            .data(data)
            .build();

        try {
            var serializedPacket = objectMapper.writeValueAsString(packet);
            session.getBasicRemote().sendText(serializedPacket);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void send(ServerPacketType type, Object data) {
        send(session, type, data);
    }
}
