package dev.yamyam.dto;

import dev.yamyam.domain.User;

public final class UserResponse {

    private final String id;
    private final String username;
    private final String name;
    private final double height;
    private final double weight;
    private final String conditions;

    public UserResponse(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.name = user.getName();
        this.height = user.getHeight();
        this.weight = user.getWeight();
        this.conditions = user.getConditions();
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
