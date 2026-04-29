package org.djezzy.pfe.service.auth;

import lombok.RequiredArgsConstructor;
import org.djezzy.pfe.dao.auth.UserDAO;
import org.djezzy.pfe.model.auth.User;
import org.djezzy.pfe.util.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserDAO userDAO;

    @Override
    public User loadUserByUsername(String username) throws UsernameNotFoundException {
        return userDAO.findByUsername(username)
                .or(() -> userDAO.findByEmail(username))
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));
    }
}


