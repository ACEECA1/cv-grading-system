package org.djezzy.pfe.model.evaluation;

import org.djezzy.pfe.model.auth.*;
import org.djezzy.pfe.model.job.*;
import org.djezzy.pfe.model.evaluation.*;
import org.djezzy.pfe.model.system.*;

import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "hr_questions")
public class HRQuestion extends BaseEntity {
    @Column(length = 4000)
    private String question;

    @Column(length = 4000)
    private String psychologicalIntent;

    @Column(length = 4000)
    private String evaluationCriteria;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_evaluation_id", nullable = false)
    private CandidateEvaluation candidateEvaluation;

    @OneToMany(mappedBy = "hrQuestion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IdealResponseIndicator> idealResponseIndicators = new ArrayList<>();

    @OneToMany(mappedBy = "hrQuestion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RedFlag> redFlags = new ArrayList<>();

    @OneToMany(mappedBy = "hrQuestion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FollowUpProbe> followUpProbes = new ArrayList<>();

    public void clearIdealResponseIndicators() {
        idealResponseIndicators.clear();
    }

    public void addIdealResponseIndicator(IdealResponseIndicator indicator) {
        if (indicator == null) {
            return;
        }
        idealResponseIndicators.add(indicator);
        indicator.setHrQuestion(this);
    }

    public void clearRedFlags() {
        redFlags.clear();
    }

    public void addRedFlag(RedFlag redFlag) {
        if (redFlag == null) {
            return;
        }
        redFlags.add(redFlag);
        redFlag.setHrQuestion(this);
    }

    public void clearFollowUpProbes() {
        followUpProbes.clear();
    }

    public void addFollowUpProbe(FollowUpProbe followUpProbe) {
        if (followUpProbe == null) {
            return;
        }
        followUpProbes.add(followUpProbe);
        followUpProbe.setHrQuestion(this);
    }
}




