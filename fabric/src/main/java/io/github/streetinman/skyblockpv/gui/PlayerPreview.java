package io.github.streetinman.skyblockpv.gui;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.mojang.authlib.GameProfile;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;

import io.github.streetinman.skyblockpv.SkyblockPvClient;
import io.github.streetinman.skyblockpv.compat.Compat;
import io.github.streetinman.skyblockpv.core.model.SkyblockItem;

/**
 * A stand-in player entity for the /pv sidebar: the viewed player's skin and armor, drawn with the
 * inventory screen's "look at the mouse" renderer. It's never added to the world.
 */
final class PlayerPreview {
	private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

	private final RemotePlayer entity;
	private volatile PlayerSkin skin;

	private PlayerPreview(ClientLevel level, GameProfile profile) {
		this.entity = new RemotePlayer(level, profile) {
			@Override
			public PlayerSkin getSkin() {
				PlayerSkin loaded = skin;
				return loaded != null ? loaded : super.getSkin();
			}
		};
	}

	/** @return null outside a world, where there's no level to build an entity in */
	static PlayerPreview create(String undashedUuid, String name) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return null;
		UUID uuid = UUID.fromString(undashedUuid.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
		PlayerPreview preview = new PlayerPreview(mc.level, new GameProfile(uuid, name));
		// The skin needs the profile's signed textures from Mojang's session server.
		CompletableFuture.supplyAsync(() -> Compat.fetchProfile(uuid))
				.thenCompose(full -> full == null ? CompletableFuture.completedFuture(Optional.<PlayerSkin>empty()) : mc.getSkinManager().get(full))
				.whenComplete((result, error) -> {
					if (error != null) SkyblockPvClient.LOGGER.debug("Couldn't load skin for {}", name, error);
					else result.ifPresent(s -> preview.skin = s);
				});
		return preview;
	}

	/** Puts the player's armor (helmet first) on the model. */
	void wear(List<SkyblockItem> armor, Function<SkyblockItem, ItemStack> toStack) {
		for (int i = 0; i < ARMOR_SLOTS.length; i++) {
			SkyblockItem item = armor != null && i < armor.size() ? armor.get(i) : null;
			entity.setItemSlot(ARMOR_SLOTS[i], item == null ? ItemStack.EMPTY : toStack.apply(item));
		}
	}

	RemotePlayer entity() {
		return entity;
	}
}
