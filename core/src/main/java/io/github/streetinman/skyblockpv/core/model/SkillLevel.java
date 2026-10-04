package io.github.streetinman.skyblockpv.core.model;

/**
 * @param progress fraction (0–1) of the way to the next level; 1 at max level
 */
public record SkillLevel(String skill, int level, int maxLevel, double xp, double progress) {
	public boolean maxed() {
		return level >= maxLevel;
	}
}
