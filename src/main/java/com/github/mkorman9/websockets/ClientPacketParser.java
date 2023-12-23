package com.github.mkorman9.websockets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Validator;

import java.util.Map;

@ApplicationScoped
public class ClientPacketParser {
    @Inject
    ObjectMapper objectMapper;

    @Inject
    Validator validator;

    public ClientPacket parse(String data) {
        try {
            var packet = objectMapper.readValue(data, RawClientPacket.class);
            if (packet.type() == null || packet.data() == null) {
                throw new UnrecognizedPacketException();
            }

            var payload = objectMapper.convertValue(packet.data(), packet.type().getPayload());
            if (!validator.validate(payload).isEmpty()) {
                throw new PacketDataValidationException();
            }

            return ClientPacket.builder()
                .type(packet.type())
                .data(payload)
                .build();
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new UnrecognizedPacketException();
        }
    }

    public record RawClientPacket(
        ClientPacketType type,
        Map<String, Object> data
    ) {
    }

    public static class UnrecognizedPacketException extends RuntimeException {
    }

    public static class PacketDataValidationException extends RuntimeException {
    }
}
