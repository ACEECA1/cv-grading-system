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
@Table(name = "ideal_response_indicators")
public class IdealResponseIndicator extends BaseEntity {
    @Column(length = 4000)
    private String text;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hr_question_id", nullable = false)
    private HRQuestion hrQuestion;
}




