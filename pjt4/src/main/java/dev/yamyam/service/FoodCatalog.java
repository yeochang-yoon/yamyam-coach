package dev.yamyam.service;

import com.google.gson.Gson;
import dev.yamyam.domain.Food;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class FoodCatalog {

    private final List<Food> foods;
    private final Map<String, Food> foodsByCode;
    private final List<Food> foodsByName;

    public FoodCatalog() {
        this(loadFoods());
    }

    public FoodCatalog(List<Food> foods) {
        List<Food> copy = new ArrayList<>(foods);
        Map<String, Food> byCode = new HashMap<>();
        for (Food food : copy) {
            food.validate();
            if (byCode.put(food.getFoodCode(), food) != null) {
                throw new IllegalArgumentException("중복된 식품코드입니다: " + food.getFoodCode());
            }
        }
        this.foods = Collections.unmodifiableList(copy);
        this.foodsByCode = byCode;

        List<Food> sortedFoods = new ArrayList<>(copy);
        sortedFoods.sort(new Comparator<Food>() {
            @Override
            public int compare(Food first, Food second) {
                int byName = searchName(first).compareTo(searchName(second));
                if (byName != 0) {
                    return byName;
                }
                return first.getFoodCode().compareTo(second.getFoodCode());
            }
        });
        this.foodsByName = sortedFoods;
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
        Food food = foodsByCode.get(id);
        if (food == null) {
            throw new IllegalArgumentException("음식을 찾을 수 없습니다: " + id);
        }
        return food;
    }

    public List<Food> search(String keyword) {
        String q = keyword == null ? "" : keyword.strip().toLowerCase(Locale.ROOT);
        List<Food> matches = new ArrayList<>();
        for (Food food : foods) {
            if (searchName(food).contains(q) || food.getFoodCode().equalsIgnoreCase(q)) {
                matches.add(food);
            }
        }
        return matches;
    }

    public List<Food> searchPrefix(String keyword) {
        String prefix = keyword == null ? "" : keyword.strip().toLowerCase(Locale.ROOT);
        int left = 0;
        int right = foodsByName.size();

        // 접두어보다 작지 않은 첫 음식의 위치를 찾는다.
        while (left < right) {
            int middle = left + (right - left) / 2;
            Food food = foodsByName.get(middle);
            if (searchName(food).compareTo(prefix) < 0) {
                left = middle + 1;
            } else {
                right = middle;
            }
        }

        List<Food> matches = new ArrayList<>();
        for (int i = left; i < foodsByName.size(); i++) {
            Food food = foodsByName.get(i);
            if (!searchName(food).startsWith(prefix)) {
                break;
            }
            matches.add(food);
        }
        return matches;
    }

    private static String searchName(Food food) {
        return food.getName().toLowerCase(Locale.ROOT);
    }

    private static final class FoodFile {

        private List<Food> foods;
    }
}
