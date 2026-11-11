package com.tustanovskyy.varianty.domain.dto.room;

/** Sent only to the host: their own questions that haven't been asked yet. */
public record CustomQuestionDto(String id, String fact, String answer) {
}
