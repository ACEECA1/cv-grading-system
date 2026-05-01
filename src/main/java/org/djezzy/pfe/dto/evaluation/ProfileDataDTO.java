package org.djezzy.pfe.dto.evaluation;


import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProfileDataDTO(
        @JsonAlias("skills")
        List<String> skills,
        @JsonAlias("hobbies")
        List<String> hobbies,
        @JsonAlias("contact_info")
        ContactInfoPayload contactInfo
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ContactInfoPayload(
            String email,
            String phone,
            String linkedin
    ) {
    }
}




