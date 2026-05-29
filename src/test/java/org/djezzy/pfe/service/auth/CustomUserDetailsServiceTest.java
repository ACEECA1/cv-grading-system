package org.djezzy.pfe.service.auth;

import org.djezzy.pfe.dao.auth.UserDAO;
import org.djezzy.pfe.model.auth.User;
import org.djezzy.pfe.util.AppException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserDAO userDAO;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void loadUserByUsername_Success_FoundByUsername() {
        User user = new User();
        user.setUsername("testuser");
        when(userDAO.findByUsername("testuser")).thenReturn(Optional.of(user));

        User result = customUserDetailsService.loadUserByUsername("testuser");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(userDAO).findByUsername("testuser");
        verify(userDAO, never()).findByEmail(anyString());
    }

    @Test
    void loadUserByUsername_Success_FoundByEmail() {
        User user = new User();
        user.setEmail("test@test.com");
        when(userDAO.findByUsername("test@test.com")).thenReturn(Optional.empty());
        when(userDAO.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        User result = customUserDetailsService.loadUserByUsername("test@test.com");

        assertNotNull(result);
        assertEquals("test@test.com", result.getEmail());
        verify(userDAO).findByUsername("test@test.com");
        verify(userDAO).findByEmail("test@test.com");
    }

    @Test
    void loadUserByUsername_Failure_NotFound() {
        when(userDAO.findByUsername("unknown")).thenReturn(Optional.empty());
        when(userDAO.findByEmail("unknown")).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> 
            customUserDetailsService.loadUserByUsername("unknown")
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("User not found", exception.getMessage());
        verify(userDAO).findByUsername("unknown");
        verify(userDAO).findByEmail("unknown");
    }
}
