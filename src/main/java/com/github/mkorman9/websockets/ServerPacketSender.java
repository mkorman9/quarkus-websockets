package com.github.mkorman9.websockets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.Session;

import java.io.IOException;
import java.util.Collection;

@ApplicationScoped
public class ServerPacketSender {
    private final ObjectMapper objectMapper;

    public ServerPacketSender(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy();
        this.objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    public void send(Session session, ServerPacketType type) {
        send(session, type, new Object());
    }

    public void send(Session session, ServerPacketType type, Object data) {
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

    public void broadcast(Collection<Session> sessions, ServerPacketType type) {
        sessions.forEach(s -> send(s, type));
    }

    public void broadcast(Collection<Session> sessions, ServerPacketType type, Object data) {
        sessions.forEach(s -> send(s, type, data));
    }
}
