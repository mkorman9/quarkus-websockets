package com.github.mkorman9.websockets;

import com.github.mkorman9.websockets.packets.client.ClientChatMessage;
import com.github.mkorman9.websockets.packets.client.ClientJoinRequest;
import com.github.mkorman9.websockets.packets.server.JoinConfirmationMessage;
import com.github.mkorman9.websockets.packets.server.ServerChatMessage;
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

    @Inject
    ServerPacketSender sender;

    @OnOpen
    public void onOpen(Session session) {
    }

    @OnClose
    public void onClose(Session session) {
        store.unregister(session);
    }

    @OnMessage
    public void onMessage(Session session, String data) {
        try {
            var packet = packetParser.parse(data);
            onPacket(session, packet);
        } catch (ClientPacketParser.UnrecognizedPacketException e) {
            // ignore packet
        } catch (ClientPacketParser.PacketDataValidationException e) {
            sender.send(session, ServerPacketType.PACKET_VALIDATION_ERROR);
        }
    }

    private void onPacket(Session session, ClientPacket packet) {
        switch (packet.type()) {
            case JOIN_REQUEST -> onJoinRequest(session, (ClientJoinRequest) packet.data());
            case CHAT_MESSAGE -> onChatMessage(session, (ClientChatMessage) packet.data());
        }
    }

    private void onJoinRequest(Session session, ClientJoinRequest joinRequest) {
        var client = store.register(session, joinRequest.username());
        if (client == null) {
            return;
        }

        sender.send(
            session,
            ServerPacketType.JOIN_CONFIRMATION,
            JoinConfirmationMessage.builder()
                .username(client.username())
                .build()
        );

        log.info("join request: {}", client.username());
    }

    private void onChatMessage(Session session, ClientChatMessage chatMessage) {
        var client = store.getClient(session);
        if (client == null) {
            return;
        }

        log.info("chat message: [{}] {}", client.username(), chatMessage.text());

        store.getClients().forEach(c -> sender.send(
            c.session(),
            ServerPacketType.CHAT_MESSAGE,
            ServerChatMessage.builder()
                .authorId(session.getId())
                .text(chatMessage.text())
                .build()
        ));
    }
}
