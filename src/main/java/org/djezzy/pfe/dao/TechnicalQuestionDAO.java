package org.djezzy.pfe.dao;

import org.djezzy.pfe.model.TechnicalQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TechnicalQuestionDAO extends JpaRepository<TechnicalQuestion, Long> {
}
