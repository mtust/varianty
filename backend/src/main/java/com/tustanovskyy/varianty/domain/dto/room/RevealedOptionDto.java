package com.tustanovskyy.varianty.domain.dto.room;

import java.util.List;

public record RevealedOptionDto(
		String id,
		String text,
		String authorLabel,
		boolean correct,
		boolean hostTrap,
		List<String> votedByDisplayNames) {
}
