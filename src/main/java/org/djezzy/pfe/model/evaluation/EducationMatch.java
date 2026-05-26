package org.djezzy.pfe.model.evaluation;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "education_matches")
public class EducationMatch extends BaseEntity {
    @Column
    private String requiredDegree;

    @Column
    private String candidateDegree;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "match_level")
    private MatchLevel matchLevel;

    @Column(length = 4000)
    private String reasoning;
}




