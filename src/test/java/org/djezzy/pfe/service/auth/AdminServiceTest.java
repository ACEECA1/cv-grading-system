package org.djezzy.pfe.service.auth;

import org.djezzy.pfe.dao.auth.UserDAO;
import org.djezzy.pfe.dto.auth.UserDTO;
import org.djezzy.pfe.model.auth.RhApprovalStatus;
import org.djezzy.pfe.model.auth.Role;
import org.djezzy.pfe.model.auth.User;
import org.djezzy.pfe.util.AppException;
import org.djezzy.pfe.util.MapperUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserDAO userDAO;

    @Mock
    private MapperUtil mapperUtil;

    @InjectMocks
    private AdminService adminService;

    private User hrUser;
    private UserDTO hrUserDTO;

    @BeforeEach
    void setUp() {
        hrUser = new User();
        hrUser.setId(1L);
        hrUser.setRole(Role.HR);
        hrUser.setRhApprovalStatus(RhApprovalStatus.PENDING);
        hrUserDTO = new UserDTO(1L, "hr_test", "HR", "Test", "hr@test.com", Role.HR, true, RhApprovalStatus.PENDING, null, null);
    }

    @Test
    void listPendingHr_ReturnsList() {
        // Arrange
        when(userDAO.findByRoleAndRhApprovalStatus(Role.HR, RhApprovalStatus.PENDING))
                .thenReturn(List.of(hrUser));
        when(mapperUtil.toUserDto(hrUser)).thenReturn(hrUserDTO);

        // Act
        List<UserDTO> result = adminService.listPendingHr();

        // Assert
        assertEquals(1, result.size());
        assertEquals(hrUserDTO, result.get(0));
        verify(userDAO, times(1)).findByRoleAndRhApprovalStatus(Role.HR, RhApprovalStatus.PENDING);
    }

    @Test
    void approveHr_Success() {
        // Arrange
        when(userDAO.findById(1L)).thenReturn(Optional.of(hrUser));
        UserDTO approvedDto = new UserDTO(1L, "hr_test", "HR", "Test", "hr@test.com", Role.HR, true, RhApprovalStatus.APPROVED, null, null);
        when(mapperUtil.toUserDto(any(User.class))).thenReturn(approvedDto);

        // Act
        UserDTO result = adminService.approveHr(1L);

        // Assert
        assertNotNull(result);
        assertEquals(RhApprovalStatus.APPROVED, result.hrApprovalStatus());
        verify(userDAO, times(1)).save(hrUser);
    }

    @Test
    void approveHr_UserNotFound_ThrowsException() {
        // Arrange
        when(userDAO.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        AppException ex = assertThrows(AppException.class, () -> adminService.approveHr(99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void approveHr_NotHrRole_ThrowsException() {
        // Arrange
        User candidate = new User();
        candidate.setId(2L);
        candidate.setRole(Role.CANDIDATE);
        when(userDAO.findById(2L)).thenReturn(Optional.of(candidate));

        // Act & Assert
        AppException ex = assertThrows(AppException.class, () -> adminService.approveHr(2L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }
}
