package org.djezzy.pfe.service.auth;

import lombok.RequiredArgsConstructor;
import org.djezzy.pfe.config.AppProperties;
import org.djezzy.pfe.dao.auth.CandidateDAO;
import org.djezzy.pfe.dao.auth.HRPersonDAO;
import org.djezzy.pfe.dao.auth.PasswordResetTokenDAO;
import org.djezzy.pfe.dao.auth.RefreshTokenDAO;
import org.djezzy.pfe.dao.auth.UserDAO;
import org.djezzy.pfe.dao.auth.VerificationCodeDAO;
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
import org.djezzy.pfe.model.auth.Candidate;
import org.djezzy.pfe.model.auth.HRPerson;
import org.djezzy.pfe.model.auth.PasswordResetToken;
import org.djezzy.pfe.model.auth.RefreshToken;
import org.djezzy.pfe.model.auth.RhApprovalStatus;
import org.djezzy.pfe.model.auth.Role;
import org.djezzy.pfe.model.auth.User;
import org.djezzy.pfe.model.auth.VerificationCode;
import org.djezzy.pfe.util.AppException;
import org.djezzy.pfe.util.CodeGeneratorUtil;
import org.djezzy.pfe.util.EmailUtil;
import org.djezzy.pfe.util.JwtUtil;
import org.djezzy.pfe.util.MapperUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final int PASSWORD_RESET_EXPIRY_MINUTES = 15;

    private final UserDAO userDAO;
    private final CandidateDAO candidateDAO;
    private final HRPersonDAO hrPersonDAO;
    private final VerificationCodeDAO verificationCodeDAO;
    private final PasswordResetTokenDAO passwordResetTokenDAO;
    private final RefreshTokenDAO refreshTokenDAO;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final CodeGeneratorUtil codeGeneratorUtil;
    private final EmailUtil emailUtil;
    private final MapperUtil mapperUtil;
    private final AppProperties appProperties;

    @Transactional
    public UserDTO registerCandidate(RegisterCandidateRequest request) {
        validateUniqueIdentity(request.username(), request.email());
        Candidate candidate = Candidate.builder()
                .username(request.username())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.CANDIDATE)
                .isEnabled(false)
                .rhApprovalStatus(RhApprovalStatus.APPROVED)
                .build();
        candidateDAO.save(candidate);
        issueVerificationCode(candidate);
        return mapperUtil.toUserDto(candidate);
    }

    @Transactional
    public UserDTO registerHr(RegisterHrRequest request) {
        validateUniqueIdentity(request.username(), request.email());
        HRPerson hr = HRPerson.builder()
                .username(request.username())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.HR)
                .isEnabled(false)
                .rhApprovalStatus(RhApprovalStatus.PENDING)
                .build();
        hrPersonDAO.save(hr);
        issueVerificationCode(hr);
        return mapperUtil.toUserDto(hr);
    }

    @Transactional
    public void verifyCode(VerifyCodeRequest request) {
        User user = userDAO.findByEmail(request.email())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));
        VerificationCode verificationCode = verificationCodeDAO
                .findByUserAndCodeAndUsedFalse(user, request.code())
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "Invalid verification code"));
        if (verificationCode.getExpiresAt().isBefore(Instant.now())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Verification code expired");
        }
        verificationCode.setUsed(true);
        verificationCodeDAO.save(verificationCode);
        user.setIsEnabled(true);
        userDAO.save(user);
    }

    @Transactional
    public void resendCode(ResendCodeRequest request) {
        User user = userDAO.findByEmail(request.email())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));
        issueVerificationCode(user);
    }

    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request) {
        User user = userDAO.findByEmail(request.email())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));

        String resetCode = codeGeneratorUtil.numericCode(6);
        PasswordResetToken passwordResetToken = passwordResetTokenDAO.findByUser(user)
                .orElseGet(PasswordResetToken::new);
        passwordResetToken.setUser(user);
        passwordResetToken.setToken(resetCode);
        passwordResetToken.setExpiryDate(LocalDateTime.now().plusMinutes(PASSWORD_RESET_EXPIRY_MINUTES));
        passwordResetTokenDAO.save(passwordResetToken);

        emailUtil.sendPasswordResetCode(user.getEmail(), resetCode);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userDAO.findByEmail(request.email())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));

        PasswordResetToken passwordResetToken = passwordResetTokenDAO.findByUserAndToken(user, request.code())
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "Invalid verification code"));

        if (passwordResetToken.isExpired()) {
            passwordResetTokenDAO.delete(passwordResetToken);
            throw new AppException(HttpStatus.BAD_REQUEST, "Verification code expired");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userDAO.save(user);
        passwordResetTokenDAO.delete(passwordResetToken);
    }

    @Transactional
    public AuthTokensDTO login(LoginRequest request) {
        User user = userDAO.findByUsername(request.usernameOrEmail())
                .or(() -> userDAO.findByEmail(request.usernameOrEmail()))
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!Boolean.TRUE.equals(user.getIsEnabled())) {
            throw new AppException(HttpStatus.FORBIDDEN, "Account is not verified");
        }
        if (user.getRole() == Role.HR && user.getRhApprovalStatus() != RhApprovalStatus.APPROVED) {
            throw new AppException(HttpStatus.FORBIDDEN, "HR account is pending admin approval");
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUsername(), request.password())
        );

        revokeActiveTokens(user);
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshTokenValue = jwtUtil.generateRefreshToken(user);
        persistRefreshToken(user, refreshTokenValue);
        return new AuthTokensDTO(
                accessToken,
                refreshTokenValue,
                "Bearer",
                jwtUtil.getAccessExpirationMs(),
                mapperUtil.toUserDto(user)
        );
    }

    @Transactional
    public AuthTokensDTO refresh(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenDAO.findByToken(request.refreshToken())
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        if (refreshToken.isRevoked() || refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Refresh token expired or revoked");
        }

        User user = refreshToken.getUser();
        if (!Boolean.TRUE.equals(user.getIsEnabled())) {
            throw new AppException(HttpStatus.FORBIDDEN, "Account is not active");
        }

        refreshToken.setRevoked(true);
        refreshTokenDAO.save(refreshToken);

        String accessToken = jwtUtil.generateAccessToken(user);
        String newRefreshToken = jwtUtil.generateRefreshToken(user);
        persistRefreshToken(user, newRefreshToken);
        return new AuthTokensDTO(
                accessToken,
                newRefreshToken,
                "Bearer",
                jwtUtil.getAccessExpirationMs(),
                mapperUtil.toUserDto(user)
        );
    }

    @Transactional
    public void logout(LogoutRequest request) {
        RefreshToken refreshToken = refreshTokenDAO.findByToken(request.refreshToken())
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        refreshToken.setRevoked(true);
        refreshTokenDAO.save(refreshToken);
    }

    private void validateUniqueIdentity(String username, String email) {
        if (userDAO.existsByUsername(username)) {
            throw new AppException(HttpStatus.CONFLICT, "Username already exists");
        }
        if (userDAO.existsByEmail(email)) {
            throw new AppException(HttpStatus.CONFLICT, "Email already exists");
        }
    }

    private void issueVerificationCode(User user) {
        String code = codeGeneratorUtil.alphanumericCode(6);
        VerificationCode verificationCode = new VerificationCode();
        verificationCode.setUser(user);
        verificationCode.setCode(code);
        verificationCode.setUsed(false);
        verificationCode.setExpiresAt(Instant.now().plusSeconds((long) appProperties.getVerificationCodeExpiryMinutes() * 60L));
        verificationCodeDAO.save(verificationCode);
        emailUtil.sendVerificationCode(user.getEmail(), code);
    }

    private void revokeActiveTokens(User user) {
        List<RefreshToken> activeTokens = refreshTokenDAO.findByUserAndRevokedFalse(user);
        activeTokens.forEach(token -> token.setRevoked(true));
        refreshTokenDAO.saveAll(activeTokens);
    }

    private void persistRefreshToken(User user, String refreshTokenValue) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(refreshTokenValue);
        refreshToken.setUser(user);
        refreshToken.setRevoked(false);
        refreshToken.setExpiresAt(jwtUtil.extractExpiration(refreshTokenValue).toInstant());
        refreshTokenDAO.save(refreshToken);
    }
}


