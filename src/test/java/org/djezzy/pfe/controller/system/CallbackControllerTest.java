package org.djezzy.pfe.controller.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.djezzy.pfe.config.AppProperties;
import org.djezzy.pfe.dto.evaluation.CandidateEvaluationDTO;
import org.djezzy.pfe.dto.job.JobOfferDTO;
import org.djezzy.pfe.dto.evaluation.N8nEvaluationPayloadDTO;
import org.djezzy.pfe.dto.job.StructuredJdCallbackRequest;
import org.djezzy.pfe.filter.ApiKeyAuthenticationFilter;
import org.djezzy.pfe.filter.JwtAuthenticationFilter;
import org.djezzy.pfe.service.auth.CustomUserDetailsService;
import org.djezzy.pfe.service.system.CallbackService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CallbackController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class CallbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CallbackService callbackService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private AppProperties appProperties;

    @Test
    void testStructuredJdCallback() throws Exception {
        StructuredJdCallbackRequest request = new StructuredJdCallbackRequest("title", null, java.util.List.of("skill"), null, null, null, null, null, null);
        JobOfferDTO offer = new JobOfferDTO(1L, "title", "raw", org.djezzy.pfe.model.job.JobOfferStatus.PUBLISHED, "req", null, null, null);
        
        Mockito.when(callbackService.handleStructuredJdCallback(eq(1L), any(StructuredJdCallbackRequest.class))).thenReturn(offer);

        mockMvc.perform(put("/api/callbacks/job-offers/1/structured-jd")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("title"));
    }

    @Test
    void testEvaluationCallback() throws Exception {
        N8nEvaluationPayloadDTO request = new N8nEvaluationPayloadDTO("status", "time", null, new N8nEvaluationPayloadDTO.MatchScoreDTO(85.0, null, null, null, null, null, null), null, null);
        CandidateEvaluationDTO eval = new CandidateEvaluationDTO(1L, null, 85.0, "fb", null, null, null, null, null);
        
        Mockito.when(callbackService.handleEvaluationCallback(eq(1L), any(N8nEvaluationPayloadDTO.class))).thenReturn(eval);

        mockMvc.perform(put("/api/callbacks/evaluations/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.overallScore").value(85.0));
    }
}
