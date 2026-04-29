package org.djezzy.pfe.service.system;

import lombok.RequiredArgsConstructor;
import org.djezzy.pfe.config.AppProperties;
import org.djezzy.pfe.dto.system.ExternalServiceStatusDTO;
import org.djezzy.pfe.dto.system.SystemHealthDTO;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemHealthService {
    private static final Duration HEALTH_TIMEOUT = Duration.ofSeconds(4);

    private final AppProperties appProperties;
    private final WebClient webClient;

    public SystemHealthDTO getSystemHealth() {
        ExternalServiceStatusDTO ocr = probeUrl("OCR Service", appProperties.getOcr().getUrl());
        ExternalServiceStatusDTO evaluation = probeUrl("n8n Evaluation", appProperties.getN8n().getEvaluationUrl());
        return new SystemHealthDTO(
                "UP",
                Instant.now(),
                List.of(ocr, evaluation)
        );
    }

    private ExternalServiceStatusDTO probeUrl(String name, String url) {
        if (url == null || url.isBlank()) {
            return new ExternalServiceStatusDTO(name, url, false, null, "URL is not configured");
        }
        try {
            HttpStatusCode status = webClient.method(HttpMethod.GET)
                    .uri(url)
                    .exchangeToMono(response -> Mono.just(response.statusCode()))
                    .timeout(HEALTH_TIMEOUT)
                    .block();
            if (status == null) {
                return new ExternalServiceStatusDTO(name, url, false, null, "No HTTP response");
            }
            boolean reachable = status.value() < 500;
            return new ExternalServiceStatusDTO(
                    name,
                    url,
                    reachable,
                    status.value(),
                    reachable ? "Reachable" : "Service returned server error"
            );
        } catch (Exception ex) {
            return new ExternalServiceStatusDTO(name, url, false, null, ex.getMessage());
        }
    }
}


