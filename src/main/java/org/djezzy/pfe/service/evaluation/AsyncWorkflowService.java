package org.djezzy.pfe.service.evaluation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.djezzy.pfe.config.AppProperties;
import org.djezzy.pfe.dao.evaluation.CVDAO;
import org.djezzy.pfe.dao.evaluation.CandidateEvaluationDAO;
import org.djezzy.pfe.model.evaluation.CV;
import org.djezzy.pfe.model.evaluation.CVProcessingStatus;
import org.djezzy.pfe.model.evaluation.CandidateEvaluation;
import org.djezzy.pfe.model.evaluation.EvaluationStatus;
import org.djezzy.pfe.model.job.JobOffer;
import org.djezzy.pfe.model.job.StructuredJd;
import org.djezzy.pfe.util.AppException;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncWorkflowService {
    private final CVDAO cvdao;
    private final CandidateEvaluationDAO candidateEvaluationDAO;
    private final WebClient webClient;
    private final AppProperties appProperties;
    private final OcrService ocrService;
    private final ObjectMapper objectMapper;

    @Async("applicationTaskExecutor")
    @Transactional
    public void triggerStructuredJdWorkflow(Long jobOfferId, String requestId, String title, String rawText) {
        Map<String, Object> payload = Map.of(
                "jobOfferId", jobOfferId,
                "requestId", requestId,
                "title", title,
                "rawText", rawText
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

    @Transactional
    public void processCvAndSendForEvaluation(Long cvId, Long evaluationId) {
        CV cv = cvdao.findById(cvId).orElse(null);
        if (cv == null) {
            log.warn("CV {} not found for evaluation workflow", cvId);
            return;
        }
        CandidateEvaluation evaluation = candidateEvaluationDAO.findById(evaluationId).orElse(null);
        if (evaluation == null) {
            log.warn("Evaluation {} not found for CV {}", evaluationId, cvId);
            return;
        }
        if (evaluation.getCv() == null || !evaluation.getCv().getId().equals(cvId)) {
            log.warn("Evaluation {} does not match CV {}", evaluationId, cvId);
            return;
        }

        try {
            OcrService.OcrResult ocrResult = ocrService.extractTextFromPdf(Path.of(cv.getFileUrl()));
            cv.setRawText(ocrResult.rawText());
            cv.setOcrPayloadJson(ocrResult.payloadJson());
            cv.setStatus(CVProcessingStatus.OCR_DONE);
            cvdao.save(cv);

            String jobDescriptionJson = buildJobDescriptionJson(cv.getJobOffer());
            Map<String, Object> payload = Map.of(
                    "evaluationId", evaluation.getId(),
                    "cv_text", ocrResult.rawText(),
                    "job_description", jobDescriptionJson
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

    private String buildJobDescriptionJson(JobOffer jobOffer) throws JsonProcessingException {
        StructuredJd structuredJd = jobOffer.getStructuredJd();
        Map<String, Object> experienceRange = new LinkedHashMap<>();
        experienceRange.put("min_years", structuredJd == null || structuredJd.getExperienceRange() == null ? null : structuredJd.getExperienceRange().getMinYears());
        experienceRange.put("max_years", structuredJd == null || structuredJd.getExperienceRange() == null ? null : structuredJd.getExperienceRange().getMaxYears());

        Map<String, Object> jobDescription = new LinkedHashMap<>();
        jobDescription.put("job_title", structuredJd != null && structuredJd.getTitle() != null ? structuredJd.getTitle() : jobOffer.getTitle());
        jobDescription.put("company_name", structuredJd == null ? null : structuredJd.getCompanyName());
        jobDescription.put("required_skills", structuredJd == null ? Collections.emptyList() : structuredJd.getRequiredSkills().stream().map(skill -> skill.getName()).toList());
        jobDescription.put("preferred_skills", structuredJd == null ? Collections.emptyList() : structuredJd.getPreferredSkills().stream().map(skill -> skill.getName()).toList());
        jobDescription.put("experience_range", experienceRange);
        jobDescription.put("responsibilities", structuredJd == null ? Collections.emptyList() : structuredJd.getResponsibilities().stream().map(item -> item.getDescription()).toList());
        jobDescription.put("qualifications", structuredJd == null ? Collections.emptyList() : structuredJd.getQualifications().stream().map(item -> item.getDescription()).toList());
        jobDescription.put("work_location", structuredJd == null ? null : structuredJd.getWorkLocation());
        jobDescription.put("employment_type", structuredJd == null ? null : structuredJd.getEmploymentType());
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jobDescription);
    }
}