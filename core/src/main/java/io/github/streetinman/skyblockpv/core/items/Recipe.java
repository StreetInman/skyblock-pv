package io.github.streetinman.skyblockpv.core.items;

import java.util.List;

/**
 * One way to get an item.
 *
 * @param type   {@code crafting}, {@code forge}, {@code npc_shop}, {@code trade}, {@code drops}, …
 * @param inputs crafting: 9 slots row by row (null = empty); other types: the ingredients or cost
 * @param output what you get, usually the item itself
 * @param note   extra detail to show, e.g. forge duration or the mob a drop comes from
 */
public record Recipe(String type, List<Ingredient> inputs, Ingredient output, String note) {
	public record Ingredient(String id, int count) {
		/** Parses NEU's {@code ID:count} (count may be fractional, e.g. drop amounts; rounded up). */
		public static Ingredient parse(String raw) {
			if (raw == null || raw.isBlank()) return null;
			int colon = raw.lastIndexOf(':');
			if (colon < 0) return new Ingredient(raw, 1);
			try {
				return new Ingredient(raw.substring(0, colon), (int) Math.ceil(Double.parseDouble(raw.substring(colon + 1))));
			} catch (NumberFormatException e) {
				return new Ingredient(raw, 1);
			}
		}
	}

	public boolean isCrafting() {
		return type.equals("crafting");
	}
}
