CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    username VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    password_hash VARCHAR(256) CHARACTER SET ascii NOT NULL,
    display_name VARCHAR(40) NOT NULL,
    height_cm DOUBLE NOT NULL,
    weight_kg DOUBLE NOT NULL,
    conditions_note VARCHAR(300) NOT NULL,
    status VARCHAR(12) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT ck_users_height CHECK (height_cm BETWEEN 50 AND 250),
    CONSTRAINT ck_users_weight CHECK (weight_kg BETWEEN 10 AND 500),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS foods (
    food_code VARCHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    food_name VARCHAR(150) NOT NULL,
    basis_amount INT NOT NULL,
    basis_unit VARCHAR(2) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    calories DOUBLE NOT NULL,
    protein DOUBLE NOT NULL,
    carbs DOUBLE NOT NULL,
    fat DOUBLE NOT NULL,
    CONSTRAINT ck_foods_basis CHECK (basis_amount > 0 AND basis_unit IN ('g', 'mL')),
    CONSTRAINT ck_foods_nutrition CHECK (calories >= 0 AND protein >= 0 AND carbs >= 0 AND fat >= 0)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS meals (
    id VARCHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    user_id VARCHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    meal_date DATE NOT NULL,
    meal_type VARCHAR(8) NOT NULL,
    note VARCHAR(500) NOT NULL,
    INDEX idx_meals_user_date (user_id, meal_date DESC, id),
    CONSTRAINT fk_meals_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_meals_type CHECK (meal_type IN ('아침', '점심', '저녁', '간식'))
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS meal_items (
    meal_id VARCHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    position_no INT NOT NULL,
    food_code VARCHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    amount DOUBLE NOT NULL,
    PRIMARY KEY (meal_id, position_no),
    INDEX idx_items_food (food_code),
    CONSTRAINT fk_items_meal FOREIGN KEY (meal_id) REFERENCES meals (id) ON DELETE CASCADE,
    CONSTRAINT fk_items_food FOREIGN KEY (food_code) REFERENCES foods (food_code),
    CONSTRAINT ck_items_position CHECK (position_no BETWEEN 1 AND 30),
    CONSTRAINT ck_items_amount CHECK (amount > 0 AND amount <= 5000)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
