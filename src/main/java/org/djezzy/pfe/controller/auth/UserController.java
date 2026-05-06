package org.djezzy.pfe.controller.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.djezzy.pfe.dto.auth.UpdateUserRequest;
import org.djezzy.pfe.dto.auth.UserDTO;
import org.djezzy.pfe.dto.system.ApiResponse;
import org.djezzy.pfe.model.auth.User;
import org.djezzy.pfe.service.auth.UserService;
import org.djezzy.pfe.util.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDTO>> getCurrentUser() {
        Long userId = resolveCurrentUserId();
        UserDTO user = userService.getUserProfile(userId);
        return ResponseEntity.ok(ApiResponse.ok("Current user fetched successfully", user));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserDTO>> updateCurrentUser(@Valid @RequestBody UpdateUserRequest request) {
        Long userId = resolveCurrentUserId();
        UserDTO user = userService.updateUserProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully", user));
    }

    private Long resolveCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Unauthenticated request");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user.getId();
        }

        return userService.resolveUserIdByPrincipal(authentication.getName());
    }
}
