package com.tustanovskyy.varianty.domain.game;

public class PlayerState {

	public final String id;
	public final String displayName;
	public final boolean registered;
	public int score;
	public boolean connected;

	public PlayerState(String id, String displayName, boolean registered) {
		this.id = id;
		this.displayName = displayName;
		this.registered = registered;
		this.score = 0;
		this.connected = true;
	}
}
