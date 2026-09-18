package com.tustanovskyy.varianty.domain.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank @Email String email,
		@NotBlank @Size(min = 6, max = 72) String password,
		@NotBlank @Size(min = 1, max = 40) String displayName) {
}
