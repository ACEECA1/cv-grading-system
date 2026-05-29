package org.djezzy.pfe.service.workflow;

import org.djezzy.pfe.config.AppProperties;
import org.djezzy.pfe.util.AppException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class N8nWorkflowServiceImplTest {

    @Mock
    private WebClient webClient;

    @Mock
    private AppProperties appProperties;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private N8nWorkflowServiceImpl n8nWorkflowService;

    @Test
    void processWorkflow_Success() {
        AppProperties.N8n n8n = new AppProperties.N8n();
        n8n.setEvaluationUrl("http://n8n-url");
        when(appProperties.getN8n()).thenReturn(n8n);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("http://n8n-url")).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(Mono.just(ResponseEntity.ok().build()));

        assertDoesNotThrow(() -> n8nWorkflowService.processWorkflow(Map.of("key", "value")));

        verify(webClient).post();
    }

    @Test
    void processWorkflow_MissingUrl() {
        AppProperties.N8n n8n = new AppProperties.N8n();
        n8n.setEvaluationUrl("");
        when(appProperties.getN8n()).thenReturn(n8n);

        AppException ex = assertThrows(AppException.class, () -> n8nWorkflowService.processWorkflow(Map.of()));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatus());
        assertEquals("N8N evaluation URL is not configured", ex.getMessage());
    }
}
