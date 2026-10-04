package io.github.streetinman.skyblockpv.core.api;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Caches in-flight and finished lookups for a fixed time, so opening /pv on the same player
 * twice costs one API request. Failed lookups are not cached.
 */
public final class TtlCache<K, V> {
	private record Entry<V>(CompletableFuture<V> future, long expiresAt) {
	}

	private final ConcurrentHashMap<K, Entry<V>> entries = new ConcurrentHashMap<>();
	private final Duration ttl;
	private final Clock clock;

	public TtlCache(Duration ttl) {
		this(ttl, Clock.systemUTC());
	}

	public TtlCache(Duration ttl, Clock clock) {
		this.ttl = ttl;
		this.clock = clock;
	}

	public CompletableFuture<V> get(K key, Function<K, CompletableFuture<V>> loader) {
		long now = clock.millis();
		Entry<V> entry = entries.compute(key, (k, existing) -> {
			if (existing != null && existing.expiresAt > now && !existing.future.isCompletedExceptionally()) {
				return existing;
			}
			return new Entry<>(loader.apply(k), now + ttl.toMillis());
		});
		return entry.future;
	}

	public void invalidate(K key) {
		entries.remove(key);
	}
}
