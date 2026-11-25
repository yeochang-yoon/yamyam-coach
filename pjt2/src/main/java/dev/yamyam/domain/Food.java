package dev.yamyam.domain;

public final class Food {

    private final String foodCode;
    private final String name;
    private final int basisAmount;
    private final String basisUnit;
    private final double calories;
    private final double protein;
    private final double carbs;
    private final double fat;

    public Food(String foodCode, String name, int basisAmount, String basisUnit, double calories,
        double protein, double carbs, double fat) {
        this.foodCode = foodCode;
        this.name = name;
        this.basisAmount = basisAmount;
        this.basisUnit = basisUnit;
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fat = fat;
        validate();
    }

    public void validate() {
        if (foodCode == null || foodCode.isBlank() || name == null || name.isBlank()) {
            throw new IllegalArgumentException("음식 정보가 없습니다.");
        }
        if (basisAmount <= 0 || !("g".equals(basisUnit) || "mL".equals(basisUnit))) {
            throw new IllegalArgumentException("영양성분 기준량 또는 단위가 올바르지 않습니다.");
        }
        for (double n : new double[] {calories, protein, carbs, fat}) {
            if (!Double.isFinite(n) || n < 0) {
                throw new IllegalArgumentException("영양 값은 0 이상이어야 합니다.");
            }
        }
    }

    public String getFoodCode() {
        return foodCode;
    }

    public String getName() {
        return name;
    }

    public int getBasisAmount() {
        return basisAmount;
    }

    public String getBasisUnit() {
        return basisUnit;
    }

    public double getCalories() {
        return calories;
    }

    public double getFat() {
        return fat;
    }

    public Nutrition at(double amount) {
        return new Nutrition(calories, protein, carbs, fat).scale(amount / basisAmount);
    }
}
