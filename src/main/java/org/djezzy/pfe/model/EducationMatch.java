package org.djezzy.pfe.model;

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

    @Column
    private String matchStatus;
}
