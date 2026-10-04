package io.github.streetinman.skyblockpv.core.dungeons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.github.streetinman.skyblockpv.core.TestNbt;
import io.github.streetinman.skyblockpv.core.model.Inventories;
import io.github.streetinman.skyblockpv.core.parse.ItemDecoder;

class DungeonCalculatorTest {
	private static final XpBoosts NONE = boosts(0, 0, false, Map.of(), 1.0);

	@Test
	void levelTableMatchesTheGame() {
		assertEquals(569_809_640L, DungeonLevels.XP_FOR_50);
		assertEquals(50.0, DungeonLevels.level(569_809_640), 1e-9);
		assertEquals(1.0, DungeonLevels.level(50), 1e-9);
		assertEquals(0.5, DungeonLevels.level(25), 1e-9);
		assertEquals(51.0, DungeonLevels.level(569_809_640 + 200_000_000.0), 1e-9);
	}

	@Test
	void catacombsXpPerM7Run() {
		// No boosts, first ever run: 95% of base.
		assertEquals(285_000, DungeonCalculator.catacombsXpPerRun(300_000, 0, NONE));
		// Ring + Hecatomb X with the 25-completion bonus maxed: 300k × (0.95 + 0.1 + 0.02 + 25 × 0.0244) = 504k.
		XpBoosts ringHec = boosts(10, 0, true, Map.of(), 1.0);
		assertEquals(504_000, DungeonCalculator.catacombsXpPerRun(300_000, 25, ringHec));
		// The completion bonus stops growing at 25.
		assertEquals(504_000, DungeonCalculator.catacombsXpPerRun(300_000, 500, ringHec));
	}

	@Test
	void runsToCatacombs50() {
		XpBoosts ringHec = boosts(10, 0, true, Map.of(), 1.0);
		assertEquals(1_131, DungeonCalculator.runsToCatacombs50(0, 25, 300_000, ringHec));
		assertEquals(0, DungeonCalculator.runsToCatacombs50(DungeonLevels.XP_FOR_50, 25, 300_000, ringHec));
	}

	@Test
	void classXpAppliesPerClassPerks() {
		XpBoosts b = boosts(10, 0.06, false, Map.of(DungeonClass.MAGE, 5), 1.0);
		// 1 + 2×0.02 + 0.10 + 0.06 = 1.20
		assertEquals(360_000, DungeonCalculator.classXpPerRun(300_000, DungeonClass.MAGE, b), 1e-6);
		// 1 + 0.04 + 0.06 = 1.10
		assertEquals(330_000, DungeonCalculator.classXpPerRun(300_000, DungeonClass.TANK, b), 1e-6);
	}

	@Test
	void shardAttributesBoostXp() {
		XpBoosts grad = new XpBoosts(0, 0, false, Map.of(), 1.0, 1.0, 10, 0, 0, XpBoosts.DEFAULT_TEAM_SHARE);
		assertEquals(360_000, DungeonCalculator.classXpPerRun(300_000, DungeonClass.MAGE, grad), 1e-6);
		XpBoosts explorer = new XpBoosts(0, 0, false, Map.of(), 1.0, 1.0, 0, 10, 0, XpBoosts.DEFAULT_TEAM_SHARE);
		assertEquals(315_000, DungeonCalculator.catacombsXpPerRun(300_000, 0, explorer));
	}

	@Test
	void shardLevelsFollowEpicThresholds() {
		assertEquals(0, ShardAttributes.levelFromStacks(0));
		assertEquals(1, ShardAttributes.levelFromStacks(1));
		assertEquals(3, ShardAttributes.levelFromStacks(5));
		assertEquals(9, ShardAttributes.levelFromStacks(31));
		assertEquals(10, ShardAttributes.levelFromStacks(32));
		assertEquals(10, ShardAttributes.levelFromStacks(500));
	}

	/**
	 * A late-game player (the case that prompted the Graduate fix): Hecatomb X, Grimoire,
	 * perks 5, Graduate X. Other calculators put this at about 1,560 runs.
	 */
	@Test
	void lateGameClassAverageMatchesOtherCalculators() {
		Map<DungeonClass, Double> xp = new EnumMap<>(DungeonClass.class);
		double[] levels = {46.61, 49.02, 47.75, 47.21, 46.29};
		DungeonClass[] order = {DungeonClass.HEALER, DungeonClass.MAGE, DungeonClass.BERSERK, DungeonClass.ARCHER, DungeonClass.TANK};
		for (int i = 0; i < order.length; i++) xp.put(order[i], xpAt(levels[i]));
		Map<DungeonClass, Integer> perks = new EnumMap<>(DungeonClass.class);
		for (DungeonClass c : DungeonClass.values()) perks.put(c, 5);

		XpBoosts withGrad = new XpBoosts(10, 0.06, true, perks, 1.0, 1.0, 10, 0, 0, XpBoosts.DEFAULT_TEAM_SHARE);
		XpBoosts noGrad = new XpBoosts(10, 0.06, true, perks, 1.0, 1.0, 0, 0, 0, XpBoosts.DEFAULT_TEAM_SHARE);
		int with = DungeonCalculator.classAverage50(xp, 300_000, withGrad).totalRuns();
		int without = DungeonCalculator.classAverage50(xp, 300_000, noGrad).totalRuns();
		assertTrue(with > 1_450 && with < 1_700, "with Graduate: " + with);
		assertTrue(without > with * 1.1, "without Graduate: " + without);
	}

	private static double xpAt(double level) {
		double lo = 0, hi = DungeonLevels.XP_FOR_50;
		for (int i = 0; i < 100; i++) {
			double mid = (lo + hi) / 2;
			if (DungeonLevels.level(mid) < level) lo = mid; else hi = mid;
		}
		return lo;
	}

	@Test
	void classAverage50FromScratchSplitsRunsEvenly() {
		var plan = DungeonCalculator.classAverage50(Map.of(), 300_000, NONE);
		int sum = plan.runsAs().values().stream().mapToInt(Integer::intValue).sum();
		assertEquals(plan.totalRuns(), sum);
		// Each run moves 300k + 4 × 75k = 600k of the 5 × 569.8M needed.
		assertTrue(plan.totalRuns() >= Math.ceil(5 * 569_809_640.0 / 600_000));
		int min = plan.runsAs().values().stream().mapToInt(Integer::intValue).min().orElseThrow();
		int max = plan.runsAs().values().stream().mapToInt(Integer::intValue).max().orElseThrow();
		assertTrue(max - min <= 1, plan.toString());
	}

	@Test
	void nearlyDoneClassIsCarriedByPassiveXp() {
		Map<DungeonClass, Double> xp = new EnumMap<>(DungeonClass.class);
		for (DungeonClass c : DungeonClass.values()) xp.put(c, 400_000_000.0);
		xp.put(DungeonClass.HEALER, 569_000_000.0);
		var plan = DungeonCalculator.classAverage50(xp, 300_000, NONE);
		assertEquals(0, plan.runsAs().get(DungeonClass.HEALER));
		assertTrue(plan.totalRuns() > 0);
	}

	@Test
	void allFiftyNeedsNoRuns() {
		Map<DungeonClass, Double> xp = new EnumMap<>(DungeonClass.class);
		for (DungeonClass c : DungeonClass.values()) xp.put(c, (double) DungeonLevels.XP_FOR_50);
		assertEquals(0, DungeonCalculator.classAverage50(xp, 300_000, NONE).totalRuns());
	}

	@Test
	void detectsBoostsFromItemsAndPerks() throws Exception {
		var bag = ItemDecoder.decodeInventory(TestNbt.inventory(Arrays.asList(
				TestNbt.item(397, "SCARF_STUDIES", "s"),
				TestNbt.item(397, "SCARF_THESIS", "t"),
				TestNbt.item(397, "CATACOMBS_EXPERT_RING", "r"))));
		var inventory = ItemDecoder.decodeInventory(TestNbt.inventory(Arrays.asList(
				TestNbt.enchanted(267, "HYPERION", "hecatomb", 7))));
		Inventories inv = new Inventories(inventory, List.of(), List.of(), List.of(), null, bag, Map.of(), Set.of());
		DungeonData dungeons = new DungeonData(0, Map.of(), null, 0, Map.of(), Map.of(), Map.of(),
				Map.of("toxophilite", 3, "cold_efficiency", 5));

		XpBoosts b = XpBoosts.detect(dungeons, inv, Map.of("shard_scarf", 20, "SHARD_BONZO", 3), "Derpy",
				1.0, -1, -1, 0, XpBoosts.DEFAULT_TEAM_SHARE);

		assertEquals(7, b.hecatombLevel());
		assertEquals(0.0152, b.hecatomb(), 1e-9);
		assertEquals(0.04, b.scarfBonus(), 1e-9);
		assertTrue(b.expertRing());
		assertEquals(0.06, b.classPerk(DungeonClass.ARCHER), 1e-9);
		assertEquals(0.10, b.classPerk(DungeonClass.MAGE), 1e-9);
		assertEquals(0.0, b.classPerk(DungeonClass.TANK), 1e-9);
		assertEquals(1.5, b.mayorMultiplier());
		assertEquals(8, b.graduateLevel());
		assertEquals(2, b.explorerLevel());

		XpBoosts none = XpBoosts.detect(dungeons, new Inventories(null, null, null, null, null, null, Map.of(), Set.of()),
				Map.of(), "Aatrox", 1.0, 10, -1, 0, XpBoosts.DEFAULT_TEAM_SHARE);
		assertFalse(none.expertRing());
		assertEquals(10, none.graduateLevel(), "a config override wins over the profile");
		assertEquals(0, none.explorerLevel());
		assertEquals(1.0, none.mayorMultiplier());
	}

	private static XpBoosts boosts(int hecatomb, double scarf, boolean ring, Map<DungeonClass, Integer> perks, double mayor) {
		return new XpBoosts(hecatomb, scarf, ring, perks, mayor, 1.0, 0, 0, 0, XpBoosts.DEFAULT_TEAM_SHARE);
	}
}
