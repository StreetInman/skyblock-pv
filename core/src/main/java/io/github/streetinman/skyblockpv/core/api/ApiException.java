package io.github.streetinman.skyblockpv.core.api;

/** A request failed in a way worth showing to the player. */
public class ApiException extends RuntimeException {
	public enum Kind { PLAYER_NOT_FOUND, INVALID_KEY, RATE_LIMITED, NO_SKYBLOCK, HTTP, NETWORK }

	private final Kind kind;

	public ApiException(Kind kind, String message) {
		super(message);
		this.kind = kind;
	}

	public ApiException(Kind kind, String message, Throwable cause) {
		super(message, cause);
		this.kind = kind;
	}

	public Kind kind() {
		return kind;
	}
}
