package com.github.mkorman9.websockets.packets;

import com.github.mkorman9.websockets.packets.client.ClientChatMessage;
import com.github.mkorman9.websockets.packets.client.ClientJoinRequest;
import lombok.Getter;

@Getter
public enum ClientPacketType {
    JOIN_REQUEST(ClientJoinRequest.class),
    CHAT_MESSAGE(ClientChatMessage.class);

    private final Class<?> payload;

    ClientPacketType(Class<?> payload) {
        this.payload = payload;
    }
}
