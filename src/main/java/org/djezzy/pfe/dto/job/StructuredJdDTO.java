package org.djezzy.pfe.dto.job;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StructuredJdDTO(
        Long id,
        @JsonAlias("job_title")
        String title,
        @JsonAlias("company_name")
        String companyName,
        @JsonAlias("required_skills")
        List<String> requiredSkills,
        @JsonAlias("preferred_skills")
        List<String> preferredSkills,
        @JsonAlias("experience_range")
        ExperienceRangeDTO experienceRange,
        @JsonAlias("responsibilities")
        List<String> responsibilities,
        @JsonAlias("qualifications")
        List<String> qualifications,
        @JsonAlias("work_location")
        String workLocation,
        @JsonAlias("employment_type")
        String employmentType
) {
}




