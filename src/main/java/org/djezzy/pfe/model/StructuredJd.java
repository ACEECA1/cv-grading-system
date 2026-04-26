package org.djezzy.pfe.model;

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
@Table(name = "structured_jds")
public class StructuredJd extends BaseEntity {
    @Column
    private String title;

    @Column
    private String companyName;

    @Column
    private String workLocation;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "experience_range_id")
    private ExperienceRange experienceRange;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_offer_id", unique = true)
    private JobOffer jobOffer;

    @OneToMany(mappedBy = "structuredJd", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RequiredSkill> requiredSkills = new ArrayList<>();

    @OneToMany(mappedBy = "structuredJd", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PreferredSkill> preferredSkills = new ArrayList<>();

    @OneToMany(mappedBy = "structuredJd", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Responsibility> responsibilities = new ArrayList<>();

    @OneToMany(mappedBy = "structuredJd", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Qualification> qualifications = new ArrayList<>();
}
