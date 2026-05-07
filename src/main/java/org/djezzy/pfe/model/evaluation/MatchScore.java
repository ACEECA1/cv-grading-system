package org.djezzy.pfe.model.evaluation;


import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
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
@Table(name = "match_scores")
public class MatchScore extends BaseEntity {
    @Column
    private Double overallScore;

    @OneToMany(mappedBy = "matchScore", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MatchedSkill> matchedSkills = new ArrayList<>();

    @OneToMany(mappedBy = "matchScore", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MissingSkill> missingSkills = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "experience_alignment_id")
    private ExperienceAlignment experienceAlignment;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "education_match_id")
    private EducationMatch educationMatch;

    @Column(length = 4000)
    private String recommendation;

    @Column(length = 8000)
    private String reasoning;

    public void clearMatchedSkills() {
        matchedSkills.clear();
    }

    public void addMatchedSkill(MatchedSkill skill) {
        if (skill == null) {
            return;
        }
        matchedSkills.add(skill);
        skill.setMatchScore(this);
    }

    public void clearMissingSkills() {
        missingSkills.clear();
    }

    public void addMissingSkill(MissingSkill skill) {
        if (skill == null) {
            return;
        }
        missingSkills.add(skill);
        skill.setMatchScore(this);
    }
}




