package dev.yamyam.controller;

import dev.yamyam.domain.User;
import dev.yamyam.dto.AccountRequest;
import dev.yamyam.dto.UserResponse;
import dev.yamyam.service.CoachService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class AccountController {

    private final CoachService coach;
    private final SessionUser sessionUser;

    public AccountController(CoachService coach, SessionUser sessionUser) {
        this.coach = coach;
        this.sessionUser = sessionUser;
    }

    @PostMapping(path = "/users", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<UserResponse> register(@RequestBody AccountRequest.Register body) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new UserResponse(coach.register(body)));
    }

    @PostMapping(path = "/session", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<UserResponse> login(@RequestBody AccountRequest.Login body,
        HttpServletRequest request) {
        User user = coach.authenticate(body.getUsername(), body.getPassword());
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호를 확인하세요.");
        }
        sessionUser.login(request, user.getId());
        return ResponseEntity.created(URI.create(request.getContextPath() + "/api/session"))
            .body(new UserResponse(user));
    }

    @GetMapping("/session")
    public ResponseEntity<UserResponse> session(HttpServletRequest request) {
        return ResponseEntity.ok(new UserResponse(sessionUser.require(request)));
    }

    @DeleteMapping("/session")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        sessionUser.logout(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/me")
    public ResponseEntity<UserResponse> profile(HttpServletRequest request) {
        return ResponseEntity.ok(new UserResponse(sessionUser.require(request)));
    }

    @PutMapping(path = "/users/me", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserResponse> updateProfile(@RequestBody AccountRequest.Profile body,
        HttpServletRequest request) {
        User user = sessionUser.require(request);
        return ResponseEntity.ok(new UserResponse(coach.updateProfile(user, body)));
    }

    @DeleteMapping(path = "/users/me", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> withdraw(@RequestBody AccountRequest.Withdrawal body,
        HttpServletRequest request) {
        coach.withdraw(sessionUser.require(request), body.getPassword());
        sessionUser.logout(request);
        return ResponseEntity.noContent().build();
    }
}
