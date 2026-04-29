package org.djezzy.pfe.dto.evaluation;

import org.djezzy.pfe.dto.auth.*;
import org.djezzy.pfe.dto.job.*;
import org.djezzy.pfe.dto.evaluation.*;
import org.djezzy.pfe.dto.system.*;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HrQuestionDTO(
        String question,
        @JsonAlias({"psychological_intent", "purpose"})
        String psychologicalIntent,
        @JsonAlias("ideal_response_indicators")
        List<String> idealResponseIndicators,
        @JsonAlias("red_flags")
        List<String> redFlags,
        @JsonAlias("follow_up_probes")
        List<String> followUpProbes,
        @JsonAlias("evaluation_criteria")
        String evaluationCriteria
) {
}




