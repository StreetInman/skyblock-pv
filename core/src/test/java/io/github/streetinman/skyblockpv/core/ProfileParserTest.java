package io.github.streetinman.skyblockpv.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.github.streetinman.skyblockpv.core.model.Inventories;
import io.github.streetinman.skyblockpv.core.model.MemberData;
import io.github.streetinman.skyblockpv.core.model.Profile;
import io.github.streetinman.skyblockpv.core.model.TrophyFish;
import io.github.streetinman.skyblockpv.core.model.TrophyFishing;
import io.github.streetinman.skyblockpv.core.model.Wardrobe;
import io.github.streetinman.skyblockpv.core.parse.ProfileParser;

/** Uses synthetic responses shaped like /v2/skyblock/profiles. Swap in real captures as they're collected. */
class ProfileParserTest {
	private static final String UUID = "0123456789abcdef0123456789abcdef";

	@Test
	void parsesSelectedProfileBasics() throws Exception {
		List<Profile> profiles = ProfileParser.parseProfiles(response(member(true), false), UUID);

		assertEquals(2, profiles.size());
		Profile selected = ProfileParser.selected(profiles);
		assertEquals("Mango", selected.cuteName());
		assertEquals(12_345_678.0, selected.bankBalance());
		MemberData m = selected.member();
		assertEquals(245.5, m.skyblockLevel(), 1e-9);
		assertEquals(1_000_000.0, m.purse());
		assertEquals(240, m.fairySouls());
		assertEquals(55_172_425.0, m.skillXp().get("FARMING"));
	}

	@Test
	void decodesArmorEquipmentAndFlagsUnknownKeys() throws Exception {
		Inventories inv = ProfileParser.parseProfiles(response(member(true), false), UUID).get(1).member().inventories();

		assertTrue(inv.apiEnabled());
		// API order is boots-first; we flip it.
		assertEquals("HELMET", inv.armor().get(0).skyblockId());
		assertEquals("BOOTS", inv.armor().get(3).skyblockId());
		assertEquals(List.of("NECKLACE", "CLOAK", "BELT", "GLOVES"),
				inv.equipment().stream().map(i -> i.skyblockId()).toList());
		assertEquals("ENDER_ITEM", inv.backpacks().get(0).getFirst().skyblockId());
		assertTrue(inv.unknownKeys().contains("loadouts_preview"));
	}

	@Test
	void splitsWardrobePagesIntoSlots() throws Exception {
		Wardrobe wardrobe = ProfileParser.parseProfiles(response(member(true), false), UUID).get(1).member().inventories().wardrobe();

		assertEquals(18, wardrobe.slots().size());
		assertEquals(3, wardrobe.equippedSlot());
		var slot2 = wardrobe.slots().get(1);
		assertEquals(2, slot2.number());
		assertEquals("W_HELM_2", slot2.helmet().skyblockId());
		assertEquals("W_BOOTS_2", slot2.boots().skyblockId());
		var slot11 = wardrobe.slots().get(10);
		assertEquals(11, slot11.number());
		assertEquals("W_CHEST_11", slot11.chestplate().skyblockId());
		assertTrue(wardrobe.slots().get(4).isEmpty());
	}

	@Test
	void parsesTrophyFishing() throws Exception {
		TrophyFishing tf = ProfileParser.parseProfiles(response(member(true), false), UUID).get(1).member().trophyFishing();

		assertEquals(57, tf.totalCaught());
		assertEquals(2, tf.rank());
		assertEquals("Silver Hunter", tf.rankName());
		assertEquals(40, tf.count(TrophyFish.BLOBFISH, TrophyFish.Tier.BRONZE));
		assertEquals(TrophyFish.Tier.DIAMOND, tf.bestTier(TrophyFish.GOLDEN_FISH));
		assertNull(tf.bestTier(TrophyFish.VANILLE));
		assertEquals(2, tf.fishAtLeast(TrophyFish.Tier.BRONZE));
		assertEquals(1, tf.fishAtLeast(TrophyFish.Tier.GOLD));
	}

	@Test
	void handlesInventoryApiDisabled() throws Exception {
		Inventories inv = ProfileParser.parseProfiles(response(member(false), false), UUID).get(1).member().inventories();
		assertFalse(inv.apiEnabled());
		assertNull(inv.wardrobe());
	}

	@Test
	void noProfilesMeansEmptyList() throws Exception {
		assertTrue(ProfileParser.parseProfiles(response(null, true), UUID).isEmpty());
	}

	private static JsonObject response(JsonObject member, boolean nullProfiles) {
		JsonObject root = new JsonObject();
		root.addProperty("success", true);
		if (nullProfiles) {
			root.add("profiles", com.google.gson.JsonNull.INSTANCE);
			return root;
		}
		JsonArray profiles = new JsonArray();
		profiles.add(profile("a", "Apple", false, new JsonObject()));
		JsonObject mango = profile("b", "Mango", true, member);
		JsonObject banking = new JsonObject();
		banking.addProperty("balance", 12_345_678.0);
		mango.add("banking", banking);
		profiles.add(mango);
		root.add("profiles", profiles);
		return root;
	}

	private static JsonObject profile(String id, String name, boolean selected, JsonObject member) {
		JsonObject p = new JsonObject();
		p.addProperty("profile_id", id);
		p.addProperty("cute_name", name);
		p.addProperty("selected", selected);
		JsonObject members = new JsonObject();
		members.add(UUID, member);
		p.add("members", members);
		return p;
	}

	private static JsonObject member(boolean inventoryApi) {
		JsonObject m = new JsonObject();
		m.add("leveling", obj("experience", 24_550));
		m.add("currencies", obj("coin_purse", 1_000_000));
		m.add("fairy_soul", obj("total_collected", 240));
		JsonObject playerData = new JsonObject();
		playerData.add("experience", obj("SKILL_FARMING", 55_172_425));
		m.add("player_data", playerData);

		JsonObject trophy = new JsonObject();
		trophy.addProperty("total_caught", 57);
		trophy.addProperty("blobfish_bronze", 40);
		trophy.addProperty("blobfish_silver", 10);
		trophy.addProperty("golden_fish_diamond", 1);
		JsonArray rewards = new JsonArray();
		rewards.add(1);
		rewards.add(2);
		trophy.add("rewards", rewards);
		m.add("trophy_fish", trophy);

		if (inventoryApi) {
			JsonObject inv = new JsonObject();
			inv.add("inv_contents", data(Arrays.asList(TestNbt.item(1, "STONE_ITEM", "§fStone"))));
			inv.add("inv_armor", data(Arrays.asList(
					TestNbt.item(301, "BOOTS", "Boots"), TestNbt.item(300, "LEGGINGS", "Leggings"),
					TestNbt.item(299, "CHESTPLATE", "Chestplate"), TestNbt.item(298, "HELMET", "Helmet"))));
			inv.add("equipment_contents", data(Arrays.asList(
					TestNbt.item(397, "NECKLACE", "N"), TestNbt.item(397, "CLOAK", "C"),
					TestNbt.item(397, "BELT", "B"), TestNbt.item(397, "GLOVES", "G"))));
			inv.add("wardrobe_contents", data(wardrobePages()));
			inv.addProperty("wardrobe_equipped_slot", 3);
			JsonObject backpacks = new JsonObject();
			backpacks.add("0", data(Arrays.asList(TestNbt.item(368, "ENDER_ITEM", "Pearl"))));
			inv.add("backpack_contents", backpacks);
			inv.add("loadouts_preview", new JsonObject());
			m.add("inventory", inv);
		}
		return m;
	}

	/** Two pages; slot 2 has a full set, slot 11 a chestplate, slot 5 nothing. */
	private static List<Map<String, Object>> wardrobePages() {
		List<Map<String, Object>> items = new ArrayList<>();
		for (int i = 0; i < 72; i++) items.add(null);
		items.set(1, TestNbt.item(298, "W_HELM_2", "h"));
		items.set(1 + 9, TestNbt.item(299, "W_CHEST_2", "c"));
		items.set(1 + 18, TestNbt.item(300, "W_LEGS_2", "l"));
		items.set(1 + 27, TestNbt.item(301, "W_BOOTS_2", "b"));
		items.set(36 + 1 + 9, TestNbt.item(299, "W_CHEST_11", "c"));
		return items;
	}

	private static JsonObject data(List<Map<String, Object>> items) {
		JsonObject o = new JsonObject();
		o.addProperty("type", 0);
		o.addProperty("data", TestNbt.inventory(items));
		return o;
	}

	private static JsonObject obj(String key, Number value) {
		JsonObject o = new JsonObject();
		o.addProperty(key, value);
		return o;
	}
}
