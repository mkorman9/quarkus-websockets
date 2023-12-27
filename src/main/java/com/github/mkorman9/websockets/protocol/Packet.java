package com.github.mkorman9.websockets.protocol;

import lombok.Builder;

@Builder
public record Packet(
    PacketType type,
    Object data
) {
}
