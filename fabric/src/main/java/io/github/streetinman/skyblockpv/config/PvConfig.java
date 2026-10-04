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

	// Dungeon calculator. Perks, Hecatomb, Scarf, Expert Ring, shard attributes and Derpy are read
	// from the profile automatically; these cover what the API can't see or gets wrong.
	/** An active global dungeon XP boost, e.g. 20 for +20%. */
	public double dungeonGlobalBoostPercent = 0;
	/** Catacombs Graduate shard attribute level (0–10), or -1 to read it from the profile. */
	public int dungeonGraduateLevel = -1;
	/** Catacombs Explorer shard attribute level (0–10), or -1 to read it from the profile. */
	public int dungeonExplorerLevel = -1;
	/** Any other class XP bonus you have that the API doesn't expose. */
	public double dungeonExtraClassBoostPercent = 0;
	/** Share of a run's class XP that the classes you didn't play receive. */
	public double dungeonTeamShare = io.github.streetinman.skyblockpv.core.dungeons.XpBoosts.DEFAULT_TEAM_SHARE;

	public static PvConfig load(Path file) {
		try {
			if (Files.exists(file)) {
				PvConfig config = GSON.fromJson(Files.readString(file), PvConfig.class);
				if (config != null) {
					// Re-save so settings added in newer versions show up in the file.
					config.save(file);
					return config;
				}
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
