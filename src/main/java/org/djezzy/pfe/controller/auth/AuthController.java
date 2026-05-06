package org.djezzy.pfe.controller.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.djezzy.pfe.dto.system.ApiResponse;
import org.djezzy.pfe.dto.auth.AuthTokensDTO;
import org.djezzy.pfe.dto.auth.ForgotPasswordRequest;
import org.djezzy.pfe.dto.auth.LoginRequest;
import org.djezzy.pfe.dto.auth.LogoutRequest;
import org.djezzy.pfe.dto.auth.RefreshTokenRequest;
import org.djezzy.pfe.dto.auth.RegisterCandidateRequest;
import org.djezzy.pfe.dto.auth.RegisterHrRequest;
import org.djezzy.pfe.dto.auth.ResetPasswordRequest;
import org.djezzy.pfe.dto.auth.ResendCodeRequest;
import org.djezzy.pfe.dto.auth.UserDTO;
import org.djezzy.pfe.dto.auth.VerifyCodeRequest;
import org.djezzy.pfe.service.auth.AuthService;
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

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.requestPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.ok("Password reset code sent", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("Password reset successful", null));
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


