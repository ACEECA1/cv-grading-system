package org.djezzy.pfe.service.system;

import org.djezzy.pfe.config.AppProperties;
import org.djezzy.pfe.dto.system.SystemHealthDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemHealthServiceTest {

    @Mock
    private AppProperties appProperties;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @InjectMocks
    private SystemHealthService systemHealthService;

    @BeforeEach
    void setUp() {
        AppProperties.Ocr ocr = new AppProperties.Ocr();
        ocr.setUrl("http://ocr-url");
        AppProperties.N8n n8n = new AppProperties.N8n();
        n8n.setEvaluationUrl("http://n8n-url");

        when(appProperties.getOcr()).thenReturn(ocr);
        when(appProperties.getN8n()).thenReturn(n8n);
    }

    @Test
    void getSystemHealth_Success() {
        when(webClient.method(HttpMethod.GET)).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("http://ocr-url")).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("http://n8n-url")).thenReturn(requestBodyUriSpec);

        when(requestBodyUriSpec.exchangeToMono(any())).thenReturn(Mono.just(HttpStatusCode.valueOf(200)));

        SystemHealthDTO health = systemHealthService.getSystemHealth();

        assertNotNull(health);
        assertEquals("UP", health.apiStatus());
        assertEquals(2, health.externalServices().size());
        assertTrue(health.externalServices().get(0).reachable());
        assertTrue(health.externalServices().get(1).reachable());
    }

    @Test
    void getSystemHealth_Error() {
        when(webClient.method(HttpMethod.GET)).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.exchangeToMono(any())).thenReturn(Mono.error(new RuntimeException("Connection refused")));

        SystemHealthDTO health = systemHealthService.getSystemHealth();

        assertNotNull(health);
        assertEquals("UP", health.apiStatus());
        assertFalse(health.externalServices().get(0).reachable());
        assertEquals("Connection refused", health.externalServices().get(0).message());
    }
}
