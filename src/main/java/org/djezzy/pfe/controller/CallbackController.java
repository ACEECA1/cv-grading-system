package org.djezzy.pfe.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.djezzy.pfe.dto.ApiResponse;
import org.djezzy.pfe.dto.CandidateEvaluationDTO;
import org.djezzy.pfe.dto.JobOfferDTO;
import org.djezzy.pfe.dto.N8nEvaluationPayloadDTO;
import org.djezzy.pfe.dto.StructuredJdCallbackRequest;
import org.djezzy.pfe.service.CallbackService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/callbacks")
@RequiredArgsConstructor
public class CallbackController {
    private final CallbackService callbackService;

    @PutMapping("/job-offers/{jobOfferId}/structured-jd")
    public ResponseEntity<ApiResponse<JobOfferDTO>> structuredJdCallback(
            @PathVariable Long jobOfferId,
            @Valid @RequestBody StructuredJdCallbackRequest request
    ) {
        JobOfferDTO response = callbackService.handleStructuredJdCallback(jobOfferId, request);
        return ResponseEntity.ok(ApiResponse.ok("Structured JD callback applied", response));
    }

    @PutMapping("/evaluations/{evaluationId}")
    public ResponseEntity<ApiResponse<CandidateEvaluationDTO>> evaluationCallback(
            @PathVariable Long evaluationId,
            @Valid @RequestBody N8nEvaluationPayloadDTO request
    ) {
        CandidateEvaluationDTO response = callbackService.handleEvaluationCallback(evaluationId, request);
        return ResponseEntity.ok(ApiResponse.ok("Evaluation callback applied", response));
    }
}
