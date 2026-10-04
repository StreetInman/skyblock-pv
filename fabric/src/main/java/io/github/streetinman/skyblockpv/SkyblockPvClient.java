package io.github.streetinman.skyblockpv;

import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import com.google.gson.GsonBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import io.github.streetinman.skyblockpv.config.PvConfig;
import io.github.streetinman.skyblockpv.core.api.HypixelClient;
import io.github.streetinman.skyblockpv.core.api.MojangClient;
import io.github.streetinman.skyblockpv.core.api.ProfileService;
import io.github.streetinman.skyblockpv.gui.PvScreen;

/**
 * Registers {@code /pv [player]} and {@code /pvdump <player>}.
 *
 * <p>Everything here is read-only: the mod calls the public Hypixel API over HTTPS and draws its
 * own screen. It never sends packets to the server or acts on the player's behalf.
 */
public final class SkyblockPvClient implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("skyblock-pv");

	private static PvConfig config;
	private static ProfileService profiles;

	@Override
	public void onInitializeClient() {
		Path configFile = FabricLoader.getInstance().getConfigDir().resolve("skyblock-pv.json");
		config = PvConfig.load(configFile);

		HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
		profiles = new ProfileService(new MojangClient(http), new HypixelClient(http, () -> config.apiBaseUrl, () -> config.apiKey));

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(ClientCommands.literal("pv")
					.executes(ctx -> open(Minecraft.getInstance().getUser().getName()))
					.then(ClientCommands.argument("player", StringArgumentType.word())
							.executes(ctx -> open(StringArgumentType.getString(ctx, "player")))));
			dispatcher.register(ClientCommands.literal("pvdump")
					.then(ClientCommands.argument("player", StringArgumentType.word())
							.executes(SkyblockPvClient::dump)));
		});
	}

	private static int open(String player) {
		Minecraft mc = Minecraft.getInstance();
		// Open next tick: the chat screen closes after the command runs and would replace ours.
		mc.schedule(() -> mc.gui.setScreen(new PvScreen(player, profiles, config)));
		return 1;
	}

	/** Saves the raw API response for a player, to inspect new fields such as loadouts. */
	private static int dump(CommandContext<FabricClientCommandSource> ctx) {
		String player = StringArgumentType.getString(ctx, "player");
		FabricClientCommandSource source = ctx.getSource();
		profiles.lookup(player).whenComplete((lookup, error) -> Minecraft.getInstance().execute(() -> {
			if (error != null) {
				source.sendError(Component.literal("pvdump failed: " + rootMessage(error)));
				return;
			}
			try {
				Path file = FabricLoader.getInstance().getConfigDir().resolve("skyblock-pv/dumps/" + lookup.player().name() + ".json");
				Files.createDirectories(file.getParent());
				Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(lookup.raw()));
				source.sendFeedback(Component.literal("Saved API response to " + file));
			} catch (Exception e) {
				source.sendError(Component.literal("Couldn't write dump: " + e.getMessage()));
			}
		}));
		return 1;
	}

	public static String rootMessage(Throwable error) {
		Throwable t = error;
		while (t.getCause() != null && (t.getMessage() == null || t instanceof java.util.concurrent.CompletionException)) {
			t = t.getCause();
		}
		return t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
	}
}
