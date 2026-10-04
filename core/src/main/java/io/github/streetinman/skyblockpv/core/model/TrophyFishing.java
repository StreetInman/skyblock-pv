package io.github.streetinman.skyblockpv.core.model;

import java.util.Map;

/**
 * Trophy fishing progress.
 *
 * @param catches     per fish, catches at each tier
 * @param totalCaught all trophy fish caught
 * @param rank        highest Trophy Hunter rank claimed from Odger (0 = none, 1 = bronze … 4 = diamond)
 */
public record TrophyFishing(Map<TrophyFish, Map<TrophyFish.Tier, Integer>> catches, int totalCaught, int rank) {
	private static final String[] RANK_NAMES = {"None", "Bronze Hunter", "Silver Hunter", "Gold Hunter", "Diamond Hunter"};

	public int count(TrophyFish fish, TrophyFish.Tier tier) {
		return catches.getOrDefault(fish, Map.of()).getOrDefault(tier, 0);
	}

	/** Highest tier caught for a fish, or null if never caught. */
	public TrophyFish.Tier bestTier(TrophyFish fish) {
		TrophyFish.Tier[] tiers = TrophyFish.Tier.values();
		for (int i = tiers.length - 1; i >= 0; i--) {
			if (count(fish, tiers[i]) > 0) return tiers[i];
		}
		return null;
	}

	/** How many distinct fish have been caught at {@code tier} or better. */
	public int fishAtLeast(TrophyFish.Tier tier) {
		int n = 0;
		for (TrophyFish fish : TrophyFish.values()) {
			TrophyFish.Tier best = bestTier(fish);
			if (best != null && best.ordinal() >= tier.ordinal()) n++;
		}
		return n;
	}

	public String rankName() {
		return RANK_NAMES[Math.max(0, Math.min(rank, RANK_NAMES.length - 1))];
	}
}
