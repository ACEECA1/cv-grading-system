package org.djezzy.pfe.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.djezzy.pfe.config.AppProperties;
import org.djezzy.pfe.dao.CVDAO;
import org.djezzy.pfe.dao.CandidateEvaluationDAO;
import org.djezzy.pfe.dao.JobOfferDAO;
import org.djezzy.pfe.model.CV;
import org.djezzy.pfe.model.CVProcessingStatus;
import org.djezzy.pfe.model.CandidateEvaluation;
import org.djezzy.pfe.model.EvaluationStatus;
import org.djezzy.pfe.model.JobOffer;
import org.djezzy.pfe.util.AppException;
import org.djezzy.pfe.util.MapperUtil;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncWorkflowService {
    private final JobOfferDAO jobOfferDAO;
    private final CVDAO cvdao;
    private final CandidateEvaluationDAO candidateEvaluationDAO;
    private final WebClient webClient;
    private final AppProperties appProperties;
    private final OcrService ocrService;
    private final ObjectMapper objectMapper;
    private final MapperUtil mapperUtil;

    @Async("applicationTaskExecutor")
    @Transactional
    public void triggerStructuredJdWorkflow(Long jobOfferId) {
        JobOffer jobOffer = jobOfferDAO.findById(jobOfferId).orElse(null);
        if (jobOffer == null) {
            log.warn("Job offer {} not found for structured JD workflow", jobOfferId);
            return;
        }
        Map<String, Object> payload = Map.of(
                "jobOfferId", jobOffer.getId(),
                "requestId", jobOffer.getJdRequestId(),
                "title", jobOffer.getTitle(),
                "rawText", jobOffer.getRawText()
        );
        webClient.post()
                .uri(appProperties.getN8n().getStructuredJdUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .toBodilessEntity()
                .doOnError(error -> log.error("Failed to trigger structured JD workflow for {}", jobOfferId, error))
                .subscribe();
    }

    @Async("applicationTaskExecutor")
    @Transactional
    public void processCvAndSendForEvaluation(Long cvId) {
        CV cv = cvdao.findById(cvId).orElse(null);
        if (cv == null) {
            log.warn("CV {} not found for evaluation workflow", cvId);
            return;
        }
        CandidateEvaluation evaluation = candidateEvaluationDAO.findByCvId(cvId).orElse(null);
        if (evaluation == null) {
            log.warn("Evaluation for CV {} not found", cvId);
            return;
        }

        try {
            OcrService.OcrResult ocrResult = ocrService.extractTextFromPdf(Path.of(cv.getFileUrl()));
            cv.setRawText(ocrResult.rawText());
            cv.setOcrPayloadJson(ocrResult.payloadJson());
            cv.setStatus(CVProcessingStatus.OCR_DONE);
            cvdao.save(cv);

            Object structuredJd = cv.getJobOffer().getStructuredJd() == null
                    ? null
                    : objectMapper.readTree(objectMapper.writeValueAsString(mapperUtil.toStructuredJdDto(cv.getJobOffer().getStructuredJd())));
            Map<String, Object> structuredPayload = new HashMap<>();
            structuredPayload.put("jobOfferId", cv.getJobOffer().getId());
            structuredPayload.put("structuredJd", structuredJd);
            Map<String, Object> payload = Map.of(
                    "evaluationId", evaluation.getId(),
                    "cvId", cv.getId(),
                    "candidateId", cv.getCandidate().getId(),
                    "cvText", ocrResult.rawText(),
                    "ocrPayloadJson", ocrResult.payloadJson(),
                    "job", structuredPayload
            );
            webClient.post()
                    .uri(appProperties.getN8n().getEvaluationUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .toBodilessEntity()
                    .doOnError(error -> log.error("Failed to dispatch evaluation webhook for CV {}", cvId, error))
                    .subscribe();
            cv.setStatus(CVProcessingStatus.SENT_FOR_EVALUATION);
            cvdao.save(cv);
        } catch (JsonProcessingException ex) {
            cv.setStatus(CVProcessingStatus.FAILED);
            evaluation.setStatus(EvaluationStatus.FAILED);
            cvdao.save(cv);
            candidateEvaluationDAO.save(evaluation);
            log.error("Failed to serialize workflow payload", ex);
        } catch (AppException ex) {
            cv.setStatus(CVProcessingStatus.FAILED);
            evaluation.setStatus(EvaluationStatus.FAILED);
            cvdao.save(cv);
            candidateEvaluationDAO.save(evaluation);
            log.error("CV processing failed", ex);
        }
    }
}
