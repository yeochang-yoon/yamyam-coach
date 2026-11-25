package dev.yamyam.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Meal {

    private final String id;
    private final String userId;
    private final String date;
    private final String type;
    private final String note;
    private final List<Item> items;

    public Meal(String id, String userId, String date, String type, String note, List<Item> items) {
        this.id = id;
        this.userId = userId;
        this.date = date;
        this.type = type;
        if (note == null) {
            note = "";
        }
        this.note = note;
        this.items = new ArrayList<>(items);
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getDate() {
        return date;
    }

    public String getType() {
        return type;
    }

    public String getNote() {
        return note;
    }

    public List<Item> getItems() {
        return Collections.unmodifiableList(items);
    }

    public static final class Item {

        private final String foodId;
        private final double amount;
        private final String unit;

        public Item(String foodId, double amount, String unit) {
            this.foodId = foodId;
            this.amount = amount;
            this.unit = unit == null ? "g" : unit;
        }

        public String getFoodId() {
            return foodId;
        }

        public double getAmount() {
            return amount;
        }

        public String getUnit() {
            return unit;
        }
    }
}
