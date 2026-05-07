package org.djezzy.pfe.dto.job;


import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record StructuredJdCallbackRequest(
        @NotBlank @JsonAlias({"jobTitle", "job_title"}) String title,
        @JsonAlias({"companyName", "company_name"}) String companyName,
        @JsonAlias({"requiredSkills", "required_skills"}) List<String> requiredSkills,
        @JsonAlias({"preferredSkills", "preferred_skills"}) List<String> preferredSkills,
        @JsonAlias({"experienceRange", "experience_range"}) ExperienceRangeDTO experienceRange,
        @JsonAlias("responsibilities") List<String> responsibilities,
        @JsonAlias("qualifications") List<String> qualifications,
        @JsonAlias({"workLocation", "work_location"}) String workLocation,
        @JsonAlias({"employmentType", "employment_type"}) String employmentType
) {
}




