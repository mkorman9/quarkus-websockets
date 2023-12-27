package com.github.mkorman9.websockets;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.Session;
import lombok.Getter;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class WebSocketClientStore {
    private final Map<String, WebSocketClient> clients = new ConcurrentHashMap<>();

    public WebSocketClient register(Session session, String username) {
        var client = WebSocketClient.builder()
            .session(session)
            .username(username)
            .build();

        if (clients.values().stream().anyMatch(
            c -> c.username().equals(username) && !c.session().getId().equals(session.getId())
        )) {
            throw new RegisterException("duplicate_username");
        }
        if (clients.putIfAbsent(session.getId(), client) != null) {
            throw new RegisterException("already_joined");
        }

        return client;
    }

    public void unregister(Session session) {
        clients.remove(session.getId());
    }

    public WebSocketClient getClient(Session session) {
        return clients.get(session.getId());
    }

    public Collection<WebSocketClient> getClients() {
        return clients.values();
    }

    @Getter
    public static class RegisterException extends RuntimeException {
        private final String reason;

        public RegisterException(String reason) {
            this.reason = reason;
        }
    }
}
