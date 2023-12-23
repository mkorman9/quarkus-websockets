package com.github.mkorman9.websockets;

import lombok.Builder;

@Builder
public record ClientPacket(
    ClientPacketType type,
    Object data
) {
}
