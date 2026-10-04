package io.github.streetinman.skyblockpv.core.dungeons;

import java.util.Map;

/**
 * Dungeon stats for one profile member.
 *
 * @param masterCompletions    floor number → Master Mode completions
 * @param floorCompletions     floor number (0 = Entrance) → normal completions
 * @param masterFastestSPlusMs floor number → fastest S+ time in milliseconds
 * @param perks                essence-shop perks ({@code player_data.perks}), e.g. {@code toxophilite → 5}
 */
public record DungeonData(
		double catacombsXp,
		Map<DungeonClass, Double> classXp,
		DungeonClass selectedClass,
		long secrets,
		Map<Integer, Integer> floorCompletions,
		Map<Integer, Integer> masterCompletions,
		Map<Integer, Long> masterFastestSPlusMs,
		Map<String, Integer> perks) {

	public double catacombsLevel() {
		return DungeonLevels.level(catacombsXp);
	}

	public double classLevel(DungeonClass c) {
		return DungeonLevels.level(classXp.getOrDefault(c, 0.0));
	}

	/** Average of the five class levels, each capped at 50 like the game's class average. */
	public double classAverage() {
		double sum = 0;
		for (DungeonClass c : DungeonClass.values()) sum += Math.min(classLevel(c), DungeonLevels.MAX_LEVEL);
		return sum / DungeonClass.values().length;
	}

	public int totalRuns() {
		int total = 0;
		for (int n : floorCompletions.values()) total += n;
		for (int n : masterCompletions.values()) total += n;
		return total;
	}
}
