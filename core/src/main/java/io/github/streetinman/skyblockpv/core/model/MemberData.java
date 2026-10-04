package io.github.streetinman.skyblockpv.core.model;

import java.util.Map;

import io.github.streetinman.skyblockpv.core.dungeons.DungeonData;

/**
 * One player's data within a profile.
 *
 * @param skillXp  skill name (e.g. {@code FARMING}) → total XP. Empty when skills API is off.
 * @param slayerXp        XP per slayer boss
 * @param attributeStacks hunting-shard ID → shards syphoned into its attribute
 */
public record MemberData(
		String uuid,
		double skyblockLevel,
		double purse,
		int fairySouls,
		Map<String, Double> skillXp,
		Inventories inventories,
		TrophyFishing trophyFishing,
		DungeonData dungeons,
		Map<Slayer, Long> slayerXp,
		Map<String, Integer> attributeStacks) {
}
