package com.tustanovskyy.varianty.domain.dto.room;

/** Host's settings for another game in the same room; null category/rounds keep the previous ones. */
public record NewGamePayload(boolean keepScores, String category, Integer rounds) {
}
