package io.github.streetinman.skyblockpv.core.dungeons;

/**
 * One dungeon floor's record. Times are in milliseconds, 0 when the floor has never been
 * completed at that grade.
 */
public record FloorStats(int completions, long fastestMs, long fastestSMs, long fastestSPlusMs, int bestScore) {
	public static final FloorStats NONE = new FloorStats(0, 0, 0, 0, 0);
}
