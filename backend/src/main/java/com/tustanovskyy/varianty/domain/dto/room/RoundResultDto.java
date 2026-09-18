package com.tustanovskyy.varianty.domain.dto.room;

import java.util.List;
import java.util.Map;

public record RoundResultDto(
		String fact,
		List<RevealedOptionDto> options,
		Map<String, Integer> scoreDeltas,
		List<PlayerScoreDto> scoreboard,
		boolean gameFinished) {
}
