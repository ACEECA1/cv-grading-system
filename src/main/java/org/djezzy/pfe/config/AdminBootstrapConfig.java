package org.djezzy.pfe.config;

import lombok.RequiredArgsConstructor;
import org.djezzy.pfe.dao.AdminDAO;
import org.djezzy.pfe.dao.UserDAO;
import org.djezzy.pfe.model.Admin;
import org.djezzy.pfe.model.RhApprovalStatus;
import org.djezzy.pfe.model.Role;
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
