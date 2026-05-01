package org.djezzy.pfe.dto.job;

import org.djezzy.pfe.dto.auth.*;
import org.djezzy.pfe.dto.job.*;
import org.djezzy.pfe.dto.evaluation.*;
import org.djezzy.pfe.dto.system.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TestJobOfferCreationDTO(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(min = 20) String rawText,
        @NotBlank @Size(max = 255) String companyName,
        @NotNull ExperienceRangeDTO experienceRange,
        @NotBlank @Size(max = 255) String workLocation,
        String employmentType,
        @NotEmpty List<@NotBlank String> requiredSkills,
        List<@NotBlank String> preferredSkills
) {
}




