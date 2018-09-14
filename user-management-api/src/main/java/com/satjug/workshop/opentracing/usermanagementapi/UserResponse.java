package com.satjug.workshop.opentracing.usermanagementapi;

import java.util.UUID;

public class UserResponse {
    private final String user;

    public UserResponse(String user) {
        this.user = user;
    }

    public String getUser() {
        return user;
    }
}
