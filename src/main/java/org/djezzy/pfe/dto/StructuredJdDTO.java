package org.djezzy.pfe.dto;

import java.util.List;

public record StructuredJdDTO(
        Long id,
        String title,
        String companyName,
        List<String> requiredSkills,
        List<String> preferredSkills,
        ExperienceRangeDTO experienceRange,
        List<String> responsibilities,
        List<String> qualifications,
        String workLocation
) {
}
