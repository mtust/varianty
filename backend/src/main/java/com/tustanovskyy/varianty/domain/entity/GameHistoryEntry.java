package com.tustanovskyy.varianty.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "game_history")
public class GameHistoryEntry {

	@Id
	private String id;

	@Column(nullable = false)
	private String userId;

	@Column(nullable = false)
	private String roomCode;

	@Column(nullable = false)
	private String displayName;

	@Column(nullable = false)
	private int score;

	@Column(nullable = false)
	private int placement;

	@Column(nullable = false)
	private int playerCount;

	@Column(nullable = false)
	private Instant playedAt;

	@PrePersist
	void prePersist() {
		if (id == null) {
			id = UUID.randomUUID().toString();
		}
	}
}
