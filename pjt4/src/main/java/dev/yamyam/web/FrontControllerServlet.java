package dev.yamyam.web;

import com.google.gson.Gson;
import dev.yamyam.domain.Food;
import dev.yamyam.domain.Meal;
import dev.yamyam.domain.Nutrition;
import dev.yamyam.domain.User;
import dev.yamyam.repository.FileRepository;
import dev.yamyam.service.CoachService;
import dev.yamyam.service.FoodCatalog;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@WebServlet(urlPatterns = "/app/*", loadOnStartup = 1)
public final class FrontControllerServlet extends HttpServlet {

    private static final int FOOD_PAGE_SIZE = 20;
    private final Gson gson = new Gson();
    private FoodCatalog catalog;
    private CoachService coach;

    @Override
    public void init() throws ServletException {
        String configuredPath = System.getProperty("yamyam.pjt4.state");
        Path stateFile = Path.of(System.getProperty("catalina.base", "."), "yamyam-pjt4",
            "state.json");
        if (configuredPath != null && !configuredPath.isBlank()) {
            stateFile = Path.of(configuredPath);
        }
        try {
            catalog = new FoodCatalog();
            coach = new CoachService(new FileRepository(stateFile), catalog);
        } catch (IllegalStateException | IllegalArgumentException error) {
            throw new ServletException("냠냠코치 데이터를 불러오지 못했습니다.", error);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        String path = route(request);
        User.Profile profile = currentProfile(request);
        try {
            switch (path) {
                case "/login":
                case "/register":
                    if (profile == null) {
                        showAuth(request, response, path);
                    } else {
                        redirect(request, response, "/meals");
                    }
                    break;
                case "/foods":
                    showFoods(request, response, profile);
                    break;
                case "/food-options":
                    showFoodOptions(request, response);
                    break;
                case "/meals":
                case "/meal":
                case "/meal/new":
                case "/meal/edit":
                case "/profile":
                    if (profile == null) {
                        redirect(request, response, "/login");
                    } else {
                        showMemberPage(request, response, profile, path);
                    }
                    break;
                default:
                    showError(request, response, profile, 404, "요청한 페이지를 찾을 수 없습니다.");
                    break;
            }
        } catch (IllegalArgumentException error) {
            showError(request, response, profile, 400, error.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        String path = route(request);
        User.Profile profile = currentProfile(request);
        if (!validToken(request)) {
            showError(request, response, profile, 403, "페이지를 다시 열고 요청을 보내세요.");
            return;
        }
        try {
            if ("/login".equals(path) || "/register".equals(path)) {
                if (profile != null) {
                    redirect(request, response, "/meals");
                    return;
                }
                if ("/login".equals(path)) {
                    login(request, response);
                } else {
                    coach.register(input(request, "loginId"), request.getParameter("password"),
                        input(request, "name"), number(input(request, "height"), "키"),
                        number(input(request, "weight"), "몸무게"), input(request, "conditions"));
                    finish(request, response, "/login", "가입했습니다. 로그인하세요.");
                }
                return;
            }
            if (profile == null) {
                redirect(request, response, "/login");
                return;
            }
            switch (path) {
                case "/logout":
                    request.getSession().invalidate();
                    finish(request, response, "/login", "로그아웃했습니다.");
                    break;
                case "/meal/save":
                    saveMeal(request, response, profile);
                    break;
                case "/meal/delete":
                    coach.deleteMeal(profile.getId(), input(request, "id"));
                    finish(request, response, "/meals", "식단을 삭제했습니다.");
                    break;
                case "/profile":
                    coach.updateProfile(profile.getId(), input(request, "name"),
                        number(input(request, "height"), "키"),
                        number(input(request, "weight"), "몸무게"), input(request, "conditions"));
                    finish(request, response, "/profile", "프로필을 저장했습니다.");
                    break;
                case "/profile/delete":
                    coach.deleteAccount(profile.getId(), request.getParameter("password"));
                    request.getSession().invalidate();
                    finish(request, response, "/login", "회원 탈퇴를 완료했습니다.");
                    break;
                default:
                    showError(request, response, profile, 404, "요청한 페이지를 찾을 수 없습니다.");
                    break;
            }
        } catch (IllegalArgumentException error) {
            showFormError(request, response, profile, path, error.getMessage());
        } catch (IllegalStateException error) {
            log("저장 요청을 처리하지 못했습니다.", error);
            showError(request, response, profile, 500, "저장 요청을 처리하지 못했습니다.");
        }
    }

    private void login(HttpServletRequest request, HttpServletResponse response)
        throws IOException {
        User.Profile user = coach.login(input(request, "loginId"),
            request.getParameter("password"));
        HttpSession session = request.getSession();
        request.changeSessionId();
        session.setAttribute("userId", user.getId());
        session.setAttribute("csrf", UUID.randomUUID().toString());
        redirect(request, response, "/meals");
    }

    private void showMemberPage(HttpServletRequest request, HttpServletResponse response,
        User.Profile user, String path) throws ServletException, IOException {
        switch (path) {
            case "/meal":
                Meal meal = coach.meal(user.getId(), input(request, "id"));
                request.setAttribute("meal", meal);
                request.setAttribute("total", displayNutrition(coach.nutrition(meal)));
                request.setAttribute("foodsByCode", foodsFor(Collections.singletonList(meal)));
                render(request, response, user, "meal", "식단 상세", "meals");
                break;
            case "/meal/new":
                showMealForm(request, response, user, null, false);
                break;
            case "/meal/edit":
                showMealForm(request, response, user,
                    coach.meal(user.getId(), input(request, "id")), false);
                break;
            case "/profile":
                showProfile(request, response, user, false);
                break;
            default:
                showMeals(request, response, user);
                break;
        }
    }

    private void showMeals(HttpServletRequest request, HttpServletResponse response,
        User.Profile user) throws ServletException, IOException {
        List<Meal> meals = coach.meals(user.getId(), null);
        String date = input(request, "date");
        if (date.isEmpty()) {
            date = meals.isEmpty() ? LocalDate.now().toString() : meals.get(0).getDate();
        }
        CoachService.Summary summary = coach.summary(user.getId(), date);
        boolean dailyOnly = "day".equals(input(request, "scope"));
        if (dailyOnly) {
            meals = coach.meals(user.getId(), date);
        }
        request.setAttribute("summary", summary);
        request.setAttribute("total", displayNutrition(summary.getTotal()));
        request.setAttribute("date", date);
        request.setAttribute("dailyOnly", dailyOnly);
        request.setAttribute("meals", meals);
        request.setAttribute("foodsByCode", foodsFor(meals));
        render(request, response, user, "meals", "식단 기록", "meals");
    }

    private Map<String, BigDecimal> displayNutrition(Nutrition nutrition) {
        return Map.of("calories", displayAmount(nutrition.getCalories()), "carbs",
            displayAmount(nutrition.getCarbs()), "protein", displayAmount(nutrition.getProtein()),
            "fat", displayAmount(nutrition.getFat()));
    }

    private BigDecimal displayAmount(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP);
    }

    private Map<String, Food> foodsFor(List<Meal> meals) {
        Map<String, Food> foods = new LinkedHashMap<>();
        for (Meal meal : meals) {
            for (Meal.Item item : meal.getItems()) {
                String code = item.getFoodId();
                if (!foods.containsKey(code)) {
                    foods.put(code, catalog.get(code));
                }
            }
        }
        return foods;
    }

    private void showFoods(HttpServletRequest request, HttpServletResponse response,
        User.Profile user) throws ServletException, IOException {
        String query = input(request, "q");
        boolean prefix = "prefix".equals(input(request, "mode"));
        List<Food> matches = prefix ? catalog.searchPrefix(query) : catalog.search(query);
        int pages = Math.max(1, (matches.size() + FOOD_PAGE_SIZE - 1) / FOOD_PAGE_SIZE);
        int page = 1;
        try {
            page = Math.max(1, Math.min(pages, Integer.parseInt(input(request, "page"))));
        } catch (NumberFormatException ignored) {
            // 페이지 번호가 없으면 첫 페이지를 보여준다.
        }
        int start = (page - 1) * FOOD_PAGE_SIZE;
        request.setAttribute("foods",
            matches.subList(start, Math.min(start + FOOD_PAGE_SIZE, matches.size())));
        request.setAttribute("foodCount", matches.size());
        request.setAttribute("pageNumber", page);
        request.setAttribute("pageCount", pages);
        request.setAttribute("query", query);
        request.setAttribute("mode", prefix ? "prefix" : "contains");
        render(request, response, user, "foods", "음식 검색", "foods");
    }

    private void showFoodOptions(HttpServletRequest request, HttpServletResponse response)
        throws IOException {
        String keyword = input(request, "q");
        List<Food> matches = catalog.search(keyword);
        response.setContentType("application/json;charset=UTF-8");
        gson.toJson(matches.subList(0, Math.min(matches.size(), FOOD_PAGE_SIZE)),
            response.getWriter());
    }

    private void saveMeal(HttpServletRequest request, HttpServletResponse response,
        User.Profile user) throws IOException {
        String id = input(request, "id");
        String date = input(request, "date");
        String[] codes = request.getParameterValues("foodCode");
        String[] amounts = request.getParameterValues("amount");
        if (codes == null || amounts == null || codes.length != amounts.length
            || codes.length > 30) {
            throw new IllegalArgumentException("음식을 1~30개 선택하고 섭취량을 입력하세요.");
        }
        List<Meal.Item> items = new ArrayList<>();
        for (int i = 0; i < codes.length; i++) {
            Food food = catalog.get(codes[i]);
            items.add(
                new Meal.Item(food.getFoodCode(), number(amounts[i], "섭취량"), food.getBasisUnit()));
        }
        coach.saveMeal(user.getId(), id.isEmpty() ? null : id, date, input(request, "type"),
            input(request, "note"), items);
        finish(request, response, "/meals?date=" + date, "식단을 저장했습니다.");
    }

    private void showMealForm(HttpServletRequest request, HttpServletResponse response,
        User.Profile user, Meal meal, boolean submitted) throws ServletException, IOException {
        List<FoodInput> rows = new ArrayList<>();
        String id = "";
        String date = input(request, "date");
        String type = "아침";
        String note = "";
        if (submitted) {
            id = input(request, "id");
            type = input(request, "type");
            note = input(request, "note");
            String[] codes = request.getParameterValues("foodCode");
            String[] amounts = request.getParameterValues("amount");
            if (codes != null && amounts != null) {
                int limit = Math.min(30, Math.min(codes.length, amounts.length));
                for (int i = 0; i < limit; i++) {
                    Food food = null;
                    try {
                        food = catalog.get(codes[i]);
                    } catch (IllegalArgumentException ignored) {
                        // 다시 선택할 수 있도록 음식 선택 칸을 비워 둔다.
                    }
                    rows.add(new FoodInput(food, amounts[i]));
                }
            }
        } else if (meal != null) {
            id = meal.getId();
            date = meal.getDate();
            type = meal.getType();
            note = meal.getNote();
            for (Meal.Item item : meal.getItems()) {
                rows.add(new FoodInput(catalog.get(item.getFoodId()),
                    Double.toString(item.getAmount())));
            }
        }
        if (date.isEmpty()) {
            date = LocalDate.now().toString();
        }
        if (rows.isEmpty()) {
            rows.add(new FoodInput(null, ""));
        }
        request.setAttribute("mealId", id);
        request.setAttribute("mealDate", date);
        request.setAttribute("mealType", type);
        request.setAttribute("mealNote", note);
        request.setAttribute("rows", rows);
        request.setAttribute("mealTypes", List.of("아침", "점심", "저녁", "간식"));
        render(request, response, user, "meal-form", id.isEmpty() ? "식단 작성" : "식단 수정", "meals");
    }

    public static final class FoodInput {

        private final Food food;
        private final String amount;

        private FoodInput(Food food, String amount) {
            this.food = food;
            this.amount = amount;
        }

        public Food getFood() {
            return food;
        }

        public String getAmount() {
            return amount;
        }
    }

    private void showAuth(HttpServletRequest request, HttpServletResponse response, String path)
        throws ServletException, IOException {
        boolean register = "/register".equals(path);
        request.setAttribute("register", register);
        render(request, response, null, "auth", register ? "회원가입" : "로그인", "auth");
    }

    private void showProfile(HttpServletRequest request, HttpServletResponse response,
        User.Profile user, boolean submitted) throws ServletException, IOException {
        request.setAttribute("submitted", submitted);
        render(request, response, user, "profile", "내 정보", "profile");
    }

    private void showFormError(HttpServletRequest request, HttpServletResponse response,
        User.Profile user, String path, String message) throws ServletException, IOException {
        response.setStatus(400);
        request.setAttribute("error", message);
        if ("/login".equals(path) || "/register".equals(path)) {
            showAuth(request, response, path);
        } else if ("/meal/save".equals(path)) {
            showMealForm(request, response, user, null, true);
        } else if ("/profile".equals(path) || "/profile/delete".equals(path)) {
            showProfile(request, response, user, "/profile".equals(path));
        } else {
            showError(request, response, user, 400, message);
        }
    }

    private void showError(HttpServletRequest request, HttpServletResponse response,
        User.Profile user, int status, String message) throws ServletException, IOException {
        response.setStatus(status);
        request.setAttribute("error", message);
        render(request, response, user, "error", "요청 안내", "");
    }

    private void render(HttpServletRequest request, HttpServletResponse response, User.Profile user,
        String view, String title, String section) throws ServletException, IOException {
        HttpSession session = request.getSession();
        String token = (String) session.getAttribute("csrf");
        if (token == null) {
            token = UUID.randomUUID().toString();
            session.setAttribute("csrf", token);
        }
        request.setAttribute("csrf", token);
        request.setAttribute("currentUser", user);
        request.setAttribute("title", title);
        request.setAttribute("section", section);
        request.setAttribute("message", session.getAttribute("message"));
        session.removeAttribute("message");
        request.getRequestDispatcher("/WEB-INF/views/" + view + ".jsp").forward(request, response);
    }

    private boolean validToken(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        String token = (String) session.getAttribute("csrf");
        return token != null && token.equals(request.getParameter("csrf"));
    }

    private User.Profile currentProfile(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            return null;
        }
        try {
            return coach.profile((String) session.getAttribute("userId"));
        } catch (IllegalArgumentException error) {
            session.invalidate();
            return null;
        }
    }

    private void finish(HttpServletRequest request, HttpServletResponse response, String path,
        String message) throws IOException {
        request.getSession().setAttribute("message", message);
        redirect(request, response, path);
    }

    private void redirect(HttpServletRequest request, HttpServletResponse response, String path)
        throws IOException {
        response.sendRedirect(request.getContextPath() + "/app" + path);
    }

    private String route(HttpServletRequest request) {
        String path = request.getPathInfo();
        if (path == null || "/".equals(path)) {
            return "/meals";
        }
        return path;
    }

    private String input(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null ? "" : value.strip();
    }

    private double number(String value, String title) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(title + "에 숫자를 입력하세요.");
        }
    }
}
