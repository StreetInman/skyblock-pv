package io.github.streetinman.skyblockpv.core.nbt;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Minimal reader for the binary NBT that Hypixel's API returns for inventories
 * (base64 → gzip → NBT). Decodes into plain Java values so the data layer does not
 * depend on Minecraft:
 * <ul>
 *   <li>compound → {@link NbtCompound}</li>
 *   <li>list → {@link List}</li>
 *   <li>byte/short/int/long/float/double → boxed number</li>
 *   <li>string → {@link String}</li>
 *   <li>byte/int/long arrays → {@code byte[]}/{@code int[]}/{@code long[]}</li>
 * </ul>
 */
public final class NbtReader {
	private static final int MAX_DEPTH = 512;

	private NbtReader() {
	}

	/** Decodes an API {@code data} string: base64 of gzipped NBT. */
	public static NbtCompound decodeBase64Gzip(String base64) throws IOException {
		byte[] compressed = Base64.getDecoder().decode(base64);
		try (DataInputStream in = new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(compressed)))) {
			return readRoot(in);
		}
	}

	/** Reads a named root compound, ignoring its (usually empty) name. */
	public static NbtCompound readRoot(DataInput in) throws IOException {
		byte type = in.readByte();
		if (type != NbtType.COMPOUND) {
			throw new IOException("Root tag is not a compound (type " + type + ")");
		}
		in.readUTF();
		return readCompound(in, 0);
	}

	private static NbtCompound readCompound(DataInput in, int depth) throws IOException {
		checkDepth(depth);
		Map<String, Object> values = new LinkedHashMap<>();
		while (true) {
			byte type = in.readByte();
			if (type == NbtType.END) {
				return new NbtCompound(values);
			}
			String name = in.readUTF();
			values.put(name, readPayload(in, type, depth + 1));
		}
	}

	private static Object readPayload(DataInput in, byte type, int depth) throws IOException {
		return switch (type) {
			case NbtType.BYTE -> in.readByte();
			case NbtType.SHORT -> in.readShort();
			case NbtType.INT -> in.readInt();
			case NbtType.LONG -> in.readLong();
			case NbtType.FLOAT -> in.readFloat();
			case NbtType.DOUBLE -> in.readDouble();
			case NbtType.BYTE_ARRAY -> {
				byte[] bytes = new byte[checkLength(in.readInt())];
				in.readFully(bytes);
				yield bytes;
			}
			case NbtType.STRING -> in.readUTF();
			case NbtType.LIST -> readList(in, depth);
			case NbtType.COMPOUND -> readCompound(in, depth);
			case NbtType.INT_ARRAY -> {
				int[] ints = new int[checkLength(in.readInt())];
				for (int i = 0; i < ints.length; i++) ints[i] = in.readInt();
				yield ints;
			}
			case NbtType.LONG_ARRAY -> {
				long[] longs = new long[checkLength(in.readInt())];
				for (int i = 0; i < longs.length; i++) longs[i] = in.readLong();
				yield longs;
			}
			default -> throw new IOException("Unknown NBT tag type " + type);
		};
	}

	private static List<Object> readList(DataInput in, int depth) throws IOException {
		checkDepth(depth);
		byte elementType = in.readByte();
		int length = checkLength(in.readInt());
		List<Object> list = new ArrayList<>(length);
		for (int i = 0; i < length; i++) {
			list.add(readPayload(in, elementType, depth + 1));
		}
		return list;
	}

	private static int checkLength(int length) throws IOException {
		if (length < 0 || length > 1 << 24) {
			throw new IOException("Invalid NBT length " + length);
		}
		return length;
	}

	private static void checkDepth(int depth) throws IOException {
		if (depth > MAX_DEPTH) {
			throw new IOException("NBT nested too deeply");
		}
	}
}
