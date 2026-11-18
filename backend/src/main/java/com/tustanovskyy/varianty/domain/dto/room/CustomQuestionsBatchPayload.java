package com.tustanovskyy.varianty.domain.dto.room;

import java.util.List;

/** Several of the host's own questions added at once (pasted as a list in the app). */
public record CustomQuestionsBatchPayload(List<CustomQuestionPayload> questions) {
}
