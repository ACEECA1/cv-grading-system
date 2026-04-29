package org.djezzy.pfe.dto.system;

import org.djezzy.pfe.dto.auth.*;
import org.djezzy.pfe.dto.job.*;
import org.djezzy.pfe.dto.evaluation.*;
import org.djezzy.pfe.dto.system.*;

public record ExternalServiceStatusDTO(
        String name,
        String url,
        boolean reachable,
        Integer statusCode,
        String message
) {
}




