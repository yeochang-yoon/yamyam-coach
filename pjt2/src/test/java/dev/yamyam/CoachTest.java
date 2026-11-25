package dev.yamyam;

import static org.junit.jupiter.api.Assertions.*;

import dev.yamyam.domain.*;
import dev.yamyam.repository.*;
import dev.yamyam.service.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class CoachTest {
  private static final String BROWN_RICE = "D101-050000000-0001";
  private static final String WATERMELON_JUICE = "D220-716000000-0068";

  @TempDir Path dir;

  @Test
  void completeAccountAndMealLifecycleSurvivesReload() throws Exception {
    Path path = dir.resolve("state.json");
    FoodCatalog foods = new FoodCatalog();
    CoachService coach = new CoachService(new FileRepository(path), foods);
    User.Profile user = coach.register("tester", "password123", "테스터", 171, 66, "고혈압");
    assertEquals("고혈압", coach.profile(user.getId()).getConditions());
    assertThrows(
        IllegalArgumentException.class,
        () -> coach.register("tester", "password123", "중복", 171, 66, ""));
    assertThrows(IllegalArgumentException.class, () -> coach.login("tester", "incorrect"));
    Meal meal =
        coach.saveMeal(
            user.getId(),
            null,
            "2026-11-04",
            "점심",
            "기록",
            Arrays.asList(new Meal.Item(BROWN_RICE, 150, "g"),
                new Meal.Item(WATERMELON_JUICE, 200, "mL")));
    assertEquals(358, coach.summary(user.getId(), "2026-11-04").getTotal().getCalories(), 1e-9);
    assertEquals(5.71, coach.nutrition(meal).getProtein(), 1e-9);
    assertEquals(0, foods.get(WATERMELON_JUICE).getFat());
    assertFalse(Files.readString(path).contains("password123"));
    CoachService reloaded = new CoachService(new FileRepository(path), foods);
    assertEquals(user.getId(), reloaded.login("tester", "password123").getId());
    assertEquals(1, reloaded.meals(user.getId(), null).size());
    Meal saved = reloaded.meal(user.getId(), meal.getId());
    assertEquals(meal.getDate(), saved.getDate());
    assertEquals(meal.getType(), saved.getType());
    assertEquals(meal.getNote(), saved.getNote());
    assertEquals(meal.getItems().size(), saved.getItems().size());
    for (int i = 0; i < meal.getItems().size(); i++) {
      Meal.Item expected = meal.getItems().get(i);
      Meal.Item actual = saved.getItems().get(i);
      assertEquals(expected.getFoodId(), actual.getFoodId());
      assertEquals(expected.getAmount(), actual.getAmount());
      assertEquals(expected.getUnit(), actual.getUnit());
    }
    reloaded.saveMeal(
        user.getId(), meal.getId(), "2026-11-05", "저녁", "수정",
        Arrays.asList(new Meal.Item(BROWN_RICE, 100, "g")));
    assertEquals(0, reloaded.meals(user.getId(), "2026-11-04").size());
    assertEquals(172, reloaded.summary(user.getId(), "2026-11-05").getTotal().getCalories());
    reloaded.updateProfile(user.getId(), "새 이름", 180, 75, "");
    assertEquals(180, new FileRepository(path).userById(user.getId()).orElseThrow().profile().getHeight());
    reloaded.deleteMeal(user.getId(), meal.getId());
    assertTrue(reloaded.meals(user.getId(), null).isEmpty());
    reloaded.deleteAccount(user.getId(), "password123");
    assertThrows(IllegalArgumentException.class, () -> reloaded.login("tester", "password123"));
  }

  @Test
  void rejectsOtherOwnersAndInvalidQuantities() {
    CoachService c = new CoachService(new FileRepository(dir.resolve("state.json")), new FoodCatalog());
    User.Profile a = c.register("alice", "password123", "A", 168, 62, "");
    User.Profile b = c.register("bobby", "password123", "B", 178, 72, "");
    Meal m =
        c.saveMeal(a.getId(), null, "2026-11-04", "아침", "",
            Arrays.asList(new Meal.Item(BROWN_RICE, 100, "g")));
    assertThrows(IllegalArgumentException.class, () -> c.meal(b.getId(), m.getId()));
    assertThrows(IllegalArgumentException.class, () -> c.deleteMeal(b.getId(), m.getId()));
    assertThrows(
        IllegalArgumentException.class,
        () -> c.saveMeal(b.getId(), m.getId(), m.getDate(), m.getType(), "", m.getItems()));
    for (double amount : new double[] {0, -1, Double.NaN, Double.POSITIVE_INFINITY, 5001})
      assertThrows(
          IllegalArgumentException.class,
          () ->
              c.saveMeal(
                  a.getId(), null, m.getDate(), m.getType(), "",
                  Arrays.asList(new Meal.Item(BROWN_RICE, amount, "g"))));
    assertThrows(
        IllegalArgumentException.class,
        () -> c.saveMeal(a.getId(), null, m.getDate(), m.getType(), "",
            Arrays.asList(new Meal.Item(WATERMELON_JUICE, 100, "g"))));
    assertThrows(
        IllegalArgumentException.class,
        () -> c.saveMeal(a.getId(), null, "2026-02-30", m.getType(), "", m.getItems()));
    c.deleteAccount(a.getId(), "password123");
    assertTrue(new FileRepository(dir.resolve("state.json")).meals(a.getId(), null).isEmpty());
  }

  @Test
  void firstRunLoadsPreparedDataAndDoesNotRestoreDeletedRecords() {
    Path path = dir.resolve("state.json");
    FoodCatalog foods = new FoodCatalog();
    CoachService coach = new CoachService(new FileRepository(path), foods);
    assertTrue(foods.all().size() >= 5789);
    assertEquals(BROWN_RICE, foods.search(BROWN_RICE).get(0).getFoodCode());
    User.Profile first = coach.login("demo01", "DemoMeal01!");
    User.Profile second = coach.login("demo02", "DemoMeal02!");
    assertEquals("1", first.getId());
    assertEquals("2", second.getId());
    assertEquals(21, coach.meals(first.getId(), null).size());
    assertEquals(21, coach.meals(second.getId(), null).size());
    assertEquals("고혈압", second.getConditions());
    assertEquals(407.9, coach.nutrition(coach.meal(first.getId(), "1")).getCalories(), 1e-9);

    for (String userId : Arrays.asList(first.getId(), second.getId())) {
      for (Meal meal : coach.meals(userId, null)) {
        for (Meal.Item item : meal.getItems()) {
          assertEquals(foods.get(item.getFoodId()).getBasisUnit(), item.getUnit());
        }
        assertTrue(coach.nutrition(meal).getCalories() > 0);
      }
    }

    coach.deleteMeal(first.getId(), "1");
    CoachService reopened = new CoachService(new FileRepository(path), foods);
    assertEquals(20, reopened.meals(first.getId(), null).size());
    assertThrows(IllegalArgumentException.class, () -> reopened.meal(first.getId(), "1"));
    assertEquals(21, reopened.meals(second.getId(), null).size());
  }

  @Test
  void existingStateKeepsLegacyMealQuantities() throws Exception {
    Path path = dir.resolve("state.json");
    String state = "{"
        + "\"users\":{\"saved\":{\"id\":\"saved\",\"username\":\"saved_user\","
        + "\"name\":\"저장 회원\",\"conditions\":\"\",\"height\":173,\"weight\":68}},"
        + "\"meals\":{\"old\":{\"id\":\"old\",\"userId\":\"saved\",\"date\":\"2026-10-01\","
        + "\"type\":\"점심\",\"items\":[{\"foodId\":\"" + BROWN_RICE + "\",\"grams\":150}]}}"
        + "}";
    Files.writeString(path, state);
    CoachService coach = new CoachService(new FileRepository(path), new FoodCatalog());
    assertEquals("저장 회원", coach.profile("saved").getName());
    Meal meal = coach.meal("saved", "old");
    assertEquals("g", meal.getItems().get(0).getUnit());
    assertEquals(258, coach.nutrition(meal).getCalories(), 1e-9);
  }

  @Test
  void refusesCorruptFilesInsteadOfOverwritingThem() throws Exception {
    Path p = dir.resolve("broken.json");
    Files.writeString(p, "{broken");
    assertThrows(IllegalStateException.class, () -> new FileRepository(p));
    assertEquals("{broken", Files.readString(p));
  }
}
