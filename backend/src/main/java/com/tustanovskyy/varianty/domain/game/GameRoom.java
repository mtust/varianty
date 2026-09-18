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
	public final Instant createdAt = Instant.now();

	public final LinkedHashMap<String, PlayerState> players = new LinkedHashMap<>();
	public final Set<String> usedQuestionIds = new HashSet<>();

	public RoomStatus status = RoomStatus.LOBBY;
	public int roundNumber = 0;
	public final int totalRounds;
	public RoundState currentRound;

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
			String category) {
		this.code = code;
		this.hostId = hostId;
		this.hostDisplayName = hostDisplayName;
		this.hostRegistered = hostRegistered;
		this.totalRounds = totalRounds;
		this.hostless = hostless;
		this.category = category;
	}
}
