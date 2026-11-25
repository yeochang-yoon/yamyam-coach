package dev.yamyam.repository;

import dev.yamyam.domain.Meal;
import dev.yamyam.domain.User;
import java.util.List;
import java.util.Optional;

public interface Repository {

    Optional<User> userByUsername(String username);

    Optional<User> userById(String id);

    void saveUser(User user);

    void deleteUser(String id);

    List<Meal> meals(String userId, String date);

    Optional<Meal> meal(String userId, String id);

    void saveMeal(Meal meal);

    void deleteMeal(String userId, String id);
}
