package com.tustanovskyy.varianty.security;

import java.security.Principal;

public record StompPrincipal(String playerId, String displayName, boolean guest) implements Principal {

	@Override
	public String getName() {
		return playerId;
	}
}
