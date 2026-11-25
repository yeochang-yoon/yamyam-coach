package dev.yamyam.controller;

import dev.yamyam.domain.Meal;
import dev.yamyam.dto.MealRequest;
import dev.yamyam.dto.MealResponse;
import dev.yamyam.service.CoachService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/meals")
public class MealController {

    private final CoachService coach;
    private final SessionUser sessionUser;

    public MealController(CoachService coach, SessionUser sessionUser) {
        this.coach = coach;
        this.sessionUser = sessionUser;
    }

    @GetMapping
    public ResponseEntity<List<MealResponse>> meals(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        HttpServletRequest request) {
        String userId = sessionUser.require(request).getId();
        List<MealResponse> response = new ArrayList<>();
        for (Meal meal : coach.meals(userId, date)) {
            response.add(toResponse(meal));
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MealResponse> meal(@PathVariable String id, HttpServletRequest request) {
        return ResponseEntity.ok(toResponse(coach.meal(sessionUser.require(request).getId(), id)));
    }

    @GetMapping("/analysis")
    public ResponseEntity<CoachService.Summary> analysis(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        HttpServletRequest request) {
        return ResponseEntity.ok(coach.summary(sessionUser.require(request).getId(), date));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<MealResponse> create(@RequestBody MealRequest body,
        HttpServletRequest request) {
        Meal meal = coach.saveMeal(sessionUser.require(request).getId(), null, body);
        URI location = URI.create(request.getContextPath() + "/api/meals/" + meal.getId());
        return ResponseEntity.created(location).body(toResponse(meal));
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MealResponse> update(@PathVariable String id,
        @RequestBody MealRequest body,
        HttpServletRequest request) {
        Meal meal = coach.saveMeal(sessionUser.require(request).getId(), id, body);
        return ResponseEntity.ok(toResponse(meal));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> delete(@PathVariable String id, HttpServletRequest request) {
        coach.deleteMeal(sessionUser.require(request).getId(), id);
        return ResponseEntity.noContent().build();
    }

    private MealResponse toResponse(Meal meal) {
        return new MealResponse(meal, coach.nutrition(meal));
    }
}
