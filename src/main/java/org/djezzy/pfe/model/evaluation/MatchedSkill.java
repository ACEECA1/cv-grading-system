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
@Table(name = "matched_skills")
public class MatchedSkill extends BaseEntity {
    @Column
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_score_id", nullable = false)
    private MatchScore matchScore;

    public MatchedSkill() {
    }

    public MatchedSkill(String name) {
        this.name = name;
    }
}




