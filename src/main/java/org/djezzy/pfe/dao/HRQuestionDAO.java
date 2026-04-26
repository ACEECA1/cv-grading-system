package org.djezzy.pfe.dao;

import org.djezzy.pfe.model.HRQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HRQuestionDAO extends JpaRepository<HRQuestion, Long> {
}
