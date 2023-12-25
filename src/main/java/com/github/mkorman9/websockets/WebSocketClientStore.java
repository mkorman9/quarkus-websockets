package com.github.mkorman9.websockets;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.Session;

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

        if (clients.put(session.getId(), client) != null) {
            return null;
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
}
