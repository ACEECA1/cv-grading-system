package org.djezzy.pfe.controller.evaluation;

import lombok.RequiredArgsConstructor;
import org.djezzy.pfe.dto.system.ApiResponse;
import org.djezzy.pfe.dto.evaluation.CandidateSubmissionDTO;
import org.djezzy.pfe.dto.job.JobOfferDTO;
import org.djezzy.pfe.dto.evaluation.UploadCvResponseDTO;
import org.djezzy.pfe.model.auth.User;
import org.djezzy.pfe.service.evaluation.CandidateService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ApiResponse<Page<JobOfferDTO>>> browseOffers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String location
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.ok("Job offers", candidateService.browseJobOffers(pageable, location)));
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


