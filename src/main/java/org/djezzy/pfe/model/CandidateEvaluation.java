package org.djezzy.pfe.model;

import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "structured_jd_id")
    private StructuredJd structuredJd;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "match_score_id")
    private MatchScore matchScore;

    @OneToMany(mappedBy = "candidateEvaluation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TechnicalQuestion> technicalQuestions = new ArrayList<>();

    @OneToMany(mappedBy = "candidateEvaluation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HRQuestion> hrQuestions = new ArrayList<>();

    @OneToMany(mappedBy = "candidateEvaluation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FollowUpProbe> followUpProbes = new ArrayList<>();

    @OneToMany(mappedBy = "candidateEvaluation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RedFlag> redFlags = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EvaluationStatus status;

    @Lob
    private String detailsJson;
}
