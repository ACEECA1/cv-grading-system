package org.djezzy.pfe.dto.job;

import org.djezzy.pfe.dto.auth.*;
import org.djezzy.pfe.dto.job.*;
import org.djezzy.pfe.dto.evaluation.*;
import org.djezzy.pfe.dto.system.*;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateJobOfferRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(min = 20) @JsonAlias("rawDescription") String rawText
) {
}




