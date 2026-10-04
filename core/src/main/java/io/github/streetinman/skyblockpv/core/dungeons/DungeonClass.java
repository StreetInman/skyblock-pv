package io.github.streetinman.skyblockpv.core.dungeons;

import java.util.Locale;

/** The five dungeon classes, with their API key and the essence-shop perk that boosts each. */
public enum DungeonClass {
	HEALER("healer", "heart_of_gold"),
	MAGE("mage", "cold_efficiency"),
	BERSERK("berserk", "unbridled_rage"),
	ARCHER("archer", "toxophilite"),
	TANK("tank", "diamond_in_the_rough");

	public final String apiKey;
	/** Key under {@code player_data.perks}; each level gives +2% class XP for this class. */
	public final String perkKey;

	DungeonClass(String apiKey, String perkKey) {
		this.apiKey = apiKey;
		this.perkKey = perkKey;
	}

	public String displayName() {
		return name().charAt(0) + name().substring(1).toLowerCase(Locale.ROOT);
	}
}
