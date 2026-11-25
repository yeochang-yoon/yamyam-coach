package dev.yamyam.repository;

import com.google.gson.Gson;
import dev.yamyam.domain.Meal;
import dev.yamyam.domain.User;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class FileRepository implements Repository {

    private final Path file;
    private final Gson gson = new Gson();
    private State state;

    public FileRepository(Path file) {
        this.file = file.toAbsolutePath();
        try {
            if (Files.notExists(file)) {
                state = initialState();
            } else {
                state = readState();
            }
        } catch (Exception e) {
            throw new IllegalStateException("저장 파일을 읽지 못했습니다. 원본을 보존하고 확인하세요: " + file, e);
        }
    }

    private State readState() throws IOException {
        State saved = gson.fromJson(Files.readString(file), State.class);
        if (saved == null || saved.users == null || saved.meals == null) {
            throw new IOException("잘못된 저장 파일");
        }

        for (Map.Entry<String, Meal> entry : saved.meals.entrySet()) {
            if (entry.getValue() != null) {
                entry.setValue(restoreMeal(entry.getValue()));
            }
        }
        return saved;
    }

    private State initialState() throws IOException {
        Map<String, User> users = new LinkedHashMap<>();
        InitialUsers initialUsers = read("/data/users.json", InitialUsers.class);
        for (InitialUser initial : initialUsers.users) {
            User user = initial.toUser();
            if (users.putIfAbsent(user.getId(), user) != null) {
                throw new IOException("초기 회원 ID가 중복됩니다: " + user.getId());
            }
        }
        Map<String, Meal> meals = new LinkedHashMap<>();
        InitialMeals initialMeals = read("/data/diet-data.json", InitialMeals.class);
        for (Meal data : initialMeals.meals) {
            Meal meal = restoreMeal(data);
            if (!users.containsKey(meal.getUserId())) {
                throw new IOException("초기 식단의 작성 회원을 찾을 수 없습니다: " + meal.getId());
            }
            if (meals.putIfAbsent(meal.getId(), meal) != null) {
                throw new IOException("초기 식단 ID가 중복됩니다: " + meal.getId());
            }
        }
        return new State(users, meals);
    }

    private Meal restoreMeal(Meal meal) {
        // Gson으로 읽은 식단도 생성자를 거쳐 기본값을 채운다.
        List<Meal.Item> items = new ArrayList<>();
        for (Meal.Item item : meal.getItems()) {
            items.add(new Meal.Item(item.getFoodId(), item.getAmount(), item.getUnit()));
        }
        return new Meal(meal.getId(), meal.getUserId(), meal.getDate(), meal.getType(),
            meal.getNote(), items);
    }

    private <T> T read(String resource, Class<T> type) throws IOException {
        InputStream stream = getClass().getResourceAsStream(resource);
        if (stream == null) {
            throw new IOException("초기 데이터 파일이 없습니다: " + resource);
        }
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, type);
        }
    }

    private void commit(State next) {
        Path temp = null;
        try {
            Files.createDirectories(file.getParent());
            temp = Files.createTempFile(file.getParent(), "state-", ".tmp");
            Files.writeString(temp, gson.toJson(next));
            try {
                Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
            state = next;
        } catch (IOException e) {
            throw new IllegalStateException("저장에 실패했습니다. 변경 사항은 반영되지 않았습니다.", e);
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                }
            }
        }
    }

    public synchronized Optional<User> userByUsername(String name) {
        for (User user : state.users.values()) {
            if (user.getUsername().equals(name)) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    public synchronized Optional<User> userById(String id) {
        return Optional.ofNullable(state.users.get(id));
    }

    public synchronized void saveUser(User user) {
        for (User existing : state.users.values()) {
            if (existing.getUsername().equals(user.getUsername()) && !existing.getId()
                .equals(user.getId())) {
                throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
            }
        }
        Map<String, User> users = new LinkedHashMap<>(state.users);
        users.put(user.getId(), user);
        commit(new State(users, state.meals));
    }

    public synchronized void deleteUser(String id) {
        Map<String, User> users = new LinkedHashMap<>(state.users);
        users.remove(id);

        Map<String, Meal> meals = new LinkedHashMap<>();
        for (Map.Entry<String, Meal> entry : state.meals.entrySet()) {
            if (!entry.getValue().getUserId().equals(id)) {
                meals.put(entry.getKey(), entry.getValue());
            }
        }
        commit(new State(users, meals));
    }

    public synchronized List<Meal> meals(String userId, String date) {
        List<Meal> matches = new ArrayList<>();
        for (Meal meal : state.meals.values()) {
            if (meal.getUserId().equals(userId) && (date == null || meal.getDate().equals(date))) {
                matches.add(meal);
            }
        }
        Collections.sort(matches, new Comparator<Meal>() {
            @Override
            public int compare(Meal first, Meal second) {
                int byDate = second.getDate().compareTo(first.getDate());
                if (byDate != 0) {
                    return byDate;
                }
                return first.getId().compareTo(second.getId());
            }
        });
        return matches;
    }

    public synchronized Optional<Meal> meal(String userId, String id) {
        Meal meal = state.meals.get(id);
        if (meal != null && meal.getUserId().equals(userId)) {
            return Optional.of(meal);
        }
        return Optional.empty();
    }

    public synchronized void saveMeal(Meal meal) {
        Map<String, Meal> meals = new LinkedHashMap<>(state.meals);
        meals.put(meal.getId(), meal);
        commit(new State(state.users, meals));
    }

    public synchronized void deleteMeal(String userId, String id) {
        if (meal(userId, id).isPresent()) {
            Map<String, Meal> meals = new LinkedHashMap<>(state.meals);
            meals.remove(id);
            commit(new State(state.users, meals));
        }
    }

    private static final class State {

        private final Map<String, User> users;
        private final Map<String, Meal> meals;

        private State(Map<String, User> users, Map<String, Meal> meals) {
            this.users = users;
            this.meals = meals;
        }
    }

    private static final class InitialUsers {

        private List<InitialUser> users;
    }

    private static final class InitialMeals {

        private List<Meal> meals;
    }

    private static final class InitialUser {

        private String id;
        private String loginId;
        private PasswordHash passwordHash;
        private String name;
        private double heightCm;
        private double weightKg;
        private List<String> conditions;
        private String status;

        private User toUser() throws IOException {
            if (!"PBKDF2WithHmacSHA256".equals(passwordHash.algorithm)) {
                throw new IOException("지원하지 않는 비밀번호 해시 형식입니다.");
            }
            String hash =
                passwordHash.iterations + ":" + passwordHash.salt + ":" + passwordHash.hash;
            return new User(id, loginId, hash, name, heightCm, weightKg,
                String.join(", ", conditions), status);
        }
    }

    private static final class PasswordHash {

        private String algorithm;
        private int iterations;
        private String salt;
        private String hash;
    }
}
