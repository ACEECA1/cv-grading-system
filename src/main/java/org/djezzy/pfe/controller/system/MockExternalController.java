package org.djezzy.pfe.controller.system;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.djezzy.pfe.dto.system.MockN8nEvaluationRequest;
import org.djezzy.pfe.dto.system.MockOcrRequestDTO;
import org.djezzy.pfe.dto.system.MockOcrResponseDTO;
import org.djezzy.pfe.service.system.MockExternalService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/mock")
@RequiredArgsConstructor
public class MockExternalController {
    private static final String MOCK_OCR_TEXT = "Karim Laceb\\nLocation: Blida, Algeria\\n\\nPROFILE:\\nInformation Systems and Software Engineering (ISIL) L3 student with hands-on experience in backend development and system architecture. Passionate about building robust applications.\\n\\nEXPERIENCE:\\nSoftware Development Intern - AGCE (Sidi Abdellah)\\n- Developed secure, scalable backend services using Java and Spring Boot.\\n- Designed and implemented RESTful APIs and managed PostgreSQL databases.\\n- Integrated custom networking and security solutions in C/C++.\\n\\nSKILLS:\\nLanguages: Java, Python, C++, Node.js\\nFrameworks: Spring Boot, Express, NestJS, LangChain\\nTools: Git, PostgreSQL, Docker";

    private final MockExternalService mockExternalService;

    @PostMapping(value = "/ocr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MockOcrResponseDTO> mockOcrMultipart(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(new MockOcrResponseDTO(MOCK_OCR_TEXT));
    }

    @PostMapping(value = "/ocr", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MockOcrResponseDTO> mockOcrJson(@Valid @RequestBody MockOcrRequestDTO request) {
        return ResponseEntity.ok(new MockOcrResponseDTO(MOCK_OCR_TEXT));
    }

    @PostMapping(value = "/n8n/evaluate", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> mockN8nEvaluation(
            @Valid @RequestBody MockN8nEvaluationRequest request,
            HttpServletRequest httpRequest
    ) {
        String callbackUrl = UriComponentsBuilder.fromHttpUrl(httpRequest.getRequestURL().toString())
                .replacePath("/api/callbacks/evaluations/" + request.evaluationId())
                .replaceQuery(null)
                .toUriString();
        mockExternalService.simulateEvaluationCallback(request, callbackUrl);
        return ResponseEntity.accepted().build();
    }
}


