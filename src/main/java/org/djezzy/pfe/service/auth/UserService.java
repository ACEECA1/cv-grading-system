package org.djezzy.pfe.service.auth;

import lombok.RequiredArgsConstructor;
import org.djezzy.pfe.dao.auth.UserDAO;
import org.djezzy.pfe.dto.auth.UpdateUserRequest;
import org.djezzy.pfe.dto.auth.UserDTO;
import org.djezzy.pfe.model.auth.User;
import org.djezzy.pfe.util.AppException;
import org.djezzy.pfe.util.MapperUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserDAO userDAO;
    private final PasswordEncoder passwordEncoder;
    private final MapperUtil mapperUtil;

    @Transactional(readOnly = true)
    public UserDTO getUserProfile(Long userId) {
        User user = userDAO.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));
        return mapperUtil.toUserDto(user);
    }

    @Transactional(readOnly = true)
    public Long resolveUserIdByPrincipal(String principalName) {
        User user = userDAO.findByUsername(principalName)
                .or(() -> userDAO.findByEmail(principalName))
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));
        return user.getId();
    }

    @Transactional
    public UserDTO updateUserProfile(Long userId, UpdateUserRequest request) {
        User user = userDAO.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));

        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());

        if (request.newPassword() != null && !request.newPassword().isBlank()) {
            if (request.currentPassword() == null || request.currentPassword().isBlank()) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Current password is incorrect.");
            }
            if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Current password is incorrect.");
            }
            user.setPassword(passwordEncoder.encode(request.newPassword()));
        }

        userDAO.save(user);
        return mapperUtil.toUserDto(user);
    }
}
