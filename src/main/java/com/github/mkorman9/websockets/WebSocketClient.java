package com.github.mkorman9.websockets;

import jakarta.websocket.Session;
import lombok.Builder;

@Builder
public record WebSocketClient(
    Session session,
    String username
) {
}
