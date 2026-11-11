package com.tustanovskyy.varianty.domain.dto.room;

/** A question the host writes for their own game: the fact (ideally with a ___ blank) and its answer. */
public record CustomQuestionPayload(String fact, String answer) {
}
