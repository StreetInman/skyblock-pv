package io.github.streetinman.skyblockpv.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.HolderLookup;

/**
 * The few calls that differ between Minecraft versions (26.3). Each supported version has its own
 * copy of this class under fabric/src/compat; the build picks one with -Pmc.
 */
public final class Compat {
	private Compat() {
	}

	public static void setScreen(Screen screen) {
		Minecraft.getInstance().gui.setScreen(screen);
	}

	/** The player's profile with skin textures, looked up through Mojang's session server. */
	public static com.mojang.authlib.GameProfile fetchProfile(java.util.UUID uuid) {
		var result = Minecraft.getInstance().services().sessionService().fetchProfile(uuid, false);
		return result == null ? null : result.profile();
	}

	/** Vanilla registries for decoding items when not connected to a server, or null if unavailable. */
	public static HolderLookup.Provider vanillaRegistries() {
		// VanillaRegistries.createLookup() is gone in 26.3; items only decode while connected.
		return null;
	}
}
