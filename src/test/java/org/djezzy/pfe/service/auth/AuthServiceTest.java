package org.djezzy.pfe.service.auth;

import org.djezzy.pfe.config.AppProperties;
import org.djezzy.pfe.dao.auth.*;
import org.djezzy.pfe.dto.auth.LoginRequest;
import org.djezzy.pfe.model.auth.Role;
import org.djezzy.pfe.model.auth.User;
import org.djezzy.pfe.util.AppException;
import org.djezzy.pfe.util.EmailUtil;
import org.djezzy.pfe.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserDAO userDAO;
    @Mock private CandidateDAO candidateDAO;
    @Mock private HRPersonDAO hrPersonDAO;
    @Mock private VerificationCodeDAO verificationCodeDAO;
    @Mock private PasswordResetTokenDAO passwordResetTokenDAO;
    @Mock private RefreshTokenDAO refreshTokenDAO;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private AppProperties appProperties;
    @Mock private EmailUtil emailUtil;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@test.com");
        testUser.setPassword("encodedPassword");
        testUser.setRole(Role.CANDIDATE);
        testUser.setIsEnabled(true);
    }

    @Test
    void login_Success() {
        // Arrange
        LoginRequest request = new LoginRequest("testuser", "password123");
        when(userDAO.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtUtil.generateAccessToken(testUser)).thenReturn("accessToken");
        when(jwtUtil.generateRefreshToken(testUser)).thenReturn("refreshToken");
        when(appProperties.getVerificationCodeExpiryMinutes()).thenReturn(15);

        // Act
        var result = authService.login(request);

        // Assert
        assertNotNull(result);
        assertEquals("accessToken", result.accessToken());
        assertEquals("refreshToken", result.refreshToken());
        verify(refreshTokenDAO, times(1)).save(any());
    }

    @Test
    void login_UnverifiedAccount_ThrowsException() {
        // Arrange
        testUser.setIsEnabled(false);
        LoginRequest request = new LoginRequest("testuser", "password123");
        when(userDAO.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> authService.login(request));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertEquals("UNVERIFIED_ACCOUNT", exception.getMessage());
    }

    @Test
    void login_InvalidPassword_ThrowsException() {
        // Arrange
        LoginRequest request = new LoginRequest("testuser", "wrongpassword");
        when(userDAO.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> authService.login(request));
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertEquals("Invalid username or password", exception.getMessage());
    }
}
