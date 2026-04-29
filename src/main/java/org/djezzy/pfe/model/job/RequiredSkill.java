package org.djezzy.pfe.model.job;

import org.djezzy.pfe.model.auth.*;
import org.djezzy.pfe.model.job.*;
import org.djezzy.pfe.model.evaluation.*;
import org.djezzy.pfe.model.system.*;

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
@Table(name = "required_skills")
public class RequiredSkill extends BaseEntity {
    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "structured_jd_id", nullable = false)
    private StructuredJd structuredJd;
}




