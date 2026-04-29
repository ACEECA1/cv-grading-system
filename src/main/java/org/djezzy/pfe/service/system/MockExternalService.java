package org.djezzy.pfe.service.system;

import lombok.RequiredArgsConstructor;
import org.djezzy.pfe.config.AppProperties;
import org.djezzy.pfe.dto.system.MockN8nEvaluationRequest;
import org.djezzy.pfe.dto.evaluation.N8nEvaluationPayloadDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class MockExternalService {
    private static final String API_KEY_HEADER = "X-API-KEY";

    private final WebClient webClient;
    private final AppProperties appProperties;
    @Qualifier("applicationTaskExecutor")
    private final Executor applicationTaskExecutor;

    public void simulateEvaluationCallback(MockN8nEvaluationRequest request, String callbackUrl) {
        CompletableFuture.runAsync(() -> sendScoredEvaluationCallback(request, callbackUrl), applicationTaskExecutor);
    }

    private void sendScoredEvaluationCallback(MockN8nEvaluationRequest request, String callbackUrl) {
        sleepThreeSeconds();
        String callbackApiKey = appProperties.getCallback().getApiKey();
        if (callbackApiKey == null || callbackApiKey.isBlank()) {
            throw new IllegalStateException("Callback API key is not configured");
        }

        N8nEvaluationPayloadDTO callbackRequest = new N8nEvaluationPayloadDTO(
                "success",
                "3.0s",
                null,
                new N8nEvaluationPayloadDTO.MatchScoreDTO(
                        randomScore(),
                        List.of(),
                        List.of(),
                        null,
                        null,
                        "Auto-scored by mock n8n",
                        "Synthetic callback payload"
                ),
                List.of(),
                List.of()
        );
        webClient.put()
                .uri(callbackUrl)
                .header(API_KEY_HEADER, callbackApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(callbackRequest)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    private void sleepThreeSeconds() {
        try {
            Thread.sleep(3_000);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Mock n8n processing interrupted", ex);
        }
    }

    private double randomScore() {
        double value = ThreadLocalRandom.current().nextDouble(70.0, 99.9);
        return Math.round(value * 10.0) / 10.0;
    }
}


