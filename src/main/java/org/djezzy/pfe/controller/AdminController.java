package org.djezzy.pfe.controller;

import lombok.RequiredArgsConstructor;
import org.djezzy.pfe.dto.ApiResponse;
import org.djezzy.pfe.dto.UserDTO;
import org.djezzy.pfe.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;

    @GetMapping("/rh/pending")
    public ResponseEntity<ApiResponse<List<UserDTO>>> pendingRh() {
        return ResponseEntity.ok(ApiResponse.ok("Pending HR accounts", adminService.listPendingHr()));
    }

    @PutMapping("/rh/{userId}/approve")
    public ResponseEntity<ApiResponse<UserDTO>> approveRh(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok("HR approved", adminService.approveHr(userId)));
    }

    @GetMapping("/hr/pending")
    public ResponseEntity<ApiResponse<List<UserDTO>>> pendingHr() {
        return ResponseEntity.ok(ApiResponse.ok("Pending HR accounts", adminService.listPendingHr()));
    }

    @PutMapping("/hr/{userId}/approve")
    public ResponseEntity<ApiResponse<UserDTO>> approveHr(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok("HR approved", adminService.approveHr(userId)));
    }
}
