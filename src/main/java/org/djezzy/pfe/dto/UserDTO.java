package org.djezzy.pfe.dto;

import org.djezzy.pfe.model.RhApprovalStatus;
import org.djezzy.pfe.model.Role;

import java.time.Instant;

public record UserDTO(
        Long id,
        String username,
        String firstName,
        String lastName,
        String email,
        Role role,
        Boolean isEnabled,
        RhApprovalStatus hrApprovalStatus,
        Instant createdAt,
        Instant updatedAt
) {
}
