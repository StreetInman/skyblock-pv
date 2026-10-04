package io.github.streetinman.skyblockpv.core.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Decoded item containers for one profile member. Lists keep empty slots as null so grids
 * line up with the game. A null container means the API did not include it (usually because
 * the player turned off inventory API access).
 *
 * @param armor       helmet, chestplate, leggings, boots (re-ordered from the API's boots-first)
 * @param equipment   necklace, cloak, belt, gloves/bracelet
 * @param backpacks   backpack index → contents
 * @param unknownKeys keys under {@code inventory} this version doesn't understand yet
 *                    (e.g. new features such as loadouts), surfaced for debugging
 */
public record Inventories(
		List<SkyblockItem> inventory,
		List<SkyblockItem> armor,
		List<SkyblockItem> equipment,
		List<SkyblockItem> enderChest,
		Wardrobe wardrobe,
		List<SkyblockItem> accessoryBag,
		Map<Integer, List<SkyblockItem>> backpacks,
		Set<String> unknownKeys) {

	public boolean apiEnabled() {
		return inventory != null;
	}
}
