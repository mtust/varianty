package com.tustanovskyy.varianty.domain.game;

import com.tustanovskyy.varianty.domain.entity.Question;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RoundState {

	public final Question question;
	public String hostTrapAnswer;
	public final Map<String, String> playerAnswers = new LinkedHashMap<>();
	public List<AnswerOption> options = List.of();
	public final Map<String, String> votes = new LinkedHashMap<>();

	public RoundState(Question question) {
		this.question = question;
	}
}
