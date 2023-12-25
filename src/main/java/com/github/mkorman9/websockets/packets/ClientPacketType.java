package com.github.mkorman9.websockets.packets;

import com.github.mkorman9.websockets.packets.client.ClientChatMessage;
import com.github.mkorman9.websockets.packets.client.ClientJoinRequest;
import com.github.mkorman9.websockets.packets.client.ClientLeaveRequest;
import lombok.Getter;

@Getter
public enum ClientPacketType {
    JOIN_REQUEST(ClientJoinRequest.class),
    LEAVE_REQUEST(ClientLeaveRequest.class),
    CHAT_MESSAGE(ClientChatMessage.class);

    private final Class<?> payload;

    ClientPacketType(Class<?> payload) {
        this.payload = payload;
    }
}
