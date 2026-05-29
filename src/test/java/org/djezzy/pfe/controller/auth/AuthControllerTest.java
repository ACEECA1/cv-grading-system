package org.djezzy.pfe.controller.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.djezzy.pfe.dto.auth.*;
import org.djezzy.pfe.service.auth.AuthService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.djezzy.pfe.filter.JwtAuthenticationFilter;
import org.djezzy.pfe.filter.ApiKeyAuthenticationFilter;
import org.djezzy.pfe.service.auth.CustomUserDetailsService;
import org.djezzy.pfe.config.AppProperties;

@WebMvcTest(controllers = AuthController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private AppProperties appProperties;

    @Test
    void testRegisterCandidate() throws Exception {
        RegisterCandidateRequest request = new RegisterCandidateRequest("user123", "John", "Doe", "john@example.com", "ValidPass123!");
        UserDTO user = new UserDTO(1L, "user123", "John", "Doe", "john@example.com", null, false, null, null, null);
        
        Mockito.when(authService.registerCandidate(any())).thenReturn(user);

        mockMvc.perform(post("/api/auth/register/candidate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Candidate registered. Verification code sent."))
                .andExpect(jsonPath("$.data.username").value("user123"));
    }

    @Test
    void testRegisterHr() throws Exception {
        RegisterHrRequest request = new RegisterHrRequest("hr123", "Jane", "Doe", "jane@example.com", "ValidPass123!");
        UserDTO user = new UserDTO(2L, "hr1234", "HR", "One", "hr1@example.com", null, false, null, null, null);
        
        Mockito.when(authService.registerHr(any())).thenReturn(user);

        mockMvc.perform(post("/api/auth/register/hr")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("HR registered. Verification code sent."))
                .andExpect(jsonPath("$.data.username").value("hr1234"));
    }

    @Test
    void testVerifyCode() throws Exception {
        VerifyCodeRequest request = new VerifyCodeRequest("user@test.com", "123456");

        mockMvc.perform(post("/api/auth/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Account verified successfully"));
    }

    @Test
    void testLogin() throws Exception {
        LoginRequest request = new LoginRequest("user", "pass");
        AuthTokensDTO tokens = new AuthTokensDTO("acc", "ref", "bearer", 3600, null);
        Mockito.when(authService.login(any())).thenReturn(tokens);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("acc"))
                .andExpect(jsonPath("$.data.refreshToken").value("ref"));
    }
}
