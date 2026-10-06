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
 * @param personalVault personal vault contents
 * @param bags        other bags under {@code bag_contents} (fishing bag, quiver…) by API name
 * @param sacks       sack item ID → amount stored
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
		List<SkyblockItem> personalVault,
		Map<String, List<SkyblockItem>> bags,
		Map<String, Long> sacks,
		Set<String> unknownKeys) {

	public static Inventories empty() {
		return new Inventories(null, null, null, null, null, null, Map.of(), null, Map.of(), Map.of(), Set.of());
	}

	public boolean apiEnabled() {
		return inventory != null;
	}
}
