package org.djezzy.pfe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.djezzy.pfe.dao.CVDAO;
import org.djezzy.pfe.dao.CandidateEvaluationDAO;
import org.djezzy.pfe.dao.JobOfferDAO;
import org.djezzy.pfe.dao.StructuredJdDAO;
import org.djezzy.pfe.dao.UserDAO;
import org.djezzy.pfe.dao.VerificationCodeDAO;
import org.djezzy.pfe.model.JobOffer;
import org.djezzy.pfe.model.JobOfferStatus;
import org.djezzy.pfe.model.User;
import org.djezzy.pfe.util.EmailUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BackendFlowIntegrationTests {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserDAO userDAO;
    @Autowired
    private VerificationCodeDAO verificationCodeDAO;
    @Autowired
    private JobOfferDAO jobOfferDAO;
    @Autowired
    private CVDAO cvdao;
    @Autowired
    private CandidateEvaluationDAO candidateEvaluationDAO;
    @Autowired
    private StructuredJdDAO structuredJdDAO;
    @MockBean
    private EmailUtil emailUtil;

    @BeforeEach
    void setup() {
        candidateEvaluationDAO.deleteAll();
        cvdao.deleteAll();
        structuredJdDAO.deleteAll();
        jobOfferDAO.deleteAll();
        verificationCodeDAO.deleteAll();
    }

    @Test
    void candidate_auth_verification_refresh_flow() throws Exception {
        String email = "candidate@mail.test";
        String username = "candidate_" + UUID.randomUUID().toString().substring(0, 8);
        String password = "Password123";

        mockMvc.perform(post("/api/auth/register/candidate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "firstName", "Ali",
                                "lastName", "Candidate",
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usernameOrEmail", username,
                                "password", password
                        ))))
                .andExpect(status().isForbidden());

        User user = userDAO.findByEmail(email).orElseThrow();
        String code = verificationCodeDAO.findTopByUserAndUsedFalseOrderByCreatedAtDesc(user).orElseThrow().getCode();

        mockMvc.perform(post("/api/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "code", code))))
                .andExpect(status().isOk());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usernameOrEmail", username,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
        String refreshToken = loginJson.path("data").path("refreshToken").asText();

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    @Test
    void role_protection_blocks_candidate_from_admin_routes() throws Exception {
        String email = "blocked-candidate@mail.test";
        String username = "cand_" + UUID.randomUUID().toString().substring(0, 8);
        String password = "Password123";

        mockMvc.perform(post("/api/auth/register/candidate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "firstName", "Test",
                                "lastName", "Candidate",
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().isOk());
        User user = userDAO.findByEmail(email).orElseThrow();
        String code = verificationCodeDAO.findTopByUserAndUsedFalseOrderByCreatedAtDesc(user).orElseThrow().getCode();
        mockMvc.perform(post("/api/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "code", code))))
                .andExpect(status().isOk());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usernameOrEmail", username,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        mockMvc.perform(get("/api/admin/rh/pending")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void rh_requires_admin_approval_before_login() throws Exception {
        String email = "rh@mail.test";
        String username = "rh_" + UUID.randomUUID().toString().substring(0, 8);
        String password = "Password123";

        mockMvc.perform(post("/api/auth/register/rh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "firstName", "Re",
                                "lastName", "Hr",
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().isOk());

        User user = userDAO.findByEmail(email).orElseThrow();
        String code = verificationCodeDAO.findTopByUserAndUsedFalseOrderByCreatedAtDesc(user).orElseThrow().getCode();
        mockMvc.perform(post("/api/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "code", code))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usernameOrEmail", username,
                                "password", password
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    void callback_endpoints_require_api_key() throws Exception {
        Optional<User> adminOpt = userDAO.findByUsername("admin");
        assertThat(adminOpt).isPresent();
        User admin = adminOpt.orElseThrow();

        JobOffer offer = new JobOffer();
        offer.setTitle("Backend Engineer");
        offer.setRawText("Java Spring Boot backend role");
        offer.setStatus(JobOfferStatus.STRUCTURING);
        offer.setCreatedBy(admin);
        offer.setJdRequestId(UUID.randomUUID().toString());
        jobOfferDAO.save(offer);

        String body = objectMapper.writeValueAsString(Map.of(
                "jobTitle", "Backend Engineer",
                "companyName", "Acme",
                "requiredSkills", new String[]{"Java", "Spring"},
                "preferredSkills", new String[]{"Docker"},
                "experienceRange", Map.of("minYears", "2", "maxYears", "5"),
                "responsibilities", new String[]{"Build APIs"},
                "qualifications", new String[]{"CS Degree"},
                "workLocation", "Remote",
                "employmentType", "Full-time"
        ));

        mockMvc.perform(put("/api/callbacks/job-offers/{id}/structured-jd", offer.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/callbacks/job-offers/{id}/structured-jd", offer.getId())
                        .header("X-API-KEY", "test-callback-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("STRUCTURED"));
    }
}
