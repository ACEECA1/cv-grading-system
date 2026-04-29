package org.djezzy.pfe.dto.auth;

import org.djezzy.pfe.dto.auth.*;
import org.djezzy.pfe.dto.job.*;
import org.djezzy.pfe.dto.evaluation.*;
import org.djezzy.pfe.dto.system.*;

public record AuthTokensDTO(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInMs,
        UserDTO user
) {
}




