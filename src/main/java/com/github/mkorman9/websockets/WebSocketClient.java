package com.github.mkorman9.websockets;

import io.vertx.core.json.JsonObject;
import jakarta.websocket.Session;
import lombok.Builder;

import java.io.IOException;

@Builder
public record WebSocketClient(
    Session session,
    String username
) {
    public static void send(Session session, String type, JsonObject data) {
        var packet = JsonObject.of()
            .put("type", type)
            .put("data", data);

        try {
            session.getBasicRemote().sendText(packet.encode());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void send(String type, JsonObject data) {
        send(session, type, data);
    }
}
