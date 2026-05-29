package org.djezzy.pfe.service.evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.djezzy.pfe.config.AppProperties;
import org.djezzy.pfe.util.AppException;
import org.djezzy.pfe.util.PdfImageUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class OcrServiceTest {

    @Mock
    private WebClient webClient;

    @Mock
    private AppProperties appProperties;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private PdfImageUtil pdfImageUtil;

    @InjectMocks
    private OcrService ocrService;

    @Test
    void extractTextFromPdf_InvalidPdf_ThrowsException() {
        AppException ex = assertThrows(AppException.class, () -> 
            ocrService.extractTextFromPdf(Path.of("non_existent_file.pdf"))
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Unable to parse uploaded PDF", ex.getMessage());
    }
}
