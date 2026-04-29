package org.djezzy.pfe.dto.job;

import org.djezzy.pfe.dto.auth.*;
import org.djezzy.pfe.dto.job.*;
import org.djezzy.pfe.dto.evaluation.*;
import org.djezzy.pfe.dto.system.*;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record StructuredJdCallbackRequest(
        @NotBlank @JsonAlias("jobTitle") String title,
        String companyName,
        List<String> requiredSkills,
        List<String> preferredSkills,
        ExperienceRangeDTO experienceRange,
        List<String> responsibilities,
        List<String> qualifications,
        String workLocation
) {
}




