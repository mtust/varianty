package com.tustanovskyy.varianty.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tustanovskyy.varianty.domain.dto.auth.AuthResponse;
import com.tustanovskyy.varianty.domain.entity.User;
import com.tustanovskyy.varianty.exception.ApiException;
import com.tustanovskyy.varianty.repository.GameHistoryRepository;
import com.tustanovskyy.varianty.repository.UserRepository;
import com.tustanovskyy.varianty.security.JwtService;
import com.tustanovskyy.varianty.security.PlayerPrincipal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AuthServiceRefreshTest {

	private UserRepository users;
	private JwtService jwtService;
	private AuthService service;

	@BeforeEach
	void setUp() {
		users = mock(UserRepository.class);
		jwtService = new JwtService("test-secret-that-is-at-least-32-bytes-long!!", 43200, 1440);
		service = new AuthService(users, mock(GameHistoryRepository.class), new BCryptPasswordEncoder(), jwtService);
	}

	@Test
	void registeredPlayerGetsFreshTokenWithCurrentDisplayName() {
		when(users.findById("u1")).thenReturn(Optional.of(User.builder().id("u1").displayName("Renamed").build()));

		AuthResponse response = service.refresh(new PlayerPrincipal("u1", "Old", false));

		assertThat(response.guest()).isFalse();
		assertThat(response.displayName()).isEqualTo("Renamed");
		PlayerPrincipal parsed = jwtService.parse(response.token());
		assertThat(parsed.playerId()).isEqualTo("u1");
		assertThat(parsed.guest()).isFalse();
	}

	@Test
	void guestCannotRefresh() {
		assertThatThrownBy(() -> service.refresh(new PlayerPrincipal("guest-1", "G", true)))
				.isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
	}

	@Test
	void deletedAccountCannotRefresh() {
		when(users.findById("gone")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.refresh(new PlayerPrincipal("gone", "X", false)))
				.isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
	}
}
