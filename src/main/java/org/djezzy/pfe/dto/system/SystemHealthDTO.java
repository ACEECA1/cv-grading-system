package org.djezzy.pfe.dto.system;


import java.time.Instant;
import java.util.List;

public record SystemHealthDTO(
        String apiStatus,
        Instant timestamp,
        List<ExternalServiceStatusDTO> externalServices
) {
}




