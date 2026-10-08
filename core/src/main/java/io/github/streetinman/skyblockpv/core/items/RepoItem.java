package io.github.streetinman.skyblockpv.core.items;

import java.util.List;
import java.util.Locale;

/**
 * One SkyBlock item from the item repository.
 *
 * @param id          SkyBlock ID, e.g. {@code HYPERION}
 * @param name        display name with § colour codes
 * @param minecraftId 1.8 item ID, e.g. {@code minecraft:skull}
 * @param nbt         1.8 SNBT item tag, parsed only when the icon is first drawn
 * @param recipes     every way to get the item that the repository knows about
 */
public record RepoItem(String id, String name, String minecraftId, int damage, String nbt, List<String> lore, List<Recipe> recipes) {
	/** Name without colour codes, lower case, for searching. */
	public String searchText() {
		return (name.replaceAll("§.", "") + " " + id).toLowerCase(Locale.ROOT);
	}

	public String plainName() {
		return name.replaceAll("§.", "");
	}
}
