package org.djezzy.pfe.model.job;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;
import org.djezzy.pfe.model.auth.User;
import org.djezzy.pfe.model.evaluation.BaseEntity;
import org.djezzy.pfe.model.evaluation.CV;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "job_offers")
@SQLDelete(sql = "UPDATE job_offers SET is_deleted = true WHERE id=?")
@SQLRestriction("is_deleted = false")
public class JobOffer extends BaseEntity {
    @Column(nullable = false)
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String rawText;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 32)
    private JobOfferStatus status;

    @Column
    private String jdRequestId;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @OneToOne(mappedBy = "jobOffer")
    private StructuredJd structuredJd;

    @OneToMany(mappedBy = "jobOffer")
    private List<CV> cvs = new ArrayList<>();
}



