package org.djezzy.pfe.dto.auth;


public record AuthTokensDTO(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInMs,
        UserDTO user
) {
}




