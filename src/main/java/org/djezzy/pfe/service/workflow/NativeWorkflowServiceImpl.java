package org.djezzy.pfe.service.workflow;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.djezzy.pfe.config.AppProperties;
import org.djezzy.pfe.dto.evaluation.N8nEvaluationPayloadDTO;
import org.djezzy.pfe.service.system.CallbackService;
import org.djezzy.pfe.util.AppException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.automation", name = "use-n8n", havingValue = "false")
public class NativeWorkflowServiceImpl implements WorkflowProcessorService {
    private static final Pattern THINK_TAGS_PATTERN = Pattern.compile("(?s)<think>.*?</think>");
    private static final Pattern JSON_OBJECT_PATTERN = Pattern.compile("\\{[\\s\\S]*}");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile("(?:\\+?\\d{1,3}[-.\\s]?)?(?:\\(?\\d{1,4}\\)?[-.\\s]?)?\\d{1,4}[-.\\s]?\\d{1,4}[-.\\s]?\\d{1,9}");
    private static final Pattern LINKEDIN_PATTERN = Pattern.compile("(?:https?://)?(?:www\\.)?linkedin\\.com/in/[A-Za-z0-9_-]+/?", Pattern.CASE_INSENSITIVE);

    private static final String CV_PARSER_PROMPT = """
            # Role & Persona
            You are an expert CV Parser. Convert unstructured CV text into structured JSON.
            Return JSON only.
            """;

    private static final String CONSISTENCY_PROMPT = """
            You are a CV & Job Description Matching Expert.
            Return only valid JSON with relevance, status, skill gaps, mismatches, strengths, and recommendation.
            """;

    private static final String SKILLS_PROMPT = """
            You are a Skills Standardization Expert.
            Return only valid JSON using key "normalized_skills" with original_name, normalized_name, category, proficiency_level, years_experience.
            """;

    private static final String MATCHING_PROMPT = """
            You are a Senior Talent Matching Analyst.
            Return only valid JSON with root key "match_score" containing:
            overall_score, matched_skills, missing_skills, experience_alignment, education_match, recommendation, reasoning.
            """;

    private static final String TECHNICAL_PROMPT = """
            You are a Senior Technical Interview Architect.
            Return only valid JSON with root key "technical_questions".
            """;

    private static final String HR_PROMPT = """
            You are an HR Behavioral Interview Specialist.
            Return only valid JSON with root key "hr_questions".
            """;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;
    private final CallbackService callbackService;
    @Qualifier("workflowProcessorExecutor")
    private final Executor workflowProcessorExecutor;

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
                    return null;
                });
    }

    private void runNativeWorkflow(WorkflowInput input) {
        long startedAt = System.currentTimeMillis();
        log.debug("[{}][evaluationId={}] Native workflow pipeline started",
                Thread.currentThread().getName(),
                input.evaluationId());

        try {
            JsonNode profileData = timedStage("CV Parsing", input.evaluationId(), () -> requestCvParser(input.cvText()));
            ObjectNode combinedPayload = timedStage("Combine CV + Job Description", input.evaluationId(), () -> {
                ObjectNode payload = objectMapper.createObjectNode();
                payload.set("cv", profileData);
                payload.put("job_description", input.jobDescription());
                return payload;
            });

            ConcurrentHashMap<String, Object> stageResults = new ConcurrentHashMap<>();

            CompletableFuture<Void> consistencyFuture = CompletableFuture
                    .supplyAsync(() -> timedStage("Consistency Validation", input.evaluationId(), () -> requestConsistencyCheck(combinedPayload)), workflowProcessorExecutor)
                    .thenAccept(result -> {
                        stageResults.put("consistency", result);
                        log.debug("[{}][evaluationId={}] Consistency Validation result stored", Thread.currentThread().getName(), input.evaluationId());
                    });
            CompletableFuture<Void> skillsFuture = CompletableFuture
                    .supplyAsync(() -> timedStage("Skills Taxonomy", input.evaluationId(), () -> requestSkillsNormalization(combinedPayload)), workflowProcessorExecutor)
                    .thenAccept(result -> {
                        stageResults.put("skills", result);
                        log.debug("[{}][evaluationId={}] Skills Taxonomy result stored", Thread.currentThread().getName(), input.evaluationId());
                    });
            CompletableFuture<Void> contactFuture = CompletableFuture
                    .supplyAsync(() -> timedStage("Personal Info Extraction", input.evaluationId(), () -> extractContactInfo(input.cvText())), workflowProcessorExecutor)
                    .thenAccept(result -> {
                        stageResults.put("contact", result);
                        log.debug("[{}][evaluationId={}] Personal Info Extraction result stored", Thread.currentThread().getName(), input.evaluationId());
                    });

            timedStage("Parallel Validation Bundle Join", input.evaluationId(), () -> {
                CompletableFuture.allOf(consistencyFuture, skillsFuture, contactFuture).join();
                return true;
            });

            JsonNode consistencyResult = (JsonNode) stageResults.get("consistency");
            JsonNode skillsResult = (JsonNode) stageResults.get("skills");
            N8nEvaluationPayloadDTO.ContactInfoDTO contactInfo = (N8nEvaluationPayloadDTO.ContactInfoDTO) stageResults.get("contact");

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

            JsonNode matchingResult = timedStage("Matching Engine", input.evaluationId(), () -> requestMatching(matchingInput));

            CompletableFuture<Void> technicalFuture = CompletableFuture
                    .supplyAsync(() -> timedStage("Technical Questions Generation", input.evaluationId(), () -> requestTechnicalQuestions(matchingResult)), workflowProcessorExecutor)
                    .thenAccept(result -> {
                        stageResults.put("technical", result);
                        log.debug("[{}][evaluationId={}] Technical Questions result stored", Thread.currentThread().getName(), input.evaluationId());
                    });
            CompletableFuture<Void> hrFuture = CompletableFuture
                    .supplyAsync(() -> timedStage("HR Questions Generation", input.evaluationId(), () -> requestHrQuestions(matchingResult)), workflowProcessorExecutor)
                    .thenAccept(result -> {
                        stageResults.put("hr", result);
                        log.debug("[{}][evaluationId={}] HR Questions result stored", Thread.currentThread().getName(), input.evaluationId());
                    });

            timedStage("Parallel QA Bundle Join", input.evaluationId(), () -> {
                CompletableFuture.allOf(technicalFuture, hrFuture).join();
                return true;
            });

            JsonNode technicalResult = (JsonNode) stageResults.get("technical");
            JsonNode hrResult = (JsonNode) stageResults.get("hr");

            ObjectNode finalResponse = timedStage("Final Response Assembly", input.evaluationId(), () -> buildFinalResponse(
                    startedAt,
                    profileWithExtras,
                    consistencyResult,
                    matchingResult,
                    technicalResult,
                    hrResult
            ));

            N8nEvaluationPayloadDTO callbackPayload = timedStage(
                    "Callback Payload Mapping",
                    input.evaluationId(),
                    () -> objectMapper.convertValue(finalResponse, N8nEvaluationPayloadDTO.class)
            );

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
        } catch (Exception ex) {
            log.error("[{}][evaluationId={}] Native workflow pipeline aborted",
                    Thread.currentThread().getName(),
                    input.evaluationId(),
                    ex);
            throw ex;
        }
    }

    private ObjectNode buildFinalResponse(
            long startedAt,
            ObjectNode profileWithExtras,
            JsonNode consistencyResult,
            JsonNode matchingResult,
            JsonNode technicalResult,
            JsonNode hrResult
    ) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("status", "success");
        response.put("processing_time", String.format("%.2fs", (System.currentTimeMillis() - startedAt) / 1000.0));
        response.set("profile_data", profileWithExtras);

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

    private JsonNode normalizeMatchScore(JsonNode rawMatchScore) {
        if (rawMatchScore == null || rawMatchScore.isNull() || !rawMatchScore.isObject()) {
            return objectMapper.createObjectNode();
        }
        ObjectNode normalized = asObjectNode(rawMatchScore).deepCopy();
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
                        item.path("original_name").asText(null)
                );
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
        if (source == null || source.isNull()) {
            normalizedAlignment.putNull("years_required");
            normalizedAlignment.putNull("years_candidate");
            normalizedAlignment.putNull("match_percentage");
            return normalizedAlignment;
        }

        if (source.isObject()) {
            putNullableDouble(normalizedAlignment, "years_required", parseDouble(source.path("years_required")));
            putNullableDouble(normalizedAlignment, "years_candidate", parseDouble(source.path("years_candidate")));
            putNullableDouble(normalizedAlignment, "match_percentage", parseDouble(source.path("match_percentage")));
            return normalizedAlignment;
        }

        Double extracted = parseDouble(source);
        putNullableDouble(normalizedAlignment, "years_required", null);
        putNullableDouble(normalizedAlignment, "years_candidate", null);
        putNullableDouble(normalizedAlignment, "match_percentage", extracted);
        return normalizedAlignment;
    }

    private ObjectNode normalizeEducationMatch(JsonNode source) {
        ObjectNode normalizedEducation = objectMapper.createObjectNode();
        if (source == null || source.isNull()) {
            normalizedEducation.putNull("required_degree");
            normalizedEducation.putNull("candidate_degree");
            normalizedEducation.putNull("match_status");
            return normalizedEducation;
        }
        if (source.isObject()) {
            putNullableText(normalizedEducation, "required_degree", source.path("required_degree").asText(null));
            putNullableText(normalizedEducation, "candidate_degree", source.path("candidate_degree").asText(null));
            putNullableText(normalizedEducation, "match_status", source.path("match_status").asText(null));
            return normalizedEducation;
        }

        putNullableText(normalizedEducation, "required_degree", null);
        putNullableText(normalizedEducation, "candidate_degree", null);
        putNullableText(normalizedEducation, "match_status", source.asText(null));
        return normalizedEducation;
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

    private void putNullableDouble(ObjectNode node, String field, Double value) {
        if (value == null) {
            node.putNull(field);
            return;
        }
        node.put(field, value);
    }

    private void putNullableText(ObjectNode node, String field, String value) {
        if (isBlank(value)) {
            node.putNull(field);
            return;
        }
        node.put(field, value);
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
        return callOpenRouterJson("CV Parsing", CV_PARSER_PROMPT, toJson(cvText), 0.2, 30000);
    }

    private JsonNode requestConsistencyCheck(JsonNode input) {
        return callOpenRouterJson("Consistency Validation", CONSISTENCY_PROMPT, toJsonTwice(input), 0.2, 30000);
    }

    private JsonNode requestSkillsNormalization(JsonNode input) {
        return callOpenRouterJson("Skills Taxonomy", SKILLS_PROMPT, toJsonTwice(input), 0.2, 20000);
    }

    private JsonNode requestMatching(JsonNode input) {
        return callOpenRouterJson("Matching Engine", MATCHING_PROMPT, toJsonTwice(input), 0.2, 100000);
    }

    private JsonNode requestTechnicalQuestions(JsonNode input) {
        return callOpenRouterJson("Technical Questions Generation", TECHNICAL_PROMPT, toJsonTwice(input), 0.7, 100000);
    }

    private JsonNode requestHrQuestions(JsonNode input) {
        return callOpenRouterJson("HR Questions Generation", HR_PROMPT, toJsonTwice(input), 0.7, 100000);
    }

    private JsonNode callOpenRouterJson(String stageName, String systemPrompt, String userContent, double temperature, int maxTokens) {
        AppProperties.Openrouter openrouter = appProperties.getOpenrouter();
        log.debug("[{}][stage={}] OpenRouter call preparation started", Thread.currentThread().getName(), stageName);
        if (openrouter == null
                || isBlank(openrouter.getApiKey())
                || isBlank(openrouter.getUrl())
                || isBlank(openrouter.getModel())) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "OpenRouter configuration is incomplete");
        }

        OpenRouterRequest request = new OpenRouterRequest(
                openrouter.getModel(),
                List.of(
                        new OpenRouterRequestMessage("system", systemPrompt),
                        new OpenRouterRequestMessage("user", userContent)
                ),
                temperature,
                maxTokens
        );

        String content = callOpenRouterJsonContent(stageName, openrouter, request);
        JsonNode parsed = parseJsonFromLlmContent(content);
        log.debug("[{}][stage={}] OpenRouter response parsing completed", Thread.currentThread().getName(), stageName);
        return parsed;
    }

    private String callOpenRouterJsonContent(String stageName, AppProperties.Openrouter openrouter, OpenRouterRequest request) {
        long requestStart = System.currentTimeMillis();
        log.debug("[{}][stage={}] OpenRouter HTTP request started (model={}, maxTokens={})",
                Thread.currentThread().getName(),
                stageName,
                openrouter.getModel(),
                request.maxTokens());
        try {
            OpenRouterResponse response = webClient.post()
                    .uri(openrouter.getUrl())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + openrouter.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, r -> r.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .map(body -> new AppException(
                                    HttpStatus.BAD_GATEWAY,
                                    "OpenRouter request failed with status " + r.statusCode().value() + compactErrorBody(body)
                            )))
                    .bodyToMono(OpenRouterResponse.class)
                    .timeout(Duration.ofSeconds(60))
                    .retryWhen(Retry.backoff(2, Duration.ofSeconds(2)).maxBackoff(Duration.ofSeconds(5)))
                    .block();
            log.debug("[{}][stage={}] OpenRouter HTTP request completed in {} ms",
                    Thread.currentThread().getName(),
                    stageName,
                    System.currentTimeMillis() - requestStart);

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                return "{}";
            }
            String content = response.choices().get(0).message() == null ? null : response.choices().get(0).message().content();
            return isBlank(content) ? "{}" : content;
        } catch (Exception e) {
            log.error("OpenRouter API permanently failed after retries. Returning empty JSON fallback.", e);
            return "{}";
        }
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
            if ((source == null || source.isNull()) && fallbackSkillsNode != null && !fallbackSkillsNode.isMissingNode()) {
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
            @JsonProperty("max_tokens") int maxTokens
    ) {
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

