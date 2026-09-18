package com.tustanovskyy.varianty.security;

public record PlayerPrincipal(String playerId, String displayName, boolean guest) {
}
