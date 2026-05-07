package org.djezzy.pfe.dto.job;


import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateJobOfferRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(min = 20) @JsonAlias("rawDescription") String rawText
) {
}




