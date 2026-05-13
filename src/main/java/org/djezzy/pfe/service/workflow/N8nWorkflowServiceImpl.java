package org.djezzy.pfe.service.workflow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.djezzy.pfe.config.AppProperties;
import org.djezzy.pfe.util.AppException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.automation", name = "use-n8n", havingValue = "true", matchIfMissing = true)
public class N8nWorkflowServiceImpl implements WorkflowProcessorService {
    private final WebClient webClient;
    private final AppProperties appProperties;

    @Override
    public void processWorkflow(Map<String, Object> payload) {
        String evaluationUrl = appProperties.getN8n().getEvaluationUrl();
        if (evaluationUrl == null || evaluationUrl.isBlank()) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "N8N evaluation URL is not configured");
        }
        webClient.post()
                .uri(evaluationUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .toBodilessEntity()
                .doOnError(error -> log.error("Failed to dispatch n8n evaluation workflow", error))
                .subscribe();
    }
}

