package io.github.streetinman.skyblockpv.core.dungeons;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Hunting-shard attributes that boost dungeon XP, read from {@code member.attributes.stacks}
 * (shard ID → shards syphoned).
 *
 * <p>Catacombs Graduate (from the Scarf shard) gives +2% class XP per level and Catacombs
 * Explorer (from the Bonzo shard) +1% Catacombs XP per level, both up to level 10.
 */
public final class ShardAttributes {
	/** Both are Epic shards: total shards needed for levels 1–10. */
	private static final int[] EPIC_STACKS = {1, 2, 4, 6, 9, 12, 16, 20, 25, 32};

	/** Key spellings seen across API versions and other tools, compared case-insensitively. */
	static final List<String> GRADUATE_KEYS = List.of("SHARD_SCARF", "SCARF", "E54", "CATACOMBS_GRADUATE");
	static final List<String> EXPLORER_KEYS = List.of("SHARD_BONZO", "BONZO", "E51", "CATACOMBS_EXPLORER");

	private ShardAttributes() {
	}

	public static int graduateLevel(Map<String, Integer> stacks) {
		return level(stacks, GRADUATE_KEYS);
	}

	public static int explorerLevel(Map<String, Integer> stacks) {
		return level(stacks, EXPLORER_KEYS);
	}

	static int level(Map<String, Integer> stacks, List<String> keys) {
		int best = 0;
		for (Map.Entry<String, Integer> e : stacks.entrySet()) {
			if (keys.contains(e.getKey().toUpperCase(Locale.ROOT))) best = Math.max(best, levelFromStacks(e.getValue()));
		}
		return best;
	}

	public static int levelFromStacks(int stacks) {
		int level = 0;
		while (level < EPIC_STACKS.length && stacks >= EPIC_STACKS[level]) level++;
		return level;
	}
}
