package io.github.streetinman.skyblockpv.core.networth;

import java.util.List;
import java.util.Map;

/**
 * A player's estimated networth.
 *
 * @param total      sum of every category
 * @param categories coins per category, in display order
 * @param topItems   the most valuable single items and pets, highest first
 */
public record Networth(double total, Map<Category, Double> categories, List<Valued> topItems) {
	public enum Category {
		PURSE("Purse"), BANK("Bank"), ARMOR("Armor"), EQUIPMENT("Equipment"), WARDROBE("Wardrobe"),
		INVENTORY("Inventory"), ENDER_CHEST("Ender Chest"), BACKPACKS("Backpacks"), ACCESSORIES("Accessories"),
		PERSONAL_VAULT("Personal Vault"), BAGS("Other Bags"), PETS("Pets"), SACKS("Sacks"), ESSENCE("Essence");

		public final String displayName;

		Category(String displayName) {
			this.displayName = displayName;
		}
	}

	/** @param name display name with § colour codes */
	public record Valued(String name, double value) {
	}
}
