package org.djezzy.pfe.dto.system;

import org.djezzy.pfe.dto.auth.*;
import org.djezzy.pfe.dto.job.*;
import org.djezzy.pfe.dto.evaluation.*;
import org.djezzy.pfe.dto.system.*;

import java.time.Instant;
import java.util.List;

public record SystemHealthDTO(
        String apiStatus,
        Instant timestamp,
        List<ExternalServiceStatusDTO> externalServices
) {
}




