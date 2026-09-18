package com.tustanovskyy.varianty.repository;

import com.tustanovskyy.varianty.domain.entity.Question;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, String> {

	List<Question> findByCategory(String category);
}
