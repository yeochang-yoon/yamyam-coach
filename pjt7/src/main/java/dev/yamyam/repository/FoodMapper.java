package dev.yamyam.repository;

import dev.yamyam.domain.Food;
import java.util.List;
import java.util.Set;
import org.apache.ibatis.annotations.Param;

public interface FoodMapper {

    List<Food> search(@Param("query") String query, @Param("prefix") boolean prefix,
        @Param("size") int size, @Param("offset") long offset);

    List<Food> findByCodes(@Param("codes") Set<String> codes);
}
