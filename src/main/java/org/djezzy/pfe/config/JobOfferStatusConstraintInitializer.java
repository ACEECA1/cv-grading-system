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
public class JobOfferStatusConstraintInitializer implements ApplicationRunner {
    private static final String TABLE_NAME = "job_offers";
    private static final String STATUS_CONSTRAINT_NAME = "job_offers_status_chk";
    private static final String STATUS_CHECK_CLAUSE = "CHECK (status IN ('DRAFT','PUBLISHED','CLOSED','FAILED'))";

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
            log.warn("Unable to resolve active schema; skipping job_offers status constraint check");
            return;
        }

        List<ConstraintInfo> statusChecks = jdbcTemplate.query(
                """
                        SELECT tc.CONSTRAINT_NAME, cc.CHECK_CLAUSE
                        FROM information_schema.TABLE_CONSTRAINTS tc
                        JOIN information_schema.CHECK_CONSTRAINTS cc
                          ON tc.CONSTRAINT_SCHEMA = cc.CONSTRAINT_SCHEMA
                         AND tc.CONSTRAINT_NAME = cc.CONSTRAINT_NAME
                        WHERE tc.CONSTRAINT_SCHEMA = ?
                          AND tc.TABLE_NAME = ?
                          AND tc.CONSTRAINT_TYPE = 'CHECK'
                          AND cc.CHECK_CLAUSE LIKE '%status%'
                        """,
                (rs, rowNum) -> new ConstraintInfo(rs.getString("CONSTRAINT_NAME"), rs.getString("CHECK_CLAUSE")),
                schemaName,
                TABLE_NAME
        );

        if (statusChecks.isEmpty()) {
            return;
        }

        boolean alreadyIncludesFailed = statusChecks.stream()
                .map(ConstraintInfo::checkClause)
                .filter(clause -> clause != null)
                .anyMatch(clause -> clause.toUpperCase(Locale.ROOT).contains("FAILED"));
        if (alreadyIncludesFailed) {
            return;
        }

        log.info("Updating {} status check constraint to include FAILED", TABLE_NAME);
        for (ConstraintInfo constraint : statusChecks) {
            jdbcTemplate.execute("ALTER TABLE " + TABLE_NAME + " DROP CHECK " + constraint.name());
        }
        jdbcTemplate.execute("ALTER TABLE " + TABLE_NAME + " ADD CONSTRAINT " + STATUS_CONSTRAINT_NAME + " " + STATUS_CHECK_CLAUSE);
    }

    private record ConstraintInfo(String name, String checkClause) {
    }
}
