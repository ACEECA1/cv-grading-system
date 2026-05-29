package org.djezzy.pfe.service.evaluation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.djezzy.pfe.config.AppProperties;
import org.djezzy.pfe.dao.evaluation.CVDAO;
import org.djezzy.pfe.dao.evaluation.CandidateEvaluationDAO;
import org.djezzy.pfe.model.evaluation.CV;
import org.djezzy.pfe.model.evaluation.CVProcessingStatus;
import org.djezzy.pfe.model.evaluation.CandidateEvaluation;
import org.djezzy.pfe.model.evaluation.EvaluationStatus;
import org.djezzy.pfe.model.job.JobOffer;
import org.djezzy.pfe.service.workflow.WorkflowProcessorService;
import org.djezzy.pfe.util.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsyncWorkflowServiceTest {

    @Mock
    private CVDAO cvdao;

    @Mock
    private CandidateEvaluationDAO candidateEvaluationDAO;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private AppProperties appProperties;

    @Mock
    private OcrService ocrService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private WorkflowProcessorService workflowProcessorService;

    @InjectMocks
    private AsyncWorkflowService asyncWorkflowService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void processCvAndSendForEvaluation_Success() throws Exception {
        Long cvId = 1L;
        Long evaluationId = 2L;

        CV cv = new CV();
        cv.setId(cvId);
        cv.setFileUrl("test.pdf");
        JobOffer jobOffer = new JobOffer();
        jobOffer.setTitle("Developer");
        cv.setJobOffer(jobOffer);

        CandidateEvaluation evaluation = new CandidateEvaluation();
        evaluation.setId(evaluationId);
        evaluation.setCv(cv);

        when(cvdao.findById(cvId)).thenReturn(Optional.of(cv));
        when(candidateEvaluationDAO.findById(evaluationId)).thenReturn(Optional.of(evaluation));
        when(cvdao.existsById(cvId)).thenReturn(true);
        when(candidateEvaluationDAO.existsById(evaluationId)).thenReturn(true);

        OcrService.OcrResult ocrResult = new OcrService.OcrResult("raw text", "json payload");
        when(ocrService.extractTextFromPdf(Path.of("test.pdf"))).thenReturn(ocrResult);

        ObjectMapper realMapper = new ObjectMapper();
        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(realMapper.writerWithDefaultPrettyPrinter());

        AppProperties.Automation automation = new AppProperties.Automation();
        automation.setUseN8n(true);
        when(appProperties.getAutomation()).thenReturn(automation);

        asyncWorkflowService.processCvAndSendForEvaluation(cvId, evaluationId);

        verify(ocrService).extractTextFromPdf(Path.of("test.pdf"));
        verify(workflowProcessorService).processWorkflow(any());
        verify(cvdao, times(2)).save(cv);
        assertEquals(CVProcessingStatus.SENT_FOR_EVALUATION, cv.getStatus());
    }

    @Test
    void processCvAndSendForEvaluation_CvNotFound() {
        when(cvdao.findById(1L)).thenReturn(Optional.empty());

        asyncWorkflowService.processCvAndSendForEvaluation(1L, 2L);

        verify(candidateEvaluationDAO, never()).findById(any());
    }

    @Test
    void processCvAndSendForEvaluation_EvaluationNotFound() {
        CV cv = new CV();
        when(cvdao.findById(1L)).thenReturn(Optional.of(cv));
        when(candidateEvaluationDAO.findById(2L)).thenReturn(Optional.empty());

        asyncWorkflowService.processCvAndSendForEvaluation(1L, 2L);

        verify(cvdao, never()).save(any());
    }

    @Test
    void processCvAndSendForEvaluation_Withdrawn() {
        Long cvId = 1L;
        Long evaluationId = 2L;

        CV cv = new CV();
        cv.setId(cvId);
        CandidateEvaluation evaluation = new CandidateEvaluation();
        evaluation.setId(evaluationId);
        evaluation.setCv(cv);

        when(cvdao.findById(cvId)).thenReturn(Optional.of(cv));
        when(candidateEvaluationDAO.findById(evaluationId)).thenReturn(Optional.of(evaluation));
        when(cvdao.existsById(cvId)).thenReturn(false);

        asyncWorkflowService.processCvAndSendForEvaluation(cvId, evaluationId);

        verify(ocrService, never()).extractTextFromPdf(any());
    }

    @Test
    void processCvAndSendForEvaluation_OcrFailure() {
        Long cvId = 1L;
        Long evaluationId = 2L;

        CV cv = new CV();
        cv.setId(cvId);
        cv.setFileUrl("test.pdf");
        CandidateEvaluation evaluation = new CandidateEvaluation();
        evaluation.setId(evaluationId);
        evaluation.setCv(cv);

        when(cvdao.findById(cvId)).thenReturn(Optional.of(cv));
        when(candidateEvaluationDAO.findById(evaluationId)).thenReturn(Optional.of(evaluation));
        when(cvdao.existsById(cvId)).thenReturn(true);
        when(candidateEvaluationDAO.existsById(evaluationId)).thenReturn(true);

        when(ocrService.extractTextFromPdf(Path.of("test.pdf"))).thenThrow(new AppException(null, "OCR Failed"));

        asyncWorkflowService.processCvAndSendForEvaluation(cvId, evaluationId);

        assertEquals(CVProcessingStatus.FAILED, cv.getStatus());
        assertEquals(EvaluationStatus.FAILED, evaluation.getStatus());
        verify(cvdao).save(cv);
        verify(candidateEvaluationDAO).save(evaluation);
    }
}
