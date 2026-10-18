package edu.geu.peerrental.service;

import edu.geu.peerrental.dto.Dtos.*;
import edu.geu.peerrental.model.User;
import edu.geu.peerrental.repository.UserRepository;
import edu.geu.peerrental.web.ApiException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class UserService {
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public UserService(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    private static String normEmail(String e) {
        return e.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional
    public User register(RegisterRequest r) {
        String email = normEmail(r.email());
        if (users.existsByEmail(email)) {
            throw ApiException.conflict("An account with this email already exists");
        }
        User u = new User();
        u.setName(r.name().trim());
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(r.password()));
        return users.save(u);
    }

    @Transactional(readOnly = true)
    public User authenticate(LoginRequest r) {
        User u = users.findByEmail(normEmail(r.email())).orElse(null);
        if (u == null || !encoder.matches(r.password(), u.getPasswordHash())) {
            throw ApiException.unauthorized("Invalid email or password");
        }
        return u;
    }

    @Transactional(readOnly = true)
    public User get(Long id) {
        return users.findById(id).orElseThrow(() -> ApiException.unauthorized("Please log in first"));
    }
}
