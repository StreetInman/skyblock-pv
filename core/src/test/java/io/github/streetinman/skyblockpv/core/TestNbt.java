package io.github.streetinman.skyblockpv.core;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import io.github.streetinman.skyblockpv.core.nbt.NbtType;

/**
 * Writes NBT in the shape Hypixel's API uses, so tests can build inventory blobs without
 * shipping real player data. Compounds are {@code Map<String, Object>}, lists are {@code List}.
 */
public final class TestNbt {
	private TestNbt() {
	}

	/** A 1.8-style item compound. */
	public static Map<String, Object> item(int id, String skyblockId, String name, String... lore) {
		Map<String, Object> display = new LinkedHashMap<>();
		display.put("Name", name);
		display.put("Lore", List.of((Object[]) lore));
		Map<String, Object> extra = new LinkedHashMap<>();
		extra.put("id", skyblockId);
		Map<String, Object> tag = new LinkedHashMap<>();
		tag.put("display", display);
		tag.put("ExtraAttributes", extra);
		Map<String, Object> item = new LinkedHashMap<>();
		item.put("id", (short) id);
		item.put("Count", (byte) 1);
		item.put("Damage", (short) 0);
		item.put("tag", tag);
		return item;
	}

	/** An inventory blob: base64(gzip(NBT {i: [slots…]})). Null slots become empty compounds. */
	public static String inventory(List<Map<String, Object>> slots) {
		List<Object> list = new ArrayList<>();
		for (Map<String, Object> slot : slots) list.add(slot == null ? Map.of() : slot);
		try {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(bytes))) {
				out.writeByte(NbtType.COMPOUND);
				out.writeUTF("");
				writeCompound(out, Map.of("i", list));
			}
			return Base64.getEncoder().encodeToString(bytes.toByteArray());
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	private static void writeCompound(DataOutputStream out, Map<String, Object> map) throws IOException {
		for (Map.Entry<String, Object> e : map.entrySet()) {
			out.writeByte(type(e.getValue()));
			out.writeUTF(e.getKey());
			writePayload(out, e.getValue());
		}
		out.writeByte(NbtType.END);
	}

	@SuppressWarnings("unchecked")
	private static void writePayload(DataOutputStream out, Object value) throws IOException {
		switch (value) {
			case Byte b -> out.writeByte(b);
			case Short s -> out.writeShort(s);
			case Integer i -> out.writeInt(i);
			case Long l -> out.writeLong(l);
			case String s -> out.writeUTF(s);
			case Map<?, ?> m -> writeCompound(out, (Map<String, Object>) m);
			case List<?> list -> {
				out.writeByte(list.isEmpty() ? NbtType.END : type(list.getFirst()));
				out.writeInt(list.size());
				for (Object o : list) writePayload(out, o);
			}
			default -> throw new IllegalArgumentException("Unsupported " + value.getClass());
		}
	}

	private static byte type(Object value) {
		return switch (value) {
			case Byte b -> NbtType.BYTE;
			case Short s -> NbtType.SHORT;
			case Integer i -> NbtType.INT;
			case Long l -> NbtType.LONG;
			case String s -> NbtType.STRING;
			case Map<?, ?> m -> NbtType.COMPOUND;
			case List<?> l -> NbtType.LIST;
			default -> throw new IllegalArgumentException("Unsupported " + value.getClass());
		};
	}
}
