package com.github.mkorman9.websockets;

import com.github.mkorman9.websockets.packets.ClientPacket;
import com.github.mkorman9.websockets.packets.ClientPacketParser;
import com.github.mkorman9.websockets.packets.ServerPacketType;
import com.github.mkorman9.websockets.packets.client.ClientChatMessage;
import com.github.mkorman9.websockets.packets.client.ClientJoinRequest;
import com.github.mkorman9.websockets.packets.server.JoinConfirmation;
import com.github.mkorman9.websockets.packets.server.JoinRejection;
import com.github.mkorman9.websockets.packets.server.ServerChatMessage;
import com.github.mkorman9.websockets.packets.server.UserJoined;
import com.github.mkorman9.websockets.packets.server.UserLeft;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;

@ServerEndpoint("/ws")
@ApplicationScoped
@Slf4j
public class ChatWebSocket {
    @Inject
    ClientPacketParser packetParser;

    @Inject
    WebSocketClientStore store;

    @OnOpen
    public void onOpen(Session session) {
    }

    @OnClose
    public void onClose(Session session) {
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

            log.info("{} left", client.username());
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
            case CHAT_MESSAGE -> onChatMessage(session, (ClientChatMessage) packet.data());
        }
    }

    private void onJoinRequest(Session session, ClientJoinRequest joinRequest) {
        var result = store.register(session, joinRequest.username());
        if (!result.success()) {
            result.client().send(
                ServerPacketType.JOIN_REJECTION,
                JoinRejection.builder()
                    .reason(result.reason())
                    .build()
            );
            return;
        }

        var client = result.client();

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
