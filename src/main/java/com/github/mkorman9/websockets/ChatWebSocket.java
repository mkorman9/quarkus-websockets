package com.github.mkorman9.websockets;

import com.github.mkorman9.websockets.protocol.Packet;
import com.github.mkorman9.websockets.protocol.PacketParser;
import com.github.mkorman9.websockets.protocol.packets.ChatMessage;
import com.github.mkorman9.websockets.protocol.packets.JoinRequest;
import com.github.mkorman9.websockets.protocol.packets.LeaveRequest;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@ServerEndpoint("/ws")
@ApplicationScoped
@Slf4j
public class ChatWebSocket {
    private static final String LEAVING_REASON = "leaving";

    @Inject
    PacketParser packetParser;

    @Inject
    WebSocketClientStore store;

    @OnOpen
    public void onOpen(Session session) {
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        var client = store.getClient(session);
        if (client != null) {
            store.getClients().stream()
                .filter(c -> !c.session().getId().equals(session.getId()))
                .forEach(c -> c.send(
                    "USER_LEFT",
                    JsonObject.of()
                        .put("username", client.username())
                ));

            if (reason.getReasonPhrase().equals(LEAVING_REASON)) {
                log.info("{} left", client.username());
            } else {
                log.info("{} timed out", client.username());
            }
        }

        store.unregister(session);
    }

    @OnMessage
    public void onMessage(Session session, String data) {
        try {
            var packet = packetParser.parse(data);
            onPacket(session, packet);
        } catch (PacketParser.PacketParsingException e) {
            // ignore packet
        }
    }

    private void onPacket(Session session, Packet packet) {
        switch (packet.type()) {
            case JOIN_REQUEST -> onJoinRequest(session, (JoinRequest) packet.data());
            case LEAVE_REQUEST -> onLeaveRequest(session, (LeaveRequest) packet.data());
            case CHAT_MESSAGE -> onChatMessage(session, (ChatMessage) packet.data());
        }
    }

    private void onJoinRequest(Session session, JoinRequest joinRequest) {
        WebSocketClient client;
        try {
            client = store.register(session, joinRequest.username());
        } catch (WebSocketClientStore.RegisterException e) {
            WebSocketClient.send(
                session,
                "JOIN_REJECTION",
                JsonObject.of()
                    .put("reason", e.getReason())
            );
            return;
        }

        client.send(
            "JOIN_CONFIRMATION",
            JsonObject.of()
                .put("username", client.username())
                .put("users", store.getClients().stream()
                    .map(c -> JsonObject.of()
                        .put("username", c.username())
                    )
                    .toList()
                )
        );

        store.getClients().stream()
            .filter(c -> !c.session().getId().equals(session.getId()))
            .forEach(c -> c.send(
                "USER_JOINED",
                JsonObject.of()
                    .put("username", client.username())
            ));

        log.info("{} joined", client.username());
    }

    private void onLeaveRequest(Session session, LeaveRequest request) {
        try {
            session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, LEAVING_REASON));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onChatMessage(Session session, ChatMessage chatMessage) {
        var client = store.getClient(session);
        if (client == null) {
            return;
        }

        log.info("[{}] {}", client.username(), chatMessage.text());

        store.getClients().forEach(c -> c.send(
            "CHAT_MESSAGE",
            JsonObject.of()
                .put("username", client.username())
                .put("text", chatMessage.text())
        ));
    }
}
