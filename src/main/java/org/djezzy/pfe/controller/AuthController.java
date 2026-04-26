package org.djezzy.pfe.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.djezzy.pfe.dto.ApiResponse;
import org.djezzy.pfe.dto.AuthTokensDTO;
import org.djezzy.pfe.dto.LoginRequest;
import org.djezzy.pfe.dto.LogoutRequest;
import org.djezzy.pfe.dto.RefreshTokenRequest;
import org.djezzy.pfe.dto.RegisterCandidateRequest;
import org.djezzy.pfe.dto.RegisterHrRequest;
import org.djezzy.pfe.dto.ResendCodeRequest;
import org.djezzy.pfe.dto.UserDTO;
import org.djezzy.pfe.dto.VerifyCodeRequest;
import org.djezzy.pfe.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register/candidate")
    public ResponseEntity<ApiResponse<UserDTO>> registerCandidate(@Valid @RequestBody RegisterCandidateRequest request) {
        UserDTO user = authService.registerCandidate(request);
        return ResponseEntity.ok(ApiResponse.ok("Candidate registered. Verification code sent.", user));
    }

    @PostMapping({"/register/hr", "/register/rh"})
    public ResponseEntity<ApiResponse<UserDTO>> registerHr(@Valid @RequestBody RegisterHrRequest request) {
        UserDTO user = authService.registerHr(request);
        return ResponseEntity.ok(ApiResponse.ok("HR registered. Verification code sent.", user));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Void>> verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
        authService.verifyCode(request);
        return ResponseEntity.ok(ApiResponse.ok("Account verified successfully", null));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(@Valid @RequestBody ResendCodeRequest request) {
        authService.resendCode(request);
        return ResponseEntity.ok(ApiResponse.ok("Verification code resent", null));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthTokensDTO>> login(@Valid @RequestBody LoginRequest request) {
        AuthTokensDTO authTokens = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", authTokens));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthTokensDTO>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthTokensDTO authTokens = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.ok("Token refreshed", authTokens));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.ok("Logged out", null));
    }
}
