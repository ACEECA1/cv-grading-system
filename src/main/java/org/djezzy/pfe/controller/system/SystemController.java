package org.djezzy.pfe.controller.system;

import lombok.RequiredArgsConstructor;
import org.djezzy.pfe.dto.system.ApiResponse;
import org.djezzy.pfe.dto.system.SystemHealthDTO;
import org.djezzy.pfe.service.system.SystemHealthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {
    private final SystemHealthService systemHealthService;

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<SystemHealthDTO>> health() {
        return ResponseEntity.ok(ApiResponse.ok("System health", systemHealthService.getSystemHealth()));
    }
}


