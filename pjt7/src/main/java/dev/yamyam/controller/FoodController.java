package dev.yamyam.controller;

import dev.yamyam.domain.Food;
import dev.yamyam.service.CoachService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FoodController {

    private final CoachService coach;

    public FoodController(CoachService coach) {
        this.coach = coach;
    }

    @GetMapping("/api/foods")
    public ResponseEntity<List<Food>> foods(
        @RequestParam(name = "q", defaultValue = "") String query,
        @RequestParam(defaultValue = "contains") String mode,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(coach.searchFoods(query, mode, page, size));
    }
}
