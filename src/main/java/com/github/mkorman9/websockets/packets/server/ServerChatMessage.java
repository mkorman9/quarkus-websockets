package com.github.mkorman9.websockets.packets.server;

import lombok.Builder;

@Builder
public record ServerChatMessage(
    String authorId,
    String text
) {
}
