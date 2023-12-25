package com.github.mkorman9.websockets.packets;

import lombok.Builder;

@Builder
public record ServerPacket(
    ServerPacketType type,
    Object data
) {
}
