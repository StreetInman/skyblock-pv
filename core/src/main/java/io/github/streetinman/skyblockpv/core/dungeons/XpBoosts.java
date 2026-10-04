package io.github.streetinman.skyblockpv.core.dungeons;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import io.github.streetinman.skyblockpv.core.model.Inventories;
import io.github.streetinman.skyblockpv.core.model.SkyblockItem;
import io.github.streetinman.skyblockpv.core.nbt.NbtCompound;

/**
 * Every boost the dungeon calculator applies.
 *
 * @param hecatombLevel     highest Hecatomb enchant found on the player's items (0–10)
 * @param scarfBonus        class XP from the best Scarf accessory: Studies 2%, Thesis 4%, Grimoire 6%
 * @param expertRing        whether the Catacombs Expert Ring is in the accessory bag (+10% Catacombs XP)
 * @param classPerkLevels   essence-shop perk level per class, +2% class XP per level
 * @param mayorMultiplier   1.5 while Derpy is mayor, otherwise 1
 * @param globalMultiplier  manual: active global dungeon XP boost (1.0 = none)
 * @param extraClassBonus   manual: any other class XP bonus not visible through the API
 * @param teamShare         fraction of a run's class XP that the classes you didn't play receive
 */
public record XpBoosts(
		int hecatombLevel,
		double scarfBonus,
		boolean expertRing,
		Map<DungeonClass, Integer> classPerkLevels,
		double mayorMultiplier,
		double globalMultiplier,
		double extraClassBonus,
		double teamShare) {

	public static final double DEFAULT_TEAM_SHARE = 0.25;

	/** Hecatomb X is 2%; each level below that is 0.16% less. */
	public double hecatomb() {
		return hecatombLevel <= 0 ? 0 : 0.004 + 0.0016 * Math.min(hecatombLevel, 10);
	}

	public double classPerk(DungeonClass c) {
		return classPerkLevels.getOrDefault(c, 0) * 0.02;
	}

	/**
	 * Works out boosts from the profile: perks from {@code player_data.perks}, Hecatomb from any
	 * item the API shows, Scarf accessories and the Expert Ring from the accessory bag.
	 *
	 * @param mayorName current SkyBlock mayor, or null if unknown
	 */
	public static XpBoosts detect(DungeonData dungeons, Inventories inv, String mayorName,
			double globalMultiplier, double extraClassBonus, double teamShare) {
		Map<DungeonClass, Integer> perks = new EnumMap<>(DungeonClass.class);
		for (DungeonClass c : DungeonClass.values()) {
			perks.put(c, dungeons.perks().getOrDefault(c.perkKey, 0));
		}

		int hecatomb = allItems(inv).mapToInt(XpBoosts::hecatombLevel).max().orElse(0);

		double scarf = 0;
		boolean ring = false;
		if (inv.accessoryBag() != null) {
			for (SkyblockItem item : inv.accessoryBag()) {
				if (item == null || item.skyblockId() == null) continue;
				switch (item.skyblockId()) {
					case "SCARF_GRIMOIRE" -> scarf = Math.max(scarf, 0.06);
					case "SCARF_THESIS" -> scarf = Math.max(scarf, 0.04);
					case "SCARF_STUDIES" -> scarf = Math.max(scarf, 0.02);
					case "CATACOMBS_EXPERT_RING" -> ring = true;
					default -> {
					}
				}
			}
		}

		double mayor = "Derpy".equalsIgnoreCase(mayorName) ? 1.5 : 1.0;
		return new XpBoosts(hecatomb, scarf, ring, perks, mayor, globalMultiplier, extraClassBonus, teamShare);
	}

	private static Stream<SkyblockItem> allItems(Inventories inv) {
		Stream<List<SkyblockItem>> lists = Stream.of(inv.inventory(), inv.armor(), inv.equipment(), inv.enderChest(), inv.accessoryBag());
		Stream<SkyblockItem> wardrobe = inv.wardrobe() == null ? Stream.empty()
				: inv.wardrobe().slots().stream().flatMap(s -> Stream.of(s.helmet(), s.chestplate(), s.leggings(), s.boots()));
		return Stream.concat(
						Stream.concat(lists.filter(Objects::nonNull).flatMap(List::stream), inv.backpacks().values().stream().flatMap(List::stream)),
						wardrobe)
				.filter(Objects::nonNull);
	}

	private static int hecatombLevel(SkyblockItem item) {
		return item.nbt().getPath("tag", "ExtraAttributes", "enchantments")
				.map((NbtCompound e) -> e.getInt("hecatomb", 0))
				.orElse(0);
	}
}
