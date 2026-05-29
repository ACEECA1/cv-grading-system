package org.djezzy.pfe.model.auth;

import org.djezzy.pfe.model.evaluation.CV;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "candidates")
public class Candidate extends User {
    @Builder.Default
    @OneToMany(mappedBy = "candidate")
    private List<CV> cvs = new ArrayList<>();
}



