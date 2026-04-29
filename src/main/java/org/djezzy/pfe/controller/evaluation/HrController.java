package org.djezzy.pfe.controller.evaluation;

import lombok.RequiredArgsConstructor;
import org.djezzy.pfe.dto.system.ApiResponse;
import org.djezzy.pfe.dto.system.DashboardStatsDTO;
import org.djezzy.pfe.dto.evaluation.HrEvaluationSummaryDTO;
import org.djezzy.pfe.service.evaluation.HrService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;

@RestController
@RequestMapping("/api/hr")
@RequiredArgsConstructor
public class HrController {
    private final HrService hrService;

    @GetMapping("/dashboard/stats")
    public ResponseEntity<ApiResponse<DashboardStatsDTO>> dashboardStats() {
        return ResponseEntity.ok(ApiResponse.ok("Dashboard statistics", hrService.dashboardStats()));
    }

    @GetMapping("/evaluations")
    public ResponseEntity<ApiResponse<Page<HrEvaluationSummaryDTO>>> listEvaluations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long jobId,
            @RequestParam(required = false) Double minScore
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.ok("Evaluations", hrService.listEvaluations(pageable, jobId, minScore)));
    }

    @GetMapping("/evaluations/{evaluationId}/cv/download")
    public ResponseEntity<Resource> downloadCv(@PathVariable Long evaluationId) {
        Path path = hrService.resolveEvaluationCvPath(evaluationId);
        Resource resource = new FileSystemResource(path);
        String fileName = path.getFileName().toString();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }
}


