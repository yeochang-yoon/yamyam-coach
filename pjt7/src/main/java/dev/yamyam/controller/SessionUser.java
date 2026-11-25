package dev.yamyam.controller;

import dev.yamyam.domain.User;
import dev.yamyam.service.CoachService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class SessionUser {

    private static final String USER_ID = "userId";
    private final CoachService coach;

    public SessionUser(CoachService coach) {
        this.coach = coach;
    }

    public void login(HttpServletRequest request, String userId) {
        HttpSession session = request.getSession();
        request.changeSessionId();
        session.setAttribute(USER_ID, userId);
    }

    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    public User require(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            String userId = (String) session.getAttribute(USER_ID);
            if (userId != null) {
                try {
                    return coach.profile(userId);
                } catch (NoSuchElementException error) {
                    session.invalidate();
                }
            }
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
    }
}
