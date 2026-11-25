package dev.yamyam.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Meal {

    private String id;
    private String userId;
    private LocalDate date;
    private String type;
    private String note;
    private List<Item> items = new ArrayList<>();

    public Meal() {
    }

    public Meal(String id, String userId, LocalDate date, String type, String note, List<Item> items) {
        this.id = id;
        this.userId = userId;
        this.date = date;
        this.type = type;
        this.note = note;
        this.items = new ArrayList<>(items);
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
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

    public static class Item {

        private int position;
        private Food food;
        private double amount;

        public Item() {
        }

        public Item(int position, Food food, double amount) {
            this.position = position;
            this.food = food;
            this.amount = amount;
        }

        public int getPosition() {
            return position;
        }

        public Food getFood() {
            return food;
        }

        public double getAmount() {
            return amount;
        }
    }
}
