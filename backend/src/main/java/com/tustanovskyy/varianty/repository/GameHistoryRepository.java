package com.tustanovskyy.varianty.repository;

import com.tustanovskyy.varianty.domain.entity.GameHistoryEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameHistoryRepository extends JpaRepository<GameHistoryEntry, String> {

	List<GameHistoryEntry> findByUserIdOrderByPlayedAtDesc(String userId);
}
