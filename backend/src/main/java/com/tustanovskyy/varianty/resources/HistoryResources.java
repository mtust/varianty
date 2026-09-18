package com.tustanovskyy.varianty.resources;

import com.tustanovskyy.varianty.domain.entity.GameHistoryEntry;
import com.tustanovskyy.varianty.exception.ApiException;
import com.tustanovskyy.varianty.repository.GameHistoryRepository;
import com.tustanovskyy.varianty.security.PlayerPrincipal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/history")
public class HistoryResources {

	private final GameHistoryRepository gameHistoryRepository;

	public HistoryResources(GameHistoryRepository gameHistoryRepository) {
		this.gameHistoryRepository = gameHistoryRepository;
	}

	@GetMapping
	public List<GameHistoryEntry> myHistory(@AuthenticationPrincipal PlayerPrincipal principal) {
		if (principal.guest()) {
			throw new ApiException(HttpStatus.FORBIDDEN, "Гостьові акаунти не мають історії ігор");
		}
		return gameHistoryRepository.findByUserIdOrderByPlayedAtDesc(principal.playerId());
	}
}
