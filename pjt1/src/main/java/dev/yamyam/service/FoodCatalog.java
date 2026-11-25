package dev.yamyam.service;

import com.google.gson.Gson;
import dev.yamyam.domain.Food;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class FoodCatalog {

    private final List<Food> foods;

    public FoodCatalog() {
        this(loadFoods());
    }

    public FoodCatalog(List<Food> foods) {
        List<Food> copy = new ArrayList<>(foods);
        for (Food food : copy) {
            food.validate();
        }
        this.foods = Collections.unmodifiableList(copy);
    }

    private static List<Food> loadFoods() {
        String resource = "/data/foods.json";
        InputStream stream = FoodCatalog.class.getResourceAsStream(resource);
        if (stream == null) {
            throw new IllegalStateException("음식 목록 파일이 없습니다: " + resource);
        }
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            FoodFile catalog = new Gson().fromJson(reader, FoodFile.class);
            return catalog.foods;
        } catch (IOException e) {
            throw new IllegalStateException("음식 목록을 읽지 못했습니다: " + resource, e);
        }
    }

    public List<Food> all() {
        return foods;
    }

    public Food get(String id) {
        for (Food food : foods) {
            if (food.getFoodCode().equals(id)) {
                return food;
            }
        }
        throw new IllegalArgumentException("음식을 찾을 수 없습니다: " + id);
    }

    public List<Food> search(String keyword) {
        String q = keyword == null ? "" : keyword.strip().toLowerCase(Locale.ROOT);
        List<Food> matches = new ArrayList<>();
        for (Food food : foods) {
            if (food.getName().toLowerCase(Locale.ROOT).contains(q)
                || food.getFoodCode().equalsIgnoreCase(q)) {
                matches.add(food);
            }
        }
        return matches;
    }

    private static final class FoodFile {

        private List<Food> foods;
    }
}
