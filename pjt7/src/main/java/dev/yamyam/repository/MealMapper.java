package dev.yamyam.repository;

import dev.yamyam.domain.Meal;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface MealMapper {

    List<Meal> findAll(@Param("userId") String userId, @Param("date") LocalDate date);

    Meal findById(@Param("userId") String userId, @Param("id") String id);

    String lockById(@Param("userId") String userId, @Param("id") String id);

    int insert(Meal meal);

    int update(Meal meal);

    int delete(@Param("userId") String userId, @Param("id") String id);

    int deleteItems(@Param("mealId") String mealId);

    int insertItems(Meal meal);
}
