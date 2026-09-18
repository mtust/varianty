package com.tustanovskyy.varianty.domain.game;

public class AnswerOption {

	public static final String CORRECT_OWNER = "CORRECT";
	public static final String HOST_OWNER = "HOST";

	public final String id;
	public final String ownerId;
	public final String text;

	public AnswerOption(String id, String ownerId, String text) {
		this.id = id;
		this.ownerId = ownerId;
		this.text = text;
	}
}
