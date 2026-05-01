package org.djezzy.pfe.service.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.djezzy.pfe.config.AppProperties;
import org.djezzy.pfe.model.job.StructuredJd;
import org.djezzy.pfe.util.AppException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class LlmParsingService {
    private static final Pattern THINK_TAGS_PATTERN = Pattern.compile("(?s)<think>.*?</think>");
    private static final Pattern MARKDOWN_JSON_PREFIX_PATTERN = Pattern.compile("^```(?:json)?\\s*", Pattern.CASE_INSENSITIVE);
    private static final Pattern JSON_OBJECT_PATTERN = Pattern.compile("\\{[\\s\\S]*}");
    private static final ObjectMapper LENIENT_JSON_MAPPER = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
            .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
            .build();
    private static final String REPAIR_SYSTEM_PROMPT = """
            Convert the user input into valid JSON only. Do not include markdown, prose, or explanations.
            Use exactly this schema with quoted keys:
            {
              "job_title": string,
              "company_name": string,
              "required_skills": string[],
              "preferred_skills": string[],
              "experience_range": {
                "min_years": string | null,
                "max_years": string | null
              },
              "responsibilities": string[],
              "qualifications": string[],
              "work_location": string,
              "employment_type": string
            }
            If a field is missing, return null (or [] for arrays). Output JSON only.
            """;

    private final WebClient webClient;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    public StructuredJd parseStructuredJd(String rawDescription) {
        AppProperties.Openrouter openrouter = appProperties.getOpenrouter();
        if (openrouter == null
                || isBlank(openrouter.getApiKey())
                || isBlank(openrouter.getUrl())
                || isBlank(openrouter.getModel())
                || isBlank(openrouter.getSystemPrompt())) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "OpenRouter configuration is incomplete");
        }
        String normalizedSystemPrompt = normalizeSystemPrompt(openrouter.getSystemPrompt());
        if (isBlank(normalizedSystemPrompt)) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Structured JD system prompt is empty after normalization");
        }
        String userContent;
        try {
            userContent = objectMapper.writeValueAsString(rawDescription);
        } catch (JsonProcessingException ex) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Unable to serialize job description");
        }

        String responseBody = callOpenRouter(openrouter, buildPrimaryPayload(openrouter, normalizedSystemPrompt, userContent), "PRIMARY");
        String rawContent = extractMessageContent(responseBody);
        if (rawContent == null || rawContent.isBlank()) {
            throw new AppException(HttpStatus.BAD_GATEWAY, "OpenRouter response did not contain message content");
        }
        if (openrouter.isLogPayloads()) {
            System.out.println("[OpenRouter][PRIMARY][CONTENT] " + truncate(rawContent, 4000));
        }

        try {
            return deserializeStructuredJd(cleanLlmContent(rawContent));
        } catch (AppException ex) {
            String repairResponse = callOpenRouter(openrouter, buildRepairPayload(openrouter, rawContent), "REPAIR");
            String repairedContent = extractMessageContent(repairResponse);
            if (repairedContent == null || repairedContent.isBlank()) {
                throw ex;
            }
            if (openrouter.isLogPayloads()) {
                System.out.println("[OpenRouter][REPAIR][CONTENT] " + truncate(repairedContent, 4000));
            }
            return deserializeStructuredJd(cleanLlmContent(repairedContent));
        }
    }

    private String cleanLlmContent(String content) {
        String withoutThink = THINK_TAGS_PATTERN.matcher(content).replaceAll("").trim();
        String withoutMarkdownPrefix = MARKDOWN_JSON_PREFIX_PATTERN.matcher(withoutThink).replaceFirst("").trim();
        String withoutMarkdown = withoutMarkdownPrefix.replaceAll("[`\\s]+$", "").trim();
        String balanced = extractBalancedJsonObject(withoutMarkdown);
        if (balanced != null) {
            return balanced;
        }
        Matcher matcher = JSON_OBJECT_PATTERN.matcher(withoutMarkdown);
        return matcher.find() ? matcher.group(0) : withoutMarkdown;
    }

    private StructuredJd deserializeStructuredJd(String cleanedJson) {
        JsonNode source = parseJsonNode(cleanedJson);
        if (source != null && source.isTextual()) {
            source = parseJsonNode(source.asText());
        }
        if (source == null || !source.isObject()) {
            throw new AppException(HttpStatus.BAD_GATEWAY, "Structured JD payload is not valid JSON" + compactErrorBody(cleanedJson));
        }

        ObjectNode normalized = objectMapper.createObjectNode();
        normalized.put("title", textOrNull(source, "job_title"));
        normalized.put("companyName", textOrNull(source, "company_name"));
        normalized.put("workLocation", textOrNull(source, "work_location"));
        normalized.put("employmentType", textOrNull(source, "employment_type"));
        normalized.set("experienceRange", normalizeExperienceRange(source.path("experience_range")));
        normalized.set("requiredSkills", normalizeNamedItems(source.path("required_skills"), "name"));
        normalized.set("preferredSkills", normalizeNamedItems(source.path("preferred_skills"), "name"));
        normalized.set("responsibilities", normalizeNamedItems(source.path("responsibilities"), "description"));
        normalized.set("qualifications", normalizeNamedItems(source.path("qualifications"), "description"));

        try {
            return objectMapper.readValue(objectMapper.writeValueAsString(normalized), StructuredJd.class);
        } catch (JsonProcessingException ex) {
            throw new AppException(HttpStatus.BAD_GATEWAY, "Unable to deserialize structured JD payload");
        }
    }

    private ObjectNode normalizeExperienceRange(JsonNode rangeNode) {
        ObjectNode normalized = objectMapper.createObjectNode();
        if (rangeNode == null || rangeNode.isNull() || !rangeNode.isObject()) {
            normalized.putNull("minYears");
            normalized.putNull("maxYears");
            return normalized;
        }
        normalized.put("minYears", textOrNull(rangeNode, "min_years"));
        normalized.put("maxYears", textOrNull(rangeNode, "max_years"));
        return normalized;
    }

    private ArrayNode normalizeNamedItems(JsonNode arrayNode, String fieldName) {
        ArrayNode normalized = objectMapper.createArrayNode();
        if (arrayNode == null || !arrayNode.isArray()) {
            return normalized;
        }
        for (JsonNode item : arrayNode) {
            if (item == null || item.isNull()) {
                continue;
            }
            String value = item.isTextual() ? item.asText() : item.toString();
            ObjectNode named = objectMapper.createObjectNode();
            named.put(fieldName, value);
            normalized.add(named);
        }
        return normalized;
    }

    private String textOrNull(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull()) {
            return null;
        }
        return field.isTextual() ? field.asText() : field.toString();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String compactErrorBody(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        String compact = body.replaceAll("\\s+", " ").trim();
        if (compact.length() > 280) {
            compact = compact.substring(0, 280) + "...";
        }
        return " - " + compact;
    }

    private Map<String, Object> buildPrimaryPayload(AppProperties.Openrouter openrouter, String systemPrompt, String userContent) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", openrouter.getModel());
        payload.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userContent)
        ));
        payload.put("temperature", 0.2);
        payload.put("max_tokens", 50000);
        payload.put("reasoning", Map.of("enabled", false));
        payload.put("response_format", Map.of("type", "json_object"));
        return payload;
    }

    private Map<String, Object> buildRepairPayload(AppProperties.Openrouter openrouter, String rawContent) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", openrouter.getModel());
        payload.put("messages", List.of(
                Map.of("role", "system", "content", REPAIR_SYSTEM_PROMPT),
                Map.of("role", "user", "content", rawContent)
        ));
        payload.put("temperature", 0);
        payload.put("max_tokens", 50000);
        payload.put("reasoning", Map.of("enabled", false));
        payload.put("response_format", Map.of("type", "json_object"));
        return payload;
    }

    private String callOpenRouter(AppProperties.Openrouter openrouter, Map<String, Object> payload, String phase) {
        if (openrouter.isLogPayloads()) {
            System.out.println("[OpenRouter][" + phase + "][REQUEST] " + toJsonForConsole(payload));
        }
        String responseBody = webClient.post()
                .uri(openrouter.getUrl())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openrouter.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .map(body -> {
                            int statusCode = response.statusCode().value();
                            String details = compactErrorBody(body);
                            if (statusCode == 401 || statusCode == 403) {
                                return new AppException(
                                        HttpStatus.BAD_GATEWAY,
                                        "OpenRouter authorization failed (check OPENROUTER_API_KEY)" + details
                                );
                            }
                            return new AppException(
                                    HttpStatus.BAD_GATEWAY,
                                    "OpenRouter request failed with status " + statusCode + details
                            );
                        }))
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(120));
        if (responseBody == null || responseBody.isBlank()) {
            throw new AppException(HttpStatus.BAD_GATEWAY, "OpenRouter returned an empty response");
        }
        if (openrouter.isLogPayloads()) {
            System.out.println("[OpenRouter][" + phase + "][RESPONSE] " + truncate(responseBody, 4000));
        }
        return responseBody;
    }

    private String extractMessageContent(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            return root.path("choices").path(0).path("message").path("content").asText(null);
        } catch (JsonProcessingException ex) {
            throw new AppException(HttpStatus.BAD_GATEWAY, "OpenRouter returned an invalid JSON payload");
        }
    }

    private JsonNode parseJsonNode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException ignored) {
        }
        try {
            return LENIENT_JSON_MAPPER.readTree(value);
        } catch (JsonProcessingException ignored) {
        }
        try {
            String unwrapped = objectMapper.readValue(value, String.class);
            return LENIENT_JSON_MAPPER.readTree(unwrapped);
        } catch (JsonProcessingException ignored) {
        }
        return null;
    }

    private String extractBalancedJsonObject(String content) {
        int startIndex = -1;
        int depth = 0;
        boolean inString = false;
        char stringDelimiter = 0;
        boolean escaped = false;

        for (int i = 0; i < content.length(); i++) {
            char current = content.charAt(i);
            if (startIndex < 0) {
                if (current == '{') {
                    startIndex = i;
                    depth = 1;
                }
                continue;
            }

            if (inString) {
                if (escaped) {
                    escaped = false;
                    continue;
                }
                if (current == '\\') {
                    escaped = true;
                    continue;
                }
                if (current == stringDelimiter) {
                    inString = false;
                }
                continue;
            }

            if (current == '"' || current == '\'') {
                inString = true;
                stringDelimiter = current;
                continue;
            }
            if (current == '{') {
                depth++;
                continue;
            }
            if (current == '}') {
                depth--;
                if (depth == 0) {
                    return content.substring(startIndex, i + 1);
                }
            }
        }
        return null;
    }

    private String toJsonForConsole(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            return String.valueOf(payload);
        }
    }

    private String truncate(String value, int maxLen) {
        if (value == null || value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen) + "...";
    }

    private String normalizeSystemPrompt(String prompt) {
        if (prompt == null) {
            return null;
        }
        String normalized = prompt.trim();
        if (normalized.startsWith("\"")) {
            normalized = normalized.substring(1);
        }
        if (normalized.endsWith("\"")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        normalized = normalized
                .replace("\\r\\n", "\n")
                .replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\\"", "\"");
        return normalized.trim();
    }
}
