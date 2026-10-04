package io.github.streetinman.skyblockpv.core.parse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.github.streetinman.skyblockpv.core.model.SkyblockItem;
import io.github.streetinman.skyblockpv.core.nbt.NbtCompound;
import io.github.streetinman.skyblockpv.core.nbt.NbtReader;

/** Turns Hypixel's inventory {@code data} blobs into {@link SkyblockItem}s. */
public final class ItemDecoder {
	private ItemDecoder() {
	}

	/**
	 * Decodes a base64+gzip NBT inventory. The root holds a list {@code i} with one compound
	 * per slot; empty slots are empty compounds and come back as null.
	 */
	public static List<SkyblockItem> decodeInventory(String base64) throws IOException {
		NbtCompound root = NbtReader.decodeBase64Gzip(base64);
		List<SkyblockItem> items = new ArrayList<>();
		for (Object slot : root.getList("i")) {
			items.add(slot instanceof NbtCompound c ? decodeItem(c) : null);
		}
		return items;
	}

	/** Decodes one slot compound, or returns null for an empty slot. */
	public static SkyblockItem decodeItem(NbtCompound slot) {
		if (!slot.contains("id")) {
			return null;
		}
		NbtCompound tag = slot.getCompound("tag").orElse(null);
		NbtCompound display = tag == null ? null : tag.getCompound("display").orElse(null);
		NbtCompound extra = tag == null ? null : tag.getCompound("ExtraAttributes").orElse(null);

		List<String> lore = new ArrayList<>();
		if (display != null) {
			for (Object line : display.getList("Lore")) {
				if (line instanceof String s) lore.add(s);
			}
		}

		return new SkyblockItem(
				slot.getInt("id", 0),
				slot.getInt("Count", 1),
				slot.getInt("Damage", 0),
				extra == null ? null : extra.getString("id").orElse(null),
				display == null ? null : display.getString("Name").orElse(null),
				List.copyOf(lore),
				extra == null ? null : extra.getString("uuid").orElse(null),
				tag == null ? null : skullTexture(tag),
				slot);
	}

	private static String skullTexture(NbtCompound tag) {
		for (Object texture : tag.getPath("SkullOwner", "Properties").map(p -> p.getList("textures")).orElse(List.of())) {
			if (texture instanceof NbtCompound t && t.getString("Value").isPresent()) {
				return t.getString("Value").get();
			}
		}
		return null;
	}

	/** Decodes the skin URL out of a skull texture value, or null if it can't. */
	public static String skinUrl(String textureValue) {
		try {
			String json = new String(Base64.getDecoder().decode(textureValue), java.nio.charset.StandardCharsets.UTF_8);
			JsonObject root = JsonParser.parseString(json).getAsJsonObject();
			return root.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();
		} catch (RuntimeException e) {
			return null;
		}
	}
}
