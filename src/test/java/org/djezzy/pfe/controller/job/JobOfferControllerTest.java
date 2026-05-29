package org.djezzy.pfe.controller.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.djezzy.pfe.config.AppProperties;
import org.djezzy.pfe.dto.job.ApplicantSummaryDTO;
import org.djezzy.pfe.dto.job.CreateJobOfferRequest;
import org.djezzy.pfe.dto.job.JobOfferDTO;
import org.djezzy.pfe.dto.job.JobOfferDetailDTO;
import org.djezzy.pfe.dto.job.ToggleJobOfferStatusRequest;
import org.djezzy.pfe.dto.job.UpdateJobOfferRequest;
import org.djezzy.pfe.filter.ApiKeyAuthenticationFilter;
import org.djezzy.pfe.filter.JwtAuthenticationFilter;
import org.djezzy.pfe.model.auth.User;
import org.djezzy.pfe.service.auth.CustomUserDetailsService;
import org.djezzy.pfe.service.job.JobOfferService;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import org.djezzy.pfe.model.auth.Role;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = JobOfferController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class JobOfferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JobOfferService jobOfferService;

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
        mockUser.setUsername("hr");
        mockUser.setRole(Role.HR);
        principal = new UsernamePasswordAuthenticationToken(mockUser, null, mockUser.getAuthorities());
    }

    @Test
    void testPublicOffers() throws Exception {
        JobOfferDTO offer = new JobOfferDTO(1L, "title", "raw", org.djezzy.pfe.model.job.JobOfferStatus.PUBLISHED, "req", null, null, null);
        Mockito.when(jobOfferService.listPublicJobOffers()).thenReturn(List.of(offer));

        mockMvc.perform(get("/api/job-offers/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").value("title"));
    }

    @Test
    void testCreateOffer() throws Exception {
        CreateJobOfferRequest request = new CreateJobOfferRequest("title", "this is a very long description that satisfies the constraint");
        JobOfferDTO offer = new JobOfferDTO(1L, "title", "raw", org.djezzy.pfe.model.job.JobOfferStatus.PUBLISHED, "req", null, null, null);
        
        Mockito.when(jobOfferService.createJobOffer(any(CreateJobOfferRequest.class), any(User.class))).thenReturn(offer);

        mockMvc.perform(post("/api/hr/job-offers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("title"));
    }

    @Test
    void testAllOffers() throws Exception {
        JobOfferDTO offer = new JobOfferDTO(1L, "title", "raw", org.djezzy.pfe.model.job.JobOfferStatus.PUBLISHED, "req", null, null, null);
        Page<JobOfferDTO> page = new PageImpl<>(List.of(offer));
        Mockito.when(jobOfferService.getJobOffers(any(), any(), any(), anyInt(), anyInt(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/hr/job-offers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].title").value("title"));
    }

    @Test
    void testGetApplicants() throws Exception {
        ApplicantSummaryDTO applicant = new ApplicantSummaryDTO(1L, "user", 80.0, "status", null);
        Mockito.when(jobOfferService.getJobApplicants(eq(1L), anyString(), anyString())).thenReturn(List.of(applicant));

        mockMvc.perform(get("/api/hr/job-offers/1/applicants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].candidateName").value("user"));
    }

    @Test
    void testUpdateOffer() throws Exception {
        UpdateJobOfferRequest request = new UpdateJobOfferRequest("new title", new UpdateJobOfferRequest.StructuredJdUpdateRequest(null, null, null, null, null, null));
        JobOfferDetailDTO offer = new JobOfferDetailDTO(1L, "new title", org.djezzy.pfe.model.job.JobOfferStatus.PUBLISHED, null, null);
        
        Mockito.when(jobOfferService.updateJobOffer(eq(1L), any(UpdateJobOfferRequest.class))).thenReturn(offer);

        mockMvc.perform(put("/api/hr/job-offers/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("new title"));
    }

    @Test
    void testToggleStatus() throws Exception {
        ToggleJobOfferStatusRequest request = new ToggleJobOfferStatusRequest(org.djezzy.pfe.model.job.JobOfferStatus.DRAFT);
        JobOfferDetailDTO offer = new JobOfferDetailDTO(1L, "title", org.djezzy.pfe.model.job.JobOfferStatus.DRAFT, null, null);
        
        Mockito.when(jobOfferService.toggleJobStatus(eq(1L), eq(org.djezzy.pfe.model.job.JobOfferStatus.DRAFT))).thenReturn(offer);

        mockMvc.perform(patch("/api/hr/job-offers/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }
}
