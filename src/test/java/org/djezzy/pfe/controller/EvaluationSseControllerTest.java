package org.djezzy.pfe.controller;

import org.djezzy.pfe.config.AppProperties;
import org.djezzy.pfe.filter.ApiKeyAuthenticationFilter;
import org.djezzy.pfe.filter.JwtAuthenticationFilter;
import org.djezzy.pfe.service.auth.CustomUserDetailsService;
import org.djezzy.pfe.service.workflow.WorkflowSseService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EvaluationSseController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class EvaluationSseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WorkflowSseService workflowSseService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private AppProperties appProperties;

    @Test
    void testSubscribeToProgress() throws Exception {
        SseEmitter emitter = new SseEmitter();
        Mockito.when(workflowSseService.subscribe(1L)).thenReturn(emitter);

        mockMvc.perform(get("/api/v1/evaluations/1/progress"))
                .andExpect(status().isOk());
    }
}
