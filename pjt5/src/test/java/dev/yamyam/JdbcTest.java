package dev.yamyam;

import dev.yamyam.domain.Meal;
import dev.yamyam.domain.User;
import dev.yamyam.repository.JdbcRepository;
import dev.yamyam.service.CoachService;
import dev.yamyam.service.FoodCatalog;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "MYSQL_TEST_URL", matches = "jdbc:mysql://.+")
class JdbcTest {

    private static final String RICE = "D101-050000000-0001";
    private static final String JUICE = "D220-716000000-0068";
    private static final String DATE = "2025-04-22";
    private static final String PASSWORD = "MealTest123!";
    private static final String URL = System.getenv("MYSQL_TEST_URL");
    private static final String USER = System.getenv("MYSQL_TEST_USER");
    private static final String DB_PASSWORD = System.getenv("MYSQL_TEST_PASSWORD");

    private JdbcRepository repository;
    private FoodCatalog catalog;
    private CoachService coach;

    @BeforeEach
    void connect() {
        repository = new JdbcRepository(URL, USER, DB_PASSWORD);
        catalog = new FoodCatalog(repository.foods());
        coach = new CoachService(repository, catalog);
    }

    @Test
    void loadsPreparedFoodsAndDietWithTheSameNutrition() {
        assertTrue(catalog.all().size() >= 5789);
        assertEquals(RICE, catalog.search(RICE).get(0).getFoodCode());
        assertFalse(catalog.searchPrefix("닭").isEmpty());
        assertEquals(0, catalog.get(JUICE).getFat());
        assertEquals("mL", catalog.get(JUICE).getBasisUnit());
        User.Profile demo = coach.login("demo01", "DemoMeal01!");
        assertEquals(21, coach.meals(demo.getId(), null).size());
        CoachService.Summary summary = coach.summary(demo.getId(), "2025-04-21");
        assertEquals(3, summary.getMealCount());
        assertEquals(1528.05, summary.getTotal().getCalories(), 0.000001);
    }

    @Test
    void jdbcLifecyclePreservesAmountsAndChecksOwnership() {
        User.Profile owner = register();
        User.Profile other = register();
        try {
            assertThrows(IllegalArgumentException.class, () -> coach.login(owner.getUsername(), "wrong"));
            assertEquals(owner.getId(), coach.login(owner.getUsername(), PASSWORD).getId());
            coach.updateProfile(owner.getId(), "새 이름", 180, 75.5, "테스트");
            Meal meal = coach.saveMeal(owner.getId(), null, DATE, "아침", "두 가지 단위",
                Arrays.asList(new Meal.Item(RICE, 0.5, "g"), new Meal.Item(JUICE, 200, "mL")));

            CoachService reopened = new CoachService(new JdbcRepository(URL, USER, DB_PASSWORD), catalog);
            assertEquals("새 이름", reopened.profile(owner.getId()).getName());
            assertEquals(75.5, reopened.profile(owner.getId()).getWeight());
            Meal saved = reopened.meals(owner.getId(), DATE).get(0);
            assertEquals(meal.getId(), saved.getId());
            assertEquals(0.5, saved.getItems().get(0).getAmount());
            assertEquals("mL", saved.getItems().get(1).getUnit());
            assertEquals(100.86, reopened.nutrition(saved).getCalories(), 0.000001);
            assertThrows(IllegalArgumentException.class, () -> reopened.meal(other.getId(), meal.getId()));
            assertThrows(IllegalArgumentException.class, () -> reopened.deleteMeal(other.getId(), meal.getId()));
            assertThrows(IllegalArgumentException.class, () -> reopened.saveMeal(other.getId(),
                meal.getId(), DATE, "점심", "", saved.getItems()));

            reopened.saveMeal(owner.getId(), meal.getId(), "2025-04-23", "저녁", "수정",
                Collections.singletonList(new Meal.Item(JUICE, 300, "mL")));
            assertTrue(reopened.meals(owner.getId(), DATE).isEmpty());
            Meal edited = reopened.meal(owner.getId(), meal.getId());
            assertEquals("수정", edited.getNote());
            assertEquals(1, edited.getItems().size());
            assertEquals(150, reopened.nutrition(edited).getCalories(), 0.000001);
            reopened.deleteMeal(owner.getId(), meal.getId());
            assertTrue(repository.meal(owner.getId(), meal.getId()).isEmpty());
        } finally {
            repository.deleteUser(owner.getId());
            repository.deleteUser(other.getId());
        }
    }

    @Test
    void foreignKeyFailureRollsBackTheWholeMeal() {
        User.Profile owner = register();
        try {
            String newId = UUID.randomUUID().toString();
            Meal invalid = new Meal(newId, owner.getId(), DATE, "아침", "저장 실패",
                Arrays.asList(new Meal.Item(RICE, 150, "g"), new Meal.Item("UNKNOWN", 100, "g")));
            assertThrows(IllegalStateException.class, () -> repository.insertMeal(invalid));
            assertTrue(repository.meal(owner.getId(), newId).isEmpty());

            Meal original = coach.saveMeal(owner.getId(), null, DATE, "아침", "원래 메모",
                Collections.singletonList(new Meal.Item(JUICE, 120, "mL")));
            Meal failedEdit = new Meal(original.getId(), owner.getId(), "2025-04-24", "저녁",
                "수정 실패", invalid.getItems());
            assertThrows(IllegalStateException.class, () -> repository.updateMeal(failedEdit));
            Meal saved = coach.meal(owner.getId(), original.getId());
            assertEquals(DATE, saved.getDate());
            assertEquals("원래 메모", saved.getNote());
            assertEquals(JUICE, saved.getItems().get(0).getFoodId());
            assertEquals(1, saved.getItems().size());
        } finally {
            repository.deleteUser(owner.getId());
        }
    }

    @Test
    void mysqlEnforcesUniqueUsernameAndCascadesAccountRemoval() throws SQLException {
        User.Profile owner = register();
        try {
            User duplicate = new User(UUID.randomUUID().toString(), owner.getUsername(),
                "sample", "중복", 170, 68, "", "ACTIVE");
            assertThrows(IllegalArgumentException.class, () -> repository.insertUser(duplicate));
            Meal meal = coach.saveMeal(owner.getId(), null, DATE, "점심", "",
                Collections.singletonList(new Meal.Item(RICE, 150, "g")));
            assertEquals(1, mealItemCount(meal.getId()));
            String savedHash = repository.userById(owner.getId()).orElseThrow().getPasswordHash();
            assertFalse(savedHash.contains(PASSWORD));
            coach.deleteAccount(owner.getId(), PASSWORD);
            assertThrows(IllegalArgumentException.class, () -> coach.login(owner.getUsername(), PASSWORD));
            assertTrue(repository.meal(owner.getId(), meal.getId()).isEmpty());
            assertEquals(0, mealItemCount(meal.getId()));
        } finally {
            repository.deleteUser(owner.getId());
        }
    }

    private User.Profile register() {
        String username = "jdbc_" + UUID.randomUUID().toString().substring(0, 8);
        return coach.register(username, PASSWORD, "테스트 회원", 173, 68, "");
    }

    private int mealItemCount(String id) throws SQLException {
        try (Connection connection = DriverManager.getConnection(URL, USER, DB_PASSWORD);
            PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM meal_items WHERE meal_id = ?")) {
            statement.setString(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                rows.next();
                return rows.getInt(1);
            }
        }
    }
}
