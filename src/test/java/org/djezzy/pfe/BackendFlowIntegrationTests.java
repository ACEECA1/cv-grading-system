package org.djezzy.pfe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.djezzy.pfe.dao.evaluation.CVDAO;
import org.djezzy.pfe.dao.evaluation.CandidateEvaluationDAO;
import org.djezzy.pfe.dao.job.JobOfferDAO;
import org.djezzy.pfe.dao.job.StructuredJdDAO;
import org.djezzy.pfe.dao.auth.UserDAO;
import org.djezzy.pfe.dao.auth.VerificationCodeDAO;
import org.djezzy.pfe.model.evaluation.CV;
import org.djezzy.pfe.model.evaluation.CVProcessingStatus;
import org.djezzy.pfe.model.auth.Candidate;
import org.djezzy.pfe.model.evaluation.CandidateEvaluation;
import org.djezzy.pfe.model.evaluation.EvaluationStatus;
import org.djezzy.pfe.model.job.JobOffer;
import org.djezzy.pfe.model.job.JobOfferStatus;
import org.djezzy.pfe.model.job.StructuredJd;
import org.djezzy.pfe.model.auth.RhApprovalStatus;
import org.djezzy.pfe.model.auth.Role;
import org.djezzy.pfe.model.auth.User;
import org.djezzy.pfe.service.evaluation.AsyncWorkflowService;
import org.djezzy.pfe.util.EmailUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @MockBean
    private EmailUtil emailUtil;
    @MockBean
    private AsyncWorkflowService asyncWorkflowService;

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
        offer.setStatus(JobOfferStatus.DRAFT);
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
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    @Test
    void mock_ocr_endpoint_accepts_json_without_auth() throws Exception {
        mockMvc.perform(post("/api/mock/ocr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "page", 1,
                                "imageBase64", "ZmFrZS1pbWFnZS1jb250ZW50"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.extractedText").isNotEmpty());
    }

    @Test
    void full_test_job_offer_endpoint_is_restricted_and_creates_structured_jd() throws Exception {
        String candidateEmail = "test-candidate@mail.test";
        String candidateUsername = "cand_" + UUID.randomUUID().toString().substring(0, 8);
        String candidatePassword = "Password123";

        mockMvc.perform(post("/api/auth/register/candidate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", candidateUsername,
                                "firstName", "Test",
                                "lastName", "Candidate",
                                "email", candidateEmail,
                                "password", candidatePassword
                        ))))
                .andExpect(status().isOk());
        User candidate = userDAO.findByEmail(candidateEmail).orElseThrow();
        String candidateCode = verificationCodeDAO.findTopByUserAndUsedFalseOrderByCreatedAtDesc(candidate).orElseThrow().getCode();
        mockMvc.perform(post("/api/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", candidateEmail, "code", candidateCode))))
                .andExpect(status().isOk());

        MvcResult candidateLogin = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usernameOrEmail", candidateUsername,
                                "password", candidatePassword
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        String candidateToken = objectMapper.readTree(candidateLogin.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        Map<String, Object> payload = Map.of(
                "title", "Backend Engineer Test",
                "rawText", "This backend role needs Java, Spring Boot, APIs, and SQL with real production delivery.",
                "companyName", "Acme",
                "experienceRange", Map.of("minYears", "2", "maxYears", "5"),
                "workLocation", "Remote",
                "requiredSkills", List.of("Java", "Spring Boot"),
                "preferredSkills", List.of("Docker")
        );

        mockMvc.perform(post("/api/test/job-offers/full")
                        .header("Authorization", "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden());

        MvcResult adminLogin = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usernameOrEmail", "admin",
                                "password", "Admin@123456"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        String adminToken = objectMapper.readTree(adminLogin.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        mockMvc.perform(post("/api/test/job-offers/full")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.structuredJd.companyName").value("Acme"))
                .andExpect(jsonPath("$.data.structuredJd.requiredSkills[0]").value("Java"));
    }

    @Test
    void candidate_can_upload_cv_with_multipart_file() throws Exception {
        String email = "upload-candidate@mail.test";
        String username = "upload_" + UUID.randomUUID().toString().substring(0, 8);
        String password = "Password123";

        mockMvc.perform(post("/api/auth/register/candidate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "firstName", "Upload",
                                "lastName", "Candidate",
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().isOk());

        User candidate = userDAO.findByEmail(email).orElseThrow();
        String code = verificationCodeDAO.findTopByUserAndUsedFalseOrderByCreatedAtDesc(candidate).orElseThrow().getCode();
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
        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        User admin = userDAO.findByUsername("admin").orElseThrow();
        JobOffer offer = new JobOffer();
        offer.setTitle("Upload Test Offer");
        offer.setRawText("This is a valid backend offer text used to test candidate CV multipart upload flow.");
        offer.setStatus(JobOfferStatus.PUBLISHED);
        offer.setCreatedBy(admin);
        offer.setJdRequestId(UUID.randomUUID().toString());
        jobOfferDAO.save(offer);

        MockMultipartFile cvFile = new MockMultipartFile(
                "file",
                "candidate-cv.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "fake pdf content".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/candidate/job-offers/{jobOfferId}/cv", offer.getId())
                        .file(cvFile)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cvStatus").value("UPLOADED"))
                .andExpect(jsonPath("$.data.evaluationStatus").value("WAITING"));
    }

    @Test
    void multiple_candidates_can_upload_cv_for_same_structured_jd_offer() throws Exception {
        User admin = userDAO.findByUsername("admin").orElseThrow();

        JobOffer offer = new JobOffer();
        offer.setTitle("Shared Structured JD Offer");
        offer.setRawText("A published backend role for multi-candidate upload testing.");
        offer.setStatus(JobOfferStatus.PUBLISHED);
        offer.setCreatedBy(admin);
        offer.setJdRequestId(UUID.randomUUID().toString());
        jobOfferDAO.save(offer);

        StructuredJd structuredJd = new StructuredJd();
        structuredJd.setTitle("Shared Backend Role");
        structuredJd.setJobOffer(offer);
        structuredJdDAO.save(structuredJd);
        offer.setStructuredJd(structuredJd);
        jobOfferDAO.save(offer);

        String tokenOne = registerAndLoginCandidate(
                "multi-upload-1@mail.test",
                "multi_upload_1_" + UUID.randomUUID().toString().substring(0, 4),
                "Password123"
        );
        String tokenTwo = registerAndLoginCandidate(
                "multi-upload-2@mail.test",
                "multi_upload_2_" + UUID.randomUUID().toString().substring(0, 4),
                "Password123"
        );

        MockMultipartFile cvOne = new MockMultipartFile(
                "file",
                "candidate-one.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "candidate one content".getBytes(StandardCharsets.UTF_8)
        );
        MockMultipartFile cvTwo = new MockMultipartFile(
                "file",
                "candidate-two.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "candidate two content".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/candidate/job-offers/{jobOfferId}/cv", offer.getId())
                        .file(cvOne)
                        .header("Authorization", "Bearer " + tokenOne))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cvStatus").value("UPLOADED"));

        mockMvc.perform(multipart("/api/candidate/job-offers/{jobOfferId}/cv", offer.getId())
                        .file(cvTwo)
                        .header("Authorization", "Bearer " + tokenTwo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cvStatus").value("UPLOADED"));

        Long evaluationsOnSameStructuredJd = jdbcTemplate.queryForObject(
                "select count(*) from candidate_evaluations where structured_jd_id = ?",
                Long.class,
                structuredJd.getId()
        );
        assertThat(evaluationsOnSameStructuredJd).isEqualTo(2L);
    }

    @Test
    void candidate_can_retry_failed_evaluation() throws Exception {
        User admin = userDAO.findByUsername("admin").orElseThrow();
        String email = "retry-candidate@mail.test";
        String token = registerAndLoginCandidate(
                email,
                "retry_candidate_" + UUID.randomUUID().toString().substring(0, 4),
                "Password123"
        );
        Candidate candidate = (Candidate) userDAO.findByEmail(email).orElseThrow();

        JobOffer offer = new JobOffer();
        offer.setTitle("Retry Evaluation Offer");
        offer.setRawText("Published offer for retry evaluation integration test.");
        offer.setStatus(JobOfferStatus.PUBLISHED);
        offer.setCreatedBy(admin);
        offer.setJdRequestId(UUID.randomUUID().toString());
        jobOfferDAO.save(offer);

        CV cv = new CV();
        cv.setCandidate(candidate);
        cv.setJobOffer(offer);
        cv.setFileUrl("target/test-uploads/retry.pdf");
        cv.setUploadDate(Instant.now());
        cv.setStatus(CVProcessingStatus.FAILED);
        cvdao.save(cv);

        CandidateEvaluation evaluation = new CandidateEvaluation();
        evaluation.setCv(cv);
        evaluation.setStructuredJd(offer.getStructuredJd());
        evaluation.setStatus(EvaluationStatus.FAILED);
        candidateEvaluationDAO.save(evaluation);
        cv.setCandidateEvaluation(evaluation);
        cvdao.save(cv);

        mockMvc.perform(post("/api/candidate/submissions/{evaluationId}/retry", evaluation.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(evaluation.getId()))
                .andExpect(jsonPath("$.data.status").value("WAITING"));

        CandidateEvaluation updatedEvaluation = candidateEvaluationDAO.findById(evaluation.getId()).orElseThrow();
        CV updatedCv = cvdao.findById(cv.getId()).orElseThrow();
        assertThat(updatedEvaluation.getStatus()).isEqualTo(EvaluationStatus.WAITING);
        assertThat(updatedCv.getStatus()).isEqualTo(CVProcessingStatus.UPLOADED);
        verify(asyncWorkflowService, timeout(1000)).processCvAndSendForEvaluation(updatedCv.getId(), updatedEvaluation.getId());
    }

    @Test
    void retry_evaluation_rejects_non_failed_status() throws Exception {
        User admin = userDAO.findByUsername("admin").orElseThrow();
        String email = "retry-invalid-status@mail.test";
        String token = registerAndLoginCandidate(
                email,
                "retry_invalid_" + UUID.randomUUID().toString().substring(0, 4),
                "Password123"
        );
        Candidate candidate = (Candidate) userDAO.findByEmail(email).orElseThrow();

        JobOffer offer = new JobOffer();
        offer.setTitle("Retry Invalid State Offer");
        offer.setRawText("Published offer for invalid retry state integration test.");
        offer.setStatus(JobOfferStatus.PUBLISHED);
        offer.setCreatedBy(admin);
        offer.setJdRequestId(UUID.randomUUID().toString());
        jobOfferDAO.save(offer);

        CV cv = new CV();
        cv.setCandidate(candidate);
        cv.setJobOffer(offer);
        cv.setFileUrl("target/test-uploads/retry-invalid.pdf");
        cv.setUploadDate(Instant.now());
        cv.setStatus(CVProcessingStatus.UPLOADED);
        cvdao.save(cv);

        CandidateEvaluation evaluation = new CandidateEvaluation();
        evaluation.setCv(cv);
        evaluation.setStructuredJd(offer.getStructuredJd());
        evaluation.setStatus(EvaluationStatus.WAITING);
        candidateEvaluationDAO.save(evaluation);
        cv.setCandidateEvaluation(evaluation);
        cvdao.save(cv);

        mockMvc.perform(post("/api/candidate/submissions/{evaluationId}/retry", evaluation.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    void evaluation_callback_accepts_extended_payload_and_maps_skill_arrays() throws Exception {
        User admin = userDAO.findByUsername("admin").orElseThrow();

        Candidate candidate = Candidate.builder()
                .username("callback-candidate")
                .firstName("Callback")
                .lastName("Candidate")
                .email("callback-candidate@mail.test")
                .password("encoded")
                .role(Role.CANDIDATE)
                .isEnabled(true)
                .rhApprovalStatus(RhApprovalStatus.APPROVED)
                .build();
        candidate = (Candidate) userDAO.save(candidate);

        JobOffer offer = new JobOffer();
        offer.setTitle("Callback Offer");
        offer.setRawText("Callback offer content for evaluation mapping");
        offer.setStatus(JobOfferStatus.PUBLISHED);
        offer.setCreatedBy(admin);
        offer.setJdRequestId(UUID.randomUUID().toString());
        jobOfferDAO.save(offer);

        CV cv = new CV();
        cv.setCandidate(candidate);
        cv.setJobOffer(offer);
        cv.setFileUrl("target/test-uploads/callback.pdf");
        cv.setUploadDate(Instant.now());
        cv.setStatus(CVProcessingStatus.SENT_FOR_EVALUATION);
        cvdao.save(cv);

        CandidateEvaluation evaluation = new CandidateEvaluation();
        evaluation.setCv(cv);
        evaluation.setStructuredJd(offer.getStructuredJd());
        evaluation.setStatus(EvaluationStatus.WAITING);
        candidateEvaluationDAO.save(evaluation);
        cv.setCandidateEvaluation(evaluation);
        cvdao.save(cv);

        Map<String, Object> body = Map.of(
                "status", "success",
                "processing_time", "3.05s",
                "profile_data", Map.of(
                        "personal_info", Map.of(
                                "first_name", "Callback",
                                "last_name", "Candidate",
                                "email", "callback-candidate@mail.test",
                                "phone", "0555555555",
                                "location", "Algiers"
                        ),
                        "skills", List.of("Java", "Spring Boot", "PostgreSQL"),
                        "hobbies", List.of("Running", "Chess")
                ),
                "match_score", Map.of(
                        "overall_score", 86.7,
                        "recommendation", "Strong fit for interview stage",
                        "matched_skills", List.of("Java", "Spring Boot", "PostgreSQL", "Docker"),
                        "missing_skills", List.of(
                                Map.of("skill_name", "Kubernetes", "importance", "HIGH"),
                                Map.of("skill_name", "SIEM tooling", "importance", "MEDIUM")
                        )
                ),
                "technical_questions", List.of(Map.of(
                        "question", "Q1",
                        "expected_answer", "A1",
                        "difficulty", "MEDIUM",
                        "skill_area", "Java",
                        "bluff_indicator", false,
                        "follow_up_questions", List.of("Q1.1")
                )),
                "hr_questions", List.of(Map.of(
                        "question", "Q2",
                        "psychological_intent", "Assess communication",
                        "ideal_response_indicators", List.of("Structured answer"),
                        "red_flags", List.of("Vague responses"),
                        "follow_up_probes", List.of("Can you give an example?"),
                        "evaluation_criteria", "Clarity"
                ))
        );

        mockMvc.perform(put("/api/callbacks/evaluations/{evaluationId}", evaluation.getId())
                        .header("X-API-KEY", "test-callback-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SCORED"))
                .andExpect(jsonPath("$.data.overallScore").value(8.67));

        Long matchScoreId = jdbcTemplate.queryForObject(
                "select match_score_id from candidate_evaluations where id = ?",
                Long.class,
                evaluation.getId()
        );
        assertThat(matchScoreId).isNotNull();
        Integer matchedCount = jdbcTemplate.queryForObject(
                "select count(*) from matched_skills where match_score_id = ?",
                Integer.class,
                matchScoreId
        );
        Integer missingCount = jdbcTemplate.queryForObject(
                "select count(*) from missing_skills where match_score_id = ?",
                Integer.class,
                matchScoreId
        );
        assertThat(matchedCount).isEqualTo(4);
        assertThat(missingCount).isEqualTo(2);
    }

    private String registerAndLoginCandidate(String email, String username, String password) throws Exception {
        mockMvc.perform(post("/api/auth/register/candidate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "firstName", "Candidate",
                                "lastName", "Test",
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().isOk());

        User candidate = userDAO.findByEmail(email).orElseThrow();
        String code = verificationCodeDAO.findTopByUserAndUsedFalseOrderByCreatedAtDesc(candidate).orElseThrow().getCode();

        mockMvc.perform(post("/api/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "code", code
                        ))))
                .andExpect(status().isOk());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usernameOrEmail", username,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data")
                .path("accessToken")
                .asText();
    }
}
