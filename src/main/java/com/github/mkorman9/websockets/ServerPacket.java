package com.github.mkorman9.websockets;

import lombok.Builder;

@Builder
public record ServerPacket(
    ServerPacketType type,
    Object data
) {
}
