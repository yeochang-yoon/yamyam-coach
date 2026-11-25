package dev.yamyam.console;

import dev.yamyam.domain.Food;
import dev.yamyam.domain.Meal;
import dev.yamyam.domain.Nutrition;
import dev.yamyam.domain.User;
import dev.yamyam.repository.FileRepository;
import dev.yamyam.service.CoachService;
import dev.yamyam.service.FoodCatalog;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public final class ConsoleApp {

    private static final int SEARCH_LIMIT = 20;

    private final Scanner input = new Scanner(System.in, java.nio.charset.StandardCharsets.UTF_8);
    private final FoodCatalog foods = new FoodCatalog();
    private final CoachService coach = new CoachService(
        new FileRepository(Path.of("runtime/state.json")), foods);
    private String userId;

    public static void main(String[] args) {
        new ConsoleApp().run();
    }

    private void run() {
        System.out.println("냠냠코치 · 식단 기록과 영양 분석");
        while (true) {
            System.out.println(userId == null
                ? "1 가입 | 2 로그인 | 3 음식 검색 | 14 이름 시작 검색 | 0 종료"
                : "3 음식 검색 | 4 식단 작성 | 5 식단 목록 | 6 상세 | 7 수정 | 8 삭제 | 9 하루 분석"
                    + " | 10 내 정보 | 11 프로필 수정 | 12 탈퇴 | 13 로그아웃 | 14 이름 시작 검색 | 0 종료");
            String command = ask(">");
            if (command.equals("0")) {
                break;
            }
            try {
                if (command.equals("1") && userId == null) {
                    User.Profile profile = coach.register(ask("아이디"), password(), ask("이름"),
                        number("키(cm)"), number("몸무게(kg)"), ask("질환 (선택)"));
                    System.out.println(profile.getName() + "님, 가입했습니다.");
                } else if (command.equals("2") && userId == null) {
                    User.Profile profile = coach.login(ask("아이디"), password());
                    userId = profile.getId();
                    System.out.println(profile.getName() + "님, 로그인했습니다.");
                } else if (command.equals("3")) {
                    showFoods(foods.search(ask("음식명 또는 식품코드")));
                } else if (command.equals("14")) {
                    showFoods(foods.searchPrefix(ask("음식명 시작 부분")));
                } else {
                    if (userId == null) {
                        throw new IllegalArgumentException("먼저 로그인하세요.");
                    }
                    switch (command) {
                        case "4":
                            write(null);
                            break;
                        case "5":
                            listMeals();
                            break;
                        case "6":
                            showMeal(coach.meal(userId, ask("식단 ID")));
                            break;
                        case "7":
                            write(ask("수정할 식단 ID"));
                            break;
                        case "8":
                            coach.deleteMeal(userId, ask("삭제할 식단 ID"));
                            System.out.println("식단을 삭제했습니다.");
                            break;
                        case "9":
                            showSummary();
                            break;
                        case "10":
                            showProfile(coach.profile(userId));
                            break;
                        case "11":
                            showProfile(coach.updateProfile(userId, ask("이름"), number("키(cm)"),
                                number("몸무게(kg)"), ask("질환 (선택)")));
                            break;
                        case "12":
                            coach.deleteAccount(userId, password());
                            userId = null;
                            System.out.println("탈퇴했습니다.");
                            break;
                        case "13":
                            userId = null;
                            System.out.println("로그아웃했습니다.");
                            break;
                        default:
                            System.out.println("메뉴 번호를 확인하세요.");
                            break;
                    }
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("안내: " + e.getMessage());
            }
        }
    }

    private void showFoods(List<Food> matches) {
        if (matches.isEmpty()) {
            System.out.println("검색 결과가 없습니다.");
        }
        for (int i = 0; i < Math.min(matches.size(), SEARCH_LIMIT); i++) {
            Food food = matches.get(i);
            System.out.printf("%s | %s | %d %s 기준 %.1f kcal%n", food.getFoodCode(), food.getName(),
                food.getBasisAmount(), food.getBasisUnit(), food.getCalories());
        }
        if (matches.size() > SEARCH_LIMIT) {
            System.out.printf("검색 결과 %d건 중 %d건입니다. 음식명을 더 구체적으로 입력하세요.%n", matches.size(),
                SEARCH_LIMIT);
        }
    }

    private void listMeals() {
        List<Meal> meals = coach.meals(userId, null);
        if (meals.isEmpty()) {
            System.out.println("등록된 식단이 없습니다.");
        }
        for (Meal meal : meals) {
            System.out.printf("%s | %s | %s | 음식 %d개%n", meal.getId(), meal.getDate(),
                meal.getType(), meal.getItems().size());
        }
    }

    private void showMeal(Meal meal) {
        System.out.printf("%s | %s | %s%n", meal.getId(), meal.getDate(), meal.getType());
        for (Meal.Item item : meal.getItems()) {
            Food food = foods.get(item.getFoodId());
            System.out.printf("%s (%s) %.2f %s%n", food.getName(), food.getFoodCode(),
                item.getAmount(), item.getUnit());
        }
        if (!meal.getNote().isBlank()) {
            System.out.println("메모: " + meal.getNote());
        }
        showNutrition(coach.nutrition(meal));
    }

    private void showSummary() {
        CoachService.Summary summary = coach.summary(userId, ask("날짜 YYYY-MM-DD"));
        System.out.printf("%s | 식단 %d건%n", summary.getDate(), summary.getMealCount());
        showNutrition(summary.getTotal());
        if (summary.getContributions().isEmpty()) {
            System.out.println("이 날짜에 기록한 음식이 없습니다.");
            return;
        }

        System.out.println("음식별 섭취 열량");
        for (CoachService.FoodContribution contribution : summary.getContributions()) {
            Food food = contribution.getFood();
            System.out.printf("%s (%s) | %.1f kcal%n",
                food.getName(), food.getFoodCode(), contribution.getCalories());
        }
    }

    private void showNutrition(Nutrition nutrition) {
        System.out.printf("열량 %.1f kcal | 탄수화물 %.1f g | 단백질 %.1f g | 지방 %.1f g%n",
            nutrition.getCalories(), nutrition.getCarbs(), nutrition.getProtein(),
            nutrition.getFat());
    }

    private void showProfile(User.Profile profile) {
        System.out.printf("%s (%s) | 키 %.1f cm | 몸무게 %.1f kg%n", profile.getName(),
            profile.getUsername(), profile.getHeight(), profile.getWeight());
        if (!profile.getConditions().isBlank()) {
            System.out.println("질환: " + profile.getConditions());
        }
    }

    private void write(String id) {
        if (id != null) {
            coach.meal(userId, id);
        }
        String date = ask("날짜 YYYY-MM-DD");
        String type = ask("아침/점심/저녁/간식");
        String note = ask("메모 (선택)");
        List<Meal.Item> items = new ArrayList<>();
        do {
            Food food = foods.get(ask("식품코드"));
            System.out.println(food.getName());
            double amount = number("섭취량(" + food.getBasisUnit() + ")");
            items.add(new Meal.Item(food.getFoodCode(), amount, food.getBasisUnit()));
        } while (ask("음식 추가? y/N").equalsIgnoreCase("y"));
        Meal meal = coach.saveMeal(userId, id, date, type, note, items);
        System.out.println("식단을 저장했습니다. ID " + meal.getId());
    }

    private String ask(String label) {
        System.out.print(label + ": ");
        if (!input.hasNextLine()) {
            return "0";
        }
        return input.nextLine().strip();
    }

    private double number(String label) {
        try {
            return Double.parseDouble(ask(label));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("숫자를 입력하세요.");
        }
    }

    private String password() {
        return System.console() == null ? ask("비밀번호 (터미널 입력이 표시됩니다)")
            : new String(System.console().readPassword("비밀번호: "));
    }
}
