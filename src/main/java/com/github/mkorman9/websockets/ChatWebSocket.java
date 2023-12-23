package com.github.mkorman9.websockets;

import com.github.mkorman9.websockets.packets.client.ClientChatMessage;
import com.github.mkorman9.websockets.packets.client.ClientJoinRequest;
import com.github.mkorman9.websockets.packets.server.ServerChatMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/ws")
@ApplicationScoped
@Slf4j
public class ChatWebSocket {
    @Inject
    ClientPacketParser packetParser;

    @Inject
    ServerPacketSender sender;

    private final Map<String, Session> clients = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session) {
        log.info("websocket connected");

        clients.put(session.getId(), session);
    }

    @OnClose
    public void onClose(Session session) {
        log.info("websocket disconnected");

        clients.remove(session.getId());
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
        log.info("join request: {}", joinRequest.username());
    }

    private void onChatMessage(Session session, ClientChatMessage chatMessage) {
        log.info("chat message: {}", chatMessage.text());

        sender.broadcast(
            clients.values(),
            ServerPacketType.CHAT_MESSAGE,
            ServerChatMessage.builder()
                .authorId(session.getId())
                .text(chatMessage.text())
            .build()
        );
    }
}
