package com.tustanovskyy.varianty.domain.dto.room;

import java.util.List;

public record RoomSnapshot(
		String code,
		String status,
		int roundNumber,
		int totalRounds,
		String hostDisplayName,
		boolean hostless,
		String fact,
		List<PlayerScoreDto> players,
		int answeredCount,
		int votedCount,
		int secondsRemaining) {
}
