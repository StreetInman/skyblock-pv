package io.github.streetinman.skyblockpv.core.model;

import java.util.List;

/**
 * @param slots        every wardrobe slot in order, including empty ones
 * @param equippedSlot 1-based number of the slot currently worn, or -1 if none. The worn set
 *                     lives in the armor inventory, so that slot here is empty.
 */
public record Wardrobe(List<WardrobeSlot> slots, int equippedSlot) {
}
