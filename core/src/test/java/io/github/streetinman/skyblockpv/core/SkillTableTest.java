package io.github.streetinman.skyblockpv.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;

import io.github.streetinman.skyblockpv.core.model.SkillLevel;
import io.github.streetinman.skyblockpv.core.parse.SkillTable;

class SkillTableTest {
	private static final SkillTable TABLE = SkillTable.fromResource(JsonParser.parseString("""
			{"success": true, "skills": {
			  "MINING": {"name": "Mining", "maxLevel": 3, "levels": [
			    {"level": 1, "totalExpRequired": 50},
			    {"level": 2, "totalExpRequired": 175},
			    {"level": 3, "totalExpRequired": 375}
			  ]}
			}}""").getAsJsonObject());

	@Test
	void levelZeroBeforeFirstThreshold() {
		SkillLevel level = TABLE.level("MINING", 25);
		assertEquals(0, level.level());
		assertEquals(0.5, level.progress(), 1e-9);
	}

	@Test
	void progressIsWithinCurrentLevel() {
		SkillLevel level = TABLE.level("MINING", 100);
		assertEquals(1, level.level());
		assertEquals(0.4, level.progress(), 1e-9);
	}

	@Test
	void capsAtMaxLevel() {
		SkillLevel level = TABLE.level("MINING", 10_000);
		assertEquals(3, level.level());
		assertTrue(level.maxed());
		assertEquals(1.0, level.progress());
	}
}
