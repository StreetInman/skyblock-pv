package io.github.streetinman.skyblockpv.core.items;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.streetinman.skyblockpv.core.nbt.NbtCompound;
import io.github.streetinman.skyblockpv.core.nbt.Snbt;

class ItemRepositoryTest {
	private static final String HYPERION = """
			{"itemid": "minecraft:iron_sword", "displayname": "§6Hyperion", "damage": 0, "internalname": "HYPERION",
			 "nbttag": "{ExtraAttributes:{id:\\"HYPERION\\"},display:{Lore:[0:\\"§7Damage\\"],Name:\\"§6Hyperion\\"}}",
			 "lore": ["§7Damage"],
			 "recipe": {"A1": "", "A2": "GIANT_FRAGMENT_LASER:8", "A3": "", "B1": "", "B2": "NECRON_BLADE:1", "B3": "", "C1": "", "C2": "WITHER_CATALYST:24", "C3": ""}}
			""";
	private static final String REFINED = """
			{"itemid": "minecraft:diamond", "displayname": "§9Refined Diamond", "internalname": "REFINED_DIAMOND",
			 "recipes": [{"type": "forge", "inputs": ["ENCHANTED_DIAMOND_BLOCK:2"], "count": 1, "duration": 28800},
			             {"type": "npc_shop", "cost": ["SKYBLOCK_COIN:100000"], "result": "REFINED_DIAMOND:1"}]}
			""";

	@Test
	void readsItemsAndRecipesFromZip(@TempDir Path dir) throws Exception {
		Path zip = dir.resolve("repo.zip");
		try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip))) {
			put(out, "NotEnoughUpdates-REPO-master/items/HYPERION.json", HYPERION);
			put(out, "NotEnoughUpdates-REPO-master/items/REFINED_DIAMOND.json", REFINED);
			put(out, "NotEnoughUpdates-REPO-master/constants/other.json", "{}");
			put(out, "NotEnoughUpdates-REPO-master/items/BROKEN.json", "{not json");
		}
		ItemRepository repo = ItemRepository.load(zip);

		assertEquals(List.of("HYPERION", "REFINED_DIAMOND"), repo.all().stream().map(RepoItem::id).toList());
		Recipe craft = repo.get("HYPERION").recipes().getFirst();
		assertEquals("crafting", craft.type());
		assertNull(craft.inputs().get(0));
		assertEquals(new Recipe.Ingredient("GIANT_FRAGMENT_LASER", 8), craft.inputs().get(1));
		assertEquals(new Recipe.Ingredient("WITHER_CATALYST", 24), craft.inputs().get(7));

		List<Recipe> refined = repo.get("REFINED_DIAMOND").recipes();
		assertEquals("forge", refined.get(0).type());
		assertEquals("Forge time: 8h 00m", refined.get(0).note());
		assertEquals("npc_shop", refined.get(1).type());

		assertEquals(List.of("HYPERION"), repo.search("hyper").stream().map(RepoItem::id).toList());
		assertEquals(List.of("REFINED_DIAMOND"), repo.search("refined dia").stream().map(RepoItem::id).toList());
		assertEquals(List.of("HYPERION"), repo.usedIn("NECRON_BLADE").stream().map(RepoItem::id).toList());
	}

	@Test
	void parsesLegacySnbt() {
		NbtCompound tag = Snbt.parseCompound(
				"{HideFlags:254,SkullOwner:{Id:\"abc\",Properties:{textures:[0:{Value:\"dGV4\"}]}},display:{Lore:[0:\"§7a\",1:\"§7b, c\"],Name:\"§aX\"},ExtraAttributes:{id:\"X\",count:5b,big:3L,f:1.5f,arr:[I;1,2]}}");
		assertEquals(254, tag.getInt("HideFlags", 0));
		assertEquals(List.of("§7a", "§7b, c"), tag.getPath("display").orElseThrow().getList("Lore"));
		NbtCompound extra = tag.getPath("ExtraAttributes").orElseThrow();
		assertEquals((byte) 5, extra.get("count"));
		assertEquals(3L, extra.get("big"));
		assertEquals(1.5f, extra.get("f"));
		assertArrayEquals(new int[] {1, 2}, (int[]) extra.get("arr"));
		NbtCompound texture = (NbtCompound) tag.getPath("SkullOwner", "Properties").orElseThrow().getList("textures").getFirst();
		assertEquals("dGV4", texture.getString("Value").orElseThrow());
	}

	private static void put(ZipOutputStream out, String name, String content) throws Exception {
		out.putNextEntry(new ZipEntry(name));
		out.write(content.getBytes(StandardCharsets.UTF_8));
		out.closeEntry();
	}
}
