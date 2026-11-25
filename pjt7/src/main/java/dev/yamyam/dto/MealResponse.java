package dev.yamyam.dto;

import dev.yamyam.domain.Food;
import dev.yamyam.domain.Meal;
import dev.yamyam.domain.Nutrition;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class MealResponse {

    private final String id;
    private final LocalDate date;
    private final String type;
    private final String note;
    private final List<Item> items;
    private final Nutrition nutrition;

    public MealResponse(Meal meal, Nutrition nutrition) {
        this.id = meal.getId();
        this.date = meal.getDate();
        this.type = meal.getType();
        this.note = meal.getNote();
        this.nutrition = nutrition;
        this.items = new ArrayList<>();
        for (Meal.Item entry : meal.getItems()) {
            items.add(new Item(entry.getFood(), entry.getAmount()));
        }
    }

    public String getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getType() {
        return type;
    }

    public String getNote() {
        return note;
    }

    public List<Item> getItems() {
        return items;
    }

    public Nutrition getNutrition() {
        return nutrition;
    }

    public static final class Item {

        private final String foodCode;
        private final String name;
        private final double amount;
        private final String unit;

        public Item(Food food, double amount) {
            this.foodCode = food.getFoodCode();
            this.name = food.getName();
            this.amount = amount;
            this.unit = food.getBasisUnit();
        }

        public String getFoodCode() {
            return foodCode;
        }

        public String getName() {
            return name;
        }

        public double getAmount() {
            return amount;
        }

        public String getUnit() {
            return unit;
        }
    }
}
