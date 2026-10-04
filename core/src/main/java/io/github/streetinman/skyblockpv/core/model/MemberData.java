package io.github.streetinman.skyblockpv.core.model;

import java.util.Map;

/**
 * One player's data within a profile.
 *
 * @param skillXp skill name (e.g. {@code FARMING}) → total XP. Empty when skills API is off.
 */
public record MemberData(
		String uuid,
		double skyblockLevel,
		double purse,
		int fairySouls,
		Map<String, Double> skillXp,
		Inventories inventories,
		TrophyFishing trophyFishing) {
}
