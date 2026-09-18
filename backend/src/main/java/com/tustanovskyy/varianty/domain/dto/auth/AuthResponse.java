package com.tustanovskyy.varianty.domain.dto.auth;

public record AuthResponse(String token, String playerId, String displayName, boolean guest) {
}
