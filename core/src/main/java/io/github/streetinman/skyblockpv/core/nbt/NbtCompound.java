package io.github.streetinman.skyblockpv.core.nbt;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Read-only view of a decoded NBT compound with forgiving typed getters. */
public final class NbtCompound {
	private final Map<String, Object> values;

	public NbtCompound(Map<String, Object> values) {
		this.values = Collections.unmodifiableMap(values);
	}

	public Set<String> keys() {
		return values.keySet();
	}

	public boolean contains(String key) {
		return values.containsKey(key);
	}

	public Object get(String key) {
		return values.get(key);
	}

	public Optional<NbtCompound> getCompound(String key) {
		return values.get(key) instanceof NbtCompound c ? Optional.of(c) : Optional.empty();
	}

	public Optional<String> getString(String key) {
		return values.get(key) instanceof String s ? Optional.of(s) : Optional.empty();
	}

	/** Any numeric tag as an int, or {@code fallback} if missing. */
	public int getInt(String key, int fallback) {
		return values.get(key) instanceof Number n ? n.intValue() : fallback;
	}

	public long getLong(String key, long fallback) {
		return values.get(key) instanceof Number n ? n.longValue() : fallback;
	}

	/** A list tag, or an empty list if missing or another type. */
	public List<Object> getList(String key) {
		return values.get(key) instanceof List<?> l ? Collections.unmodifiableList(l) : List.of();
	}

	/** Follows a path of compound keys, e.g. {@code getPath("tag", "ExtraAttributes")}. */
	public Optional<NbtCompound> getPath(String... keys) {
		NbtCompound current = this;
		for (String key : keys) {
			Optional<NbtCompound> next = current.getCompound(key);
			if (next.isEmpty()) return Optional.empty();
			current = next.get();
		}
		return Optional.of(current);
	}

	@Override
	public String toString() {
		return values.toString();
	}
}
