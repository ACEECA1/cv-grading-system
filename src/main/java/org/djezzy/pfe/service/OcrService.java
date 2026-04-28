package org.djezzy.pfe.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.djezzy.pfe.config.AppProperties;
import org.djezzy.pfe.util.AppException;
import org.djezzy.pfe.util.PdfImageUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OcrService {
    private static final int MIN_DIRECT_TEXT_LENGTH = 120;
    private static final int MAX_IMAGE_BASE64_LENGTH = 180_000;

    private final WebClient webClient;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final PdfImageUtil pdfImageUtil;

    public OcrResult extractTextFromPdf(Path pdfPath) {
        String directText = extractDirectPdfText(pdfPath);
        if (isSubstantialText(directText)) {
            try {
                String payloadJson = objectMapper.writeValueAsString(Map.of(
                        "mode", "direct_pdf_text",
                        "text", directText
                ));
                return new OcrResult(directText, payloadJson);
            } catch (JsonProcessingException ex) {
                throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to serialize OCR payload");
            }
        }

        List<String> base64Pages = pdfImageUtil.toBase64PngPages(pdfPath);
        if (base64Pages.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "PDF has no pages to process");
        }

        List<String> pageTexts = new ArrayList<>();
        List<Map<String, Object>> pagePayload = new ArrayList<>();
        for (int pageIndex = 0; pageIndex < base64Pages.size(); pageIndex++) {
            int pageNumber = pageIndex + 1;
            String imageBase64 = enforceImageSizeLimit(base64Pages.get(pageIndex), pageNumber);
            String pageText = callOcr(imageBase64);
            pageTexts.add(pageText);
            pagePayload.add(Map.of(
                    "page", pageNumber,
                    "text", pageText
            ));
        }

        String rawText = String.join("\n", pageTexts).trim();
        try {
            String payloadJson = objectMapper.writeValueAsString(Map.of(
                    "mode", "nvidia_ocr",
                    "pages", pagePayload
            ));
            return new OcrResult(rawText, payloadJson);
        } catch (JsonProcessingException ex) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to serialize OCR payload");
        }
    }

    private String extractDirectPdfText(Path pdfPath) {
        try (PDDocument document = PDDocument.load(pdfPath.toFile())) {
            PDFTextStripper textStripper = new PDFTextStripper();
            String text = textStripper.getText(document);
            return text == null ? "" : text.trim();
        } catch (IOException ex) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Unable to parse uploaded PDF");
        }
    }

    private boolean isSubstantialText(String text) {
        if (text == null) {
            return false;
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        return normalized.length() >= MIN_DIRECT_TEXT_LENGTH;
    }

    private String callOcr(String imageBase64) {
        String apiKey = appProperties.getOcr().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "OCR API key is not configured");
        }

        Map<String, Object> input = new HashMap<>();
        input.put("type", "image_url");
        input.put("url", "data:image/png;base64," + imageBase64);

        Map<String, Object> payload = new HashMap<>();
        payload.put("input", List.of(input));

        String responseBody = webClient.post()
                .uri(appProperties.getOcr().getUrl())
                .header("Authorization", "Bearer " + apiKey)
                .accept(MediaType.APPLICATION_JSON)
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
            List<String> chunks = new ArrayList<>();
            collectTextChunks(jsonNode, chunks);
            String extractedText = String.join("\n", chunks).trim();
            if (!extractedText.isBlank()) {
                return extractedText;
            }
        } catch (JsonProcessingException ignored) {
            return responseBody;
        }
        return responseBody;
    }

    private void collectTextChunks(JsonNode node, List<String> chunks) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            JsonNode extractedText = node.get("extractedText");
            if (extractedText != null && extractedText.isTextual() && !extractedText.asText().isBlank()) {
                chunks.add(extractedText.asText());
            }
            JsonNode text = node.get("text");
            if (text != null && text.isTextual() && !text.asText().isBlank()) {
                chunks.add(text.asText());
            }
            JsonNode content = node.get("content");
            if (content != null && content.isTextual() && !content.asText().isBlank()) {
                chunks.add(content.asText());
            }
            node.elements().forEachRemaining(child -> collectTextChunks(child, chunks));
            return;
        }
        if (node.isArray()) {
            node.forEach(child -> collectTextChunks(child, chunks));
        }
    }

    private String enforceImageSizeLimit(String imageBase64, int pageNumber) {
        if (imageBase64.length() <= MAX_IMAGE_BASE64_LENGTH) {
            return imageBase64;
        }
        log.warn("OCR page {} image exceeds {} chars, attempting to downscale", pageNumber, MAX_IMAGE_BASE64_LENGTH);
        BufferedImage image = decodeImage(imageBase64);
        double scale = 0.85d;
        BufferedImage current = image;
        while (current.getWidth() > 240 && current.getHeight() > 240) {
            String resizedBase64 = encodeImageToBase64Png(current);
            if (resizedBase64.length() <= MAX_IMAGE_BASE64_LENGTH) {
                return resizedBase64;
            }
            int nextWidth = Math.max(240, (int) Math.round(current.getWidth() * scale));
            int nextHeight = Math.max(240, (int) Math.round(current.getHeight() * scale));
            current = resizeImage(current, nextWidth, nextHeight);
        }
        throw new AppException(HttpStatus.BAD_REQUEST, "OCR image payload is too large to process");
    }

    private BufferedImage decodeImage(String imageBase64) {
        try {
            byte[] bytes = Base64.getDecoder().decode(imageBase64);
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Unable to decode OCR image payload");
            }
            return image;
        } catch (IllegalArgumentException | IOException ex) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Unable to decode OCR image payload");
        }
    }

    private BufferedImage resizeImage(BufferedImage source, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resized.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(source, 0, 0, width, height, null);
        graphics.dispose();
        return resized;
    }

    private String encodeImageToBase64Png(BufferedImage image) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (IOException ex) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to encode OCR image payload");
        }
    }

    public record OcrResult(String rawText, String payloadJson) {
    }
}
