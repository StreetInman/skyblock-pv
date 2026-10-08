package io.github.streetinman.skyblockpv.core.model;

import java.util.Map;

/**
 * Smaller stats shown on the Misc tab. Anything the API leaves out is 0, null or empty.
 *
 * @param firstJoinMs        when the player joined this profile (epoch millis)
 * @param kuudraCompletions  Kuudra tier (none, hot, burning, fiery, infernal) → completions
 * @param jacobMedals        gold/silver/bronze → medals in the inventory
 * @param craftedMinions     unique minion tiers crafted
 * @param coopMembers        players on the profile, including this one
 */
public record ProfileExtras(
		long firstJoinMs,
		int coopMembers,
		long deaths,
		long kills,
		double highestCritDamage,
		long itemsFished,
		long giftsGiven,
		long giftsReceived,
		double hotmXp,
		long mithrilPowder,
		long gemstonePowder,
		long glacitePowder,
		Map<String, Integer> kuudraCompletions,
		String crimsonFaction,
		long mageReputation,
		long barbarianReputation,
		Map<String, Integer> jacobMedals,
		int jacobDoubleDrops,
		int jacobFarmingLevelCap,
		int jacobContests,
		long motes,
		int craftedMinions) {

	public static final ProfileExtras NONE = new ProfileExtras(0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, Map.of(), null, 0, 0, Map.of(), 0, 0, 0, 0, 0);

	/** Total XP needed for Heart of the Mountain tiers 1–10. */
	private static final long[] HOTM_XP = {0, 3_000, 12_000, 37_000, 97_000, 197_000, 347_000, 557_000, 847_000, 1_247_000};

	public int hotmTier() {
		int tier = 0;
		while (tier < HOTM_XP.length && hotmXp >= HOTM_XP[tier]) tier++;
		return tier;
	}

	public int kuudraTotal() {
		return kuudraCompletions.values().stream().mapToInt(Integer::intValue).sum();
	}
}
