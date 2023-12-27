package com.github.mkorman9.websockets.protocol;

import com.github.mkorman9.websockets.protocol.packets.ChatMessage;
import com.github.mkorman9.websockets.protocol.packets.JoinRequest;
import com.github.mkorman9.websockets.protocol.packets.LeaveRequest;
import lombok.Getter;

@Getter
public enum PacketType {
    JOIN_REQUEST(JoinRequest.class),
    LEAVE_REQUEST(LeaveRequest.class),
    CHAT_MESSAGE(ChatMessage.class);

    private final Class<?> payload;

    PacketType(Class<?> payload) {
        this.payload = payload;
    }
}
