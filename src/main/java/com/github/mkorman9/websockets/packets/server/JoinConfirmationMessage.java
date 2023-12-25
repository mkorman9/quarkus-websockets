package com.github.mkorman9.websockets.packets.server;

import lombok.Builder;

@Builder
public record JoinConfirmationMessage(
    String username
) {
}
