package org.djezzy.pfe.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "experience_alignments")
public class ExperienceAlignment extends BaseEntity {
    @Column
    private String alignmentLevel;

    @Column
    private String notes;
}
