package com.tustanovskyy.varianty.domain.dto.room;

/** Host's settings for another game in the same room; null fields keep the previous settings. */
public record NewGamePayload(
		boolean keepScores, String category, Integer rounds, Integer answerSeconds, Boolean manualTimer) {
}
