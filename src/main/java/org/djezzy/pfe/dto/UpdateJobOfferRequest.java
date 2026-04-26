package org.djezzy.pfe.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateJobOfferRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(min = 20) @JsonAlias("rawDescription") String rawText
) {
}
