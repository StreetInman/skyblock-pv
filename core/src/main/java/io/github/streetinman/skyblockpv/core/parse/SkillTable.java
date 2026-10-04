package io.github.streetinman.skyblockpv.core.parse;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.github.streetinman.skyblockpv.core.model.SkillLevel;

/**
 * Skill XP thresholds, loaded from {@code /v2/resources/skyblock/skills} (no API key needed) so
 * level caps stay current when Hypixel changes them.
 */
public final class SkillTable {
	private final Map<String, double[]> cumulativeXp;
	private final Map<String, Integer> maxLevels;

	private SkillTable(Map<String, double[]> cumulativeXp, Map<String, Integer> maxLevels) {
		this.cumulativeXp = cumulativeXp;
		this.maxLevels = maxLevels;
	}

	public static SkillTable fromResource(JsonObject resource) {
		Map<String, double[]> xp = new HashMap<>();
		Map<String, Integer> max = new HashMap<>();
		JsonObject skills = resource.getAsJsonObject("skills");
		for (Map.Entry<String, JsonElement> entry : skills.entrySet()) {
			JsonObject skill = entry.getValue().getAsJsonObject();
			JsonArray levels = skill.getAsJsonArray("levels");
			double[] thresholds = new double[levels.size()];
			for (int i = 0; i < levels.size(); i++) {
				thresholds[i] = levels.get(i).getAsJsonObject().get("totalExpRequired").getAsDouble();
			}
			xp.put(entry.getKey(), thresholds);
			max.put(entry.getKey(), skill.has("maxLevel") ? skill.get("maxLevel").getAsInt() : thresholds.length);
		}
		return new SkillTable(xp, max);
	}

	public boolean knows(String skill) {
		return cumulativeXp.containsKey(skill);
	}

	/** @param skill upper-case skill name as used in the resource, e.g. {@code FARMING} */
	public SkillLevel level(String skill, double xp) {
		double[] thresholds = cumulativeXp.get(skill);
		if (thresholds == null) {
			throw new IllegalArgumentException("Unknown skill " + skill);
		}
		int maxLevel = Math.min(maxLevels.get(skill), thresholds.length);
		int level = 0;
		while (level < maxLevel && xp >= thresholds[level]) {
			level++;
		}
		if (level >= maxLevel) {
			return new SkillLevel(skill, maxLevel, maxLevel, xp, 1.0);
		}
		double floor = level == 0 ? 0 : thresholds[level - 1];
		double progress = (xp - floor) / (thresholds[level] - floor);
		return new SkillLevel(skill, level, maxLevel, xp, progress);
	}
}
