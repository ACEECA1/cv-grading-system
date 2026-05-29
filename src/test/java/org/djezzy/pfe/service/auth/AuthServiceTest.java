package org.djezzy.pfe.service.auth;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserDAO userDAO;
    @Mock
    private CandidateDAO candidateDAO;
    @Mock
    private HRPersonDAO hrPersonDAO;
    @Mock
    private VerificationCodeDAO verificationCodeDAO;
    @Mock
    private PasswordResetTokenDAO passwordResetTokenDAO;
    @Mock
    private RefreshTokenDAO refreshTokenDAO;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private CodeGeneratorUtil codeGeneratorUtil;
    @Mock
    private EmailUtil emailUtil;
    @Mock
    private MapperUtil mapperUtil;
    @Mock
    private AppProperties appProperties;

    @InjectMocks
    private AuthService authService;

    private User user;
    private Candidate candidate;
    private HRPerson hrPerson;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword");
        user.setRole(Role.CANDIDATE);
        user.setIsEnabled(true);
        user.setRhApprovalStatus(RhApprovalStatus.APPROVED);

        candidate = new Candidate();
        candidate.setId(1L);
        candidate.setUsername("testcandidate");
        candidate.setEmail("candidate@example.com");
        candidate.setPassword("encodedPassword");
        candidate.setRole(Role.CANDIDATE);

        hrPerson = new HRPerson();
        hrPerson.setId(2L);
        hrPerson.setUsername("testhr");
        hrPerson.setEmail("hr@example.com");
        hrPerson.setPassword("encodedPassword");
        hrPerson.setRole(Role.HR);
        hrPerson.setRhApprovalStatus(RhApprovalStatus.PENDING);
    }

    @Test
    void registerCandidate_Success() {
        RegisterCandidateRequest request = mock(RegisterCandidateRequest.class);
        when(request.username()).thenReturn("newcandidate");
        when(request.email()).thenReturn("new@example.com");
        when(request.password()).thenReturn("password");
        when(request.firstName()).thenReturn("First");
        when(request.lastName()).thenReturn("Last");

        when(userDAO.existsByUsername("newcandidate")).thenReturn(false);
        when(userDAO.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        when(appProperties.getVerificationCodeExpiryMinutes()).thenReturn(15);
        when(codeGeneratorUtil.alphanumericCode(6)).thenReturn("123456");

        UserDTO mockUserDto = mock(UserDTO.class);
        when(mapperUtil.toUserDto(any(Candidate.class))).thenReturn(mockUserDto);

        UserDTO result = authService.registerCandidate(request);

        assertNotNull(result);
        verify(candidateDAO).save(any(Candidate.class));
        verify(verificationCodeDAO).save(any(VerificationCode.class));
        verify(emailUtil).sendVerificationCode("new@example.com", "123456");
    }

    @Test
    void registerCandidate_UsernameExists() {
        RegisterCandidateRequest request = mock(RegisterCandidateRequest.class);
        when(request.username()).thenReturn("existing");
        when(request.email()).thenReturn("new@example.com");

        when(userDAO.existsByUsername("existing")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> authService.registerCandidate(request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("Username already exists", ex.getMessage());
    }

    @Test
    void registerCandidate_EmailExists() {
        RegisterCandidateRequest request = mock(RegisterCandidateRequest.class);
        when(request.username()).thenReturn("new");
        when(request.email()).thenReturn("existing@example.com");

        when(userDAO.existsByUsername("new")).thenReturn(false);
        when(userDAO.existsByEmail("existing@example.com")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> authService.registerCandidate(request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("Email already exists", ex.getMessage());
    }

    @Test
    void registerHr_Success() {
        RegisterHrRequest request = mock(RegisterHrRequest.class);
        when(request.username()).thenReturn("newhr");
        when(request.email()).thenReturn("newhr@example.com");
        when(request.password()).thenReturn("password");
        when(request.firstName()).thenReturn("First");
        when(request.lastName()).thenReturn("Last");

        when(userDAO.existsByUsername("newhr")).thenReturn(false);
        when(userDAO.existsByEmail("newhr@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        when(appProperties.getVerificationCodeExpiryMinutes()).thenReturn(15);
        when(codeGeneratorUtil.alphanumericCode(6)).thenReturn("123456");

        UserDTO mockUserDto = mock(UserDTO.class);
        when(mapperUtil.toUserDto(any(HRPerson.class))).thenReturn(mockUserDto);

        UserDTO result = authService.registerHr(request);

        assertNotNull(result);
        verify(hrPersonDAO).save(any(HRPerson.class));
        verify(verificationCodeDAO).save(any(VerificationCode.class));
        verify(emailUtil).sendVerificationCode("newhr@example.com", "123456");
    }

    @Test
    void verifyCode_Success() {
        VerifyCodeRequest request = mock(VerifyCodeRequest.class);
        when(request.email()).thenReturn("test@example.com");
        when(request.code()).thenReturn("123456");

        VerificationCode code = new VerificationCode();
        code.setCode("123456");
        code.setUsed(false);
        code.setExpiresAt(Instant.now().plusSeconds(600));
        code.setUser(user);

        when(userDAO.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(verificationCodeDAO.findByUserAndCodeAndUsedFalse(user, "123456")).thenReturn(Optional.of(code));

        authService.verifyCode(request);

        assertTrue(code.isUsed());
        assertTrue(user.getIsEnabled());
        verify(verificationCodeDAO).save(code);
        verify(userDAO).save(user);
    }

    @Test
    void verifyCode_CodeExpired() {
        VerifyCodeRequest request = mock(VerifyCodeRequest.class);
        when(request.email()).thenReturn("test@example.com");
        when(request.code()).thenReturn("123456");

        VerificationCode code = new VerificationCode();
        code.setCode("123456");
        code.setUsed(false);
        code.setExpiresAt(Instant.now().minusSeconds(600)); // Expired
        code.setUser(user);

        when(userDAO.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(verificationCodeDAO.findByUserAndCodeAndUsedFalse(user, "123456")).thenReturn(Optional.of(code));

        AppException ex = assertThrows(AppException.class, () -> authService.verifyCode(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Verification code expired", ex.getMessage());
    }

    @Test
    void resendCode_Success() {
        ResendCodeRequest request = mock(ResendCodeRequest.class);
        when(request.email()).thenReturn("test@example.com");

        when(userDAO.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(appProperties.getVerificationCodeExpiryMinutes()).thenReturn(15);
        when(codeGeneratorUtil.alphanumericCode(6)).thenReturn("123456");

        authService.resendCode(request);

        verify(verificationCodeDAO).save(any(VerificationCode.class));
        verify(emailUtil).sendVerificationCode("test@example.com", "123456");
    }

    @Test
    void requestPasswordReset_Success() {
        ForgotPasswordRequest request = mock(ForgotPasswordRequest.class);
        when(request.email()).thenReturn("test@example.com");

        when(userDAO.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(codeGeneratorUtil.numericCode(6)).thenReturn("654321");
        when(passwordResetTokenDAO.findByUser(user)).thenReturn(Optional.empty());

        authService.requestPasswordReset(request);

        verify(passwordResetTokenDAO).save(any(PasswordResetToken.class));
        verify(emailUtil).sendPasswordResetCode("test@example.com", "654321");
    }

    @Test
    void resetPassword_Success() {
        ResetPasswordRequest request = mock(ResetPasswordRequest.class);
        when(request.email()).thenReturn("test@example.com");
        when(request.code()).thenReturn("654321");
        when(request.newPassword()).thenReturn("newPass");

        PasswordResetToken token = new PasswordResetToken();
        token.setToken("654321");
        token.setExpiryDate(LocalDateTime.now().plusMinutes(10));
        token.setUser(user);

        when(userDAO.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenDAO.findByUserAndToken(user, "654321")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("newPass")).thenReturn("encodedNewPass");

        authService.resetPassword(request);

        assertEquals("encodedNewPass", user.getPassword());
        verify(userDAO).save(user);
        verify(passwordResetTokenDAO).delete(token);
    }

    @Test
    void resetPassword_TokenExpired() {
        ResetPasswordRequest request = mock(ResetPasswordRequest.class);
        when(request.email()).thenReturn("test@example.com");
        when(request.code()).thenReturn("654321");

        PasswordResetToken token = new PasswordResetToken();
        token.setToken("654321");
        token.setExpiryDate(LocalDateTime.now().minusMinutes(10)); // Expired
        token.setUser(user);

        when(userDAO.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenDAO.findByUserAndToken(user, "654321")).thenReturn(Optional.of(token));

        AppException ex = assertThrows(AppException.class, () -> authService.resetPassword(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Verification code expired", ex.getMessage());
        verify(passwordResetTokenDAO).delete(token);
    }

    @Test
    void login_Success() {
        LoginRequest request = mock(LoginRequest.class);
        when(request.usernameOrEmail()).thenReturn("testuser");
        when(request.password()).thenReturn("password");

        when(userDAO.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);
        
        RefreshToken activeToken = new RefreshToken();
        activeToken.setRevoked(false);
        when(refreshTokenDAO.findByUserAndRevokedFalse(user)).thenReturn(List.of(activeToken));

        when(jwtUtil.generateAccessToken(user)).thenReturn("accessToken");
        when(jwtUtil.generateRefreshToken(user)).thenReturn("refreshToken");
        when(jwtUtil.extractExpiration("refreshToken")).thenReturn(new Date(System.currentTimeMillis() + 100000));
        when(jwtUtil.getAccessExpirationMs()).thenReturn(3600000L);

        UserDTO mockUserDto = mock(UserDTO.class);
        when(mapperUtil.toUserDto(user)).thenReturn(mockUserDto);

        AuthTokensDTO tokens = authService.login(request);

        assertNotNull(tokens);
        assertEquals("accessToken", tokens.accessToken());
        assertEquals("refreshToken", tokens.refreshToken());
        assertTrue(activeToken.isRevoked()); // old token revoked
        verify(refreshTokenDAO).saveAll(anyList());
        verify(refreshTokenDAO).save(any(RefreshToken.class)); // new token persisted
    }

    @Test
    void login_InvalidCredentials() {
        LoginRequest request = mock(LoginRequest.class);
        when(request.usernameOrEmail()).thenReturn("testuser");
        when(request.password()).thenReturn("wrongpassword");

        when(userDAO.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

        AppException ex = assertThrows(AppException.class, () -> authService.login(request));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        assertEquals("Invalid credentials", ex.getMessage());
    }

    @Test
    void login_UnverifiedAccount() {
        LoginRequest request = mock(LoginRequest.class);
        when(request.usernameOrEmail()).thenReturn("testuser");
        when(request.password()).thenReturn("password");

        user.setIsEnabled(false);

        when(userDAO.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> authService.login(request));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("UNVERIFIED_ACCOUNT", ex.getMessage());
    }

    @Test
    void login_PendingHrAccount() {
        LoginRequest request = mock(LoginRequest.class);
        when(request.usernameOrEmail()).thenReturn("testhr");
        when(request.password()).thenReturn("password");

        hrPerson.setIsEnabled(true);
        hrPerson.setRhApprovalStatus(RhApprovalStatus.PENDING);

        when(userDAO.findByUsername("testhr")).thenReturn(Optional.of(hrPerson));
        when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> authService.login(request));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("HR account is pending admin approval", ex.getMessage());
    }

    @Test
    void refresh_Success() {
        RefreshTokenRequest request = mock(RefreshTokenRequest.class);
        when(request.refreshToken()).thenReturn("oldRefreshToken");

        RefreshToken oldToken = new RefreshToken();
        oldToken.setToken("oldRefreshToken");
        oldToken.setRevoked(false);
        oldToken.setExpiresAt(Instant.now().plusSeconds(1000));
        oldToken.setUser(user);

        when(refreshTokenDAO.findByToken("oldRefreshToken")).thenReturn(Optional.of(oldToken));
        when(jwtUtil.generateAccessToken(user)).thenReturn("newAccessToken");
        when(jwtUtil.generateRefreshToken(user)).thenReturn("newRefreshToken");
        when(jwtUtil.extractExpiration("newRefreshToken")).thenReturn(new Date(System.currentTimeMillis() + 100000));

        UserDTO mockUserDto = mock(UserDTO.class);
        when(mapperUtil.toUserDto(user)).thenReturn(mockUserDto);

        AuthTokensDTO tokens = authService.refresh(request);

        assertNotNull(tokens);
        assertEquals("newAccessToken", tokens.accessToken());
        assertEquals("newRefreshToken", tokens.refreshToken());
        assertTrue(oldToken.isRevoked());
        verify(refreshTokenDAO, times(2)).save(any(RefreshToken.class));
    }

    @Test
    void refresh_ExpiredToken() {
        RefreshTokenRequest request = mock(RefreshTokenRequest.class);
        when(request.refreshToken()).thenReturn("expiredRefreshToken");

        RefreshToken expiredToken = new RefreshToken();
        expiredToken.setToken("expiredRefreshToken");
        expiredToken.setRevoked(false);
        expiredToken.setExpiresAt(Instant.now().minusSeconds(1000));
        expiredToken.setUser(user);

        when(refreshTokenDAO.findByToken("expiredRefreshToken")).thenReturn(Optional.of(expiredToken));

        AppException ex = assertThrows(AppException.class, () -> authService.refresh(request));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        assertEquals("Refresh token expired or revoked", ex.getMessage());
    }

    @Test
    void logout_Success() {
        LogoutRequest request = mock(LogoutRequest.class);
        when(request.refreshToken()).thenReturn("refreshToken");

        RefreshToken token = new RefreshToken();
        token.setToken("refreshToken");
        token.setRevoked(false);

        when(refreshTokenDAO.findByToken("refreshToken")).thenReturn(Optional.of(token));

        authService.logout(request);

        assertTrue(token.isRevoked());
        verify(refreshTokenDAO).save(token);
    }
}
