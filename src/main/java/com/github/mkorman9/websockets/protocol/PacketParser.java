package com.github.mkorman9.websockets.protocol;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Validator;

import java.util.Map;

@ApplicationScoped
public class PacketParser {
    @Inject
    ObjectMapper objectMapper;

    @Inject
    Validator validator;

    public Packet parse(String data) {
        try {
            var packet = objectMapper.readValue(data, RawPacket.class);
            if (packet.type() == null || packet.data() == null) {
                throw new PacketParsingException();
            }

            var payload = objectMapper.convertValue(packet.data(), packet.type().getPayload());
            if (!validator.validate(payload).isEmpty()) {
                throw new PacketParsingException();
            }

            return Packet.builder()
                .type(packet.type())
                .data(payload)
                .build();
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new PacketParsingException();
        }
    }

    public record RawPacket(
        PacketType type,
        Map<String, Object> data
    ) {
    }

    public static class PacketParsingException extends RuntimeException {
    }
}
