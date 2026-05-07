package org.djezzy.pfe.model.evaluation;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "missing_skills")
public class MissingSkill extends BaseEntity {
    @Column
    private String skillName;

    @Column
    private String importance;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_score_id", nullable = false)
    private MatchScore matchScore;

    public MissingSkill() {
    }

    public MissingSkill(String skillName, String importance) {
        this.skillName = skillName;
        this.importance = importance;
    }
}




