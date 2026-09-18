package com.tustanovskyy.varianty.domain.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GuestLoginRequest(@NotBlank @Size(min = 1, max = 40) String displayName) {
}
