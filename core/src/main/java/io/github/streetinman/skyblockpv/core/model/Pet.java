package io.github.streetinman.skyblockpv.core.model;

/**
 * One pet from {@code pets_data.pets}.
 *
 * @param type     e.g. {@code ENDER_DRAGON}
 * @param tier     rarity, e.g. {@code LEGENDARY}
 * @param heldItem held pet item ID, or null
 * @param skin     skin name without the {@code PET_SKIN_} prefix, or null
 */
public record Pet(String type, String tier, double exp, boolean active, String heldItem, String skin) {
	private static final java.util.List<String> TIERS = java.util.List.of("COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC");

	/** Rarity as the 0–5 index price lists use in keys like {@code ENDER_DRAGON;4}, or -1 if unknown. */
	public int tierIndex() {
		return tier == null ? -1 : TIERS.indexOf(tier);
	}
}
