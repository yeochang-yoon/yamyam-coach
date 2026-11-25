package dev.yamyam.service;

import dev.yamyam.domain.Food;
import dev.yamyam.domain.Meal;
import dev.yamyam.domain.Nutrition;
import dev.yamyam.domain.User;
import dev.yamyam.repository.Repository;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class CoachService {

    private final Repository repository;
    private final FoodCatalog catalog;

    public CoachService(Repository repository, FoodCatalog catalog) {
        this.repository = repository;
        this.catalog = catalog;
    }

    public synchronized User.Profile register(String username, String password, String name,
        double height, double weight, String conditions) {
        if (username == null || !username.matches("[A-Za-z0-9_]{3,24}")) {
            throw new IllegalArgumentException("아이디는 영문·숫자·밑줄 3~24자여야 합니다.");
        }
        name = text(name, 40, false);
        range(height, 50, 250, "키");
        range(weight, 10, 500, "몸무게");
        conditions = text(conditions, 300, true);
        if (repository.userByUsername(username).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        User user = new User(UUID.randomUUID().toString(), username, Passwords.hash(password), name,
            height, weight, conditions, "ACTIVE");
        repository.saveUser(user);
        return user.profile();
    }

    public User.Profile login(String username, String password) {
        Optional<User> found = repository.userByUsername(username);
        if (found.isEmpty()) {
            throw new IllegalArgumentException("아이디 또는 비밀번호를 확인하세요.");
        }
        User user = found.get();
        if (!"ACTIVE".equals(user.getStatus()) || !Passwords.matches(password,
            user.getPasswordHash())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호를 확인하세요.");
        }
        return user.profile();
    }

    public User.Profile profile(String userId) {
        return user(userId).profile();
    }

    private User user(String id) {
        Optional<User> found = repository.userById(id);
        if (found.isEmpty()) {
            throw new IllegalArgumentException("회원을 찾을 수 없습니다.");
        }
        return found.get();
    }

    public User.Profile updateProfile(String userId, String name, double height, double weight,
        String conditions) {
        range(height, 50, 250, "키");
        range(weight, 10, 500, "몸무게");
        User old = user(userId);
        User next = new User(old.getId(), old.getUsername(), old.getPasswordHash(),
            text(name, 40, false), height, weight, text(conditions, 300, true), old.getStatus());
        repository.saveUser(next);
        return next.profile();
    }

    public void deleteAccount(String id, String password) {
        if (!Passwords.matches(password, user(id).getPasswordHash())) {
            throw new IllegalArgumentException("비밀번호를 확인하세요.");
        }
        repository.deleteUser(id);
    }

    public List<Meal> meals(String userId, String date) {
        user(userId);
        if (date != null) {
            date(date);
        }
        return repository.meals(userId, date);
    }

    public Meal meal(String userId, String id) {
        Optional<Meal> found = repository.meal(userId, id);
        if (found.isEmpty()) {
            throw new IllegalArgumentException("식단을 찾을 수 없습니다.");
        }
        return found.get();
    }

    public Meal saveMeal(String userId, String id, String date, String type, String note,
        List<Meal.Item> items) {
        user(userId);
        date(date);
        if (!Arrays.asList("아침", "점심", "저녁", "간식").contains(type)) {
            throw new IllegalArgumentException("식사 구분을 선택하세요.");
        }
        if (items == null || items.isEmpty() || items.size() > 30) {
            throw new IllegalArgumentException("음식은 1~30개 선택하세요.");
        }
        for (Meal.Item item : items) {
            if (item == null) {
                throw new IllegalArgumentException("음식 항목이 없습니다.");
            }
            Food food = catalog.get(item.getFoodId());
            if (!food.getBasisUnit().equals(item.getUnit())) {
                throw new IllegalArgumentException(
                    food.getName() + "의 섭취 단위는 " + food.getBasisUnit() + "입니다.");
            }
            double amount = item.getAmount();
            if (!Double.isFinite(amount) || amount <= 0 || amount > 5000) {
                throw new IllegalArgumentException("섭취량은 0보다 크고 5000 이하여야 합니다.");
            }
        }
        if (id != null) {
            meal(userId, id);
        }
        Meal meal = new Meal(id == null ? UUID.randomUUID().toString() : id, userId, date, type,
            text(note, 500, true), items);
        repository.saveMeal(meal);
        return meal;
    }

    public void deleteMeal(String userId, String id) {
        meal(userId, id);
        repository.deleteMeal(userId, id);
    }

    public Nutrition nutrition(Meal meal) {
        Nutrition total = Nutrition.ZERO;
        for (Meal.Item item : meal.getItems()) {
            total = total.add(nutrition(item));
        }
        return total;
    }

    private Nutrition nutrition(Meal.Item item) {
        Food food = catalog.get(item.getFoodId());
        return food.at(item.getAmount());
    }

    public Summary summary(String userId, String date) {
        date(date);
        user(userId);
        List<Meal> meals = repository.meals(userId, date);
        Nutrition total = Nutrition.ZERO;
        Map<String, Double> caloriesByFood = new HashMap<>();
        for (Meal meal : meals) {
            for (Meal.Item item : meal.getItems()) {
                Nutrition eaten = nutrition(item);
                total = total.add(eaten);

                String code = item.getFoodId();
                double calories = caloriesByFood.getOrDefault(code, 0.0);
                caloriesByFood.put(code, calories + eaten.getCalories());
            }
        }

        List<FoodContribution> contributions = new ArrayList<>();
        for (Map.Entry<String, Double> entry : caloriesByFood.entrySet()) {
            Food food = catalog.get(entry.getKey());
            contributions.add(new FoodContribution(food, entry.getValue()));
        }
        contributions.sort(new Comparator<FoodContribution>() {
            @Override
            public int compare(FoodContribution first, FoodContribution second) {
                int byCalories = Double.compare(second.getCalories(), first.getCalories());
                if (byCalories != 0) {
                    return byCalories;
                }
                return first.getFood().getFoodCode().compareTo(second.getFood().getFoodCode());
            }
        });
        return new Summary(date, total, meals.size(), contributions);
    }

    public static final class Summary {

        private final String date;
        private final Nutrition total;
        private final int mealCount;
        private final List<FoodContribution> contributions;

        public Summary(String date, Nutrition total, int mealCount,
            List<FoodContribution> contributions) {
            this.date = date;
            this.total = total;
            this.mealCount = mealCount;
            this.contributions = Collections.unmodifiableList(new ArrayList<>(contributions));
        }

        public String getDate() {
            return date;
        }

        public Nutrition getTotal() {
            return total;
        }

        public int getMealCount() {
            return mealCount;
        }

        public List<FoodContribution> getContributions() {
            return contributions;
        }
    }

    public static final class FoodContribution {

        private final Food food;
        private final double calories;

        private FoodContribution(Food food, double calories) {
            this.food = food;
            this.calories = calories;
        }

        public Food getFood() {
            return food;
        }

        public double getCalories() {
            return calories;
        }
    }

    private static void date(String date) {
        try {
            if (!LocalDate.parse(date).toString().equals(date)) {
                throw new DateTimeException("format");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("날짜는 YYYY-MM-DD 형식이어야 합니다.");
        }
    }

    private static void range(double n, double min, double max, String label) {
        if (!Double.isFinite(n) || n < min || n > max) {
            throw new IllegalArgumentException(label + " 범위: " + min + "~" + max);
        }
    }

    private static String text(String value, int max, boolean blank) {
        String s = value == null ? "" : value.strip();
        if ((!blank && s.isEmpty()) || s.length() > max) {
            throw new IllegalArgumentException("입력 길이를 확인하세요. 최대 " + max + "자");
        }
        return s;
    }
}
