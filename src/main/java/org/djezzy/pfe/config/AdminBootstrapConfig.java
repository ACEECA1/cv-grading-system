package org.djezzy.pfe.config;

import lombok.RequiredArgsConstructor;
import org.djezzy.pfe.dao.auth.AdminDAO;
import org.djezzy.pfe.dao.auth.UserDAO;
import org.djezzy.pfe.model.auth.Admin;
import org.djezzy.pfe.model.auth.RhApprovalStatus;
import org.djezzy.pfe.model.auth.Role;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminBootstrapConfig {
    private final UserDAO userDAO;
    private final AdminDAO adminDAO;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void createDefaultAdmin() {
        if (appProperties.getAdmin() == null || appProperties.getAdmin().getUsername() == null) {
            return;
        }
        if (userDAO.existsByUsername(appProperties.getAdmin().getUsername())
                || userDAO.existsByEmail(appProperties.getAdmin().getEmail())) {
            return;
        }
        Admin admin = Admin.builder()
                .username(appProperties.getAdmin().getUsername())
                .firstName(appProperties.getAdmin().getFirstName())
                .lastName(appProperties.getAdmin().getLastName())
                .email(appProperties.getAdmin().getEmail())
                .password(passwordEncoder.encode(appProperties.getAdmin().getPassword()))
                .role(Role.ADMIN)
                .isEnabled(true)
                .rhApprovalStatus(RhApprovalStatus.APPROVED)
                .build();
        adminDAO.save(admin);
    }
}

