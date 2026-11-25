package dev.yamyam.service;

import dev.yamyam.domain.Food;
import dev.yamyam.domain.Meal;
import dev.yamyam.domain.Nutrition;
import dev.yamyam.domain.User;
import dev.yamyam.dto.AccountRequest;
import dev.yamyam.dto.MealRequest;
import dev.yamyam.repository.FoodMapper;
import dev.yamyam.repository.MealMapper;
import dev.yamyam.repository.UserMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CoachService {

    private final UserMapper users;
    private final FoodMapper foods;
    private final MealMapper meals;

    public CoachService(UserMapper users, FoodMapper foods, MealMapper meals) {
        this.users = users;
        this.foods = foods;
        this.meals = meals;
    }

    public User register(AccountRequest.Register request) {
        String username = text(request.getUsername(), 24, false);
        if (!username.matches("[A-Za-z0-9_]{3,24}")) {
            throw new IllegalArgumentException("아이디는 영문·숫자·밑줄 3~24자여야 합니다.");
        }
        User user = withProfile(UUID.randomUUID().toString(), username,
            Passwords.hash(request.getPassword()), request);
        users.insert(user);
        return user;
    }

    public User authenticate(String username, String password) {
        if (username == null || !username.strip().matches("[A-Za-z0-9_]{3,24}")) {
            return null;
        }
        User user = users.findByUsername(username.strip());
        if (!isActive(user) || !Passwords.matches(password, user.getPasswordHash())) {
            return null;
        }
        return user;
    }

    public User profile(String userId) {
        User user = users.findById(userId);
        if (!isActive(user)) {
            throw new NoSuchElementException("회원을 찾을 수 없습니다.");
        }
        return user;
    }

    public User updateProfile(User current, AccountRequest.Profile request) {
        User updated = withProfile(current.getId(), current.getUsername(),
            current.getPasswordHash(), request);
        if (users.updateProfile(updated) == 0) {
            profile(current.getId());
        }
        return updated;
    }

    public void withdraw(User user, String password) {
        if (!Passwords.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("비밀번호를 확인하세요.");
        }
        if (users.delete(user.getId()) == 0) {
            throw new NoSuchElementException("회원을 찾을 수 없습니다.");
        }
    }

    private User withProfile(String id, String username, String hash, AccountRequest.Profile request) {
        range(request.getHeight(), 50, 250, "키");
        range(request.getWeight(), 10, 500, "몸무게");
        return new User(id, username, hash, text(request.getName(), 40, false),
            request.getHeight(), request.getWeight(), text(request.getConditions(), 300, true), "ACTIVE");
    }

    private boolean isActive(User user) {
        return user != null && "ACTIVE".equals(user.getStatus());
    }

    public List<Food> searchFoods(String query, String mode, int page, int size) {
        if (!"contains".equals(mode) && !"prefix".equals(mode)) {
            throw new IllegalArgumentException("검색 방식은 contains 또는 prefix여야 합니다.");
        }
        if (page < 1 || size < 1 || size > 100) {
            throw new IllegalArgumentException("페이지는 1부터 시작하고 크기는 1~100이어야 합니다.");
        }
        return foods.search(text(query, 150, true), "prefix".equals(mode), size, (long) (page - 1) * size);
    }

    public List<Meal> meals(String userId, LocalDate date) {
        if (date != null) {
            validateDate(date);
        }
        return meals.findAll(userId, date);
    }

    public Meal meal(String userId, String id) {
        Meal meal = meals.findById(userId, id);
        if (meal == null) {
            throw missingMeal();
        }
        return meal;
    }

    @Transactional
    public Meal saveMeal(String userId, String id, MealRequest request) {
        validateDate(request.getDate());
        if (!Arrays.asList("아침", "점심", "저녁", "간식").contains(request.getType())) {
            throw new IllegalArgumentException("식사 구분을 선택하세요.");
        }
        String note = text(request.getNote(), 500, true);
        List<Meal.Item> items = makeItems(request.getItems());
        String mealId = id == null ? UUID.randomUUID().toString() : id;
        Meal meal = new Meal(mealId, userId, request.getDate(), request.getType(), note, items);

        if (id == null) {
            meals.insert(meal);
        } else {
            if (meals.lockById(userId, id) == null) {
                throw missingMeal();
            }
            meals.update(meal);
            meals.deleteItems(id);
        }
        meals.insertItems(meal);
        return meal;
    }

    private List<Meal.Item> makeItems(List<MealRequest.Item> requested) {
        if (requested == null || requested.isEmpty() || requested.size() > 30) {
            throw new IllegalArgumentException("음식은 1~30개 선택하세요.");
        }
        List<String> itemCodes = new ArrayList<>();
        Set<String> codes = new LinkedHashSet<>();
        for (MealRequest.Item item : requested) {
            if (item == null) {
                throw new IllegalArgumentException("음식 항목이 없습니다.");
            }
            String code = text(item.getFoodCode(), 36, false);
            itemCodes.add(code);
            codes.add(code);
        }

        Map<String, Food> foodsByCode = new HashMap<>();
        for (Food food : foods.findByCodes(codes)) {
            foodsByCode.put(food.getFoodCode(), food);
        }
        List<Meal.Item> items = new ArrayList<>();
        for (int i = 0; i < requested.size(); i++) {
            MealRequest.Item item = requested.get(i);
            String code = itemCodes.get(i);
            Food food = foodsByCode.get(code);
            if (food == null) {
                throw new IllegalArgumentException("음식을 찾을 수 없습니다: " + code);
            }
            if (item.getUnit() != null && !food.getBasisUnit().equals(item.getUnit())) {
                throw new IllegalArgumentException(food.getName() + "의 섭취 단위는 " + food.getBasisUnit() + "입니다.");
            }
            double amount = item.getAmount();
            if (!Double.isFinite(amount) || amount <= 0 || amount > 5000) {
                throw new IllegalArgumentException("섭취량은 0보다 크고 5000 이하여야 합니다.");
            }
            items.add(new Meal.Item(i + 1, food, amount));
        }
        return items;
    }

    public void deleteMeal(String userId, String id) {
        if (meals.delete(userId, id) == 0) {
            throw missingMeal();
        }
    }

    public Nutrition nutrition(Meal meal) {
        Nutrition total = Nutrition.ZERO;
        for (Meal.Item item : meal.getItems()) {
            total = total.add(item.getFood().at(item.getAmount()));
        }
        return total;
    }

    public Summary summary(String userId, LocalDate date) {
        validateDate(date);
        List<Meal> dailyMeals = meals.findAll(userId, date);
        Nutrition total = Nutrition.ZERO;
        Map<String, FoodContribution> contributionsByCode = new HashMap<>();

        for (Meal meal : dailyMeals) {
            for (Meal.Item item : meal.getItems()) {
                Food food = item.getFood();
                Nutrition eaten = food.at(item.getAmount());
                total = total.add(eaten);

                FoodContribution contribution = contributionsByCode.get(food.getFoodCode());
                if (contribution == null) {
                    contribution = new FoodContribution(food);
                    contributionsByCode.put(food.getFoodCode(), contribution);
                }
                contribution.calories += eaten.getCalories();
            }
        }
        List<FoodContribution> contributions = new ArrayList<>(contributionsByCode.values());
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
        return new Summary(date, total, dailyMeals.size(), contributions);
    }

    private NoSuchElementException missingMeal() {
        return new NoSuchElementException("식단을 찾을 수 없습니다.");
    }

    private void validateDate(LocalDate date) {
        if (date == null || date.getYear() < 1000 || date.getYear() > 9999) {
            throw new IllegalArgumentException("날짜를 YYYY-MM-DD 형식으로 입력하세요.");
        }
    }

    private void range(double value, double min, double max, String name) {
        if (!Double.isFinite(value) || value < min || value > max) {
            throw new IllegalArgumentException(name + " 범위: " + min + "~" + max);
        }
    }

    private String text(String value, int max, boolean blank) {
        String trimmed = value == null ? "" : value.strip();
        if ((!blank && trimmed.isEmpty()) || trimmed.length() > max) {
            throw new IllegalArgumentException("입력 길이를 확인하세요. 최대 " + max + "자");
        }
        return trimmed;
    }

    public static final class Summary {

        private final LocalDate date;
        private final Nutrition total;
        private final int mealCount;
        private final List<FoodContribution> contributions;

        public Summary(LocalDate date, Nutrition total, int mealCount, List<FoodContribution> contributions) {
            this.date = date;
            this.total = total;
            this.mealCount = mealCount;
            this.contributions = contributions;
        }

        public LocalDate getDate() {
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
        private double calories;

        private FoodContribution(Food food) {
            this.food = food;
        }

        public Food getFood() {
            return food;
        }

        public double getCalories() {
            return calories;
        }
    }
}
