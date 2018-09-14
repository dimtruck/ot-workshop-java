package com.satjug.workshop.opentracing.authapi;

import java.util.UUID;

public class TokenResponse {
    private final UUID token;

    public TokenResponse(UUID token) {
        this.token = token;
    }

    public UUID getToken() {
        return token;
    }
}
