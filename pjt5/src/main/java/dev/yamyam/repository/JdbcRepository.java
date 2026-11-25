package dev.yamyam.repository;

import com.mysql.cj.jdbc.MysqlDataSource;
import dev.yamyam.domain.Food;
import dev.yamyam.domain.Meal;
import dev.yamyam.domain.User;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcRepository implements Repository {

    private static final String USER_COLUMNS = "id, username, password_hash, display_name, "
        + "height_cm, weight_kg, conditions_note, status";
    private static final String MEAL_ROWS = "SELECT m.id, m.meal_date, m.meal_type, m.note, "
        + "i.food_code, i.amount, f.basis_unit FROM meals m "
        + "LEFT JOIN meal_items i ON m.id = i.meal_id "
        + "LEFT JOIN foods f ON i.food_code = f.food_code WHERE m.user_id = ?";

    private final MysqlDataSource dataSource;

    public JdbcRepository(String url, String username, String password) {
        dataSource = new MysqlDataSource();
        dataSource.setURL(url);
        dataSource.setUser(username);
        dataSource.setPassword(password);
    }

    public List<Food> foods() {
        String sql = "SELECT food_code, food_name, basis_amount, basis_unit, calories, protein, "
            + "carbs, fat FROM foods ORDER BY food_code";
        List<Food> foods = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                foods.add(new Food(rows.getString("food_code"), rows.getString("food_name"),
                    rows.getInt("basis_amount"), rows.getString("basis_unit"),
                    rows.getDouble("calories"), rows.getDouble("protein"),
                    rows.getDouble("carbs"), rows.getDouble("fat")));
            }
            return foods;
        } catch (SQLException error) {
            throw databaseFailure(error);
        }
    }

    @Override
    public Optional<User> userByUsername(String username) {
        return findUser("SELECT " + USER_COLUMNS + " FROM users WHERE username = ?", username);
    }

    @Override
    public Optional<User> userById(String id) {
        return findUser("SELECT " + USER_COLUMNS + " FROM users WHERE id = ?", id);
    }

    private Optional<User> findUser(String sql, String value) {
        try (Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet rows = statement.executeQuery()) {
                if (rows.next()) {
                    User user = new User(rows.getString("id"), rows.getString("username"),
                        rows.getString("password_hash"), rows.getString("display_name"),
                        rows.getDouble("height_cm"), rows.getDouble("weight_kg"),
                        rows.getString("conditions_note"), rows.getString("status"));
                    return Optional.of(user);
                }
                return Optional.empty();
            }
        } catch (SQLException error) {
            throw databaseFailure(error);
        }
    }

    @Override
    public void insertUser(User user) {
        String sql = "INSERT INTO users (" + USER_COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        User.Profile profile = user.profile();
        try (Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getId());
            statement.setString(2, user.getUsername());
            statement.setString(3, user.getPasswordHash());
            statement.setString(4, profile.getName());
            statement.setDouble(5, profile.getHeight());
            statement.setDouble(6, profile.getWeight());
            statement.setString(7, profile.getConditions());
            statement.setString(8, user.getStatus());
            statement.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException error) {
            if (error.getErrorCode() == 1062) {
                throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
            }
            throw databaseFailure(error);
        } catch (SQLException error) {
            throw databaseFailure(error);
        }
    }

    @Override
    public void updateUser(User user) {
        String sql = "UPDATE users SET display_name = ?, height_cm = ?, weight_kg = ?, "
            + "conditions_note = ? WHERE id = ?";
        User.Profile profile = user.profile();
        try (Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, profile.getName());
            statement.setDouble(2, profile.getHeight());
            statement.setDouble(3, profile.getWeight());
            statement.setString(4, profile.getConditions());
            statement.setString(5, user.getId());
            statement.executeUpdate();
        } catch (SQLException error) {
            throw databaseFailure(error);
        }
    }

    @Override
    public void deleteUser(String id) {
        try (Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM users WHERE id = ?")) {
            statement.setString(1, id);
            statement.executeUpdate();
        } catch (SQLException error) {
            throw databaseFailure(error);
        }
    }

    @Override
    public List<Meal> meals(String userId, String date) {
        return findMeals(userId, date, null);
    }

    @Override
    public Optional<Meal> meal(String userId, String id) {
        List<Meal> matches = findMeals(userId, null, id);
        if (matches.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(matches.get(0));
    }

    private List<Meal> findMeals(String userId, String date, String id) {
        String sql = MEAL_ROWS;
        if (date != null) {
            sql += " AND m.meal_date = ?";
        }
        if (id != null) {
            sql += " AND m.id = ?";
        }
        sql += " ORDER BY m.meal_date DESC, m.id, i.position_no";

        try (Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {
            int parameter = 1;
            statement.setString(parameter++, userId);
            if (date != null) {
                statement.setDate(parameter++, Date.valueOf(date));
            }
            if (id != null) {
                statement.setString(parameter, id);
            }
            try (ResultSet rows = statement.executeQuery()) {
                return readMeals(userId, rows);
            }
        } catch (SQLException error) {
            throw databaseFailure(error);
        }
    }

    private List<Meal> readMeals(String userId, ResultSet rows) throws SQLException {
        List<Meal> meals = new ArrayList<>();
        boolean hasRow = rows.next();
        while (hasRow) {
            String id = rows.getString("id");
            String date = rows.getDate("meal_date").toLocalDate().toString();
            String type = rows.getString("meal_type");
            String note = rows.getString("note");
            List<Meal.Item> items = new ArrayList<>();
            do {
                String code = rows.getString("food_code");
                if (code != null) {
                    items.add(new Meal.Item(code, rows.getDouble("amount"),
                        rows.getString("basis_unit")));
                }
                hasRow = rows.next();
            } while (hasRow && id.equals(rows.getString("id")));
            meals.add(new Meal(id, userId, date, type, note, items));
        }
        return meals;
    }

    @Override
    public void insertMeal(Meal meal) {
        writeMeal(meal, false);
    }

    @Override
    public void updateMeal(Meal meal) {
        writeMeal(meal, true);
    }

    private void writeMeal(Meal meal, boolean update) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (update) {
                    lockMeal(connection, meal);
                }
                writeMealHeader(connection, meal, update);
                if (update) {
                    try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM meal_items WHERE meal_id = ?")) {
                        statement.setString(1, meal.getId());
                        statement.executeUpdate();
                    }
                }
                insertMealItems(connection, meal);
                connection.commit();
            } catch (SQLException | RuntimeException error) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    error.addSuppressed(rollbackError);
                }
                throw error;
            }
        } catch (SQLException error) {
            throw databaseFailure(error);
        }
    }

    private void lockMeal(Connection connection, Meal meal) throws SQLException {
        String sql = "SELECT id FROM meals WHERE id = ? AND user_id = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, meal.getId());
            statement.setString(2, meal.getUserId());
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) {
                    throw new IllegalArgumentException("식단을 찾을 수 없습니다.");
                }
            }
        }
    }

    private void writeMealHeader(Connection connection, Meal meal, boolean update)
        throws SQLException {
        String sql;
        if (update) {
            sql = "UPDATE meals SET meal_date = ?, meal_type = ?, note = ? "
                + "WHERE id = ? AND user_id = ?";
        } else {
            sql = "INSERT INTO meals (meal_date, meal_type, note, id, user_id) VALUES (?, ?, ?, ?, ?)";
        }
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(meal.getDate()));
            statement.setString(2, meal.getType());
            statement.setString(3, meal.getNote());
            statement.setString(4, meal.getId());
            statement.setString(5, meal.getUserId());
            statement.executeUpdate();
        }
    }

    private void insertMealItems(Connection connection, Meal meal) throws SQLException {
        String sql = "INSERT INTO meal_items (meal_id, position_no, food_code, amount) "
            + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int position = 1;
            for (Meal.Item item : meal.getItems()) {
                statement.setString(1, meal.getId());
                statement.setInt(2, position);
                statement.setString(3, item.getFoodId());
                statement.setDouble(4, item.getAmount());
                statement.addBatch();
                position++;
            }
            statement.executeBatch();
        }
    }

    @Override
    public void deleteMeal(String userId, String id) {
        String sql = "DELETE FROM meals WHERE id = ? AND user_id = ?";
        try (Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            statement.setString(2, userId);
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("식단을 찾을 수 없습니다.");
            }
        } catch (SQLException error) {
            throw databaseFailure(error);
        }
    }

    private static IllegalStateException databaseFailure(SQLException error) {
        return new IllegalStateException("MySQL 요청을 처리하지 못했습니다.", error);
    }
}
