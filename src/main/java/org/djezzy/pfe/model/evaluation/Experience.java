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
@Table(name = "experiences")
public class Experience extends BaseEntity {
    @Column
    private String title;

    @Column
    private String company;

    @Column
    private String startDate;

    @Column
    private String endDate;

    @Column(length = 4000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_data_id", nullable = false)
    private ProfileData profileData;
}




