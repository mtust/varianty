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
		int secondsRemaining,
		String category,
		/** Host's own questions still waiting to be asked; players see only how many there are. */
		int customQuestionCount,
		/** Seconds players get to write an answer each round (the host's setting). */
		int answerSeconds,
		/** True when the host ends answering and voting by hand; secondsRemaining is then 0. */
		boolean manualTimer) {
}
