package com.tustanovskyy.varianty.service;

import com.tustanovskyy.varianty.domain.dto.auth.AuthResponse;
import com.tustanovskyy.varianty.domain.dto.auth.GuestLoginRequest;
import com.tustanovskyy.varianty.domain.dto.auth.LoginRequest;
import com.tustanovskyy.varianty.domain.dto.auth.RegisterRequest;
import com.tustanovskyy.varianty.domain.entity.User;
import com.tustanovskyy.varianty.exception.ApiException;
import com.tustanovskyy.varianty.repository.UserRepository;
import com.tustanovskyy.varianty.security.JwtService;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	public AuthResponse register(RegisterRequest request) {
		String email = request.email().trim().toLowerCase();
		if (userRepository.existsByEmail(email)) {
			throw new ApiException(HttpStatus.CONFLICT, "Email is already registered");
		}
		User user = User.builder()
				.email(email)
				.passwordHash(passwordEncoder.encode(request.password()))
				.displayName(request.displayName().trim())
				.createdAt(Instant.now())
				.build();
		user = userRepository.save(user);
		return issue(user.getId(), user.getDisplayName(), false);
	}

	public AuthResponse login(LoginRequest request) {
		User user = userRepository.findByEmail(request.email().trim().toLowerCase())
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
		}
		return issue(user.getId(), user.getDisplayName(), false);
	}

	public AuthResponse guest(GuestLoginRequest request) {
		String guestId = "guest-" + UUID.randomUUID();
		return issue(guestId, request.displayName().trim(), true);
	}

	private AuthResponse issue(String playerId, String displayName, boolean guest) {
		String token = jwtService.issueToken(playerId, displayName, guest);
		return new AuthResponse(token, playerId, displayName, guest);
	}
}
