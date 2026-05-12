package org.djezzy.pfe.controller.evaluation;

import lombok.RequiredArgsConstructor;
import org.djezzy.pfe.dto.system.ApiResponse;
import org.djezzy.pfe.dto.evaluation.CandidateSubmissionDTO;
import org.djezzy.pfe.dto.job.JobOfferDTO;
import org.djezzy.pfe.dto.evaluation.UploadCvResponseDTO;
import org.djezzy.pfe.model.auth.User;
import org.djezzy.pfe.service.evaluation.CandidateService;
import org.djezzy.pfe.util.AppException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/candidate")
@RequiredArgsConstructor
public class CandidateController {
    private final CandidateService candidateService;

    @GetMapping("/job-offers")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<Page<JobOfferDTO>>> browseOffers(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String location,
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir
    ) {
        Pageable effectivePageable = pageable;
        if ((sortBy != null && !sortBy.isBlank()) || (sortDir != null && !sortDir.isBlank())) {
            Sort.Direction direction = Sort.Direction.DESC;
            if (sortDir != null && !sortDir.isBlank()) {
                try {
                    direction = Sort.Direction.fromString(sortDir);
                } catch (IllegalArgumentException ex) {
                    throw new AppException(HttpStatus.BAD_REQUEST, "Invalid sort direction. Use 'asc' or 'desc'.");
                }
            }
            String sortProperty = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
            effectivePageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(direction, sortProperty));
        }
        return ResponseEntity.ok(ApiResponse.ok("Job offers", candidateService.browseJobOffers(title, location, effectivePageable)));
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

    @GetMapping("/submissions/{jobOfferId}/cv/download")
    public ResponseEntity<Resource> downloadMyCv(@PathVariable Long jobOfferId, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Path path = candidateService.resolveSubmissionCvPath(jobOfferId, user);
        Resource resource = new FileSystemResource(path);
        String fileName = path.getFileName().toString();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }

    @DeleteMapping("/submissions/{jobOfferId}/cv")
    public ResponseEntity<ApiResponse<Void>> withdrawSubmission(@PathVariable Long jobOfferId, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        candidateService.withdrawSubmission(jobOfferId, user);
        return ResponseEntity.ok(ApiResponse.ok("Submission withdrawn successfully", null));
    }
}


