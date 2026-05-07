package org.djezzy.pfe.model.evaluation;


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
@Table(name = "technical_questions")
public class TechnicalQuestion extends BaseEntity {
    @Column(length = 4000)
    private String question;

    @Column(length = 4000)
    private String expectedAnswer;

    @Column
    private String difficulty;

    @Column
    private String skillArea;

    @Column
    private Boolean bluffIndicator;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_evaluation_id", nullable = false)
    private CandidateEvaluation candidateEvaluation;

    @OneToMany(mappedBy = "technicalQuestion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FollowUpQuestion> followUpQuestions = new ArrayList<>();

    public void clearFollowUpQuestions() {
        followUpQuestions.clear();
    }

    public void addFollowUpQuestion(FollowUpQuestion followUpQuestion) {
        if (followUpQuestion == null) {
            return;
        }
        followUpQuestions.add(followUpQuestion);
        followUpQuestion.setTechnicalQuestion(this);
    }
}




