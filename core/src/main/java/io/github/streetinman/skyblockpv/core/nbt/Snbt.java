package io.github.streetinman.skyblockpv.core.nbt;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses stringified NBT as written by Minecraft 1.8, including its quirks: list elements may
 * carry an index prefix ({@code [0:"a",1:"b"]}), keys and strings may be unquoted, and numbers
 * use type suffixes ({@code 1b}, {@code 3s}, {@code 5L}, {@code 1.0f}).
 */
public final class Snbt {
	private final String s;
	private int pos;

	private Snbt(String s) {
		this.s = s;
	}

	public static NbtCompound parseCompound(String snbt) {
		Snbt p = new Snbt(snbt);
		p.skipWhitespace();
		Object value = p.value();
		if (!(value instanceof NbtCompound c)) throw new IllegalArgumentException("Not a compound: " + snbt);
		return c;
	}

	private Object value() {
		skipWhitespace();
		if (pos >= s.length()) throw error("Unexpected end");
		char c = s.charAt(pos);
		if (c == '{') return compound();
		if (c == '[') return list();
		if (c == '"' || c == '\'') return quoted();
		return scalar(unquoted());
	}

	private NbtCompound compound() {
		expect('{');
		Map<String, Object> map = new LinkedHashMap<>();
		skipWhitespace();
		while (peek() != '}') {
			skipWhitespace();
			String key = peek() == '"' || peek() == '\'' ? quoted() : unquoted();
			skipWhitespace();
			expect(':');
			map.put(key, value());
			skipWhitespace();
			if (peek() == ',') pos++;
			skipWhitespace();
		}
		expect('}');
		return new NbtCompound(map);
	}

	private Object list() {
		expect('[');
		skipWhitespace();
		// Typed arrays: [I;1,2,3], [B;...], [L;...]
		if (pos + 1 < s.length() && s.charAt(pos + 1) == ';' && "BIL".indexOf(s.charAt(pos)) >= 0) {
			char type = s.charAt(pos);
			pos += 2;
			List<Object> values = new ArrayList<>();
			skipWhitespace();
			while (peek() != ']') {
				values.add(value());
				skipWhitespace();
				if (peek() == ',') pos++;
				skipWhitespace();
			}
			expect(']');
			return switch (type) {
				case 'B' -> {
					byte[] out = new byte[values.size()];
					for (int i = 0; i < out.length; i++) out[i] = ((Number) values.get(i)).byteValue();
					yield out;
				}
				case 'L' -> values.stream().mapToLong(v -> ((Number) v).longValue()).toArray();
				default -> values.stream().mapToInt(v -> ((Number) v).intValue()).toArray();
			};
		}
		List<Object> values = new ArrayList<>();
		while (peek() != ']') {
			skipWhitespace();
			skipIndexPrefix();
			values.add(value());
			skipWhitespace();
			if (peek() == ',') pos++;
			skipWhitespace();
		}
		expect(']');
		return values;
	}

	/** 1.8 writes list entries as {@code 0:value}; skip the {@code 0:}. */
	private void skipIndexPrefix() {
		int start = pos;
		while (pos < s.length() && Character.isDigit(s.charAt(pos))) pos++;
		if (pos > start && pos < s.length() && s.charAt(pos) == ':') {
			pos++;
		} else {
			pos = start;
		}
	}

	private String quoted() {
		char quote = s.charAt(pos++);
		StringBuilder out = new StringBuilder();
		while (pos < s.length()) {
			char c = s.charAt(pos++);
			if (c == '\\' && pos < s.length()) {
				out.append(s.charAt(pos++));
			} else if (c == quote) {
				return out.toString();
			} else {
				out.append(c);
			}
		}
		throw error("Unterminated string");
	}

	private String unquoted() {
		int start = pos;
		while (pos < s.length()) {
			char c = s.charAt(pos);
			if (c == ',' || c == '}' || c == ']' || c == ':' || Character.isWhitespace(c)) break;
			pos++;
		}
		return s.substring(start, pos);
	}

	/** Numbers by suffix; anything that isn't a number is kept as a string, as 1.8 did. */
	private static Object scalar(String token) {
		if (token.isEmpty()) return "";
		char last = Character.toLowerCase(token.charAt(token.length() - 1));
		String body = token.substring(0, token.length() - 1);
		try {
			switch (last) {
				case 'b' -> {
					return Byte.parseByte(body);
				}
				case 's' -> {
					return Short.parseShort(body);
				}
				case 'l' -> {
					return Long.parseLong(body);
				}
				case 'f' -> {
					return Float.parseFloat(body);
				}
				case 'd' -> {
					return Double.parseDouble(body);
				}
				default -> {
				}
			}
			if (token.contains(".") || token.contains("e") || token.contains("E")) return Double.parseDouble(token);
			long value = Long.parseLong(token);
			return value == (int) value ? (Object) (int) value : (Object) value;
		} catch (NumberFormatException e) {
			if (token.equals("true")) return (byte) 1;
			if (token.equals("false")) return (byte) 0;
			return token;
		}
	}

	private char peek() {
		if (pos >= s.length()) throw error("Unexpected end");
		return s.charAt(pos);
	}

	private void expect(char c) {
		skipWhitespace();
		if (peek() != c) throw error("Expected '" + c + "'");
		pos++;
	}

	private void skipWhitespace() {
		while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++;
	}

	private IllegalArgumentException error(String message) {
		return new IllegalArgumentException(message + " at " + pos + " in SNBT");
	}
}
