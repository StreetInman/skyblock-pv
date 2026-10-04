package io.github.streetinman.skyblockpv.core.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class ApiTest {
	@Test
	void mapsHypixelErrors() {
		assertEquals(ApiException.Kind.INVALID_KEY,
				assertThrows(ApiException.class, () -> HypixelClient.parse(403, "{\"success\":false,\"cause\":\"Invalid API key\"}")).kind());
		assertEquals(ApiException.Kind.RATE_LIMITED,
				assertThrows(ApiException.class, () -> HypixelClient.parse(429, "")).kind());
		assertTrue(assertThrows(ApiException.class, () -> HypixelClient.parse(400, "{\"success\":false,\"cause\":\"Malformed UUID\"}"))
				.getMessage().contains("Malformed UUID"));
		assertTrue(HypixelClient.parse(200, "{\"success\":true}").get("success").getAsBoolean());
	}

	@Test
	void cacheReusesUntilExpiryAndRetriesFailures() {
		MutableClock clock = new MutableClock();
		TtlCache<String, String> cache = new TtlCache<>(Duration.ofMinutes(5), clock);
		AtomicInteger loads = new AtomicInteger();

		CompletableFuture<String> first = cache.get("a", k -> CompletableFuture.completedFuture("v" + loads.incrementAndGet()));
		assertSame(first, cache.get("a", k -> CompletableFuture.completedFuture("v" + loads.incrementAndGet())));

		clock.now = clock.now.plus(Duration.ofMinutes(6));
		assertEquals("v2", cache.get("a", k -> CompletableFuture.completedFuture("v" + loads.incrementAndGet())).join());

		cache.get("b", k -> CompletableFuture.failedFuture(new RuntimeException()));
		assertEquals("ok", cache.get("b", k -> CompletableFuture.completedFuture("ok")).join());
	}

	private static final class MutableClock extends Clock {
		Instant now = Instant.EPOCH;

		@Override
		public ZoneOffset getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(java.time.ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return now;
		}
	}
}
