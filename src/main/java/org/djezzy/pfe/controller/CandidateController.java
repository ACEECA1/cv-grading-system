package org.djezzy.pfe.controller;

import lombok.RequiredArgsConstructor;
import org.djezzy.pfe.dto.ApiResponse;
import org.djezzy.pfe.dto.CandidateSubmissionDTO;
import org.djezzy.pfe.dto.JobOfferDTO;
import org.djezzy.pfe.dto.UploadCvResponseDTO;
import org.djezzy.pfe.model.User;
import org.djezzy.pfe.service.CandidateService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/candidate")
@RequiredArgsConstructor
public class CandidateController {
    private final CandidateService candidateService;

    @GetMapping("/job-offers")
    public ResponseEntity<ApiResponse<List<JobOfferDTO>>> browseOffers() {
        return ResponseEntity.ok(ApiResponse.ok("Job offers", candidateService.browseJobOffers()));
    }

    @PostMapping("/job-offers/{jobOfferId}/cv")
    public ResponseEntity<ApiResponse<UploadCvResponseDTO>> uploadCv(
            @PathVariable Long jobOfferId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        UploadCvResponseDTO response = candidateService.uploadCv(jobOfferId, file, user);
        return ResponseEntity.ok(ApiResponse.ok("CV uploaded and queued for evaluation", response));
    }

    @GetMapping("/submissions")
    public ResponseEntity<ApiResponse<List<CandidateSubmissionDTO>>> mySubmissions(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.ok("Candidate submissions", candidateService.mySubmissions(user)));
    }
}
