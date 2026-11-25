package dev.yamyam;

import static org.junit.jupiter.api.Assertions.*;

import dev.yamyam.domain.Food;
import dev.yamyam.domain.Meal;
import dev.yamyam.domain.Nutrition;
import dev.yamyam.repository.FileRepository;
import dev.yamyam.service.CoachService;
import dev.yamyam.service.FoodCatalog;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AlgorithmTest {

    private static final String BROWN_RICE = "D101-050000000-0001";
    private static final String WATERMELON_JUICE = "D220-716000000-0068";
    private static final String TEA = "D220-752000000-0093";
    private static final String TEST_DATE = "2026-11-23";

    @TempDir Path dir;

    @Test
    void codeIndexMatchesEveryFoodAndRejectsDuplicates() {
        FoodCatalog catalog = new FoodCatalog();
        for (Food food : catalog.all()) {
            assertSame(food, catalog.get(food.getFoodCode()));
        }
        assertThrows(IllegalArgumentException.class, () -> catalog.get("UNKNOWN"));
        Food duplicate = catalog.get(BROWN_RICE);
        assertThrows(IllegalArgumentException.class,
            () -> new FoodCatalog(Arrays.asList(duplicate, duplicate)));
    }

    @Test
    void prefixSearchMatchesRealCatalogAndOrdersDuplicateNamesByCode() {
        FoodCatalog catalog = new FoodCatalog();
        Set<String> expected = new HashSet<>();
        for (Food food : catalog.all()) {
            if (food.getName().startsWith("닭")) {
                expected.add(food.getFoodCode());
            }
        }
        assertFalse(expected.isEmpty());
        List<Food> results = catalog.searchPrefix("닭");
        Set<String> actual = new HashSet<>();
        for (Food food : results) {
            actual.add(food.getFoodCode());
        }
        assertEquals(expected, actual);
        assertEquals(expected.size(), results.size());
        assertTrue(catalog.searchPrefix("일치하는 음식이 없는 이름").isEmpty());

        FoodCatalog small = new FoodCatalog(Arrays.asList(
            new Food("A3", "apple tea", 100, "mL", 40, 0, 10, 0),
            new Food("A2", "Apple", 100, "g", 50, 0, 12, 0),
            new Food("B1", "Banana", 100, "g", 80, 1, 20, 0),
            new Food("A1", "Apple", 100, "g", 50, 0, 12, 0)));
        List<String> codes = new ArrayList<>();
        for (Food food : small.searchPrefix(" APP ")) {
            codes.add(food.getFoodCode());
        }
        assertEquals(Arrays.asList("A1", "A2", "A3"), codes);
        assertEquals(4, small.searchPrefix("").size());
        assertTrue(small.searchPrefix("tea").isEmpty());
        assertEquals("A3", small.search("tea").get(0).getFoodCode());
        assertTrue(new FoodCatalog(Collections.emptyList()).searchPrefix("apple").isEmpty());
    }

    @Test
    void dailyAnalysisGroupsEqualFoodsInCalorieOrderAndKeepsZeroValues() {
        CoachService coach = new CoachService(
            new FileRepository(dir.resolve("state.json")), new FoodCatalog());
        Meal breakfast = coach.saveMeal("1", null, TEST_DATE, "아침", "",
            Arrays.asList(new Meal.Item(BROWN_RICE, 100, "g"),
                new Meal.Item(WATERMELON_JUICE, 120, "mL"), new Meal.Item(TEA, 150, "mL")));
        Meal lunch = coach.saveMeal("1", null, TEST_DATE, "점심", "",
            Arrays.asList(new Meal.Item(BROWN_RICE, 50, "g"),
                new Meal.Item(WATERMELON_JUICE, 100, "mL")));
        coach.saveMeal("2", null, TEST_DATE, "저녁", "",
            Collections.singletonList(new Meal.Item(BROWN_RICE, 800, "g")));

        CoachService.Summary summary = coach.summary("1", TEST_DATE);
        assertEquals(2, summary.getMealCount());
        Nutrition total = summary.getTotal();
        Nutrition expected = coach.nutrition(breakfast).add(coach.nutrition(lunch));
        assertEquals(368, total.getCalories(), 1e-9);
        assertEquals(expected.getProtein(), total.getProtein(), 1e-9);
        assertEquals(expected.getCarbs(), total.getCarbs(), 1e-9);
        assertEquals(expected.getFat(), total.getFat(), 1e-9);

        List<CoachService.FoodContribution> foods = summary.getContributions();
        assertEquals(3, foods.size());
        assertContribution(foods.get(0), BROWN_RICE, 258);
        assertContribution(foods.get(1), WATERMELON_JUICE, 110);
        assertContribution(foods.get(2), TEA, 0);

        CoachService.Summary empty = coach.summary("1", "2026-11-24");
        assertEquals(0, empty.getTotal().getCalories());
        assertTrue(empty.getContributions().isEmpty());
    }

    private void assertContribution(CoachService.FoodContribution food, String code, double kcal) {
        assertEquals(code, food.getFood().getFoodCode());
        assertEquals(kcal, food.getCalories(), 1e-9);
    }
}
