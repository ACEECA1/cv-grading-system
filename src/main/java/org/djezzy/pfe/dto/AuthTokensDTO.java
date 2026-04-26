package org.djezzy.pfe.dto;

public record AuthTokensDTO(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInMs,
        UserDTO user
) {
}
