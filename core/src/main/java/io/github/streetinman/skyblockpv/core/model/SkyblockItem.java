package io.github.streetinman.skyblockpv.core.model;

import java.util.List;

import io.github.streetinman.skyblockpv.core.nbt.NbtCompound;

/**
 * One item as Hypixel stores it: legacy (1.8.9) NBT. The GUI converts this into a modern
 * ItemStack; everything here is the raw, version-independent view.
 *
 * @param legacyId     numeric 1.8 item id (e.g. 397 = skull)
 * @param damage       1.8 damage/metadata value
 * @param skyblockId   {@code ExtraAttributes.id}, e.g. {@code HYPERION}; null for vanilla items
 * @param name         display name with § colour codes
 * @param lore         lore lines with § colour codes
 * @param uuid         {@code ExtraAttributes.uuid} if the item has one
 * @param skullTexture base64 texture value for player heads, else null
 * @param nbt          the full item compound, for anything not pulled out above
 */
public record SkyblockItem(
		int legacyId,
		int count,
		int damage,
		String skyblockId,
		String name,
		List<String> lore,
		String uuid,
		String skullTexture,
		NbtCompound nbt) {
}
