package io.github.streetinman.skyblockpv.core.api;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Resolves usernames to UUIDs through Mojang's public API. */
public final class MojangClient {
	private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");

	public record PlayerId(String uuid, String name) {
	}

	private final HttpClient http;
	private final TtlCache<String, PlayerId> cache = new TtlCache<>(Duration.ofHours(1));

	public MojangClient(HttpClient http) {
		this.http = http;
	}

	public CompletableFuture<PlayerId> lookup(String username) {
		if (!VALID_NAME.matcher(username).matches()) {
			return CompletableFuture.failedFuture(new ApiException(ApiException.Kind.PLAYER_NOT_FOUND, "Invalid username: " + username));
		}
		return cache.get(username.toLowerCase(Locale.ROOT), this::fetch);
	}

	private CompletableFuture<PlayerId> fetch(String username) {
		HttpRequest request = HttpRequest.newBuilder(
						URI.create("https://api.mojang.com/users/profiles/minecraft/" + URLEncoder.encode(username, StandardCharsets.UTF_8)))
				.timeout(Duration.ofSeconds(10))
				.GET().build();
		return http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
				.handle((response, error) -> {
					if (error != null) {
						throw new ApiException(ApiException.Kind.NETWORK, "Couldn't reach Mojang", error);
					}
					if (response.statusCode() == 204 || response.statusCode() == 404) {
						throw new ApiException(ApiException.Kind.PLAYER_NOT_FOUND, "No player named " + username);
					}
					if (response.statusCode() != 200) {
						throw new ApiException(ApiException.Kind.HTTP, "Mojang returned HTTP " + response.statusCode());
					}
					JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
					return new PlayerId(body.get("id").getAsString(), body.get("name").getAsString());
				});
	}
}
