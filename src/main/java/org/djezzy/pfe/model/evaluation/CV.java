package org.djezzy.pfe.model.evaluation;

import org.djezzy.pfe.model.auth.*;
import org.djezzy.pfe.model.job.*;

import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "cvs")
public class CV extends BaseEntity {
    @Column(nullable = false)
    private String fileUrl;

    @Column(nullable = false)
    private Instant uploadDate;

    @Lob
    private String rawText;

    @Lob
    private String ocrPayloadJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CVProcessingStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_offer_id", nullable = false)
    private JobOffer jobOffer;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "profile_data_id")
    private ProfileData profileData;

    @OneToOne(mappedBy = "cv")
    private CandidateEvaluation candidateEvaluation;
}




