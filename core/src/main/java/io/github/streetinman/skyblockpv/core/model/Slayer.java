package io.github.streetinman.skyblockpv.core.model;

/** Slayer bosses with their API key and cumulative XP needed for each level. */
public enum Slayer {
	ZOMBIE("zombie", "Revenant", new int[] {5, 15, 200, 1_000, 5_000, 20_000, 100_000, 400_000, 1_000_000}),
	SPIDER("spider", "Tarantula", new int[] {5, 25, 200, 1_000, 5_000, 20_000, 100_000, 400_000, 1_000_000}),
	WOLF("wolf", "Sven", new int[] {10, 30, 250, 1_500, 5_000, 20_000, 100_000, 400_000, 1_000_000}),
	ENDERMAN("enderman", "Voidgloom", new int[] {10, 30, 250, 1_500, 5_000, 20_000, 100_000, 400_000, 1_000_000}),
	BLAZE("blaze", "Inferno", new int[] {10, 30, 250, 1_500, 5_000, 20_000, 100_000, 400_000, 1_000_000}),
	VAMPIRE("vampire", "Riftstalker", new int[] {20, 75, 240, 840, 2_400});

	public final String apiKey;
	public final String displayName;
	private final int[] thresholds;

	Slayer(String apiKey, String displayName, int[] thresholds) {
		this.apiKey = apiKey;
		this.displayName = displayName;
		this.thresholds = thresholds;
	}

	public int level(long xp) {
		int level = 0;
		while (level < thresholds.length && xp >= thresholds[level]) level++;
		return level;
	}

	public int maxLevel() {
		return thresholds.length;
	}
}
