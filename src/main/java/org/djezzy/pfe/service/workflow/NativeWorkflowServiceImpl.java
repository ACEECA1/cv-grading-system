package org.djezzy.pfe.service.workflow;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.djezzy.pfe.config.AppProperties;
import org.djezzy.pfe.dao.evaluation.CVDAO;
import org.djezzy.pfe.dao.evaluation.CandidateEvaluationDAO;
import org.djezzy.pfe.dto.evaluation.N8nEvaluationPayloadDTO;
import org.djezzy.pfe.model.evaluation.CV;
import org.djezzy.pfe.model.evaluation.CVProcessingStatus;
import org.djezzy.pfe.model.evaluation.CandidateEvaluation;
import org.djezzy.pfe.model.evaluation.EvaluationStatus;
import org.djezzy.pfe.service.system.CallbackService;
import org.djezzy.pfe.util.AppException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.djezzy.pfe.event.EvaluationCancelledEvent;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.automation", name = "use-n8n", havingValue = "false")
public class NativeWorkflowServiceImpl implements WorkflowProcessorService {
    private final Map<Long, Thread> activeThreads = new ConcurrentHashMap<>();

    private static final Pattern THINK_TAGS_PATTERN = Pattern.compile("(?s)<think>.*?</think>");
    private static final Pattern JSON_OBJECT_PATTERN = Pattern.compile("\\{[\\s\\S]*}");
    private static final Pattern EMAIL_PATTERN = Pattern
            .compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
    private static final Pattern PHONE_PATTERN = Pattern
            .compile("(?:\\+?\\d{1,3}[-.\\s]?)?(?:\\(?\\d{1,4}\\)?[-.\\s]?)?\\d{1,4}[-.\\s]?\\d{1,4}[-.\\s]?\\d{1,9}");
    private static final Pattern LINKEDIN_PATTERN = Pattern
            .compile("(?:https?://)?(?:www\\.)?linkedin\\.com/in/[A-Za-z0-9_-]+/?", Pattern.CASE_INSENSITIVE);
    private static final String[] TEXT_HINT_KEYS = {
            "value", "text", "label", "name", "full_name", "fullName", "location", "city", "state", "country",
            "address", "full_address", "display", "description"
    };

    private static final String CV_PARSER_PROMPT = """
            # Role & Persona
            You are an expert CV Parser with 15+ years of experience in recruitment data extraction. Your specialty is converting unstructured resume text into precise, structured candidate profiles while maintaining 100% accuracy on dates, employment history, and education.

            # Standard Operating Procedure
            Step 1: Scan the document sequentially.
            Step 2: Extract the candidate's work and project experience.
            Step 3: Extract the candidate's educational background, including degrees and institutions.
            Step 4: Format the extracted data strictly into the JSON schema provided below.

            # Output Constraints
            Return ONLY valid JSON. Do not include markdown formatting or conversational text.
            Your response must strictly match this exact schema for the experience and education arrays:
            {
            "experience": [
                {
                "title": "<The job or role title>",
                "company": "<The company, organization, or university club>",
                "duration": "<The exact dates, e.g., 'Jan 2023 - Present' or '2022'>",
                "description": "<A brief summary of their responsibilities and achievements>"
                }
            ],
            "education": [
                {
                "degree": "<The degree or qualification, e.g., 'Bachelor of Science in Computer Science'>",
                "institution": "<The university, college, or school name>",
                "start_date": "<The start date, if available, or null>",
                "end_date": "<The graduation or end date, e.g., '2024' or 'Present'>",
                "honors": "<Any honors, distinctions, or GPA, if available, or null>"
                }
            ]
            }
            """;

    private static final String CONSISTENCY_PROMPT = """
            You are a CV & Job Description Matching Expert. Your job is to analyze compatibility between a candidate CV and a job description. You must detect skill gaps, experience mismatches, missing requirements, and relevance score.

            # Output Rules
            - Return ONLY valid JSON.
            - No markdown, no explanations, no extra text.
            - Do not include any fields outside the specified schema.

            # Required JSON Schema
            {
            "relevance_score": number (0-100),
            "match_status": string (e.g., "Highly Recommended", "Partial Match", "Not Recommended"),
            "skill_gaps": ["string"],
            "experience_mismatch": boolean,
            "missing_requirements": ["string"],
            "strengths": ["string"],
            "recommendation": string
            }

            # Classification Rules
            - relevance_score: overall match percentage based on skills, experience, education, location.
            - match_status: descriptive summary of the match quality.
            - skill_gaps: list of required or preferred skills missing from CV.
            - experience_mismatch: true if candidate's years of experience are below minimum required.
            - missing_requirements: any non-skill requirements not met (e.g., missing degree, location, language).
            - strengths: positive points of the candidate relative to the job.
            - recommendation: brief explanation of whether to proceed and why.

            # Important
            - Be objective and data-driven.
            - Use only the information provided in the CV and job description.
            - If a field is not applicable, use an empty array or null as appropriate.

            Now analyze the following input.        
            """;

    private static final String SKILLS_PROMPT = """
            You are a Skills Standardization Expert. Normalize skill names and categorize them. Return ONLY a valid JSON object, no markdown, no extra text. The JSON must have the following structure:
            {
            "normalized_skills": [
                {
                "original_name": "<string>",
                "normalized_name": "<string>",
                "category": "<'frontend' | 'backend' | 'database' | 'devops' | 'cloud' | 'mobile' | 'testing' | 'data_science' | 'machine_learning' | 'security' | 'design' | 'project_management' | 'soft_skills' | 'other'>",
                "proficiency_level": "<string>",
                "years_experience": <number>
                }
            ]
            }

            Examples: k8s -> Kubernetes (devops), React.js -> React (frontend), Postgres -> PostgreSQL (database).
            """;

    private static final String MATCHING_PROMPT = """
            You are a Senior Talent Matching Analyst. Your job is to compare a candidate's CV to the job requirements and calculate a precise match score.

            # Output Rules
            - Return ONLY valid JSON.
            - No markdown, no explanations, no extra text.
            - The JSON must have exactly one root key: "match_score".
            - Do not include any fields outside the specified schema.

            # Required JSON Schema
            {
            "match_score": {
                "overall_match_score": <number strictly between 0.0 and 10.0, e.g., 8.5>,
                "matched_skills": ["<string>"],
                "missing_skills": [
                {
                    "skill_name": "<string>",
                    "importance": "<'Low' | 'Medium' | 'High'>"
                }
                ],
                "experience_alignment": {
                "years_required": <integer: exact years required from JD, use 0 if none specified>,
                "years_candidate": <integer: exact years calculated from CV, use 0 if none>,
                "match_score": <number 0.0 to 10.0>
                },
                "education_match": {
                "required_degree": "<string: exact degree required, e.g. 'Bachelor', or 'Not specified'>",
                "candidate_degree": "<string: candidate's highest degree, e.g. 'Master', or 'None'>",
                "match_level": "<'MATCH' | 'MISMATCH' | 'EXCEEDS'>",
                "reasoning": "<string: explanation>"
                },
                "recommendation": "<string>",
                "reasoning": "<string>"
            }
            }

            # Scoring Rules
            - The overall_match_score MUST be a decimal number from 0.0 to 10.0. Never exceed 10 or go below 0.
            - Score 8.0-10.0 -> recommend to hire.
            - Score 5.0-7.9 -> recommend to interview.
            - Score 0.0-4.9 -> recommend to reject.
            - Be objective and data-driven. Use only the provided CV and JD data.

            Now analyze the following input.
            """;

    private static final String TECHNICAL_PROMPT = """
            You are a Senior Technical Interview Architect. Your task is to generate 5 to 7 technical interview questions based on the candidate's CV and the job description provided.

            # Output Rules
            - Return ONLY valid JSON.
            - No markdown, no explanations, no extra text.
            - The JSON must have exactly one root key: "technical_questions".
            - Do not include any fields outside the specified schema.

            # Required JSON Schema
            {
            "technical_questions": [
                {
                "question": "<The technical question>",
                "expected_answer": "<The expected correct answer>",
                "difficulty": "<'Beginner' | 'Intermediate' | 'Advanced' | 'Expert'>",
                "skill_area": "<The specific skill being tested>",
                "bluff_indicator": <boolean: true if this question is designed to catch candidates exaggerating their skills>,
                "follow_up_questions": ["<follow up 1>", "<follow up 2>"]
                }
            ]
            }

            # Distribution Rules
            - Provide a balanced mix: 2 Beginner/Intermediate questions, 2-3 Advanced questions, and 1-2 Expert questions.

            # Bluff Indicator Logic
            - Set bluff_indicator to true for questions specifically designed to test the true depth of claimed expertise (e.g., advanced framework internals, performance edge cases, or common misconceptions).

            Now generate the technical questions.
            """;

    private static final String HR_PROMPT = """
            You are an HR Behavioral Interview Specialist. Your task is to generate exactly 4 behavioral interview questions based on the candidate's CV and the job description provided.

            # Output Rules
            - Return ONLY valid JSON.
            - No markdown, no explanations, no extra text.
            - The JSON must have exactly one root key: "hr_questions".
            - Do not include any fields outside the specified schema.

            # Required JSON Schema
            {
            "hr_questions": [
                {
                "question": "<The behavioral/HR question>",
                "psychological_intent": "<What this question is actually trying to reveal about the candidate>",
                "evaluation_criteria": "<How to grade the candidate's answer>",
                "ideal_response_indicators": ["<indicator 1>", "<indicator 2>"],
                "red_flags": ["<warning sign 1>", "<warning sign 2>"],
                "follow_up_probes": ["<probe 1>", "<probe 2>"]
                }
            ]
            }

            # Focus Areas
            Ensure the questions cover a mix of the following areas:
            - Cultural fit
            - Motivation
            - Teamwork
            - Conflict resolution
            - Career goals

            Now generate the behavioral questions.
            """;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;
    private final CallbackService callbackService;
    private final CVDAO cvdao;
    private final CandidateEvaluationDAO candidateEvaluationDAO;
    private final TransactionTemplate transactionTemplate;
    @Qualifier("workflowProcessorExecutor")
    private final Executor workflowProcessorExecutor;
    private final WorkflowSseService workflowSseService;

    @Override
    public void processWorkflow(Map<String, Object> payload) {
        WorkflowInput input = WorkflowInput.from(payload);
        log.debug("[{}][evaluationId={}] Native workflow dispatch requested. Payload keys={}",
                Thread.currentThread().getName(),
                input.evaluationId(),
                payload.keySet());
        CompletableFuture.runAsync(() -> runNativeWorkflow(input), workflowProcessorExecutor)
                .exceptionally(error -> {
                    log.error("[{}][evaluationId={}] Native workflow failed in async executor",
                            Thread.currentThread().getName(),
                            input.evaluationId(),
                            error);
                    workflowSseService.fail(input.evaluationId(), "Workflow failed: " + error.getMessage());
                    return null;
                });
    }

    private void runNativeWorkflow(WorkflowInput input) {
        activeThreads.put(input.evaluationId(), Thread.currentThread());
        long startedAt = System.currentTimeMillis();
        log.debug("[{}][evaluationId={}] Native workflow pipeline started",
                Thread.currentThread().getName(),
                input.evaluationId());

        try {
            workflowSseService.sendProgress(input.evaluationId(), 10, "Extracting text from CV...");
            JsonNode profileData = timedStage("CV Parsing", input.evaluationId(),
                    () -> requestCvParser(input.cvText()));
            workflowSseService.sendProgress(input.evaluationId(), 25, "Analyzing candidate profile...");
            ObjectNode combinedPayload = timedStage("Combine CV + Job Description", input.evaluationId(), () -> {
                ObjectNode payload = objectMapper.createObjectNode();
                payload.set("cv", profileData);
                payload.put("job_description", input.jobDescription());
                return payload;
            });

            ConcurrentHashMap<String, Object> stageResults = new ConcurrentHashMap<>();

            CompletableFuture<Void> consistencyFuture = CompletableFuture
                    .supplyAsync(() -> timedStage("Consistency Validation", input.evaluationId(),
                            () -> requestConsistencyCheck(combinedPayload)), workflowProcessorExecutor)
                    .thenAccept(result -> {
                        stageResults.put("consistency", result);
                        log.debug("[{}][evaluationId={}] Consistency Validation result stored",
                                Thread.currentThread().getName(), input.evaluationId());
                    });
            CompletableFuture<Void> skillsFuture = CompletableFuture
                    .supplyAsync(() -> timedStage("Skills Taxonomy", input.evaluationId(),
                            () -> requestSkillsNormalization(combinedPayload)), workflowProcessorExecutor)
                    .thenAccept(result -> {
                        stageResults.put("skills", result);
                        log.debug("[{}][evaluationId={}] Skills Taxonomy result stored",
                                Thread.currentThread().getName(), input.evaluationId());
                    });
            CompletableFuture<Void> contactFuture = CompletableFuture
                    .supplyAsync(() -> timedStage("Personal Info Extraction", input.evaluationId(),
                            () -> extractContactInfo(input.cvText())), workflowProcessorExecutor)
                    .thenAccept(result -> {
                        stageResults.put("contact", result);
                        log.debug("[{}][evaluationId={}] Personal Info Extraction result stored",
                                Thread.currentThread().getName(), input.evaluationId());
                    });

            timedStage("Parallel Validation Bundle Join", input.evaluationId(), () -> {
                CompletableFuture.allOf(consistencyFuture, skillsFuture, contactFuture).join();
                return true;
            });
            workflowSseService.sendProgress(input.evaluationId(), 50, "Validating consistency & skills...");

            JsonNode consistencyResult = (JsonNode) stageResults.get("consistency");
            JsonNode skillsResult = (JsonNode) stageResults.get("skills");
            N8nEvaluationPayloadDTO.ContactInfoDTO contactInfo = (N8nEvaluationPayloadDTO.ContactInfoDTO) stageResults
                    .get("contact");

            ObjectNode profileWithExtras = timedStage("Profile Enrichment", input.evaluationId(), () -> {
                ObjectNode enriched = asObjectNode(profileData).deepCopy();
                if (contactInfo != null) {
                    enriched.set("contact_info", objectMapper.valueToTree(contactInfo));
                }
                JsonNode normalized = skillsResult != null ? skillsResult.path("normalized_skills") : null;
                if (normalized != null && normalized.isArray()) {
                    enriched.set("normalized_skills", normalized);
                }
                ArrayList<String> flatSkills = extractFlatSkillsList(skillsResult, enriched.path("skills"));
                enriched.set("skills", objectMapper.valueToTree(flatSkills));
                return enriched;
            });

            JsonNode normalizedSkills = skillsResult != null ? skillsResult.path("normalized_skills") : null;
            ObjectNode matchingInput = timedStage("Prepare Matching Input", input.evaluationId(), () -> {
                ObjectNode matchingCv = profileWithExtras.deepCopy();
                if (normalizedSkills != null && normalizedSkills.isArray()) {
                    matchingCv.set("skills", normalizedSkills);
                }
                ObjectNode payload = objectMapper.createObjectNode();
                payload.set("cv", matchingCv);
                payload.put("job_description", input.jobDescription());
                return payload;
            });

            JsonNode matchingResult = timedStage("Matching Engine", input.evaluationId(),
                    () -> requestMatching(matchingInput));
            workflowSseService.sendProgress(input.evaluationId(), 75, "Generating match score...");

            if (matchingResult != null && matchingResult.isObject()) {
                JsonNode matchScoreNode = matchingResult.has("match_score") ? matchingResult.get("match_score") : matchingResult;
                if (matchScoreNode != null && matchScoreNode.isObject()) {
                    JsonNode experienceAlignment = matchScoreNode.get("experience_alignment");
                    if (experienceAlignment == null || experienceAlignment.isNull() || experienceAlignment.isMissingNode()) {
                        experienceAlignment = matchScoreNode.get("experienceAlignment");
                    }
                    if (experienceAlignment != null && experienceAlignment.isObject()) {
                        double yearsCandidate = 0.0;
                        if (experienceAlignment.has("years_candidate") && !experienceAlignment.get("years_candidate").isNull()) {
                            yearsCandidate = experienceAlignment.get("years_candidate").asDouble(0.0);
                        } else if (experienceAlignment.has("yearsCandidate") && !experienceAlignment.get("yearsCandidate").isNull()) {
                            yearsCandidate = experienceAlignment.get("yearsCandidate").asDouble(0.0);
                        }
                    
                    double minRequired = 0.0;
                    double maxRequired = 0.0;
                    if (input.jobDescription() != null && !input.jobDescription().isBlank()) {
                        try {
                            JsonNode jdNode = objectMapper.readTree(input.jobDescription());
                            if (jdNode.has("experience_range")) {
                                JsonNode expRange = jdNode.get("experience_range");
                                if (expRange.has("min_years") && !expRange.get("min_years").isNull()) {
                                    minRequired = expRange.get("min_years").asDouble(0.0);
                                }
                                if (expRange.has("max_years") && !expRange.get("max_years").isNull()) {
                                    maxRequired = expRange.get("max_years").asDouble(0.0);
                                }
                            }
                        } catch (Exception e) {
                            log.warn("Failed to parse JD for experience calculation", e);
                        }
                    }

                    double matchScore = 0.0;
                    if (minRequired <= 0 && maxRequired <= 0) {
                        matchScore = 10.0;
                    } else if (maxRequired > minRequired && minRequired > 0) {
                        if (yearsCandidate >= maxRequired) {
                            matchScore = 10.0;
                        } else if (yearsCandidate <= minRequired) {
                            matchScore = (yearsCandidate / minRequired) * 7.0;
                        } else {
                            matchScore = 7.0 + ((yearsCandidate - minRequired) / (maxRequired - minRequired)) * 3.0;
                        }
                    } else if (minRequired > 0) {
                        if (yearsCandidate >= minRequired) {
                            matchScore = 10.0;
                        } else {
                            matchScore = (yearsCandidate / minRequired) * 10.0;
                        }
                    } else {
                        matchScore = 10.0;
                    }
                    
                    matchScore = Math.max(0.0, Math.min(10.0, matchScore));
                    matchScore = Math.round(matchScore * 10.0) / 10.0;
                    
                    ObjectNode yrsObj = objectMapper.createObjectNode();
                    yrsObj.put("min", minRequired);
                    yrsObj.put("max", maxRequired);
                    ((ObjectNode) experienceAlignment).set("years_required", yrsObj);
                    ((ObjectNode) experienceAlignment).remove("yearsRequired");
                    ((ObjectNode) experienceAlignment).put("match_score", matchScore);
                }
                }
            }

            CompletableFuture<Void> technicalFuture = CompletableFuture
                    .supplyAsync(() -> timedStage("Technical Questions Generation", input.evaluationId(),
                            () -> requestTechnicalQuestions(matchingResult)), workflowProcessorExecutor)
                    .thenAccept(result -> {
                        stageResults.put("technical", result);
                        log.debug("[{}][evaluationId={}] Technical Questions result stored",
                                Thread.currentThread().getName(), input.evaluationId());
                    });
            CompletableFuture<Void> hrFuture = CompletableFuture
                    .supplyAsync(() -> timedStage("HR Questions Generation", input.evaluationId(),
                            () -> requestHrQuestions(matchingResult)), workflowProcessorExecutor)
                    .thenAccept(result -> {
                        stageResults.put("hr", result);
                        log.debug("[{}][evaluationId={}] HR Questions result stored", Thread.currentThread().getName(),
                                input.evaluationId());
                    });

            timedStage("Parallel QA Bundle Join", input.evaluationId(), () -> {
                CompletableFuture.allOf(technicalFuture, hrFuture).join();
                return true;
            });
            workflowSseService.sendProgress(input.evaluationId(), 90, "Generating interview questions...");

            JsonNode technicalResult = (JsonNode) stageResults.get("technical");
            JsonNode hrResult = (JsonNode) stageResults.get("hr");

            ObjectNode finalResponse = timedStage("Final Response Assembly", input.evaluationId(),
                    () -> buildFinalResponse(
                            startedAt,
                            profileWithExtras,
                            consistencyResult,
                            matchingResult,
                            technicalResult,
                            hrResult));

            N8nEvaluationPayloadDTO callbackPayload = timedStage(
                    "Callback Payload Mapping",
                    input.evaluationId(),
                    () -> objectMapper.convertValue(finalResponse, N8nEvaluationPayloadDTO.class));

            try {
                timedStage("Evaluation Callback Dispatch", input.evaluationId(), () -> {
                    callbackService.handleEvaluationCallback(input.evaluationId(), callbackPayload);
                    return true;
                });
            } catch (AppException ex) {
                if (ex.getStatus() == HttpStatus.NOT_FOUND) {
                    log.info("[{}][evaluationId={}] Evaluation no longer exists (likely withdrawn). Callback skipped.",
                            Thread.currentThread().getName(),
                            input.evaluationId());
                    return;
                }
                log.error("[{}][evaluationId={}] Callback dispatch failed",
                        Thread.currentThread().getName(),
                        input.evaluationId(),
                        ex);
                throw ex;
            }

            log.debug("[{}][evaluationId={}] Native workflow pipeline completed in {} ms",
                    Thread.currentThread().getName(),
                    input.evaluationId(),
                    System.currentTimeMillis() - startedAt);
            
            workflowSseService.complete(input.evaluationId());
        } catch (Exception ex) {
            log.error("[{}][evaluationId={}] Native workflow pipeline aborted",
                    Thread.currentThread().getName(),
                    input.evaluationId(),
                    ex);
            workflowSseService.fail(input.evaluationId(), ex.getMessage());
            markEvaluationFailed(input.evaluationId());
            throw ex;
        } finally {
            activeThreads.remove(input.evaluationId());
        }
    }

    @EventListener
    public void onEvaluationCancelled(EvaluationCancelledEvent event) {
        Thread thread = activeThreads.get(event.evaluationId());
        if (thread != null) {
            log.info("Interrupting active workflow for evaluation {}", event.evaluationId());
            thread.interrupt();
        }
    }

    private void markEvaluationFailed(Long evaluationId) {
        transactionTemplate.execute(status -> {
            CandidateEvaluation evaluation = candidateEvaluationDAO.findById(evaluationId).orElse(null);
            if (evaluation == null) {
                log.info("[{}][evaluationId={}] Cannot mark FAILED: evaluation no longer exists",
                        Thread.currentThread().getName(),
                        evaluationId);
                return null;
            }

            evaluation.setStatus(EvaluationStatus.FAILED);
            candidateEvaluationDAO.save(evaluation);

            CV cv = evaluation.getCv();
            if (cv != null) {
                cv.setStatus(CVProcessingStatus.FAILED);
                cvdao.save(cv);
            }
            return null;
        });
    }

    private ObjectNode buildFinalResponse(
            long startedAt,
            ObjectNode profileWithExtras,
            JsonNode consistencyResult,
            JsonNode matchingResult,
            JsonNode technicalResult,
            JsonNode hrResult) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("status", "success");
        response.put("processing_time", String.format("%.2fs", (System.currentTimeMillis() - startedAt) / 1000.0));
        response.set("profile_data", normalizeProfileData(profileWithExtras));

        JsonNode validationReport = consistencyResult != null && consistencyResult.has("validation_report")
                ? consistencyResult.get("validation_report")
                : consistencyResult;
        response.set("validation_report", validationReport == null ? objectMapper.nullNode() : validationReport);
        response.putNull("structured_jd");

        JsonNode rawMatchScore = matchingResult != null && matchingResult.has("match_score")
                ? matchingResult.get("match_score")
                : matchingResult;
        response.set("match_score", normalizeMatchScore(rawMatchScore));
        response.set("technical_questions", extractArray(technicalResult, "technical_questions"));
        response.set("hr_questions", extractArray(hrResult, "hr_questions"));
        return response;
    }

    private ObjectNode normalizeProfileData(ObjectNode source) {
        ObjectNode normalized = source == null ? objectMapper.createObjectNode() : source.deepCopy();

        JsonNode personalInfoNode = normalized.path("personal_info");
        if (personalInfoNode.isObject()) {
            ObjectNode personalInfo = (ObjectNode) personalInfoNode;
            coerceTextField(personalInfo, "first_name");
            coerceTextField(personalInfo, "last_name");
            coerceTextField(personalInfo, "email");
            coerceTextField(personalInfo, "phone");
            coerceTextField(personalInfo, "location");
        }

        JsonNode contactInfoNode = normalized.path("contact_info");
        if (contactInfoNode.isObject()) {
            ObjectNode contactInfo = (ObjectNode) contactInfoNode;
            coerceTextField(contactInfo, "email");
            coerceTextField(contactInfo, "phone");
            coerceTextField(contactInfo, "linkedin");
        }

        JsonNode normalizedSkillsNode = normalized.path("normalized_skills");
        if (normalizedSkillsNode.isArray()) {
            for (JsonNode skillNode : normalizedSkillsNode) {
                if (!skillNode.isObject()) {
                    continue;
                }
                ObjectNode skill = (ObjectNode) skillNode;
                coerceTextField(skill, "original_name");
                coerceTextField(skill, "normalized_name");
                coerceTextField(skill, "category");
                coerceTextField(skill, "proficiency_level");
                coerceDoubleField(skill, "years_experience");
            }
        }

        return normalized;
    }

    private JsonNode normalizeMatchScore(JsonNode rawMatchScore) {
        if (rawMatchScore == null || rawMatchScore.isNull() || !rawMatchScore.isObject()) {
            return objectMapper.createObjectNode();
        }
        ObjectNode normalized = asObjectNode(rawMatchScore).deepCopy();
        clampOverallScore(normalized);
        ArrayNode matchedSkills = objectMapper.createArrayNode();
        JsonNode sourceSkills = normalized.path("matched_skills");
        if (sourceSkills.isArray()) {
            for (JsonNode item : sourceSkills) {
                if (item == null || item.isNull()) {
                    continue;
                }
                if (item.isTextual()) {
                    matchedSkills.add(item.asText());
                } else if (item.isObject()) {
                    String skillName = item.path("skill_name").asText(null);
                    if (skillName == null || skillName.isBlank()) {
                        skillName = item.path("normalized_name").asText(null);
                    }
                    if (skillName != null && !skillName.isBlank()) {
                        matchedSkills.add(skillName);
                    }
                }
            }
        }
        normalized.set("matched_skills", matchedSkills);
        normalized.set("missing_skills", normalizeMissingSkills(normalized.path("missing_skills")));
        normalized.set("experience_alignment", normalizeExperienceAlignment(normalized.path("experience_alignment")));
        normalized.set("education_match", normalizeEducationMatch(normalized.path("education_match")));

        if (normalized.path("recommendation").isObject() || normalized.path("recommendation").isArray()) {
            normalized.put("recommendation", normalized.path("recommendation").toString());
        }
        if (normalized.path("reasoning").isObject() || normalized.path("reasoning").isArray()) {
            normalized.put("reasoning", normalized.path("reasoning").toString());
        }
        return normalized;
    }

    private void clampOverallScore(ObjectNode node) {
        Double parsedScore = parseDouble(node.path("overall_score"));
        if (parsedScore == null) {
            parsedScore = parseDouble(node.path("overall_match_score"));
        }
        double finalScore = parsedScore == null ? 0.0 : parsedScore;
        if (finalScore > 10.0) {
            finalScore = finalScore / 10.0;
        } else if (finalScore <= 1.0 && finalScore > 0.0) {
            finalScore = finalScore * 10.0;
        }
        finalScore = Math.min(Math.max(finalScore, 0.0), 10.0);
        node.put("overall_score", Math.round(finalScore * 100.0) / 100.0);
        node.remove("overall_match_score");
    }

    private ArrayNode normalizeMissingSkills(JsonNode source) {
        ArrayNode normalizedMissing = objectMapper.createArrayNode();
        if (source == null || source.isNull() || !source.isArray()) {
            return normalizedMissing;
        }
        for (JsonNode item : source) {
            if (item == null || item.isNull()) {
                continue;
            }
            ObjectNode row = objectMapper.createObjectNode();
            if (item.isTextual()) {
                row.put("skill_name", item.asText());
                row.put("importance", "Medium");
                normalizedMissing.add(row);
                continue;
            }
            if (item.isObject()) {
                String skillName = firstNonBlank(
                        item.path("skill_name").asText(null),
                        item.path("normalized_name").asText(null),
                        item.path("original_name").asText(null));
                if (isBlank(skillName)) {
                    continue;
                }
                row.put("skill_name", skillName);
                row.put("importance", firstNonBlank(item.path("importance").asText(null), "Medium"));
                normalizedMissing.add(row);
            }
        }
        return normalizedMissing;
    }

    private ObjectNode normalizeExperienceAlignment(JsonNode source) {
        ObjectNode normalizedAlignment = objectMapper.createObjectNode();
        JsonNode yrsReqNode = source != null && source.isObject() ? source.path("years_required") : null;
        if (yrsReqNode == null || yrsReqNode.isMissingNode()) {
            yrsReqNode = source != null && source.isObject() ? source.path("yearsRequired") : null;
        }
        ObjectNode yearsRequiredObj = objectMapper.createObjectNode();
        if (yrsReqNode != null && yrsReqNode.isObject()) {
             yearsRequiredObj.put("min", firstNonNull(parseInteger(yrsReqNode.path("min")), 0));
             yearsRequiredObj.put("max", firstNonNull(parseInteger(yrsReqNode.path("max")), 0));
        } else {
             int yr = yrsReqNode != null ? firstNonNull(parseInteger(yrsReqNode), 0) : 0;
             yearsRequiredObj.put("min", yr);
             yearsRequiredObj.put("max", yr);
        }
        
        Integer yearsCandidate = 0;
        Double matchScore = 0.0;

        if (source != null && !source.isNull()) {
            if (source.isObject()) {
                yearsCandidate = firstNonNull(parseInteger(source.path("years_candidate")), 0);
                JsonNode matchNode = source.has("match_score") ? source.path("match_score") : source.path("match_percentage");
                Double parsedMatch = parseDouble(matchNode);
                matchScore = parsedMatch == null ? 0.0 : normalizeSubScore(parsedMatch);
            } else {
                Double parsedMatch = parseDouble(source);
                matchScore = parsedMatch == null ? 0.0 : normalizeSubScore(parsedMatch);
            }
        }

        normalizedAlignment.set("years_required", yearsRequiredObj);
        normalizedAlignment.put("years_candidate", yearsCandidate);
        normalizedAlignment.put("match_score", matchScore);
        normalizedAlignment.remove("match_percentage");
        return normalizedAlignment;
    }

    private ObjectNode normalizeEducationMatch(JsonNode source) {
        ObjectNode normalizedEducation = objectMapper.createObjectNode();
        if (source == null || source.isNull()) {
            normalizedEducation.put("required_degree", "Not specified");
            normalizedEducation.put("candidate_degree", "Not specified");
            normalizedEducation.put("match_level", "MISMATCH");
            normalizedEducation.put("reasoning", "Not specified");
            return normalizedEducation;
        }
        if (source.isObject()) {
            normalizedEducation.put(
                    "required_degree",
                    firstNonBlank(source.path("required_degree").asText(null), "Not specified"));
            normalizedEducation.put(
                    "candidate_degree",
                    firstNonBlank(source.path("candidate_degree").asText(null), "Not specified"));
            normalizedEducation.put(
                    "match_level",
                    firstNonBlank(source.path("match_level").asText(null), "MISMATCH"));
            normalizedEducation.put(
                    "reasoning",
                    firstNonBlank(source.path("reasoning").asText(null), "Not specified"));
            return normalizedEducation;
        }

        normalizedEducation.put("required_degree", "Not specified");
        normalizedEducation.put("candidate_degree", "Not specified");
        normalizedEducation.put("match_level", "MISMATCH");
        normalizedEducation.put("reasoning", firstNonBlank(source.asText(null), "Not specified"));
        return normalizedEducation;
    }

    private Integer parseInteger(JsonNode node) {
        Double parsed = parseDouble(node);
        if (parsed == null) {
            return null;
        }
        long rounded = Math.round(parsed);
        if (rounded < 0) {
            return 0;
        }
        if (rounded > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) rounded;
    }

    private double normalizeSubScore(double value) {
        double normalized = value;
        if (normalized > 10.0) {
            normalized = normalized / 10.0;
        } else if (normalized <= 1.0 && normalized > 0.0) {
            normalized = normalized * 10.0;
        }
        normalized = Math.max(0.0, Math.min(normalized, 10.0));
        return Math.round(normalized * 100.0) / 100.0;
    }

    private <T> T firstNonNull(T value, T fallback) {
        return value == null ? fallback : value;
    }

    private Double parseDouble(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.asDouble();
        }
        String text = node.asText(null);
        if (isBlank(text)) {
            return null;
        }
        Matcher matcher = Pattern.compile("-?\\d+(?:\\.\\d+)?").matcher(text);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Double.parseDouble(matcher.group());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void coerceTextField(ObjectNode node, String field) {
        JsonNode raw = node.get(field);
        String text = coerceText(raw);
        if (isBlank(text) || isPlaceholderText(text)) {
            node.putNull(field);
            return;
        }
        node.put(field, text);
    }

    private void coerceDoubleField(ObjectNode node, String field) {
        Double value = parseDouble(node.get(field));
        if (value == null) {
            node.putNull(field);
            return;
        }
        node.put(field, value);
    }

    private String coerceText(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isNumber() || node.isBoolean()) {
            return node.asText();
        }
        if (node.isArray()) {
            ArrayNode array = (ArrayNode) node;
            ArrayList<String> parts = new ArrayList<>();
            for (JsonNode item : array) {
                String value = coerceText(item);
                if (!isBlank(value)) {
                    parts.add(value);
                }
            }
            return parts.isEmpty() ? null : String.join(", ", parts);
        }
        if (node.isObject()) {
            for (String key : TEXT_HINT_KEYS) {
                if (node.has(key)) {
                    String value = coerceText(node.get(key));
                    if (!isBlank(value)) {
                        return value;
                    }
                }
            }
            return null;
        }
        return null;
    }

    private boolean isPlaceholderText(String value) {
        if (isBlank(value)) {
            return true;
        }
        String normalized = value.trim().toLowerCase();
        return normalized.equals("n/a")
                || normalized.equals("na")
                || normalized.equals("none")
                || normalized.equals("null")
                || normalized.equals("not available")
                || normalized.equals("unknown");
    }

    private String firstNonBlank(String... candidates) {
        if (candidates == null) {
            return null;
        }
        for (String candidate : candidates) {
            if (!isBlank(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private ArrayNode extractArray(JsonNode root, String key) {
        if (root == null || root.isNull()) {
            return objectMapper.createArrayNode();
        }
        JsonNode array = root.has(key) ? root.get(key) : root;
        if (array == null || !array.isArray()) {
            return objectMapper.createArrayNode();
        }
        return (ArrayNode) array;
    }

    private JsonNode requestCvParser(String cvText) {
        return callOpenRouterJson("CV Parsing", CV_PARSER_PROMPT, toJson(cvText), 0.2, 4096, true);
    }

    private JsonNode requestConsistencyCheck(JsonNode input) {
        return callOpenRouterJson("Consistency Validation", CONSISTENCY_PROMPT, toJsonTwice(input), 0.2, 4096, false);
    }

    private JsonNode requestSkillsNormalization(JsonNode input) {
        return callOpenRouterJson("Skills Taxonomy", SKILLS_PROMPT, toJsonTwice(input), 0.2, 4096, false);
    }

    private JsonNode requestMatching(JsonNode input) {
        return callOpenRouterJson("Matching Engine", MATCHING_PROMPT, toJsonTwice(input), 0.2, 8192, false);
    }

    private JsonNode requestTechnicalQuestions(JsonNode input) {
        return callOpenRouterJson("Technical Questions Generation", TECHNICAL_PROMPT, toJsonTwice(input), 0.7, 4096,
                true);
    }

    private JsonNode requestHrQuestions(JsonNode input) {
        return callOpenRouterJson("HR Questions Generation", HR_PROMPT, toJsonTwice(input), 0.7, 4096, true);
    }

    private JsonNode callOpenRouterJson(String stageName, String systemPrompt, String userContent, double temperature,
            int maxTokens, boolean enforceJsonMode) {
        AppProperties.Openrouter openrouter = appProperties.getOpenrouter();
        log.debug("[{}][stage={}] OpenRouter call preparation started", Thread.currentThread().getName(), stageName);
        if (openrouter == null
                || isBlank(openrouter.getApiKey())
                || isBlank(openrouter.getUrl())
                || isBlank(openrouter.getModel())) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "OpenRouter configuration is incomplete");
        }

        boolean supportsJsonMode = !openrouter.getModel().contains("stepfun");
        Map<String, String> responseFormat = (enforceJsonMode && supportsJsonMode) ? Map.of("type", "json_object") : null;
        OpenRouterRequest request = new OpenRouterRequest(
                openrouter.getModel(),
                List.of(
                        new OpenRouterRequestMessage("system", systemPrompt),
                        new OpenRouterRequestMessage("user", userContent)),
                temperature,
                maxTokens,
                responseFormat);

        String content = callOpenRouterJsonContent(stageName, openrouter, request);
        JsonNode parsed = parseJsonFromLlmContent(content);
        log.debug("[{}][stage={}] OpenRouter response parsing completed", Thread.currentThread().getName(), stageName);
        return parsed;
    }

    private String callOpenRouterJsonContent(String stageName, AppProperties.Openrouter openrouter,
            OpenRouterRequest request) {
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            long requestStart = System.currentTimeMillis();
            log.debug("[{}][stage={}] OpenRouter HTTP request started (model={}, maxTokens={}, attempt={})",
                    Thread.currentThread().getName(),
                    stageName,
                    openrouter.getModel(),
                    request.maxTokens(),
                    attempt);
            try {
                OpenRouterResponse response = webClient.post()
                        .uri(openrouter.getUrl())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + openrouter.getApiKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .exchangeToMono(clientResponse -> {
                            if (clientResponse.statusCode().is2xxSuccessful()) {
                                return clientResponse.bodyToMono(OpenRouterResponse.class);
                            }
                            return clientResponse.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .flatMap(body -> Mono.error(new RuntimeException(
                                            "OpenRouter request failed with status " + clientResponse.statusCode().value()
                                                    + compactErrorBody(body))));
                        })
                        .timeout(Duration.ofSeconds(360))
                        .retryWhen(Retry.backoff(2, Duration.ofSeconds(2)).maxBackoff(Duration.ofSeconds(5)))
                        .block();
                log.debug("[{}][stage={}] OpenRouter HTTP request completed in {} ms",
                        Thread.currentThread().getName(),
                        stageName,
                        System.currentTimeMillis() - requestStart);

                if (response == null || response.choices() == null || response.choices().isEmpty()) {
                    throw new RuntimeException("OpenRouter returned an empty response payload");
                }
                String content = response.choices().get(0).message() == null ? null
                        : response.choices().get(0).message().content();
                if (isBlank(content)) {
                    throw new RuntimeException("OpenRouter returned empty completion content");
                }
                return content;
            } catch (RuntimeException e) {
                if (attempt == maxRetries) {
                    log.error("OpenRouter API call failed for stage {}", stageName, e);
                    throw e;
                }
                log.warn("OpenRouter call failed (attempt {}), retrying...", attempt, e);
                try {
                    Thread.sleep(2000L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            } catch (Exception e) {
                log.error("OpenRouter API call failed for stage {}", stageName, e);
                throw new RuntimeException("OpenRouter call failed for stage " + stageName, e);
            }
        }
        return null;
    }

    private JsonNode parseJsonFromLlmContent(String content) {
        String cleaned = THINK_TAGS_PATTERN.matcher(content).replaceAll("").trim();
        String extracted = extractBalancedJson(cleaned);
        if (isBlank(extracted)) {
            Matcher matcher = JSON_OBJECT_PATTERN.matcher(cleaned);
            if (matcher.find()) {
                extracted = matcher.group(0);
            } else {
                extracted = cleaned;
            }
        }
        try {
            JsonNode parsed = objectMapper.readTree(extracted);
            if (parsed != null && parsed.isTextual()) {
                return objectMapper.readTree(parsed.asText());
            }
            return parsed;
        } catch (Exception ex) {
            log.error("[{}] Failed to parse LLM content as JSON", Thread.currentThread().getName(), ex);
            throw new AppException(HttpStatus.BAD_GATEWAY, "OpenRouter response content is not valid JSON");
        }
    }

    private String extractBalancedJson(String content) {
        if (isBlank(content)) {
            return null;
        }
        int start = -1;
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        char quote = 0;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (start < 0) {
                if (c == '{') {
                    start = i;
                    depth = 1;
                }
                continue;
            }
            if (inString) {
                if (escaped) {
                    escaped = false;
                    continue;
                }
                if (c == '\\') {
                    escaped = true;
                    continue;
                }
                if (c == quote) {
                    inString = false;
                }
                continue;
            }
            if (c == '"' || c == '\'') {
                inString = true;
                quote = c;
                continue;
            }
            if (c == '{') {
                depth++;
                continue;
            }
            if (c == '}') {
                depth--;
                if (depth == 0) {
                    return content.substring(start, i + 1);
                }
            }
        }
        return null;
    }

    private N8nEvaluationPayloadDTO.ContactInfoDTO extractContactInfo(String cvText) {
        if (isBlank(cvText)) {
            return new N8nEvaluationPayloadDTO.ContactInfoDTO(null, null, null);
        }
        String email = firstMatch(EMAIL_PATTERN, cvText);
        String phone = firstValidPhone(cvText);
        String linkedin = firstMatch(LINKEDIN_PATTERN, cvText);
        return new N8nEvaluationPayloadDTO.ContactInfoDTO(email, phone, linkedin);
    }

    private String firstValidPhone(String text) {
        Matcher matcher = PHONE_PATTERN.matcher(text);
        while (matcher.find()) {
            String value = matcher.group();
            String digits = value.replaceAll("\\D", "");
            if (digits.length() >= 10 && digits.length() <= 15) {
                return value;
            }
        }
        return null;
    }

    private String firstMatch(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group() : null;
    }

    private ArrayList<String> extractFlatSkillsList(JsonNode taxonomyResult, JsonNode fallbackSkillsNode) {
        try {
            ArrayList<String> flatSkills = new ArrayList<>();
            JsonNode source = taxonomyResult;

            if (source != null && source.isTextual()) {
                source = objectMapper.readTree(source.asText());
            }
            if (source != null && source.isObject() && source.has("normalized_skills")) {
                source = source.get("normalized_skills");
            }
            if ((source == null || source.isNull()) && fallbackSkillsNode != null
                    && !fallbackSkillsNode.isMissingNode()) {
                source = fallbackSkillsNode;
            }
            if (source != null && source.isTextual()) {
                source = objectMapper.readTree(source.asText());
            }

            if (source != null && source.isArray()) {
                for (JsonNode item : source) {
                    if (item == null || item.isNull()) {
                        continue;
                    }
                    if (item.isTextual()) {
                        String value = item.asText(null);
                        if (!isBlank(value)) {
                            flatSkills.add(value);
                        }
                        continue;
                    }
                    if (item.isObject()) {
                        String normalizedName = item.path("normalized_name").asText(null);
                        if (isBlank(normalizedName)) {
                            normalizedName = item.path("original_name").asText(null);
                        }
                        if (isBlank(normalizedName)) {
                            normalizedName = item.path("skill_name").asText(null);
                        }
                        if (!isBlank(normalizedName)) {
                            flatSkills.add(normalizedName);
                        }
                    }
                }
            }

            return flatSkills;
        } catch (Exception e) {
            log.warn("Failed to extract flat skills list, defaulting to empty list", e);
            return new ArrayList<>();
        }
    }

    private ObjectNode asObjectNode(JsonNode node) {
        if (node == null || !node.isObject()) {
            return objectMapper.createObjectNode();
        }
        return (ObjectNode) node;
    }

    private String toJsonTwice(Object value) {
        return toJson(toJson(value));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Failed to serialize workflow payload");
        }
    }

    private String compactErrorBody(String body) {
        if (isBlank(body)) {
            return "";
        }
        String compact = body.replaceAll("\\s+", " ").trim();
        return compact.length() <= 280 ? " - " + compact : " - " + compact.substring(0, 280) + "...";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private <T> T timedStage(String stageName, Long evaluationId, Supplier<T> supplier) {
        long stageStart = System.currentTimeMillis();
        log.debug("[{}][evaluationId={}] {} - started",
                Thread.currentThread().getName(),
                evaluationId,
                stageName);
        T result = supplier.get();
        log.debug("[{}][evaluationId={}] {} - completed in {} ms",
                Thread.currentThread().getName(),
                evaluationId,
                stageName,
                System.currentTimeMillis() - stageStart);
        return result;
    }

    private record WorkflowInput(Long evaluationId, String cvText, String jobDescription) {
        static WorkflowInput from(Map<String, Object> payload) {
            if (payload == null) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Workflow payload is required");
            }
            Long evaluationId = asLong(payload.get("evaluationId"));
            String cvText = asString(payload.get("cv_text"));
            String jobDescription = asString(payload.get("job_description"));
            if (evaluationId == null) {
                throw new AppException(HttpStatus.BAD_REQUEST, "evaluationId is required");
            }
            if (cvText == null || cvText.isBlank()) {
                throw new AppException(HttpStatus.BAD_REQUEST, "cv_text is required");
            }
            if (jobDescription == null || jobDescription.isBlank()) {
                throw new AppException(HttpStatus.BAD_REQUEST, "job_description is required");
            }
            return new WorkflowInput(evaluationId, cvText, jobDescription);
        }

        private static Long asLong(Object value) {
            if (value == null) {
                return null;
            }
            if (value instanceof Number number) {
                return number.longValue();
            }
            try {
                return Long.parseLong(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        private static String asString(Object value) {
            return value == null ? null : String.valueOf(value);
        }
    }

    private record OpenRouterRequest(
            String model,
            List<OpenRouterRequestMessage> messages,
            double temperature,
            @JsonProperty("max_tokens") int maxTokens,
            @JsonProperty("response_format") Map<String, String> responseFormat) {
    }

    private record OpenRouterRequestMessage(String role, String content) {
    }

    private record OpenRouterResponse(List<OpenRouterResponseChoice> choices) {
    }

    private record OpenRouterResponseChoice(OpenRouterResponseMessage message) {
    }

    private record OpenRouterResponseMessage(String content) {
    }
}
