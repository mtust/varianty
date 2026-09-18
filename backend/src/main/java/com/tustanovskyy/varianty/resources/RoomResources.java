package com.tustanovskyy.varianty.resources;

import com.tustanovskyy.varianty.domain.dto.room.RoomCreatedResponse;
import com.tustanovskyy.varianty.security.PlayerPrincipal;
import com.tustanovskyy.varianty.service.QuestionSeeder;
import com.tustanovskyy.varianty.service.RoomService;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
public class RoomResources {

	private final RoomService roomService;

	public RoomResources(RoomService roomService) {
		this.roomService = roomService;
	}

	@PostMapping
	public RoomCreatedResponse createRoom(
			@AuthenticationPrincipal PlayerPrincipal host,
			@RequestParam(defaultValue = "false") boolean hostless,
			@RequestParam(defaultValue = QuestionSeeder.CATEGORY_FACTS) String category) {
		return new RoomCreatedResponse(roomService.createRoom(host, hostless, category));
	}

	@GetMapping("/{code}/exists")
	public Map<String, Boolean> exists(@PathVariable String code) {
		return Map.of("exists", roomService.exists(code.toUpperCase()));
	}
}
