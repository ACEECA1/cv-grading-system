package org.djezzy.pfe.controller.system;

import org.djezzy.pfe.config.AppProperties;
import org.djezzy.pfe.dto.system.SystemHealthDTO;
import org.djezzy.pfe.filter.ApiKeyAuthenticationFilter;
import org.djezzy.pfe.filter.JwtAuthenticationFilter;
import org.djezzy.pfe.service.auth.CustomUserDetailsService;
import org.djezzy.pfe.service.system.SystemHealthService;
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

@WebMvcTest(controllers = SystemController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class SystemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SystemHealthService systemHealthService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private AppProperties appProperties;

    @Test
    void testHealth() throws Exception {
        SystemHealthDTO health = new SystemHealthDTO("UP", null, null);
        Mockito.when(systemHealthService.getSystemHealth()).thenReturn(health);

        mockMvc.perform(get("/api/system/health")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.apiStatus").value("UP"));
    }
}
