package org.djezzy.pfe.service.system;

import org.djezzy.pfe.dao.evaluation.CVDAO;
import org.djezzy.pfe.dao.evaluation.CandidateEvaluationDAO;
import org.djezzy.pfe.dto.evaluation.CandidateEvaluationDTO;
import org.djezzy.pfe.dto.evaluation.N8nEvaluationPayloadDTO;
import org.djezzy.pfe.dto.job.JobOfferDTO;
import org.djezzy.pfe.dto.job.StructuredJdCallbackRequest;
import org.djezzy.pfe.model.evaluation.CV;
import org.djezzy.pfe.model.evaluation.CVProcessingStatus;
import org.djezzy.pfe.model.evaluation.CandidateEvaluation;
import org.djezzy.pfe.model.evaluation.EvaluationStatus;
import org.djezzy.pfe.service.job.JobOfferService;
import org.djezzy.pfe.util.AppException;
import org.djezzy.pfe.util.MapperUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CallbackServiceTest {

    @Mock
    private JobOfferService jobOfferService;

    @Mock
    private CandidateEvaluationDAO candidateEvaluationDAO;

    @Mock
    private CVDAO cvdao;

    @Mock
    private MapperUtil mapperUtil;

    @InjectMocks
    private CallbackService callbackService;

    @Test
    void handleStructuredJdCallback_Success() {
        Long jobId = 1L;
        StructuredJdCallbackRequest request = new StructuredJdCallbackRequest(null, null, null, null, null, null, null, null, null);
        JobOfferDTO expectedDto = new JobOfferDTO(jobId, null, null, null, null, null, null, null);

        when(jobOfferService.applyStructuredJdCallback(jobId, request)).thenReturn(expectedDto);

        JobOfferDTO result = callbackService.handleStructuredJdCallback(jobId, request);
        assertEquals(expectedDto, result);
    }

    @Test
    void handleEvaluationCallback_NullPayload() {
        AppException ex = assertThrows(AppException.class, () -> callbackService.handleEvaluationCallback(1L, null));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Evaluation callback payload is required", ex.getMessage());
    }

    @Test
    void handleEvaluationCallback_EvaluationNotFound() {
        N8nEvaluationPayloadDTO payload = new N8nEvaluationPayloadDTO("success", "time", null, null, null, null);
        when(candidateEvaluationDAO.findById(1L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> callbackService.handleEvaluationCallback(1L, payload));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void handleEvaluationCallback_CvNull() {
        N8nEvaluationPayloadDTO payload = new N8nEvaluationPayloadDTO("success", "time", null, null, null, null);
        CandidateEvaluation evaluation = new CandidateEvaluation();
        when(candidateEvaluationDAO.findById(1L)).thenReturn(Optional.of(evaluation));

        AppException ex = assertThrows(AppException.class, () -> callbackService.handleEvaluationCallback(1L, payload));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void handleEvaluationCallback_Success() {
        N8nEvaluationPayloadDTO payload = new N8nEvaluationPayloadDTO("success", "time", null, null, null, null);
        CandidateEvaluation evaluation = new CandidateEvaluation();
        CV cv = new CV();
        evaluation.setCv(cv);

        when(candidateEvaluationDAO.findById(1L)).thenReturn(Optional.of(evaluation));
        CandidateEvaluationDTO expectedDto = new CandidateEvaluationDTO(1L, null, null, null, null, null, null, null, null);
        when(mapperUtil.toCandidateEvaluationDto(any())).thenReturn(expectedDto);

        CandidateEvaluationDTO result = callbackService.handleEvaluationCallback(1L, payload);

        assertEquals(EvaluationStatus.SCORED, evaluation.getStatus());
        assertEquals(CVProcessingStatus.EVALUATED, cv.getStatus());
        verify(candidateEvaluationDAO).save(evaluation);
        verify(cvdao).save(cv);
        assertEquals(expectedDto, result);
    }
}
