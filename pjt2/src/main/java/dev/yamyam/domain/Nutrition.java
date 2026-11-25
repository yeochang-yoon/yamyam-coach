package dev.yamyam.domain;

public final class Nutrition {

    public static final Nutrition ZERO = new Nutrition(0, 0, 0, 0);

    private final double calories;
    private final double protein;
    private final double carbs;
    private final double fat;

    public Nutrition(double calories, double protein, double carbs, double fat) {
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fat = fat;
    }

    public double getCalories() {
        return calories;
    }

    public double getProtein() {
        return protein;
    }

    public double getCarbs() {
        return carbs;
    }

    public double getFat() {
        return fat;
    }

    public Nutrition add(Nutrition n) {
        return new Nutrition(calories + n.calories, protein + n.protein, carbs + n.carbs,
            fat + n.fat);
    }

    public Nutrition scale(double n) {
        return new Nutrition(calories * n, protein * n, carbs * n, fat * n);
    }
}
