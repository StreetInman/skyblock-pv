package io.github.streetinman.skyblockpv.core.networth;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.github.streetinman.skyblockpv.core.model.Inventories;
import io.github.streetinman.skyblockpv.core.model.MemberData;
import io.github.streetinman.skyblockpv.core.model.Pet;
import io.github.streetinman.skyblockpv.core.model.SkyblockItem;
import io.github.streetinman.skyblockpv.core.model.WardrobeSlot;
import io.github.streetinman.skyblockpv.core.nbt.NbtCompound;
import io.github.streetinman.skyblockpv.core.networth.Networth.Category;

/**
 * Estimates networth the way networth mods and sites do: each item is worth its lowest BIN or
 * bazaar price plus what was applied to it (enchantments, recombobulator, potato books,
 * gemstones, master stars, scrolls…). It's an estimate: it can't know what a buyer would pay
 * for a specific roll, and dungeon stars bought with essence aren't counted.
 */
public final class NetworthCalculator {
	private static final int TOP_ITEMS = 5;
	private static final String[] MASTER_STARS = {"FIRST_MASTER_STAR", "SECOND_MASTER_STAR", "THIRD_MASTER_STAR", "FOURTH_MASTER_STAR", "FIFTH_MASTER_STAR"};
	/** Gem slot types that hold whichever gemstone is put in, recorded under {@code <slot>_gem}. */
	private static final Set<String> FLEXIBLE_GEM_SLOTS = Set.of("COMBAT", "OFFENSIVE", "DEFENSIVE", "MINING", "UNIVERSAL", "CHISEL");

	private NetworthCalculator() {
	}

	public static Networth calculate(MemberData m, Double bankBalance, Prices prices) {
		Map<Category, Double> totals = new EnumMap<>(Category.class);
		List<Networth.Valued> valued = new ArrayList<>();
		Inventories inv = m.inventories();

		totals.put(Category.PURSE, m.purse());
		totals.put(Category.BANK, bankBalance == null ? 0 : bankBalance);
		addItems(totals, valued, Category.ARMOR, inv.armor(), prices);
		addItems(totals, valued, Category.EQUIPMENT, inv.equipment(), prices);
		if (inv.wardrobe() != null) {
			addItems(totals, valued, Category.WARDROBE, inv.wardrobe().slots().stream()
					.flatMap((WardrobeSlot s) -> java.util.stream.Stream.of(s.helmet(), s.chestplate(), s.leggings(), s.boots()))
					.filter(Objects::nonNull).toList(), prices);
		}
		addItems(totals, valued, Category.INVENTORY, inv.inventory(), prices);
		addItems(totals, valued, Category.ENDER_CHEST, inv.enderChest(), prices);
		for (List<SkyblockItem> backpack : inv.backpacks().values()) addItems(totals, valued, Category.BACKPACKS, backpack, prices);
		addItems(totals, valued, Category.ACCESSORIES, inv.accessoryBag(), prices);
		addItems(totals, valued, Category.PERSONAL_VAULT, inv.personalVault(), prices);
		for (List<SkyblockItem> bag : inv.bags().values()) addItems(totals, valued, Category.BAGS, bag, prices);

		for (Pet pet : m.pets()) {
			double value = petValue(pet, prices);
			totals.merge(Category.PETS, value, Double::sum);
			if (value > 0) valued.add(new Networth.Valued(petName(pet), value));
		}
		for (Map.Entry<String, Long> sack : inv.sacks().entrySet()) {
			totals.merge(Category.SACKS, prices.of(sack.getKey()) * sack.getValue(), Double::sum);
		}
		for (Map.Entry<String, Long> essence : m.essence().entrySet()) {
			totals.merge(Category.ESSENCE, prices.of("ESSENCE_" + essence.getKey().toUpperCase(Locale.ROOT)) * essence.getValue(), Double::sum);
		}

		Map<Category, Double> ordered = new EnumMap<>(Category.class);
		for (Category c : Category.values()) ordered.put(c, totals.getOrDefault(c, 0.0));
		double total = ordered.values().stream().mapToDouble(Double::doubleValue).sum();
		valued.sort(Comparator.comparingDouble(Networth.Valued::value).reversed());
		return new Networth(total, ordered, List.copyOf(valued.subList(0, Math.min(TOP_ITEMS, valued.size()))));
	}

	private static void addItems(Map<Category, Double> totals, List<Networth.Valued> valued, Category category,
			List<SkyblockItem> items, Prices prices) {
		if (items == null) return;
		for (SkyblockItem item : items) {
			if (item == null) continue;
			double value = itemValue(item, prices);
			totals.merge(category, value, Double::sum);
			if (value > 0) valued.add(new Networth.Valued(item.name(), value));
		}
	}

	/** Coins one stack is worth, including everything applied to it. */
	public static double itemValue(SkyblockItem item, Prices prices) {
		String id = item.skyblockId();
		if (id == null) return 0;
		NbtCompound extra = item.nbt().getPath("tag", "ExtraAttributes").orElse(null);
		if (extra == null) return prices.of(id) * item.count();

		if (id.equals("PET")) {
			return extra.getString("petInfo").map(NetworthCalculator::petFromInfo).map(p -> petValue(p, prices)).orElse(0.0);
		}

		double enchants = 0;
		NbtCompound enchantments = extra.getCompound("enchantments").orElse(null);
		if (enchantments != null) {
			for (String name : enchantments.keys()) {
				enchants += prices.of("ENCHANTMENT_" + name.toUpperCase(Locale.ROOT) + "_" + enchantments.getInt(name, 0));
			}
		}
		// A book's whole value is its enchantments.
		if (id.equals("ENCHANTED_BOOK")) return enchants * item.count();

		double value = prices.of(id) * item.count() + enchants;
		if (extra.getInt("rarity_upgrades", 0) > 0) value += prices.of("RECOMBOBULATOR_3000");

		int potatoBooks = extra.getInt("hot_potato_count", 0);
		value += Math.min(potatoBooks, 10) * prices.of("HOT_POTATO_BOOK");
		value += Math.max(0, potatoBooks - 10) * prices.of("FUMING_POTATO_BOOK");
		value += extra.getInt("art_of_war_count", 0) * prices.of("THE_ART_OF_WAR");
		value += extra.getInt("wood_singularity_count", 0) * prices.of("WOOD_SINGULARITY");
		value += extra.getInt("transmission_tuner_count", 0) * prices.of("TRANSMISSION_TUNER");
		value += extra.getInt("mana_disintegrator_count", 0) * prices.of("MANA_DISINTEGRATOR");
		if (extra.getInt("ethermerge", 0) > 0) value += prices.of("ETHERWARP_CONDUIT") + prices.of("ETHERWARP_MERGER");

		int stars = Math.max(extra.getInt("upgrade_level", 0), extra.getInt("dungeon_item_level", 0));
		for (int i = 0; i < Math.min(Math.max(0, stars - 5), MASTER_STARS.length); i++) value += prices.of(MASTER_STARS[i]);

		for (Object scroll : extra.getList("ability_scroll")) {
			if (scroll instanceof String s) value += prices.of(s);
		}
		value += extra.getString("power_ability_scroll").map(prices::of).orElse(0.0);
		value += extra.getString("talisman_enrichment").map(e -> prices.of("TALISMAN_ENRICHMENT_" + e.toUpperCase(Locale.ROOT))).orElse(0.0);
		value += extra.getString("dye_item").map(prices::of).orElse(0.0);
		value += extra.getString("skin").map(prices::of).orElse(0.0);
		value += extra.getCompound("gems").map(g -> gemsValue(g, prices)).orElse(0.0);
		return value;
	}

	/** Gem slots look like {@code RUBY_0: "FINE"} or, for flexible slots, {@code COMBAT_0: "PERFECT"} plus {@code COMBAT_0_gem: "JASPER"}. */
	static double gemsValue(NbtCompound gems, Prices prices) {
		double value = 0;
		for (String slot : gems.keys()) {
			if (slot.endsWith("_gem") || slot.equals("unlocked_slots")) continue;
			Object raw = gems.get(slot);
			String quality = raw instanceof String s ? s
					: raw instanceof NbtCompound c ? c.getString("quality").orElse(null) : null;
			if (quality == null) continue;
			String type = slot.contains("_") ? slot.substring(0, slot.lastIndexOf('_')) : slot;
			if (FLEXIBLE_GEM_SLOTS.contains(type)) type = gems.getString(slot + "_gem").orElse(null);
			if (type != null) value += prices.of(quality + "_" + type + "_GEM");
		}
		return value;
	}

	static double petValue(Pet pet, Prices prices) {
		if (pet.type() == null) return 0;
		double value = prices.of(pet.type() + ";" + pet.tierIndex());
		if (pet.heldItem() != null) value += prices.of(pet.heldItem());
		if (pet.skin() != null) value += prices.of("PET_SKIN_" + pet.skin());
		return value;
	}

	private static Pet petFromInfo(String json) {
		try {
			JsonObject o = JsonParser.parseString(json).getAsJsonObject();
			return new Pet(string(o, "type"), string(o, "tier"), o.has("exp") ? o.get("exp").getAsDouble() : 0, false,
					string(o, "heldItem"), string(o, "skin"));
		} catch (RuntimeException e) {
			return new Pet(null, null, 0, false, null, null);
		}
	}

	private static String string(JsonObject o, String key) {
		return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : null;
	}

	private static String petName(Pet pet) {
		String type = pet.type().charAt(0) + pet.type().substring(1).toLowerCase(Locale.ROOT).replace('_', ' ');
		String[] colours = {"§f", "§a", "§9", "§5", "§6", "§d"};
		int tier = pet.tierIndex();
		return (tier >= 0 ? colours[tier] : "§f") + type + " Pet";
	}
}
