package org.djezzy.pfe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.djezzy.pfe.config.AppProperties;
import org.djezzy.pfe.util.AppException;
import org.djezzy.pfe.util.PdfImageUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OcrService {
    private final WebClient webClient;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final PdfImageUtil pdfImageUtil;

    public OcrResult extractTextFromPdf(Path pdfPath) {
        List<String> pageResponses = new ArrayList<>();
        List<String> base64Pages = pdfImageUtil.toBase64PngPages(pdfPath);
        for (int pageIndex = 0; pageIndex < base64Pages.size(); pageIndex++) {
            String ocrResponse = callOcr(base64Pages.get(pageIndex), pageIndex + 1);
            pageResponses.add(ocrResponse);
        }

        String rawText = String.join("\n", pageResponses);
        try {
            String payloadJson = objectMapper.writeValueAsString(Map.of("pages", pageResponses));
            return new OcrResult(rawText, payloadJson);
        } catch (JsonProcessingException ex) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to serialize OCR payload");
        }
    }

    private String callOcr(String imageBase64, int page) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("page", page);
        payload.put("imageBase64", imageBase64);
        String responseBody = webClient.post()
                .uri(appProperties.getOcrUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        if (responseBody == null || responseBody.isBlank()) {
            throw new AppException(HttpStatus.BAD_GATEWAY, "OCR service returned an empty response");
        }
        return extractText(responseBody);
    }

    private String extractText(String responseBody) {
        try {
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            JsonNode extractedText = jsonNode.get("extractedText");
            if (extractedText != null && !extractedText.asText().isBlank()) {
                return extractedText.asText();
            }
        } catch (JsonProcessingException ignored) {
            return responseBody;
        }
        return responseBody;
    }

    public record OcrResult(String rawText, String payloadJson) {
    }
}
