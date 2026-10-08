package io.github.streetinman.skyblockpv.core.parse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.github.streetinman.skyblockpv.core.dungeons.DungeonClass;
import io.github.streetinman.skyblockpv.core.dungeons.DungeonData;
import io.github.streetinman.skyblockpv.core.model.AccessoryPower;
import io.github.streetinman.skyblockpv.core.model.Inventories;
import io.github.streetinman.skyblockpv.core.model.MemberData;
import io.github.streetinman.skyblockpv.core.model.Pet;
import io.github.streetinman.skyblockpv.core.model.Profile;
import io.github.streetinman.skyblockpv.core.model.SkyblockItem;
import io.github.streetinman.skyblockpv.core.model.Slayer;
import io.github.streetinman.skyblockpv.core.model.TrophyFish;
import io.github.streetinman.skyblockpv.core.model.TrophyFishing;
import io.github.streetinman.skyblockpv.core.model.Wardrobe;
import io.github.streetinman.skyblockpv.core.model.WardrobeSlot;

/** Parses {@code /v2/skyblock/profiles} responses into {@link Profile}s for one player. */
public final class ProfileParser {
	/** The in-game wardrobe shows 9 slots per page, each slot a column of 4 armor pieces. */
	private static final int WARDROBE_SLOTS_PER_PAGE = 9;
	private static final int WARDROBE_PAGE_SIZE = WARDROBE_SLOTS_PER_PAGE * 4;

	private static final Set<String> KNOWN_INVENTORY_KEYS = Set.of(
			"inv_contents", "inv_armor", "equipment_contents", "ender_chest_contents",
			"wardrobe_contents", "wardrobe_equipped_slot", "bag_contents", "backpack_contents",
			"backpack_icons", "personal_vault_contents", "sacks_counts");

	private ProfileParser() {
	}

	/**
	 * @param response the full JSON body
	 * @param uuid     undashed UUID of the player being viewed
	 * @return every profile the player is a member of; empty if they have never played SkyBlock
	 */
	public static List<Profile> parseProfiles(JsonObject response, String uuid) throws IOException {
		if (!response.has("profiles") || response.get("profiles").isJsonNull()) {
			return List.of();
		}
		List<Profile> profiles = new ArrayList<>();
		for (JsonElement element : response.getAsJsonArray("profiles")) {
			JsonObject profile = element.getAsJsonObject();
			JsonObject members = obj(profile, "members");
			JsonObject member = members == null ? null : obj(members, uuid);
			if (member == null) continue;

			JsonObject banking = obj(profile, "banking");
			profiles.add(new Profile(
					str(profile, "profile_id"),
					str(profile, "cute_name"),
					str(profile, "game_mode"),
					profile.has("selected") && profile.get("selected").getAsBoolean(),
					banking != null && banking.has("balance") ? banking.get("balance").getAsDouble() : null,
					parseMember(uuid, member)));
		}
		return profiles;
	}

	/** The profile the player last played on, falling back to the first. */
	public static Profile selected(List<Profile> profiles) {
		return profiles.stream().filter(Profile::selected).findFirst()
				.orElse(profiles.isEmpty() ? null : profiles.getFirst());
	}

	static MemberData parseMember(String uuid, JsonObject member) throws IOException {
		Map<String, Double> skillXp = new TreeMap<>();
		JsonObject experience = path(member, "player_data", "experience");
		if (experience != null) {
			for (Map.Entry<String, JsonElement> e : experience.entrySet()) {
				if (e.getKey().startsWith("SKILL_")) {
					skillXp.put(e.getKey().substring("SKILL_".length()), e.getValue().getAsDouble());
				}
			}
		}

		return new MemberData(
				uuid,
				num(path(member, "leveling"), "experience") / 100.0,
				num(path(member, "currencies"), "coin_purse"),
				(int) num(path(member, "fairy_soul"), "total_collected"),
				Collections.unmodifiableMap(skillXp),
				parseInventories(obj(member, "inventory")),
				parseTrophyFish(obj(member, "trophy_fish")),
				parseDungeons(member),
				parseSlayers(path(member, "slayer", "slayer_bosses")),
				parseAttributeStacks(path(member, "attributes", "stacks")),
				parsePets(path(member, "pets_data")),
				parseEssence(path(member, "currencies", "essence")),
				parsePower(obj(member, "accessory_bag_storage")));
	}

	static AccessoryPower parsePower(JsonObject storage) {
		if (storage == null) return AccessoryPower.NONE;
		Map<String, Integer> tuning = new java.util.LinkedHashMap<>();
		JsonObject slot = path(storage, "tuning", "slot_0");
		if (slot != null) {
			for (Map.Entry<String, JsonElement> e : slot.entrySet()) {
				if (e.getValue().isJsonPrimitive() && e.getValue().getAsInt() != 0) tuning.put(e.getKey(), e.getValue().getAsInt());
			}
		}
		int unlocked = storage.has("unlocked_powers") && storage.get("unlocked_powers").isJsonArray()
				? storage.getAsJsonArray("unlocked_powers").size() : 0;
		return new AccessoryPower((int) num(storage, "highest_magical_power"), str(storage, "selected_power"),
				Collections.unmodifiableMap(tuning), unlocked);
	}

	static List<Pet> parsePets(JsonObject petsData) {
		if (petsData == null || !petsData.has("pets") || !petsData.get("pets").isJsonArray()) return List.of();
		List<Pet> pets = new ArrayList<>();
		for (JsonElement e : petsData.getAsJsonArray("pets")) {
			if (!e.isJsonObject()) continue;
			JsonObject p = e.getAsJsonObject();
			pets.add(new Pet(str(p, "type"), str(p, "tier"), num(p, "exp"),
					p.has("active") && p.get("active").isJsonPrimitive() && p.get("active").getAsBoolean(),
					str(p, "heldItem"), str(p, "skin")));
		}
		return List.copyOf(pets);
	}

	static Map<String, Long> parseEssence(JsonObject essence) {
		Map<String, Long> result = new TreeMap<>();
		if (essence != null) {
			for (Map.Entry<String, JsonElement> e : essence.entrySet()) {
				if (e.getValue().isJsonObject()) result.put(e.getKey(), (long) num(e.getValue().getAsJsonObject(), "current"));
			}
		}
		return Collections.unmodifiableMap(result);
	}

	static Map<String, Integer> parseAttributeStacks(JsonObject stacks) {
		Map<String, Integer> result = new TreeMap<>();
		if (stacks != null) {
			for (Map.Entry<String, JsonElement> e : stacks.entrySet()) {
				if (e.getValue().isJsonPrimitive()) result.put(e.getKey(), e.getValue().getAsInt());
			}
		}
		return Collections.unmodifiableMap(result);
	}

	static DungeonData parseDungeons(JsonObject member) {
		JsonObject dungeons = obj(member, "dungeons");
		JsonObject cata = path(dungeons, "dungeon_types", "catacombs");
		JsonObject master = path(dungeons, "dungeon_types", "master_catacombs");

		Map<DungeonClass, Double> classXp = new EnumMap<>(DungeonClass.class);
		JsonObject classes = obj(dungeons, "player_classes");
		for (DungeonClass c : DungeonClass.values()) {
			classXp.put(c, num(obj(classes, c.apiKey), "experience"));
		}

		DungeonClass selected = null;
		String selectedKey = dungeons == null ? null : str(dungeons, "selected_dungeon_class");
		for (DungeonClass c : DungeonClass.values()) {
			if (c.apiKey.equals(selectedKey)) selected = c;
		}

		Map<Integer, Long> fastest = new TreeMap<>();
		JsonObject fastestTimes = obj(master, "fastest_time_s_plus");
		if (fastestTimes != null) {
			for (Map.Entry<String, JsonElement> e : fastestTimes.entrySet()) {
				if (isFloor(e.getKey())) fastest.put(Integer.parseInt(e.getKey()), e.getValue().getAsLong());
			}
		}

		Map<String, Integer> perks = new TreeMap<>();
		JsonObject perksJson = path(member, "player_data", "perks");
		if (perksJson != null) {
			for (Map.Entry<String, JsonElement> e : perksJson.entrySet()) {
				if (e.getValue().isJsonPrimitive()) perks.put(e.getKey(), e.getValue().getAsInt());
			}
		}

		return new DungeonData(
				num(cata, "experience"),
				Collections.unmodifiableMap(classXp),
				selected,
				(long) num(dungeons, "secrets"),
				completions(obj(cata, "tier_completions")),
				completions(obj(master, "tier_completions")),
				Collections.unmodifiableMap(fastest),
				Collections.unmodifiableMap(perks));
	}

	private static Map<Integer, Integer> completions(JsonObject tiers) {
		Map<Integer, Integer> result = new TreeMap<>();
		if (tiers != null) {
			for (Map.Entry<String, JsonElement> e : tiers.entrySet()) {
				if (isFloor(e.getKey())) result.put(Integer.parseInt(e.getKey()), e.getValue().getAsInt());
			}
		}
		return Collections.unmodifiableMap(result);
	}

	private static boolean isFloor(String key) {
		return key.matches("\\d+");
	}

	static Map<Slayer, Long> parseSlayers(JsonObject bosses) {
		Map<Slayer, Long> xp = new EnumMap<>(Slayer.class);
		for (Slayer s : Slayer.values()) {
			xp.put(s, (long) num(obj(bosses, s.apiKey), "xp"));
		}
		return Collections.unmodifiableMap(xp);
	}

	static Inventories parseInventories(JsonObject inv) throws IOException {
		if (inv == null) {
			return Inventories.empty();
		}

		List<SkyblockItem> armor = decodeField(inv, "inv_armor");
		if (armor != null) {
			// The API stores armor boots-first; show it helmet-first like the game does.
			armor = new ArrayList<>(armor);
			Collections.reverse(armor);
		}

		Map<Integer, List<SkyblockItem>> backpacks = new TreeMap<>();
		JsonObject backpackContents = obj(inv, "backpack_contents");
		if (backpackContents != null) {
			for (Map.Entry<String, JsonElement> e : backpackContents.entrySet()) {
				List<SkyblockItem> items = decodeData(e.getValue().getAsJsonObject());
				if (items != null) backpacks.put(Integer.parseInt(e.getKey()), items);
			}
		}

		Set<String> unknown = new LinkedHashSet<>();
		for (String key : inv.keySet()) {
			if (!KNOWN_INVENTORY_KEYS.contains(key)) unknown.add(key);
		}

		JsonObject bags = obj(inv, "bag_contents");
		Map<String, List<SkyblockItem>> otherBags = new TreeMap<>();
		if (bags != null) {
			for (String key : bags.keySet()) {
				if (key.equals("talisman_bag")) continue;
				List<SkyblockItem> items = decodeField(bags, key);
				if (items != null) otherBags.put(key, items);
			}
		}
		Map<String, Long> sacks = new TreeMap<>();
		JsonObject sackCounts = obj(inv, "sacks_counts");
		if (sackCounts != null) {
			for (Map.Entry<String, JsonElement> e : sackCounts.entrySet()) {
				if (e.getValue().isJsonPrimitive() && e.getValue().getAsLong() > 0) sacks.put(e.getKey(), e.getValue().getAsLong());
			}
		}
		return new Inventories(
				decodeField(inv, "inv_contents"),
				armor,
				decodeField(inv, "equipment_contents"),
				decodeField(inv, "ender_chest_contents"),
				parseWardrobe(decodeField(inv, "wardrobe_contents"),
						inv.has("wardrobe_equipped_slot") ? inv.get("wardrobe_equipped_slot").getAsInt() : -1),
				bags == null ? null : decodeField(bags, "talisman_bag"),
				Collections.unmodifiableMap(backpacks),
				decodeField(inv, "personal_vault_contents"),
				Collections.unmodifiableMap(otherBags),
				Collections.unmodifiableMap(sacks),
				Collections.unmodifiableSet(unknown));
	}

	/**
	 * Wardrobe items come as pages of 36: rows are helmet, chestplate, leggings, boots and each
	 * of the 9 columns is one slot.
	 */
	static Wardrobe parseWardrobe(List<SkyblockItem> items, int equippedSlot) {
		if (items == null) return null;
		List<WardrobeSlot> slots = new ArrayList<>();
		int pages = (items.size() + WARDROBE_PAGE_SIZE - 1) / WARDROBE_PAGE_SIZE;
		for (int page = 0; page < pages; page++) {
			for (int col = 0; col < WARDROBE_SLOTS_PER_PAGE; col++) {
				int base = page * WARDROBE_PAGE_SIZE + col;
				slots.add(new WardrobeSlot(
						page * WARDROBE_SLOTS_PER_PAGE + col + 1,
						at(items, base),
						at(items, base + WARDROBE_SLOTS_PER_PAGE),
						at(items, base + 2 * WARDROBE_SLOTS_PER_PAGE),
						at(items, base + 3 * WARDROBE_SLOTS_PER_PAGE)));
			}
		}
		return new Wardrobe(List.copyOf(slots), equippedSlot);
	}

	static TrophyFishing parseTrophyFish(JsonObject trophy) {
		Map<TrophyFish, Map<TrophyFish.Tier, Integer>> catches = new EnumMap<>(TrophyFish.class);
		if (trophy == null) {
			return new TrophyFishing(catches, 0, 0);
		}
		for (TrophyFish fish : TrophyFish.values()) {
			Map<TrophyFish.Tier, Integer> tiers = new EnumMap<>(TrophyFish.Tier.class);
			for (TrophyFish.Tier tier : TrophyFish.Tier.values()) {
				String key = fish.apiKey + "_" + tier.apiSuffix();
				if (trophy.has(key)) tiers.put(tier, trophy.get(key).getAsInt());
			}
			if (!tiers.isEmpty()) catches.put(fish, Collections.unmodifiableMap(tiers));
		}
		int rank = 0;
		if (trophy.has("rewards")) {
			for (JsonElement reward : trophy.getAsJsonArray("rewards")) {
				rank = Math.max(rank, reward.getAsInt());
			}
		}
		return new TrophyFishing(Collections.unmodifiableMap(catches), (int) num(trophy, "total_caught"), rank);
	}

	private static List<SkyblockItem> decodeField(JsonObject parent, String key) throws IOException {
		JsonObject container = obj(parent, key);
		return container == null ? null : decodeData(container);
	}

	private static List<SkyblockItem> decodeData(JsonObject container) throws IOException {
		String data = str(container, "data");
		return data == null || data.isEmpty() ? null : Collections.unmodifiableList(ItemDecoder.decodeInventory(data));
	}

	private static SkyblockItem at(List<SkyblockItem> items, int index) {
		return index < items.size() ? items.get(index) : null;
	}

	private static JsonObject obj(JsonObject parent, String key) {
		if (parent == null) return null;
		JsonElement e = parent.get(key);
		return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
	}

	private static JsonObject path(JsonObject root, String... keys) {
		JsonObject current = root;
		for (String key : keys) current = obj(current, key);
		return current;
	}

	private static String str(JsonObject parent, String key) {
		JsonElement e = parent.get(key);
		return e != null && e.isJsonPrimitive() ? e.getAsString() : null;
	}

	private static double num(JsonObject parent, String key) {
		if (parent == null) return 0;
		JsonElement e = parent.get(key);
		return e != null && e.isJsonPrimitive() ? e.getAsDouble() : 0;
	}
}
