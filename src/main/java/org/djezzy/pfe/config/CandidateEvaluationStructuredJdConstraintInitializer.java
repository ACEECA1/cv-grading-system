package org.djezzy.pfe.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class CandidateEvaluationStructuredJdConstraintInitializer implements ApplicationRunner {
    private static final String TABLE_NAME = "candidate_evaluations";
    private static final String COLUMN_NAME = "structured_jd_id";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        String databaseProduct = jdbcTemplate.execute((ConnectionCallback<String>) connection ->
                connection.getMetaData().getDatabaseProductName());
        if (databaseProduct == null || !databaseProduct.toLowerCase(Locale.ROOT).contains("mysql")) {
            return;
        }

        String schemaName = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
        if (schemaName == null || schemaName.isBlank()) {
            log.warn("Unable to resolve active schema; skipping structured_jd_id unique index check");
            return;
        }

        List<String> uniqueIndexes = jdbcTemplate.query(
                """
                        SELECT DISTINCT INDEX_NAME
                        FROM information_schema.STATISTICS
                        WHERE TABLE_SCHEMA = ?
                          AND TABLE_NAME = ?
                          AND COLUMN_NAME = ?
                          AND NON_UNIQUE = 0
                          AND INDEX_NAME <> 'PRIMARY'
                        """,
                (rs, rowNum) -> rs.getString("INDEX_NAME"),
                schemaName,
                TABLE_NAME,
                COLUMN_NAME
        );

        if (uniqueIndexes.isEmpty()) {
            return;
        }

        for (String indexName : uniqueIndexes) {
            log.info("Dropping stale unique index {} on {}.{}", indexName, TABLE_NAME, COLUMN_NAME);
            jdbcTemplate.execute("ALTER TABLE " + TABLE_NAME + " DROP INDEX `" + indexName + "`");
        }
    }
}

