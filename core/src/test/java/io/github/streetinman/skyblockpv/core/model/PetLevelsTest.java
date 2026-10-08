package io.github.streetinman.skyblockpv.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PetLevelsTest {
	@Test
	void matchesKnownLevel100Totals() {
		assertEquals(100, PetLevels.level(pet("ENDER_DRAGON", "LEGENDARY", 25_353_230)), 1e-9);
		assertEquals(100, PetLevels.level(pet("BEE", "COMMON", 5_624_785)), 1e-9);
		assertEquals(99, Math.floor(PetLevels.level(pet("ENDER_DRAGON", "LEGENDARY", 25_353_229))));
		assertEquals(1, PetLevels.level(pet("BEE", "COMMON", 0)), 1e-9);
		assertEquals(2, PetLevels.level(pet("BEE", "COMMON", 100)), 1e-9);
	}

	@Test
	void goldenDragonGoesPast100() {
		assertEquals(200, PetLevels.maxLevel("GOLDEN_DRAGON"));
		assertEquals(true, PetLevels.level(pet("GOLDEN_DRAGON", "LEGENDARY", 100_000_000)) > 100);
	}

	private static Pet pet(String type, String tier, double exp) {
		return new Pet(type, tier, exp, false, null, null);
	}
}
