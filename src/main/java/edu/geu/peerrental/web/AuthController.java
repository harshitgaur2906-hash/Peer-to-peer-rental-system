package edu.geu.peerrental.web;

import edu.geu.peerrental.dto.Dtos.*;
import edu.geu.peerrental.model.User;
import edu.geu.peerrental.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService users;
    private final SessionAuth auth;

    public AuthController(UserService users, SessionAuth auth) {
        this.users = users;
        this.auth = auth;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest r, HttpServletRequest req) {
        User u = users.register(r);
        auth.login(req, u.getId());
        return UserResponse.from(u);
    }

    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest r, HttpServletRequest req) {
        User u = users.authenticate(r);
        auth.login(req, u.getId());
        return UserResponse.from(u);
    }

    @PostMapping("/logout")
    public Map<String, String> logout(HttpServletRequest req) {
        auth.logout(req);
        return Map.of("status", "logged out");
    }

    @GetMapping("/me")
    public UserResponse me(HttpServletRequest req) {
        Long id = auth.requireUserId(req);
        try {
            return UserResponse.from(users.get(id));
        } catch (ApiException e) {
            auth.logout(req);   // account no longer exists
            throw e;
        }
    }
}
