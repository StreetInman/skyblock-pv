package io.github.streetinman.skyblockpv.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.registries.VanillaRegistries;

/**
 * The few calls that differ between Minecraft versions (26.2). Each supported version has its own
 * copy of this class under fabric/src/compat; the build picks one with -Pmc.
 */
public final class Compat {
	private Compat() {
	}

	public static void setScreen(Screen screen) {
		Minecraft.getInstance().gui.setScreen(screen);
	}

	/** Vanilla registries for decoding items when not connected to a server, or null if unavailable. */
	public static HolderLookup.Provider vanillaRegistries() {
		return VanillaRegistries.createLookup();
	}
}
