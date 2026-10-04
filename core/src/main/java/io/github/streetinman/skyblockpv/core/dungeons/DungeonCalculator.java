package io.github.streetinman.skyblockpv.core.dungeons;

import java.util.EnumMap;
import java.util.Map;

/**
 * Runs needed to reach Catacombs 50 and class average 50.
 *
 * <p>Follows the model used by adjectils.com's dungeon calculator (and the MIT-licensed
 * rtca-bot that ports it): every run scores 300 (S+), Catacombs XP grows with floor completions
 * up to a cap, and the classes you didn't play get a share of the run's class XP.
 */
public final class DungeonCalculator {
	/** Base XP for an S+ Master Mode Floor 7 run, for both Catacombs and class XP. */
	public static final double M7_BASE_XP = 300_000;
	private static final int MAX_SIMULATED_RUNS = 1_000_000;

	public record ClassPlan(int totalRuns, Map<DungeonClass, Integer> runsAs) {
	}

	private DungeonCalculator() {
	}

	/**
	 * Catacombs XP from one run. Completions on the floor add a bonus that stops growing at 25
	 * for Master Mode floors (50 for F6, 75 for lower floors).
	 */
	public static double catacombsXpPerRun(double baseXp, int completions, XpBoosts b) {
		int cap = baseXp >= 15_000 ? 25 : baseXp == 4_880 ? 50 : 75;
		int n = Math.min(completions, cap);
		double ring = b.expertRing() ? 0.10 : 0;
		double hec = b.hecatomb();
		double mayorBonus = b.mayorMultiplier() - 1;
		double multiplier = 0.95 + ring + hec + n * ((b.expertRing() ? 0.024 : 0.022) + hec / 50);
		if (mayorBonus > 0) multiplier += mayorBonus + n / 100.0;
		// Small epsilon so float error (e.g. 504000.0000001) does not round up a whole XP.
		return Math.ceil(baseXp * multiplier * b.globalMultiplier() - 1e-6);
	}

	/** Class XP for the class you played; the others get {@link XpBoosts#teamShare()} of their own amount. */
	public static double classXpPerRun(double baseXp, DungeonClass c, XpBoosts b) {
		double bonus = 1 + 2 * b.hecatomb() + b.classPerk(c) + b.scarfBonus() + b.extraClassBonus();
		return baseXp * bonus * b.globalMultiplier() * b.mayorMultiplier();
	}

	/** Runs of the given floor to reach Catacombs 50, or 0 if already there. */
	public static int runsToCatacombs50(double catacombsXp, int floorCompletions, double baseXp, XpBoosts b) {
		double xp = catacombsXp;
		int runs = 0;
		while (xp < DungeonLevels.XP_FOR_50 && runs < MAX_SIMULATED_RUNS) {
			xp += catacombsXpPerRun(baseXp, floorCompletions + runs, b);
			runs++;
		}
		return runs;
	}

	/**
	 * Plays whichever class is furthest from 50 each run (so every class finishes together) and
	 * counts how many runs you play as each. A class stops needing runs once the passive share
	 * from the others' runs will carry it the rest of the way.
	 */
	public static ClassPlan classAverage50(Map<DungeonClass, Double> classXp, double baseXp, XpBoosts b) {
		Map<DungeonClass, Double> left = new EnumMap<>(DungeonClass.class);
		Map<DungeonClass, Integer> runsAs = new EnumMap<>(DungeonClass.class);
		for (DungeonClass c : DungeonClass.values()) {
			left.put(c, Math.max(0, DungeonLevels.XP_FOR_50 - classXp.getOrDefault(c, 0.0)));
			runsAs.put(c, 0);
		}

		int runs = 0;
		while (runs < MAX_SIMULATED_RUNS) {
			DungeonClass play = null;
			double most = 0;
			for (DungeonClass c : DungeonClass.values()) {
				if (left.get(c) > most) {
					most = left.get(c);
					play = c;
				}
			}
			if (play == null) break;

			runs++;
			runsAs.merge(play, 1, Integer::sum);
			for (DungeonClass c : DungeonClass.values()) {
				double gained = classXpPerRun(baseXp, c, b) * (c == play ? 1 : b.teamShare());
				left.put(c, left.get(c) - gained);
			}
		}
		return new ClassPlan(runs, runsAs);
	}
}
