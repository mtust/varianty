package com.tustanovskyy.varianty.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

	private final Key signingKey;
	private final long expirationMinutes;

	public JwtService(
			@Value("${varianty.jwt.secret}") String secret,
			@Value("${varianty.jwt.expiration-minutes}") long expirationMinutes) {
		this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.expirationMinutes = expirationMinutes;
	}

	public String issueToken(String playerId, String displayName, boolean guest) {
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(playerId)
				.claim("displayName", displayName)
				.claim("guest", guest)
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
				.signWith(signingKey)
				.compact();
	}

	public PlayerPrincipal parse(String token) {
		Claims claims = Jwts.parser()
				.verifyWith((javax.crypto.SecretKey) signingKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();
		return new PlayerPrincipal(
				claims.getSubject(),
				claims.get("displayName", String.class),
				Boolean.TRUE.equals(claims.get("guest", Boolean.class)));
	}
}
