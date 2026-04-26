package org.djezzy.pfe.service;

import lombok.RequiredArgsConstructor;
import org.djezzy.pfe.dao.UserDAO;
import org.djezzy.pfe.dto.UserDTO;
import org.djezzy.pfe.model.RhApprovalStatus;
import org.djezzy.pfe.model.Role;
import org.djezzy.pfe.model.User;
import org.djezzy.pfe.util.AppException;
import org.djezzy.pfe.util.MapperUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserDAO userDAO;
    private final MapperUtil mapperUtil;

    @Transactional(readOnly = true)
    public List<UserDTO> listPendingHr() {
        return userDAO.findByRoleAndRhApprovalStatus(Role.HR, RhApprovalStatus.PENDING)
                .stream()
                .map(mapperUtil::toUserDto)
                .toList();
    }

    @Transactional
    public UserDTO approveHr(Long userId) {
        User user = userDAO.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getRole() != Role.HR) {
            throw new AppException(HttpStatus.BAD_REQUEST, "User is not HR");
        }
        user.setRhApprovalStatus(RhApprovalStatus.APPROVED);
        userDAO.save(user);
        return mapperUtil.toUserDto(user);
    }
}
