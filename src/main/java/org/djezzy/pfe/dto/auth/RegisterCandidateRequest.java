package org.djezzy.pfe.dto.auth;

import org.djezzy.pfe.dto.auth.*;
import org.djezzy.pfe.dto.job.*;
import org.djezzy.pfe.dto.evaluation.*;
import org.djezzy.pfe.dto.system.*;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterCandidateRequest(
        @NotBlank @Size(min = 3, max = 60) String username,
        @NotBlank @Size(min = 2, max = 60) String firstName,
        @NotBlank @Size(min = 2, max = 60) String lastName,
        @NotBlank @Email String email,
        @NotBlank
        @Size(min = 8, max = 100)
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Password must contain letters and numbers")
        String password
) {
}




