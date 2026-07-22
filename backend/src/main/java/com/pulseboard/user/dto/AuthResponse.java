package com.pulseboard.user.dto;

public record AuthResponse(String token, UserResponse user) {
}
