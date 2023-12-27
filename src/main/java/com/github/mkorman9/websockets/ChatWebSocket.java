package com.github.mkorman9.websockets;

import com.github.mkorman9.websockets.packets.ClientPacket;
import com.github.mkorman9.websockets.packets.ClientPacketParser;
import com.github.mkorman9.websockets.packets.ServerPacketType;
import com.github.mkorman9.websockets.packets.client.ClientChatMessage;
import com.github.mkorman9.websockets.packets.client.ClientJoinRequest;
import com.github.mkorman9.websockets.packets.client.ClientLeaveRequest;
import com.github.mkorman9.websockets.packets.server.JoinConfirmation;
import com.github.mkorman9.websockets.packets.server.JoinRejection;
import com.github.mkorman9.websockets.packets.server.ServerChatMessage;
import com.github.mkorman9.websockets.packets.server.UserJoined;
import com.github.mkorman9.websockets.packets.server.UserLeft;
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
    ClientPacketParser packetParser;

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
                    ServerPacketType.USER_LEFT,
                    UserLeft.builder()
                        .username(client.username())
                        .build()
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
        } catch (ClientPacketParser.PacketParsingException e) {
            // ignore packet
        }
    }

    private void onPacket(Session session, ClientPacket packet) {
        switch (packet.type()) {
            case JOIN_REQUEST -> onJoinRequest(session, (ClientJoinRequest) packet.data());
            case LEAVE_REQUEST -> onLeaveRequest(session, (ClientLeaveRequest) packet.data());
            case CHAT_MESSAGE -> onChatMessage(session, (ClientChatMessage) packet.data());
        }
    }

    private void onJoinRequest(Session session, ClientJoinRequest joinRequest) {
        WebSocketClient client;
        try {
            client = store.register(session, joinRequest.username());
        } catch (WebSocketClientStore.RegisterException e) {
            WebSocketClient.send(
                session,
                ServerPacketType.JOIN_REJECTION,
                JoinRejection.builder()
                    .reason(e.getReason())
                    .build()
            );
            return;
        }

        client.send(
            ServerPacketType.JOIN_CONFIRMATION,
            JoinConfirmation.builder()
                .username(client.username())
                .build()
        );

        store.getClients().stream()
            .filter(c -> !c.session().getId().equals(session.getId()))
            .forEach(c -> c.send(
                ServerPacketType.USER_JOINED,
                UserJoined.builder()
                    .username(client.username())
                    .build()
            ));

        log.info("{} joined", client.username());
    }

    private void onLeaveRequest(Session session, ClientLeaveRequest request) {
        try {
            session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, LEAVING_REASON));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onChatMessage(Session session, ClientChatMessage chatMessage) {
        var client = store.getClient(session);
        if (client == null) {
            return;
        }

        log.info("[{}] {}", client.username(), chatMessage.text());

        store.getClients().forEach(c -> c.send(
            ServerPacketType.CHAT_MESSAGE,
            ServerChatMessage.builder()
                .username(client.username())
                .text(chatMessage.text())
                .build()
        ));
    }
}
