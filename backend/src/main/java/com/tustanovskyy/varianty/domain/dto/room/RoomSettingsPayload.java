package com.tustanovskyy.varianty.domain.dto.room;

/** Host's timer settings changed in the lobby; null fields stay as they are. */
public record RoomSettingsPayload(Integer answerSeconds, Boolean manualTimer) {
}
