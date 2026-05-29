package org.djezzy.pfe.controller.evaluation;

import org.djezzy.pfe.config.AppProperties;
import org.djezzy.pfe.dto.evaluation.HrEvaluationDetailDTO;
import org.djezzy.pfe.dto.system.DashboardStatsDTO;
import org.djezzy.pfe.filter.ApiKeyAuthenticationFilter;
import org.djezzy.pfe.filter.JwtAuthenticationFilter;
import org.djezzy.pfe.service.auth.CustomUserDetailsService;
import org.djezzy.pfe.service.evaluation.HrService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HrController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class HrControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HrService hrService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private AppProperties appProperties;

    @Test
    void testDashboardStats() throws Exception {
        DashboardStatsDTO stats = new DashboardStatsDTO(10, 5, 20);
        Mockito.when(hrService.dashboardStats()).thenReturn(stats);

        mockMvc.perform(get("/api/hr/dashboard/stats")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCvsProcessed").value(10));
    }

    @Test
    void testGetEvaluation() throws Exception {
        HrEvaluationDetailDTO eval = new HrEvaluationDetailDTO(1L, null, null, null, null, null, null, null, null, null, null, null, "user", null, null, null, null, null);
        Mockito.when(hrService.getEvaluation(1L)).thenReturn(eval);

        mockMvc.perform(get("/api/hr/evaluations/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.candidateFullName").value("user"));
    }
}
