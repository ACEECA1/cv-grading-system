package org.djezzy.pfe.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.djezzy.pfe.dto.ApiResponse;
import org.djezzy.pfe.dto.JobOfferDTO;
import org.djezzy.pfe.dto.TestJobOfferCreationDTO;
import org.djezzy.pfe.model.User;
import org.djezzy.pfe.service.JobOfferService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {
    private final JobOfferService jobOfferService;

    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    @PostMapping("/job-offers/full")
    public ResponseEntity<ApiResponse<JobOfferDTO>> createFullJobOffer(
            @Valid @RequestBody TestJobOfferCreationDTO request,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        JobOfferDTO response = jobOfferService.createTestJobOfferWithStructuredJd(request, user);
        return ResponseEntity.ok(ApiResponse.ok("Test job offer created", response));
    }
}
