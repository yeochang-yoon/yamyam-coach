package dev.yamyam.console;

import dev.yamyam.domain.Food;
import dev.yamyam.service.FoodCatalog;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class Benchmark {

    private static final int QUERY_COUNT = 5000;
    private static final int ROUNDS = 5;

    public static void main(String[] args) {
        long started = System.nanoTime();
        FoodCatalog catalog = new FoodCatalog();
        long catalogLoadTime = System.nanoTime() - started;
        List<Food> foods = catalog.all();
        String[] codes = new String[QUERY_COUNT];
        Random random = new Random(42);
        for (int i = 0; i < codes.length; i++) {
            codes[i] = foods.get(random.nextInt(foods.size())).getFoodCode();
        }

        for (int i = 0; i < 3; i++) {
            verify(scan(foods, codes), lookup(catalog, codes));
        }

        long[] scanTimes = new long[ROUNDS];
        long[] lookupTimes = new long[ROUNDS];
        long checksum = 0;
        for (int i = 0; i < ROUNDS; i++) {
            long start = System.nanoTime();
            long scanResult = scan(foods, codes);
            scanTimes[i] = System.nanoTime() - start;

            start = System.nanoTime();
            checksum = lookup(catalog, codes);
            lookupTimes[i] = System.nanoTime() - start;
            verify(scanResult, checksum);
        }

        Arrays.sort(scanTimes);
        Arrays.sort(lookupTimes);
        System.out.printf(Locale.ROOT,
            "foods,queries,catalog_init_ms,list_median_ms,map_median_ms,checksum%n"
                + "%d,%d,%.3f,%.3f,%.3f,%d%n",
            foods.size(), codes.length, catalogLoadTime / 1_000_000.0,
            scanTimes[ROUNDS / 2] / 1_000_000.0, lookupTimes[ROUNDS / 2] / 1_000_000.0, checksum);
    }

    private static long scan(List<Food> foods, String[] codes) {
        long checksum = 0;
        for (String code : codes) {
            for (Food food : foods) {
                if (food.getFoodCode().equals(code)) {
                    checksum += food.getFoodCode().hashCode();
                    break;
                }
            }
        }
        return checksum;
    }

    private static long lookup(FoodCatalog catalog, String[] codes) {
        long checksum = 0;
        for (String code : codes) {
            Food food = catalog.get(code);
            checksum += food.getFoodCode().hashCode();
        }
        return checksum;
    }

    private static void verify(long scanResult, long lookupResult) {
        if (scanResult != lookupResult) {
            throw new IllegalStateException("두 음식 조회 결과가 일치하지 않습니다.");
        }
    }
}
