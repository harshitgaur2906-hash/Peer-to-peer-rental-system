package edu.geu.peerrental.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

/** Tiny helper around the HTTP session: who is logged in? */
@Component
public class SessionAuth {
    private static final String KEY = "userId";

    public Long requireUserId(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        Object v = (s == null) ? null : s.getAttribute(KEY);
        if (v instanceof Long id) return id;
        throw ApiException.unauthorized("Please log in first");
    }

    public void login(HttpServletRequest req, Long userId) {
        if (req.getSession(false) != null) {
            req.changeSessionId();            // prevents session fixation
        }
        req.getSession(true).setAttribute(KEY, userId);
    }

    public void logout(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s != null) s.invalidate();
    }
}
