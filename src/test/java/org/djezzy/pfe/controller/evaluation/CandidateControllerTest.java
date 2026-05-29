package org.djezzy.pfe.controller.evaluation;

import org.djezzy.pfe.config.AppProperties;
import org.djezzy.pfe.dto.evaluation.CandidateEvaluationDTO;
import org.djezzy.pfe.dto.evaluation.CandidateSubmissionDTO;
import org.djezzy.pfe.dto.job.JobOfferDTO;
import org.djezzy.pfe.dto.evaluation.UploadCvResponseDTO;
import org.djezzy.pfe.filter.ApiKeyAuthenticationFilter;
import org.djezzy.pfe.filter.JwtAuthenticationFilter;
import org.djezzy.pfe.model.auth.User;
import org.djezzy.pfe.service.auth.CustomUserDetailsService;
import org.djezzy.pfe.service.evaluation.CandidateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import org.djezzy.pfe.model.auth.Role;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CandidateController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class CandidateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CandidateService candidateService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private AppProperties appProperties;

    private User mockUser;
    private UsernamePasswordAuthenticationToken principal;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("candidate");
        mockUser.setRole(Role.CANDIDATE);
        principal = new UsernamePasswordAuthenticationToken(mockUser, null, mockUser.getAuthorities());
    }

    @Test
    void testBrowseOffers() throws Exception {
        JobOfferDTO offer = new JobOfferDTO(1L, "title", "raw", org.djezzy.pfe.model.job.JobOfferStatus.PUBLISHED, "req", null, null, null);
        Page<JobOfferDTO> page = new PageImpl<>(List.of(offer));
        Mockito.when(candidateService.browseJobOffers(any(), any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/candidate/job-offers")
                .principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].title").value("title"));
    }

    @Test
    void testUploadCv() throws Exception {
        UploadCvResponseDTO response = new UploadCvResponseDTO(1L, 1L, null, null, null);
        Mockito.when(candidateService.uploadCv(eq(1L), any(), any())).thenReturn(response);

        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", "dummy".getBytes());

        mockMvc.perform(multipart("/api/candidate/job-offers/1/cv")
                .file(file)
                .principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.cvId").value(1));
    }

    @Test
    void testMySubmissions() throws Exception {
        CandidateSubmissionDTO sub = new CandidateSubmissionDTO(1L, "title", "user", null, null, null, null);
        Mockito.when(candidateService.mySubmissions(any())).thenReturn(List.of(sub));

        mockMvc.perform(get("/api/candidate/submissions")
                .principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].fileUrl").value("title"));
    }

    @Test
    void testRetryEvaluation() throws Exception {
        CandidateEvaluationDTO eval = new CandidateEvaluationDTO(1L, null, null, null, null, null, null, null, null);
        Mockito.when(candidateService.retryEvaluation(eq(1L), any())).thenReturn(eval);

        mockMvc.perform(post("/api/candidate/submissions/1/retry")
                .principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testWithdrawSubmission() throws Exception {
        mockMvc.perform(delete("/api/candidate/submissions/1")
                .principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        Mockito.verify(candidateService).withdrawSubmission(eq(1L), any());
    }
}
