package org.djezzy.pfe.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WorkflowLoggingConfig {
    private static final String WORKFLOW_PACKAGE = "org.djezzy.pfe.service.workflow";

    private final AppProperties appProperties;

    @PostConstruct
    public void configureWorkflowLogger() {
        LogLevel resolvedLevel = appProperties.getLogging().isShowWorkflow() ? LogLevel.DEBUG : LogLevel.INFO;
        LoggingSystem loggingSystem = LoggingSystem.get(getClass().getClassLoader());
        loggingSystem.setLogLevel(WORKFLOW_PACKAGE, resolvedLevel);
        log.info("Workflow logger level set to {} (SHOW_LOG_WORKFLOW={})",
                resolvedLevel,
                appProperties.getLogging().isShowWorkflow());
    }
}

