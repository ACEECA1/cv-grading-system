package org.djezzy.pfe.service.auth;

import org.djezzy.pfe.dao.auth.UserDAO;
import org.djezzy.pfe.dto.auth.UpdateUserRequest;
import org.djezzy.pfe.dto.auth.UserDTO;
import org.djezzy.pfe.model.auth.Role;
import org.djezzy.pfe.model.auth.User;
import org.djezzy.pfe.util.AppException;
import org.djezzy.pfe.util.MapperUtil;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserDAO userDAO;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MapperUtil mapperUtil;

    @InjectMocks
    private UserService userService;

    @Test
    void getUserProfile_Success() {
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        UserDTO userDTO = new UserDTO(userId, "john", "John", "Doe", "john@doe.com", Role.HR, true, org.djezzy.pfe.model.auth.RhApprovalStatus.APPROVED, null, null);
        
        when(userDAO.findById(userId)).thenReturn(Optional.of(user));
        when(mapperUtil.toUserDto(user)).thenReturn(userDTO);

        UserDTO result = userService.getUserProfile(userId);

        assertNotNull(result);
        assertEquals(userId, result.id());
        verify(userDAO).findById(userId);
        verify(mapperUtil).toUserDto(user);
    }

    @Test
    void getUserProfile_Failure_NotFound() {
        Long userId = 1L;
        when(userDAO.findById(userId)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> 
            userService.getUserProfile(userId)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void resolveUserIdByPrincipal_Success_ByUsername() {
        User user = new User();
        user.setId(1L);
        when(userDAO.findByUsername("testuser")).thenReturn(Optional.of(user));

        Long result = userService.resolveUserIdByPrincipal("testuser");

        assertEquals(1L, result);
        verify(userDAO).findByUsername("testuser");
    }

    @Test
    void resolveUserIdByPrincipal_Success_ByEmail() {
        User user = new User();
        user.setId(2L);
        when(userDAO.findByUsername("test@test.com")).thenReturn(Optional.empty());
        when(userDAO.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        Long result = userService.resolveUserIdByPrincipal("test@test.com");

        assertEquals(2L, result);
        verify(userDAO).findByUsername("test@test.com");
        verify(userDAO).findByEmail("test@test.com");
    }

    @Test
    void resolveUserIdByPrincipal_Failure_NotFound() {
        when(userDAO.findByUsername("unknown")).thenReturn(Optional.empty());
        when(userDAO.findByEmail("unknown")).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> 
            userService.resolveUserIdByPrincipal("unknown")
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void updateUserProfile_Success_WithoutPassword() {
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        user.setFirstName("Old");
        user.setLastName("Name");

        UpdateUserRequest request = new UpdateUserRequest("New", "Name", null, null);
        UserDTO userDTO = new UserDTO(userId, "user", "New", "Name", "email", Role.HR, true, org.djezzy.pfe.model.auth.RhApprovalStatus.APPROVED, null, null);

        when(userDAO.findById(userId)).thenReturn(Optional.of(user));
        when(userDAO.save(any(User.class))).thenReturn(user);
        when(mapperUtil.toUserDto(any(User.class))).thenReturn(userDTO);

        UserDTO result = userService.updateUserProfile(userId, request);

        assertNotNull(result);
        assertEquals("New", user.getFirstName());
        assertEquals("Name", user.getLastName());
        verify(userDAO).save(user);
    }

    @Test
    void updateUserProfile_Success_WithPassword() {
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        user.setPassword("encodedOldPassword");

        UpdateUserRequest request = new UpdateUserRequest("First", "Last", "oldPass", "newPass");
        UserDTO userDTO = new UserDTO(userId, "user", "First", "Last", "email", Role.HR, true, org.djezzy.pfe.model.auth.RhApprovalStatus.APPROVED, null, null);

        when(userDAO.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPass", "encodedOldPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPass")).thenReturn("encodedNewPassword");
        when(userDAO.save(any(User.class))).thenReturn(user);
        when(mapperUtil.toUserDto(any(User.class))).thenReturn(userDTO);

        UserDTO result = userService.updateUserProfile(userId, request);

        assertNotNull(result);
        assertEquals("encodedNewPassword", user.getPassword());
        verify(passwordEncoder).encode("newPass");
        verify(userDAO).save(user);
    }

    @Test
    void updateUserProfile_Failure_InvalidCurrentPassword() {
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        user.setPassword("encodedOldPassword");

        UpdateUserRequest request = new UpdateUserRequest("First", "Last", "wrongOldPass", "newPass");

        when(userDAO.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongOldPass", "encodedOldPassword")).thenReturn(false);

        AppException exception = assertThrows(AppException.class, () -> 
            userService.updateUserProfile(userId, request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Current password is incorrect.", exception.getMessage());
    }
    
    @Test
    void updateUserProfile_Failure_MissingCurrentPasswordWhenUpdating() {
        Long userId = 1L;
        User user = new User();
        user.setId(userId);

        UpdateUserRequest request = new UpdateUserRequest("First", "Last", "", "newPass");

        when(userDAO.findById(userId)).thenReturn(Optional.of(user));

        AppException exception = assertThrows(AppException.class, () -> 
            userService.updateUserProfile(userId, request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Current password is incorrect.", exception.getMessage());
    }

    @Test
    void updateUserProfile_Failure_NotFound() {
        when(userDAO.findById(1L)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> 
            userService.updateUserProfile(1L, new UpdateUserRequest("First", "Last", null, null))
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }
}
