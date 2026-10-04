package io.github.streetinman.skyblockpv.core.api;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonObject;

import io.github.streetinman.skyblockpv.core.model.Profile;
import io.github.streetinman.skyblockpv.core.parse.ProfileParser;
import io.github.streetinman.skyblockpv.core.parse.SkillTable;

/** Everything /pv needs for one player, fetched and parsed off the render thread. */
public final class ProfileService {
	public record Lookup(MojangClient.PlayerId player, List<Profile> profiles, Profile selected, SkillTable skills, JsonObject raw) {
	}

	private final MojangClient mojang;
	private final HypixelClient hypixel;

	public ProfileService(MojangClient mojang, HypixelClient hypixel) {
		this.mojang = mojang;
		this.hypixel = hypixel;
	}

	public CompletableFuture<Lookup> lookup(String username) {
		return mojang.lookup(username).thenCompose(player -> hypixel.skyblockProfiles(player.uuid())
				.thenCombine(hypixel.resource("skills"), (raw, skills) -> {
					List<Profile> profiles;
					try {
						profiles = ProfileParser.parseProfiles(raw, player.uuid());
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
					if (profiles.isEmpty()) {
						throw new ApiException(ApiException.Kind.NO_SKYBLOCK, player.name() + " has never played SkyBlock");
					}
					return new Lookup(player, profiles, ProfileParser.selected(profiles), SkillTable.fromResource(skills), raw);
				}));
	}

	public HypixelClient hypixel() {
		return hypixel;
	}
}
