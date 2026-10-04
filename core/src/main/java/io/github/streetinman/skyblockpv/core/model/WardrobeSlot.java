package io.github.streetinman.skyblockpv.core.model;

/**
 * One wardrobe slot (a column in the in-game wardrobe). Pieces are null when empty.
 *
 * @param number 1-based slot number as shown in game
 */
public record WardrobeSlot(int number, SkyblockItem helmet, SkyblockItem chestplate, SkyblockItem leggings, SkyblockItem boots) {
	public boolean isEmpty() {
		return helmet == null && chestplate == null && leggings == null && boots == null;
	}
}
