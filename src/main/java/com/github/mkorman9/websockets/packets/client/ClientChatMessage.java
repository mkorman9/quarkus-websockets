package com.github.mkorman9.websockets.packets.client;

import jakarta.validation.constraints.NotBlank;

public record ClientChatMessage(
    @NotBlank String text
) {
}
