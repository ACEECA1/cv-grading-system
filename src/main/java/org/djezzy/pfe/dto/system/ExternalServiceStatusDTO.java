package org.djezzy.pfe.dto.system;


public record ExternalServiceStatusDTO(
        String name,
        String url,
        boolean reachable,
        Integer statusCode,
        String message
) {
}




