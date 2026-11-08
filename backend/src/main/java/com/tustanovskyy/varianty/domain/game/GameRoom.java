package com.tustanovskyy.varianty.domain.game;

import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;

public class GameRoom {

	public final String code;
	public final String hostId;
	public final String hostDisplayName;
	public final boolean hostRegistered;
	public final boolean hostless;
	public final String category;
	public final String language;
	public final Instant createdAt = Instant.now();

	public final LinkedHashMap<String, PlayerState> players = new LinkedHashMap<>();
	public final Set<String> usedQuestionIds = new HashSet<>();
	/** Players the host removed; they can't join this room again. */
	public final Set<String> kickedPlayerIds = new HashSet<>();

	public RoomStatus status = RoomStatus.LOBBY;
	public int roundNumber = 0;
	public final int totalRounds;
	public RoundState currentRound;
	/** Result of the last resolved round, re-sent to players who reconnect during RESULTS. */
	public com.tustanovskyy.varianty.domain.dto.room.RoundResultDto lastRoundResult;

	/** Deadline for the current timed phase (ANSWERING/VOTING), or null when the phase isn't timed. */
	public Instant phaseDeadline;
	public ScheduledFuture<?> phaseTimeoutTask;

	public GameRoom(
			String code,
			String hostId,
			String hostDisplayName,
			boolean hostRegistered,
			int totalRounds,
			boolean hostless,
			String category,
			String language) {
		this.code = code;
		this.hostId = hostId;
		this.hostDisplayName = hostDisplayName;
		this.hostRegistered = hostRegistered;
		this.totalRounds = totalRounds;
		this.hostless = hostless;
		this.category = category;
		this.language = language;
	}
}
