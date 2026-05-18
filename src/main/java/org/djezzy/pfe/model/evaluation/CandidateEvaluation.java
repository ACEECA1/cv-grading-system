package org.djezzy.pfe.model.evaluation;

import org.djezzy.pfe.model.job.StructuredJd;

import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "candidate_evaluations")
public class CandidateEvaluation extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cv_id", unique = true)
    private CV cv;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "structured_jd_id")
    private StructuredJd structuredJd;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "match_score_id")
    private MatchScore matchScore;

    @OneToMany(mappedBy = "candidateEvaluation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TechnicalQuestion> technicalQuestions = new ArrayList<>();

    @OneToMany(mappedBy = "candidateEvaluation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HRQuestion> hrQuestions = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EvaluationStatus status;

    public void clearTechnicalQuestions() {
        technicalQuestions.clear();
    }

    public void addTechnicalQuestion(TechnicalQuestion technicalQuestion) {
        if (technicalQuestion == null) {
            return;
        }
        technicalQuestions.add(technicalQuestion);
        technicalQuestion.setCandidateEvaluation(this);
    }

    public void clearHrQuestions() {
        hrQuestions.clear();
    }

    public void addHrQuestion(HRQuestion hrQuestion) {
        if (hrQuestion == null) {
            return;
        }
        hrQuestions.add(hrQuestion);
        hrQuestion.setCandidateEvaluation(this);
    }
}



