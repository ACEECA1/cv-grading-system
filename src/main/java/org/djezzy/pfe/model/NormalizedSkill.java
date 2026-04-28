package org.djezzy.pfe.model;

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
@Table(name = "normalized_skills")
public class NormalizedSkill extends BaseEntity {
    @Column
    private String originalName;

    @Column
    private String normalizedName;

    @Column
    private String category;

    @Column
    private String proficiencyLevel;

    @Column
    private Double yearsExperience;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_data_id", nullable = false)
    private ProfileData profileData;
}
