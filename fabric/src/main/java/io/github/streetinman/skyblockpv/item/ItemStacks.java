package io.github.streetinman.skyblockpv.item;

import java.util.List;
import java.util.Map;

import com.mojang.serialization.Dynamic;

import net.azureaaron.legacyitemdfu.LegacyItemStackFixer;
import net.azureaaron.legacyitemdfu.TypeReferences;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.TooltipDisplay;

import io.github.streetinman.skyblockpv.SkyblockPvClient;
import io.github.streetinman.skyblockpv.compat.Compat;
import io.github.streetinman.skyblockpv.core.model.SkyblockItem;
import io.github.streetinman.skyblockpv.core.nbt.NbtCompound;

/**
 * Builds real ItemStacks from Hypixel's 1.8-format item NBT, using AzureAaron's legacy item data
 * fixer, so the GUI can draw proper icons (including player-head textures) and tooltips.
 */
public final class ItemStacks {
	private static HolderLookup.Provider fallbackLookup;

	private ItemStacks() {
	}

	/** The converted stack, or a named barrier if conversion failed. */
	public static ItemStack convert(SkyblockItem item) {
		try {
			ItemStack stack = fix(toTag(item.nbt()));
			if (!stack.isEmpty()) {
				if (item.name() != null) stack.set(DataComponents.CUSTOM_NAME, LegacyText.parse(item.name()));
				List<Component> lore = item.lore().stream().map(line -> (Component) LegacyText.parse(line)).toList();
				stack.set(DataComponents.LORE, new ItemLore(lore));
				// Hypixel writes stats into the lore; hide vanilla's duplicate attribute/enchant lines.
				stack.set(DataComponents.TOOLTIP_DISPLAY, stack.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT)
						.withHidden(DataComponents.ATTRIBUTE_MODIFIERS, true)
						.withHidden(DataComponents.ENCHANTMENTS, true));
				return stack;
			}
		} catch (RuntimeException | LinkageError e) {
			// LinkageError: a fixer built against a different Minecraft; show a placeholder, don't crash.
			SkyblockPvClient.LOGGER.warn("Couldn't convert item {}", item.skyblockId(), e);
		}
		return placeholder(item.name() != null ? item.name() : "§c" + item.skyblockId());
	}

	/**
	 * A named barrier, or EMPTY outside a world: from 26.2, item components aren't bound until the
	 * client has joined a world, and creating any ItemStack before that throws.
	 */
	public static ItemStack placeholder(String legacyName) {
		try {
			ItemStack barrier = new ItemStack(Items.BARRIER);
			barrier.set(DataComponents.CUSTOM_NAME, LegacyText.parse(legacyName));
			return barrier;
		} catch (NullPointerException e) {
			return ItemStack.EMPTY;
		}
	}

	private static ItemStack fix(CompoundTag nbt) {
		HolderLookup.Provider registries = registries();
		if (registries == null) return ItemStack.EMPTY;
		RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
		Dynamic<Tag> fixed = LegacyItemStackFixer.getFixer().update(TypeReferences.LEGACY_ITEM_STACK, new Dynamic<>(ops, nbt),
				LegacyItemStackFixer.getFirstVersion(), LegacyItemStackFixer.getLatestVersion());
		return ItemStack.CODEC.parse(fixed)
				.resultOrPartial(error -> SkyblockPvClient.LOGGER.debug("Item fix error: {}", error))
				.orElse(ItemStack.EMPTY);
	}

	private static HolderLookup.Provider registries() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.getConnection() != null) return mc.getConnection().registryAccess();
		if (fallbackLookup == null) fallbackLookup = Compat.vanillaRegistries();
		return fallbackLookup;
	}

	private static CompoundTag toTag(NbtCompound compound) {
		CompoundTag tag = new CompoundTag();
		for (String key : compound.keys()) {
			Tag value = toTag(compound.get(key));
			if (value != null) tag.put(key, value);
		}
		return tag;
	}

	private static Tag toTag(Object value) {
		return switch (value) {
			case NbtCompound c -> toTag(c);
			case Byte b -> ByteTag.valueOf(b);
			case Short s -> ShortTag.valueOf(s);
			case Integer i -> IntTag.valueOf(i);
			case Long l -> LongTag.valueOf(l);
			case Float f -> FloatTag.valueOf(f);
			case Double d -> DoubleTag.valueOf(d);
			case String s -> StringTag.valueOf(s);
			case byte[] bytes -> new ByteArrayTag(bytes);
			case int[] ints -> new IntArrayTag(ints);
			case long[] longs -> new LongArrayTag(longs);
			case List<?> list -> {
				ListTag listTag = new ListTag();
				for (Object element : list) {
					Tag t = toTag(element);
					if (t != null) listTag.add(t);
				}
				yield listTag;
			}
			default -> null;
		};
	}

	/** Remembers conversions for the lifetime of one screen. */
	public static final class Cache {
		private final Map<SkyblockItem, ItemStack> stacks = new java.util.IdentityHashMap<>();

		public ItemStack get(SkyblockItem item) {
			return stacks.computeIfAbsent(item, ItemStacks::convert);
		}
	}
}
