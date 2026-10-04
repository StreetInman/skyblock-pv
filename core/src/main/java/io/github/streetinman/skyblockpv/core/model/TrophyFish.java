package io.github.streetinman.skyblockpv.core.model;

/** The Crimson Isle trophy fish, keyed by their API id. */
public enum TrophyFish {
	BLOBFISH("blobfish", "Blobfish"),
	FLYFISH("flyfish", "Flyfish"),
	GOLDEN_FISH("golden_fish", "Golden Fish"),
	GUSHER("gusher", "Gusher"),
	KARATE_FISH("karate_fish", "Karate Fish"),
	LAVA_HORSE("lava_horse", "Lavahorse"),
	MANA_RAY("mana_ray", "Mana Ray"),
	MOLDFIN("moldfin", "Moldfin"),
	OBFUSCATED_FISH_1("obfuscated_fish_1", "Obfuscated 1"),
	OBFUSCATED_FISH_2("obfuscated_fish_2", "Obfuscated 2"),
	OBFUSCATED_FISH_3("obfuscated_fish_3", "Obfuscated 3"),
	SKELETON_FISH("skeleton_fish", "Skeleton Fish"),
	SLUGFISH("slugfish", "Slugfish"),
	SOUL_FISH("soul_fish", "Soul Fish"),
	STEAMING_HOT_FLOUNDER("steaming_hot_flounder", "Steaming-Hot Flounder"),
	SULPHUR_SKITTER("sulphur_skitter", "Sulphur Skitter"),
	VANILLE("vanille", "Vanille"),
	VOLCANIC_STONEFISH("volcanic_stonefish", "Volcanic Stonefish");

	public final String apiKey;
	public final String displayName;

	TrophyFish(String apiKey, String displayName) {
		this.apiKey = apiKey;
		this.displayName = displayName;
	}

	public enum Tier {
		BRONZE, SILVER, GOLD, DIAMOND;

		public String apiSuffix() {
			return name().toLowerCase(java.util.Locale.ROOT);
		}
	}
}
