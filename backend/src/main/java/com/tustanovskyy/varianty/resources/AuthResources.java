package com.tustanovskyy.varianty.resources;

import com.tustanovskyy.varianty.domain.dto.auth.AuthResponse;
import com.tustanovskyy.varianty.domain.dto.auth.GuestLoginRequest;
import com.tustanovskyy.varianty.domain.dto.auth.LoginRequest;
import com.tustanovskyy.varianty.domain.dto.auth.RegisterRequest;
import com.tustanovskyy.varianty.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthResources {

	private final AuthService authService;

	public AuthResources(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
		return authService.register(request);
	}

	@PostMapping("/login")
	public AuthResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}

	@PostMapping("/guest")
	public AuthResponse guest(@Valid @RequestBody GuestLoginRequest request) {
		return authService.guest(request);
	}
}
