package org.djezzy.pfe.dto.evaluation;


import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TechnicalQuestionDTO(
        String question,
        @JsonAlias("expected_answer")
        String expectedAnswer,
        String difficulty,
        @JsonAlias("skill_area")
        String skillArea,
        @JsonAlias("bluff_indicator")
        Boolean bluffIndicator,
        @JsonAlias("follow_up_questions")
        List<String> followUpQuestions
) {
}




