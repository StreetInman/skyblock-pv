package io.github.streetinman.skyblockpv.core.dungeons;

/** Catacombs and class levels share one XP table: 50 levels, then 200M XP per level after. */
public final class DungeonLevels {
	/** XP needed to go from level i-1 to i. */
	private static final long[] PER_LEVEL = {
			0, 50, 75, 110, 160, 230, 330, 470, 670, 950, 1340,
			1890, 2665, 3760, 5260, 7380, 10300, 14400, 20000, 27600,
			38000, 52500, 71500, 97000, 132000, 180000, 243000, 328000,
			445000, 600000, 800000, 1065000, 1410000, 1900000, 2500000,
			3300000, 4300000, 5600000, 7200000, 9200000, 12000000, 15000000,
			19000000, 24000000, 30000000, 38000000, 48000000, 60000000, 75000000,
			93000000, 116250000};
	private static final long OVERFLOW_PER_LEVEL = 200_000_000L;

	public static final int MAX_LEVEL = 50;
	/** Total XP for level 50 (569,809,640). */
	public static final long XP_FOR_50 = totalXpFor(MAX_LEVEL);

	private DungeonLevels() {
	}

	public static long totalXpFor(int level) {
		long total = 0;
		for (int i = 1; i <= Math.min(level, MAX_LEVEL); i++) total += PER_LEVEL[i];
		if (level > MAX_LEVEL) total += (level - MAX_LEVEL) * OVERFLOW_PER_LEVEL;
		return total;
	}

	/** Fractional level, e.g. 42.37. Past 50 it keeps counting at 200M XP per level. */
	public static double level(double xp) {
		double remaining = xp;
		for (int i = 1; i <= MAX_LEVEL; i++) {
			if (remaining < PER_LEVEL[i]) return i - 1 + remaining / PER_LEVEL[i];
			remaining -= PER_LEVEL[i];
		}
		return MAX_LEVEL + remaining / OVERFLOW_PER_LEVEL;
	}
}
