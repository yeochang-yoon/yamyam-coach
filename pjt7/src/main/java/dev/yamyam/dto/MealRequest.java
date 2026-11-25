package dev.yamyam.dto;

import java.time.LocalDate;
import java.util.List;

public class MealRequest {

    private LocalDate date;
    private String type;
    private String note;
    private List<Item> items;

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public static class Item {

        private String foodCode;
        private double amount;
        private String unit;

        public String getFoodCode() {
            return foodCode;
        }

        public void setFoodCode(String foodCode) {
            this.foodCode = foodCode;
        }

        public double getAmount() {
            return amount;
        }

        public void setAmount(double amount) {
            this.amount = amount;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }
    }
}
