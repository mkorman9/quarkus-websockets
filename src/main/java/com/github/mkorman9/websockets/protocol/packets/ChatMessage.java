package com.github.mkorman9.websockets.protocol.packets;

import jakarta.validation.constraints.NotBlank;

public record ChatMessage(
    @NotBlank String text
) {
}
