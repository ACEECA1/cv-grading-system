package org.djezzy.pfe.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.djezzy.pfe.dto.ApiResponse;
import org.djezzy.pfe.dto.CreateJobOfferRequest;
import org.djezzy.pfe.dto.JobOfferDTO;
import org.djezzy.pfe.dto.UpdateJobOfferRequest;
import org.djezzy.pfe.model.User;
import org.djezzy.pfe.service.JobOfferService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @GetMapping({"/api/hr/job-offers", "/api/rh/job-offers"})
    public ResponseEntity<ApiResponse<List<JobOfferDTO>>> allOffers() {
        return ResponseEntity.ok(ApiResponse.ok("Job offers", jobOfferService.listAllJobOffers()));
    }

    @PostMapping({"/api/hr/job-offers", "/api/rh/job-offers"})
    public ResponseEntity<ApiResponse<JobOfferDTO>> createOffer(
            @Valid @RequestBody CreateJobOfferRequest request,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.ok("Job offer created", jobOfferService.createJobOffer(request, user)));
    }

    @PutMapping({"/api/hr/job-offers/{jobOfferId}", "/api/rh/job-offers/{jobOfferId}"})
    public ResponseEntity<ApiResponse<JobOfferDTO>> updateOffer(
            @PathVariable Long jobOfferId,
            @Valid @RequestBody UpdateJobOfferRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Job offer updated", jobOfferService.updateJobOffer(jobOfferId, request)));
    }

    @DeleteMapping({"/api/hr/job-offers/{jobOfferId}", "/api/rh/job-offers/{jobOfferId}"})
    public ResponseEntity<ApiResponse<Void>> deleteOffer(@PathVariable Long jobOfferId) {
        jobOfferService.deleteJobOffer(jobOfferId);
        return ResponseEntity.ok(ApiResponse.ok("Job offer deleted", null));
    }
}
