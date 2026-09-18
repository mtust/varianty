package com.tustanovskyy.varianty.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
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
@Table(name = "questions")
public class Question {

	@Id
	private String id;

	@Column(nullable = false, columnDefinition = "text")
	private String fact;

	@Column(nullable = false)
	private String correctAnswer;

	private String category;

	@PrePersist
	void prePersist() {
		if (id == null) {
			id = UUID.randomUUID().toString();
		}
	}
}
