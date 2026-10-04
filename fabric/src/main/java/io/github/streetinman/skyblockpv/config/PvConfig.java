package io.github.streetinman.skyblockpv.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.github.streetinman.skyblockpv.core.api.HypixelClient;

/**
 * Settings stored in {@code config/skyblock-pv.json}.
 *
 * <p>{@code apiKey} is only for development: paste your own key from developer.hypixel.net. It
 * stays on your computer. Release builds will point {@code apiBaseUrl} at the project proxy
 * and leave the key empty.
 */
public final class PvConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public String apiKey = "";
	public String apiBaseUrl = HypixelClient.HYPIXEL_BASE_URL;

	public static PvConfig load(Path file) {
		try {
			if (Files.exists(file)) {
				PvConfig config = GSON.fromJson(Files.readString(file), PvConfig.class);
				if (config != null) return config;
			}
		} catch (IOException | RuntimeException e) {
			io.github.streetinman.skyblockpv.SkyblockPvClient.LOGGER.warn("Couldn't read {}, using defaults", file, e);
		}
		PvConfig config = new PvConfig();
		config.save(file);
		return config;
	}

	public void save(Path file) {
		try {
			Files.createDirectories(file.getParent());
			Files.writeString(file, GSON.toJson(this));
		} catch (IOException e) {
			io.github.streetinman.skyblockpv.SkyblockPvClient.LOGGER.warn("Couldn't save {}", file, e);
		}
	}
}
