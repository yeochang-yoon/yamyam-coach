package dev.yamyam.domain;

public final class User {

    private final String id;
    private final String username;
    private final String passwordHash;
    private final String name;
    private final double height;
    private final double weight;
    private final String conditions;
    private final String status;

    public User(String id, String username, String passwordHash, String name, double height,
        double weight, String conditions, String status) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.name = name;
        this.height = height;
        this.weight = weight;
        this.conditions = conditions;
        this.status = status == null ? "ACTIVE" : status;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getStatus() {
        return status == null ? "ACTIVE" : status;
    }

    public Profile profile() {
        return new Profile(id, username, name, height, weight, conditions);
    }

    public static final class Profile {

        private final String id;
        private final String username;
        private final String name;
        private final double height;
        private final double weight;
        private final String conditions;

        public Profile(String id, String username, String name, double height, double weight,
            String conditions) {
            this.id = id;
            this.username = username;
            this.name = name;
            this.height = height;
            this.weight = weight;
            this.conditions = conditions;
        }

        public String getId() {
            return id;
        }

        public String getUsername() {
            return username;
        }

        public String getName() {
            return name;
        }

        public double getHeight() {
            return height;
        }

        public double getWeight() {
            return weight;
        }

        public String getConditions() {
            return conditions;
        }
    }
}
