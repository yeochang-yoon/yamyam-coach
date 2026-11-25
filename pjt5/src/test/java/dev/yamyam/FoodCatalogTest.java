package dev.yamyam;

import dev.yamyam.domain.Food;
import dev.yamyam.service.FoodCatalog;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FoodCatalogTest {

    private final Food apple = new Food("A1", "Apple", 100, "g", 52, 0, 14, 0);
    private final Food juice = new Food("A2", "Apple Juice", 100, "mL", 40, 0, 10, 0);

    @Test
    void indexesExactCodesAndSearchesFoodNames() {
        FoodCatalog catalog = new FoodCatalog(Arrays.asList(juice, apple));
        assertSame(juice, catalog.get("A2"));
        assertSame(juice, catalog.search("juice").get(0));
        List<Food> prefix = catalog.searchPrefix(" APP ");
        assertEquals(Arrays.asList(apple, juice), prefix);
        assertTrue(catalog.searchPrefix("juice").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> catalog.get("UNKNOWN"));
        assertThrows(IllegalArgumentException.class,
            () -> new FoodCatalog(Arrays.asList(apple, apple)));
    }

    @Test
    void scalesFoodsFromTheirOwnReferenceAmounts() {
        Food tea = new Food("T1", "Tea", 200, "mL", 60, 0, 15, 0);
        assertEquals(30, tea.at(100).getCalories(), 0.000001);
        assertEquals(0, tea.at(100).getFat(), 0.000001);
    }
}
