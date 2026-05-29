package org.djezzy.pfe.service.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.djezzy.pfe.config.AppProperties;
import org.djezzy.pfe.util.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LlmParsingServiceTest {

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
    private LlmParsingService llmParsingService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        llmParsingService = new LlmParsingService(webClient, appProperties, objectMapper);
    }

    @Test
    void parseStructuredJd_MissingConfig() {
        AppProperties.Openrouter config = new AppProperties.Openrouter();
        when(appProperties.getOpenrouter()).thenReturn(config);

        AppException ex = assertThrows(AppException.class, () -> llmParsingService.parseStructuredJd("Job desc"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatus());
        assertEquals("OpenRouter configuration is incomplete", ex.getMessage());
    }

    @Test
    void parseStructuredJd_EmptySystemPrompt() {
        AppProperties.Openrouter config = new AppProperties.Openrouter();
        config.setApiKey("key");
        config.setUrl("url");
        config.setModel("model");
        config.setSystemPrompt("");
        when(appProperties.getOpenrouter()).thenReturn(config);

        AppException ex = assertThrows(AppException.class, () -> llmParsingService.parseStructuredJd("Job desc"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatus());
    }
}
