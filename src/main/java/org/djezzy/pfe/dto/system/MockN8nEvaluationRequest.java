package org.djezzy.pfe.dto.system;

import org.djezzy.pfe.dto.auth.*;
import org.djezzy.pfe.dto.job.*;
import org.djezzy.pfe.dto.evaluation.*;
import org.djezzy.pfe.dto.system.*;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MockN8nEvaluationRequest(
        @NotNull @JsonAlias("candidateEvaluationId") Long evaluationId,
        @NotBlank String cvText,
        @NotNull @Valid JobPayload job
) {
    public record JobPayload(
            Long jobOfferId,
            @NotNull StructuredJdDTO structuredJd
    ) {
    }
}




