package org.djezzy.pfe.model.job;

import org.djezzy.pfe.model.evaluation.*;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "experience_ranges")
public class ExperienceRange extends BaseEntity {
    @Column
    private String minYears;

    @Column
    private String maxYears;
}




