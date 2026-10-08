package io.github.streetinman.skyblockpv.core.model;

/**
 * Pet level from pet XP. Each rarity starts further into the same XP table, which is why
 * a Legendary pet needs about 25.4M XP for level 100 and a Common one 5.6M.
 */
public final class PetLevels {
	private static final int[] XP = {100, 110, 120, 130, 145, 160, 175, 190, 210, 230, 250, 275, 300, 330, 360, 400, 440, 490, 540, 600, 660, 730, 800, 880, 960, 1050, 1150, 1260, 1380, 1510, 1650, 1800, 1960, 2130, 2310, 2500, 2700, 2920, 3160, 3420, 3700, 4000, 4350, 4750, 5200, 5700, 6300, 7000, 7800, 8700, 9700, 10800, 12000, 13300, 14700, 16200, 17800, 19500, 21300, 23200, 25200, 27400, 29800, 32400, 35200, 38200, 41400, 44800, 48400, 52200, 56200, 60400, 64800, 69400, 74200, 79200, 84700, 90700, 97200, 104200, 111700, 119700, 128200, 137200, 146700, 156700, 167700, 179700, 192700, 206700, 221700, 237700, 254700, 272700, 291700, 311700, 333700, 357700, 383700, 411700, 441700, 476700, 516700, 561700, 611700, 666700, 726700, 791700, 861700, 936700, 1016700, 1101700, 1191700, 1286700, 1386700, 1496700, 1616700, 1746700, 1886700};
	/** Where each rarity (common → mythic) starts in the table. */
	private static final int[] OFFSET = {0, 6, 11, 16, 20, 20};

	private PetLevels() {
	}

	/** Max level: 200 for Golden Dragon (and other 200-level pets), else 100. */
	public static int maxLevel(String type) {
		return "GOLDEN_DRAGON".equals(type) || "JADE_DRAGON".equals(type) || "ROSE_DRAGON".equals(type) ? 200 : 100;
	}

	/** Fractional level, e.g. 87.4. Levels past 100 (Golden Dragon) use the last step of the table. */
	public static double level(Pet pet) {
		int offset = OFFSET[Math.max(0, Math.min(pet.tierIndex(), OFFSET.length - 1))];
		int max = maxLevel(pet.type());
		double xp = pet.exp();
		int level = 1;
		while (level < max) {
			int index = offset + level - 1;
			int needed = index < XP.length ? XP[index] : XP[XP.length - 1];
			if (xp < needed) return level + xp / needed;
			xp -= needed;
			level++;
		}
		return max;
	}
}
