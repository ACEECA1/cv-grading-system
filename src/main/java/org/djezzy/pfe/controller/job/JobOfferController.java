package org.djezzy.pfe.controller.job;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.djezzy.pfe.dto.system.ApiResponse;
import org.djezzy.pfe.dto.job.ApplicantSummaryDTO;
import org.djezzy.pfe.dto.job.CreateJobOfferRequest;
import org.djezzy.pfe.dto.job.JobOfferDetailDTO;
import org.djezzy.pfe.dto.job.JobOfferDTO;
import org.djezzy.pfe.dto.job.ToggleJobOfferStatusRequest;
import org.djezzy.pfe.dto.job.UpdateJobOfferRequest;
import org.djezzy.pfe.model.auth.User;
import org.djezzy.pfe.service.job.JobOfferService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class JobOfferController {
    private final JobOfferService jobOfferService;

    @GetMapping("/api/job-offers/public")
    public ResponseEntity<ApiResponse<List<JobOfferDTO>>> publicOffers() {
        return ResponseEntity.ok(ApiResponse.ok("Public job offers", jobOfferService.listPublicJobOffers()));
    }

    @GetMapping("/api/job-offers/public/{jobOfferId}")
    public ResponseEntity<ApiResponse<JobOfferDetailDTO>> publicOffer(@PathVariable Long jobOfferId) {
        return ResponseEntity.ok(ApiResponse.ok("Public job offer details", jobOfferService.getPublicJobOffer(jobOfferId)));
    }

    @GetMapping({"/api/hr/job-offers", "/api/rh/job-offers"})
    public ResponseEntity<ApiResponse<Page<JobOfferDTO>>> allOffers(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Boolean isPublished,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "newest") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Job offers",
                jobOfferService.getJobOffers(title, location, isPublished, page, size, sortBy, direction)
        ));
    }

    @GetMapping({"/api/hr/job-offers/{jobOfferId}", "/api/rh/job-offers/{jobOfferId}"})
    public ResponseEntity<ApiResponse<JobOfferDetailDTO>> getOffer(@PathVariable Long jobOfferId) {
        return ResponseEntity.ok(ApiResponse.ok("Job offer details", jobOfferService.getJobOffer(jobOfferId)));
    }

    @GetMapping({"/api/hr/job-offers/{jobOfferId}/applicants", "/api/rh/job-offers/{jobOfferId}/applicants"})
    public ResponseEntity<ApiResponse<List<ApplicantSummaryDTO>>> getApplicants(
            @PathVariable Long jobOfferId,
            @RequestParam(defaultValue = "score") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Job offer applicants", jobOfferService.getJobApplicants(jobOfferId, sortBy, direction)));
    }

    @PostMapping({"/api/hr/job-offers", "/api/rh/job-offers"})
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<ApiResponse<JobOfferDTO>> createOffer(
            @Valid @RequestBody CreateJobOfferRequest request,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.ok("Job offer created", jobOfferService.createJobOffer(request, user)));
    }

    @PostMapping({"/api/hr/job-offers/{jobOfferId}/retry", "/api/rh/job-offers/{jobOfferId}/retry"})
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<ApiResponse<JobOfferDTO>> retryOffer(@PathVariable Long jobOfferId) {
        return ResponseEntity.ok(ApiResponse.ok("Retry started", jobOfferService.retryJobDescriptionProcessing(jobOfferId)));
    }

    @PutMapping({"/api/hr/job-offers/{jobOfferId}", "/api/rh/job-offers/{jobOfferId}"})
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<ApiResponse<JobOfferDetailDTO>> updateOffer(
            @PathVariable Long jobOfferId,
            @Valid @RequestBody UpdateJobOfferRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Job offer updated", jobOfferService.updateJobOffer(jobOfferId, request)));
    }

    @PatchMapping({"/api/hr/job-offers/{jobOfferId}/status", "/api/rh/job-offers/{jobOfferId}/status"})
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<ApiResponse<JobOfferDetailDTO>> toggleStatus(
            @PathVariable Long jobOfferId,
            @Valid @RequestBody ToggleJobOfferStatusRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Job offer status updated", jobOfferService.toggleJobStatus(jobOfferId, request.status())));
    }

    @DeleteMapping({"/api/hr/job-offers/{jobOfferId}", "/api/rh/job-offers/{jobOfferId}"})
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteOffer(@PathVariable Long jobOfferId) {
        jobOfferService.deleteJobOffer(jobOfferId);
        return ResponseEntity.ok(ApiResponse.ok("Job offer deleted", null));
    }
}
