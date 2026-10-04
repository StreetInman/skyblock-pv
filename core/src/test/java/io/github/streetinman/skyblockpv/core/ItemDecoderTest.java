package io.github.streetinman.skyblockpv.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.streetinman.skyblockpv.core.model.SkyblockItem;
import io.github.streetinman.skyblockpv.core.parse.ItemDecoder;

class ItemDecoderTest {
	@Test
	void decodesItemsAndKeepsEmptySlots() throws Exception {
		String blob = TestNbt.inventory(Arrays.asList(
				TestNbt.item(267, "HYPERION", "§dHeroic Hyperion", "§7Damage: §c+260", "§d§lMYTHIC DUNGEON SWORD"),
				null,
				TestNbt.item(397, "ENDER_ARTIFACT", "§6Ender Artifact")));

		List<SkyblockItem> items = ItemDecoder.decodeInventory(blob);

		assertEquals(3, items.size());
		SkyblockItem sword = items.get(0);
		assertEquals(267, sword.legacyId());
		assertEquals("HYPERION", sword.skyblockId());
		assertEquals("§dHeroic Hyperion", sword.name());
		assertEquals(List.of("§7Damage: §c+260", "§d§lMYTHIC DUNGEON SWORD"), sword.lore());
		assertNull(items.get(1));
		assertEquals("ENDER_ARTIFACT", items.get(2).skyblockId());
	}

	@Test
	void readsSkinUrlFromTexture() {
		String texture = java.util.Base64.getEncoder().encodeToString(
				"{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/abc\"}}}".getBytes());
		assertEquals("http://textures.minecraft.net/texture/abc", ItemDecoder.skinUrl(texture));
		assertNull(ItemDecoder.skinUrl("not base64 json"));
	}
}
