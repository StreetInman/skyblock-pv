package io.github.streetinman.skyblockpv.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import io.github.streetinman.skyblockpv.SkyblockPvClient;
import io.github.streetinman.skyblockpv.core.api.ProfileService;
import io.github.streetinman.skyblockpv.core.model.Inventories;
import io.github.streetinman.skyblockpv.core.model.MemberData;
import io.github.streetinman.skyblockpv.core.model.Profile;
import io.github.streetinman.skyblockpv.core.model.SkillLevel;
import io.github.streetinman.skyblockpv.core.model.SkyblockItem;
import io.github.streetinman.skyblockpv.core.model.TrophyFish;
import io.github.streetinman.skyblockpv.core.model.TrophyFishing;
import io.github.streetinman.skyblockpv.core.model.Wardrobe;
import io.github.streetinman.skyblockpv.core.model.WardrobeSlot;

/**
 * The /pv window: a panel with tabs along the top. Data loads asynchronously; until then the
 * panel shows "Loading…" or the error.
 *
 * <p>Items are drawn as rarity-coloured slots with their real name and lore on hover. Turning
 * Hypixel's 1.8 item NBT into real modern item icons is the next milestone.
 */
public class PvScreen extends Screen {
	private static final int PANEL_WIDTH = 330;
	private static final int PANEL_HEIGHT = 210;
	private static final int SLOT = 18;
	private static final int WHITE = 0xFFFFFFFF;
	private static final int GREY = 0xFFAAAAAA;
	private static final int ACCESSORIES_PER_PAGE = 54;

	private enum Tab {
		OVERVIEW("Overview"), SKILLS("Skills"), INVENTORY("Inventory"), WARDROBE("Wardrobe"),
		ACCESSORIES("Accessories"), TROPHY_FISH("Trophy Fish");

		final String label;

		Tab(String label) {
			this.label = label;
		}
	}

	private final String requestedName;
	private ProfileService.Lookup lookup;
	private Profile profile;
	private String error;
	private Tab tab = Tab.OVERVIEW;
	private int page;

	private int left;
	private int top;
	private List<Component> hoveredTooltip;

	public PvScreen(String playerName, ProfileService service) {
		super(Component.literal("Profile Viewer: " + playerName));
		this.requestedName = playerName;
		service.lookup(playerName).whenCompleteAsync((result, failure) -> {
			if (failure != null) {
				error = SkyblockPvClient.rootMessage(failure);
				SkyblockPvClient.LOGGER.warn("Lookup for {} failed", playerName, failure);
			} else {
				lookup = result;
				profile = result.selected();
			}
			rebuildWidgets();
		}, Minecraft.getInstance());
	}

	@Override
	protected void init() {
		left = (width - PANEL_WIDTH) / 2;
		top = (height - PANEL_HEIGHT) / 2 + 10;

		int x = left;
		for (Tab t : Tab.values()) {
			int w = font.width(t.label) + 10;
			addRenderableWidget(Button.builder(Component.literal(t.label), b -> {
				tab = t;
				page = 0;
				rebuildWidgets();
			}).bounds(x, top - 22, w, 20).build()).active = t != tab;
			x += w + 2;
		}

		if (lookup != null && lookup.profiles().size() > 1) {
			addRenderableWidget(Button.builder(Component.literal("Profile ▸"), b -> {
				int i = lookup.profiles().indexOf(profile);
				profile = lookup.profiles().get((i + 1) % lookup.profiles().size());
				page = 0;
				rebuildWidgets();
			}).bounds(left + PANEL_WIDTH - 70, top + 4, 66, 16).build());
		}

		int pages = pageCount();
		if (pages > 1) {
			addRenderableWidget(Button.builder(Component.literal("<"), b -> {
				page = (page + pages - 1) % pages;
				rebuildWidgets();
			}).bounds(left + PANEL_WIDTH - 50, top + PANEL_HEIGHT - 20, 20, 16).build());
			addRenderableWidget(Button.builder(Component.literal(">"), b -> {
				page = (page + 1) % pages;
				rebuildWidgets();
			}).bounds(left + PANEL_WIDTH - 26, top + PANEL_HEIGHT - 20, 20, 16).build());
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		hoveredTooltip = null;

		g.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xE0101018);
		g.text(font, lookup != null ? lookup.player().name() : requestedName, left + 6, top + 6, WHITE, true);

		if (error != null) {
			g.centeredText(font, "§c" + error, left + PANEL_WIDTH / 2, top + PANEL_HEIGHT / 2, WHITE);
		} else if (profile == null) {
			g.centeredText(font, "Loading…", left + PANEL_WIDTH / 2, top + PANEL_HEIGHT / 2, GREY);
		} else {
			String mode = profile.gameMode() == null ? "" : " §7(" + profile.gameMode() + ")";
			g.text(font, "§a" + profile.cuteName() + mode, left + 6, top + 18, WHITE, false);
			int contentTop = top + 32;
			switch (tab) {
				case OVERVIEW -> drawOverview(g, contentTop);
				case SKILLS -> drawSkills(g, contentTop);
				case INVENTORY -> drawInventory(g, contentTop, mouseX, mouseY);
				case WARDROBE -> drawWardrobe(g, contentTop, mouseX, mouseY);
				case ACCESSORIES -> drawAccessories(g, contentTop, mouseX, mouseY);
				case TROPHY_FISH -> drawTrophyFish(g, contentTop);
			}
			if (pageCount() > 1) {
				g.text(font, "Page " + (page + 1) + "/" + pageCount(), left + PANEL_WIDTH - 110, top + PANEL_HEIGHT - 16, GREY, false);
			}
		}

		if (hoveredTooltip != null) {
			g.setComponentTooltipForNextFrame(font, hoveredTooltip, mouseX, mouseY);
		}
	}

	private void drawOverview(GuiGraphicsExtractor g, int y) {
		MemberData m = profile.member();
		List<String> lines = new ArrayList<>();
		lines.add("§bSkyBlock Level: §f" + String.format(Locale.ROOT, "%.2f", m.skyblockLevel()));
		lines.add("§6Purse: §f" + coins(m.purse()));
		lines.add("§6Bank: §f" + (profile.bankBalance() == null ? "§7API off / none" : coins(profile.bankBalance())));
		lines.add("§dFairy Souls: §f" + m.fairySouls());
		double average = skills().stream().mapToInt(SkillLevel::level).average().orElse(0);
		lines.add("§eSkill Average: §f" + (skills().isEmpty() ? "§7API off" : String.format(Locale.ROOT, "%.2f", average)));
		lines.add("§3Trophy Fish: §f" + m.trophyFishing().totalCaught() + " §7(" + m.trophyFishing().rankName() + ")");
		for (String line : lines) {
			g.text(font, line, left + 8, y, WHITE, false);
			y += 12;
		}
	}

	private void drawSkills(GuiGraphicsExtractor g, int y) {
		List<SkillLevel> skills = skills();
		if (skills.isEmpty()) {
			g.text(font, "§7Skills API is turned off for this player.", left + 8, y, WHITE, false);
			return;
		}
		int col = 0;
		for (SkillLevel skill : skills) {
			int x = left + 8 + col * 160;
			String name = skill.skill().charAt(0) + skill.skill().substring(1).toLowerCase(Locale.ROOT);
			g.text(font, (skill.maxed() ? "§6" : "§f") + name + " " + skill.level(), x, y, WHITE, false);
			int barY = y + 10;
			g.fill(x, barY, x + 140, barY + 4, 0xFF333333);
			g.fill(x, barY, x + (int) (140 * skill.progress()), barY + 4, skill.maxed() ? 0xFFFFAA00 : 0xFF55FF55);
			if (++col == 2) {
				col = 0;
				y += 22;
			}
		}
	}

	private void drawInventory(GuiGraphicsExtractor g, int y, int mouseX, int mouseY) {
		Inventories inv = profile.member().inventories();
		if (!inv.apiEnabled()) {
			g.text(font, "§7Inventory API is turned off for this player.", left + 8, y, WHITE, false);
			return;
		}
		g.text(font, "§7Armor", left + 8, y, WHITE, false);
		drawGrid(g, inv.armor(), left + 8, y + 10, 1, mouseX, mouseY);
		g.text(font, "§7Equip", left + 32, y, WHITE, false);
		drawGrid(g, inv.equipment(), left + 32, y + 10, 1, mouseX, mouseY);

		// The API lists the hotbar first; show it last, under the main inventory, like the game.
		List<SkyblockItem> items = inv.inventory();
		int gridX = left + 70;
		g.text(font, "§7Inventory", gridX, y, WHITE, false);
		if (items.size() > 9) {
			drawGrid(g, items.subList(9, items.size()), gridX, y + 10, 9, mouseX, mouseY);
		}
		drawGrid(g, items.subList(0, Math.min(9, items.size())), gridX, y + 10 + 3 * SLOT + 4, 9, mouseX, mouseY);
	}

	private void drawWardrobe(GuiGraphicsExtractor g, int y, int mouseX, int mouseY) {
		Wardrobe wardrobe = profile.member().inventories().wardrobe();
		if (wardrobe == null) {
			g.text(font, "§7Wardrobe isn't available (inventory API off).", left + 8, y, WHITE, false);
			return;
		}
		List<WardrobeSlot> slots = wardrobe.slots().subList(page * 9, Math.min(wardrobe.slots().size(), page * 9 + 9));
		for (int i = 0; i < slots.size(); i++) {
			WardrobeSlot slot = slots.get(i);
			int x = left + 8 + i * (SLOT + 4);
			boolean equipped = slot.number() == wardrobe.equippedSlot();
			g.text(font, (equipped ? "§a" : "§7") + slot.number(), x + 4, y, WHITE, false);
			if (equipped) {
				g.fill(x - 1, y + 9, x + SLOT + 1, y + 10 + 4 * SLOT + 1, 0xFF55FF55);
				g.text(font, "§a(worn)", x - 2, y + 12 + 4 * SLOT, WHITE, false);
			}
			drawGrid(g, java.util.Arrays.asList(slot.helmet(), slot.chestplate(), slot.leggings(), slot.boots()), x, y + 10, 1, mouseX, mouseY);
		}
	}

	private void drawAccessories(GuiGraphicsExtractor g, int y, int mouseX, int mouseY) {
		List<SkyblockItem> bag = profile.member().inventories().accessoryBag();
		if (bag == null) {
			g.text(font, "§7Accessory bag isn't available (inventory API off).", left + 8, y, WHITE, false);
			return;
		}
		int from = page * ACCESSORIES_PER_PAGE;
		drawGrid(g, bag.subList(Math.min(from, bag.size()), Math.min(bag.size(), from + ACCESSORIES_PER_PAGE)), left + 8, y, 9, mouseX, mouseY);
	}

	private void drawTrophyFish(GuiGraphicsExtractor g, int y) {
		TrophyFishing tf = profile.member().trophyFishing();
		g.text(font, "§6" + tf.rankName() + " §7· " + tf.totalCaught() + " caught · "
				+ tf.fishAtLeast(TrophyFish.Tier.DIAMOND) + "/" + TrophyFish.values().length + " at diamond", left + 8, y, WHITE, false);
		y += 12;
		String[] tierColors = {"§c", "§7", "§6", "§b"};
		int col = 0;
		int rowY = y;
		for (TrophyFish fish : TrophyFish.values()) {
			int x = left + 8 + col * 160;
			TrophyFish.Tier best = tf.bestTier(fish);
			g.text(font, (best == null ? "§8" : tierColors[best.ordinal()]) + fish.displayName, x, rowY, WHITE, false);
			StringBuilder counts = new StringBuilder();
			for (TrophyFish.Tier tier : TrophyFish.Tier.values()) {
				counts.append(tierColors[tier.ordinal()]).append(tf.count(fish, tier)).append(' ');
			}
			g.text(font, counts.toString(), x + 100, rowY, WHITE, false);
			if (++col == 2) {
				col = 0;
				rowY += 11;
			}
		}
	}

	private void drawGrid(GuiGraphicsExtractor g, List<SkyblockItem> items, int x, int y, int columns, int mouseX, int mouseY) {
		if (items == null) return;
		for (int i = 0; i < items.size(); i++) {
			int sx = x + (i % columns) * SLOT;
			int sy = y + (i / columns) * SLOT;
			g.fill(sx, sy, sx + SLOT - 1, sy + SLOT - 1, 0xFF373737);
			SkyblockItem item = items.get(i);
			if (item == null) continue;
			g.fill(sx + 2, sy + 2, sx + SLOT - 3, sy + SLOT - 3, rarityColor(item));
			if (mouseX >= sx && mouseX < sx + SLOT && mouseY >= sy && mouseY < sy + SLOT) {
				g.fill(sx, sy, sx + SLOT - 1, sy + SLOT - 1, 0x80FFFFFF);
				hoveredTooltip = tooltip(item);
			}
		}
	}

	private int pageCount() {
		if (profile == null) return 1;
		Inventories inv = profile.member().inventories();
		return switch (tab) {
			case WARDROBE -> inv.wardrobe() == null ? 1 : Math.max(1, (inv.wardrobe().slots().size() + 8) / 9);
			case ACCESSORIES -> inv.accessoryBag() == null ? 1
					: Math.max(1, (inv.accessoryBag().size() + ACCESSORIES_PER_PAGE - 1) / ACCESSORIES_PER_PAGE);
			default -> 1;
		};
	}

	private List<SkillLevel> skills() {
		List<SkillLevel> levels = new ArrayList<>();
		profile.member().skillXp().forEach((skill, xp) -> {
			if (lookup.skills().knows(skill)) levels.add(lookup.skills().level(skill, xp));
		});
		return levels;
	}

	private static List<Component> tooltip(SkyblockItem item) {
		List<Component> lines = new ArrayList<>();
		lines.add(Component.literal(item.name() != null ? item.name() : String.valueOf(item.skyblockId())));
		for (String line : item.lore()) lines.add(Component.literal(line));
		return lines;
	}

	/** Colour of the rarity line (last lore line), e.g. §6 for legendary. */
	private static int rarityColor(SkyblockItem item) {
		if (!item.lore().isEmpty()) {
			String last = item.lore().getLast();
			int idx = last.indexOf('§');
			if (idx >= 0 && idx + 1 < last.length()) {
				ChatFormatting format = ChatFormatting.getByCode(last.charAt(idx + 1));
				if (format != null && format.getColor() != null) return 0xFF000000 | format.getColor();
			}
		}
		return 0xFFAAAAAA;
	}

	private static String coins(double amount) {
		return String.format(Locale.ROOT, "%,.0f", amount);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
