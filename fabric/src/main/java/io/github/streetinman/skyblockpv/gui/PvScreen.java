package io.github.streetinman.skyblockpv.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import io.github.streetinman.skyblockpv.SkyblockPvClient;
import io.github.streetinman.skyblockpv.config.PvConfig;
import io.github.streetinman.skyblockpv.core.api.ProfileService;
import io.github.streetinman.skyblockpv.core.dungeons.DungeonCalculator;
import io.github.streetinman.skyblockpv.core.dungeons.DungeonClass;
import io.github.streetinman.skyblockpv.core.dungeons.DungeonData;
import io.github.streetinman.skyblockpv.core.dungeons.DungeonLevels;
import io.github.streetinman.skyblockpv.core.dungeons.XpBoosts;
import io.github.streetinman.skyblockpv.core.model.Inventories;
import io.github.streetinman.skyblockpv.core.model.MemberData;
import io.github.streetinman.skyblockpv.core.model.Profile;
import io.github.streetinman.skyblockpv.core.model.SkillLevel;
import io.github.streetinman.skyblockpv.core.model.SkyblockItem;
import io.github.streetinman.skyblockpv.core.model.Slayer;
import io.github.streetinman.skyblockpv.core.model.TrophyFish;
import io.github.streetinman.skyblockpv.core.model.TrophyFishing;
import io.github.streetinman.skyblockpv.item.ItemStacks;

/**
 * The /pv window: a panel with tabs along the top. Data loads asynchronously; until then the
 * panel shows "Loading…" or the error.
 */
public class PvScreen extends Screen {
	private static final int PANEL_WIDTH = 370;
	private static final int PANEL_HEIGHT = 220;
	private static final int SLOT = 18;
	private static final int WHITE = 0xFFFFFFFF;
	private static final int GREY = 0xFFAAAAAA;
	private static final int ACCESSORIES_PER_PAGE = 54;
	/** The API stores every ender chest page back to back; the game shows 45 slots per page. */
	private static final int ENDER_CHEST_PAGE = 45;
	private static final String[] ROMAN = {"0", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

	private enum Tab {
		OVERVIEW("Stats"), SKILLS("Skills"), DUNGEONS("Dungeons"), INVENTORY("Inv"), STORAGE("Storage"),
		ACCESSORIES("Accs"), TROPHY_FISH("Trophy");

		final String label;

		Tab(String label) {
			this.label = label;
		}
	}

	/** Calculator results, computed once per profile since the class simulation loops per run. */
	private record DungeonPlan(XpBoosts boosts, int runsToCata50, DungeonCalculator.ClassPlan classPlan) {
	}

	/** One flippable page of the Storage tab: an ender chest page or a backpack. */
	private record StoragePage(String title, List<SkyblockItem> items) {
	}

	private final String requestedName;
	private final ProfileService service;
	private final PvConfig config;
	private final ItemStacks.Cache stacks = new ItemStacks.Cache();
	private ProfileService.Lookup lookup;
	private Profile profile;
	private DungeonPlan dungeonPlan;
	private List<StoragePage> storagePages = List.of();
	private String error;
	private Tab tab = Tab.OVERVIEW;
	private int page;
	private EditBox searchBox;
	private String searchText = "";

	private int left;
	private int top;
	private ItemStack hoveredStack;
	private List<Component> hoveredText;

	public PvScreen(String playerName, ProfileService service, PvConfig config) {
		super(Component.literal("Profile Viewer: " + playerName));
		this.requestedName = playerName;
		this.service = service;
		this.config = config;
		service.lookup(playerName).whenCompleteAsync((result, failure) -> {
			if (failure != null) {
				error = SkyblockPvClient.rootMessage(failure);
				SkyblockPvClient.LOGGER.warn("Lookup for {} failed", playerName, failure);
			} else {
				lookup = result;
				selectProfile(result.selected());
			}
			rebuildWidgets();
		}, Minecraft.getInstance());
	}

	private void selectProfile(Profile p) {
		profile = p;
		page = 0;
		MemberData m = p.member();
		XpBoosts boosts = XpBoosts.detect(m.dungeons(), m.inventories(), m.attributeStacks(), lookup.mayor(),
				1 + config.dungeonGlobalBoostPercent / 100.0, config.dungeonGraduateLevel, config.dungeonExplorerLevel,
				config.dungeonExtraClassBoostPercent / 100.0, config.dungeonTeamShare);
		DungeonData d = m.dungeons();
		dungeonPlan = new DungeonPlan(boosts,
				DungeonCalculator.runsToCatacombs50(d.catacombsXp(), d.masterCompletions().getOrDefault(7, 0), DungeonCalculator.M7_BASE_XP, boosts),
				DungeonCalculator.classAverage50(d.classXp(), DungeonCalculator.M7_BASE_XP, boosts));
		storagePages = storagePages(m.inventories());
	}

	private static List<StoragePage> storagePages(Inventories inv) {
		List<StoragePage> pages = new ArrayList<>();
		List<SkyblockItem> ender = inv.enderChest();
		if (ender != null) {
			for (int from = 0, n = 1; from < ender.size(); from += ENDER_CHEST_PAGE, n++) {
				pages.add(new StoragePage("Ender Chest " + n, ender.subList(from, Math.min(ender.size(), from + ENDER_CHEST_PAGE))));
			}
		}
		// Backpack keys are 0-based slot numbers in the storage menu.
		inv.backpacks().forEach((slot, items) -> pages.add(new StoragePage("Backpack " + (slot + 1), items)));
		return List.copyOf(pages);
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

		// Look up someone else without closing the window.
		searchBox = new EditBox(font, left + PANEL_WIDTH - 200, top + 4, 100, 16, Component.literal("Player name"));
		searchBox.setMaxLength(16);
		searchBox.setHint(Component.literal("§7Search player…"));
		searchBox.setValue(searchText);
		searchBox.setResponder(text -> searchText = text);
		addRenderableWidget(searchBox);
		addRenderableWidget(Button.builder(Component.literal("Go"), b -> search())
				.bounds(left + PANEL_WIDTH - 98, top + 4, 24, 16).build());

		if (lookup != null && lookup.profiles().size() > 1) {
			addRenderableWidget(Button.builder(Component.literal("Profile ▸"), b -> {
				int i = lookup.profiles().indexOf(profile);
				selectProfile(lookup.profiles().get((i + 1) % lookup.profiles().size()));
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

	private void search() {
		String name = searchText.trim();
		if (name.isEmpty()) return;
		Minecraft.getInstance().gui.setScreen(new PvScreen(name, service, config));
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (searchBox != null && searchBox.isFocused()
				&& (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER)) {
			search();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		hoveredStack = null;
		hoveredText = null;

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
				case DUNGEONS -> drawDungeons(g, contentTop, mouseX, mouseY);
				case INVENTORY -> drawInventory(g, contentTop, mouseX, mouseY);
				case STORAGE -> drawStorage(g, contentTop, mouseX, mouseY);
				case ACCESSORIES -> drawAccessories(g, contentTop, mouseX, mouseY);
				case TROPHY_FISH -> drawTrophyFish(g, contentTop);
			}
			if (pageCount() > 1) {
				g.text(font, "Page " + (page + 1) + "/" + pageCount(), left + PANEL_WIDTH - 110, top + PANEL_HEIGHT - 16, GREY, false);
			}
		}

		if (hoveredStack != null) {
			g.setTooltipForNextFrame(font, hoveredStack, mouseX, mouseY);
		} else if (hoveredText != null) {
			g.setComponentTooltipForNextFrame(font, hoveredText, mouseX, mouseY);
		}
	}

	private void drawOverview(GuiGraphicsExtractor g, int y) {
		MemberData m = profile.member();
		DungeonData d = m.dungeons();
		double average = skills().stream().mapToInt(SkillLevel::level).average().orElse(0);
		List<String> stats = List.of(
				"§bSkyBlock Level: §f" + decimal(m.skyblockLevel()),
				"§6Purse: §f" + coins(m.purse()),
				"§6Bank: §f" + (profile.bankBalance() == null ? "§7API off / none" : coins(profile.bankBalance())),
				"§dFairy Souls: §f" + m.fairySouls(),
				"§eSkill Average: §f" + (skills().isEmpty() ? "§7API off" : decimal(average)),
				"§cCatacombs: §f" + decimal(d.catacombsLevel()),
				"§cClass Average: §f" + decimal(d.classAverage()),
				"§3Trophy Fish: §f" + m.trophyFishing().totalCaught() + " §7(" + m.trophyFishing().rankName() + ")");
		drawLines(g, stats, left + 8, y);

		List<String> slayers = new ArrayList<>();
		slayers.add("§5Slayers");
		long totalSlayerXp = 0;
		for (Slayer s : Slayer.values()) {
			long xp = m.slayerXp().getOrDefault(s, 0L);
			totalSlayerXp += xp;
			int level = s.level(xp);
			slayers.add((level == s.maxLevel() ? "§6" : "§f") + s.displayName + " " + level + " §7" + compact(xp) + " XP");
		}
		slayers.add("§7Total: " + compact(totalSlayerXp) + " XP");
		drawLines(g, slayers, Math.max(left + 180, left + 8 + widest(stats) + 16), y);
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
			drawBar(g, x, y + 10, 140, skill.progress(), skill.maxed());
			if (++col == 2) {
				col = 0;
				y += 22;
			}
		}
	}

	private void drawDungeons(GuiGraphicsExtractor g, int y, int mouseX, int mouseY) {
		DungeonData d = profile.member().dungeons();
		int x = left + 8;

		List<String> classLines = new ArrayList<>();
		for (DungeonClass c : DungeonClass.values()) {
			double level = d.classLevel(c);
			String marker = c == d.selectedClass() ? " §a◆" : "";
			classLines.add((level >= 50 ? "§6" : "§f") + c.displayName() + " " + decimal(level) + marker);
		}
		// Bars start after the widest class line so long names never run into them.
		int barX = x + widest(classLines) + 6;
		int barWidth = 50;

		Long best = d.masterFastestSPlusMs().get(7);
		List<String> stats = List.of(
				"§7Secrets: §f" + String.format(Locale.ROOT, "%,d", d.secrets()),
				"§7Runs: §f" + String.format(Locale.ROOT, "%,d", d.totalRuns())
						+ " §7(M7: §f" + d.masterCompletions().getOrDefault(7, 0) + "§7)",
				"§7Best M7 S+: §f" + (best == null ? "—" : time(best)));
		int columnEnd = Math.max(barX + barWidth, x + widest(stats));

		double cata = d.catacombsLevel();
		g.text(font, "§cCatacombs " + decimal(cata), x, y, WHITE, false);
		drawBar(g, x, y + 10, columnEnd - x, cata >= 50 ? 1 : cata - Math.floor(cata), cata >= 50);
		y += 18;
		for (int i = 0; i < classLines.size(); i++) {
			double level = d.classLevel(DungeonClass.values()[i]);
			g.text(font, classLines.get(i), x, y, WHITE, false);
			drawBar(g, barX, y + 2, barWidth, level >= 50 ? 1 : level - Math.floor(level), level >= 50);
			y += 11;
		}
		g.text(font, "§eClass Average " + decimal(d.classAverage()), x, y + 2, WHITE, false);
		y += 16;
		drawLines(g, stats, x, y);

		drawCalculator(g, columnEnd + 14, top + 32, mouseX, mouseY);
	}

	private void drawCalculator(GuiGraphicsExtractor g, int x, int y, int mouseX, int mouseY) {
		int startY = y;
		g.text(font, "§6§lM7 Calculator", x, y, WHITE, false);
		y += 13;

		if (dungeonPlan.runsToCata50() > 0) {
			g.text(font, "§fCata 50: §e" + String.format(Locale.ROOT, "%,d", dungeonPlan.runsToCata50()) + " runs", x, y, WHITE, false);
			y += 13;
		}

		DungeonCalculator.ClassPlan plan = dungeonPlan.classPlan();
		if (plan.totalRuns() > 0) {
			g.text(font, "§fClass Avg 50: §e" + String.format(Locale.ROOT, "%,d", plan.totalRuns()) + " runs", x, y, WHITE, false);
			y += 11;
			for (DungeonClass c : DungeonClass.values()) {
				int runs = plan.runsAs().get(c);
				String line = runs == 0 ? "§7" + c.displayName() + ": §apassive only" : "§7" + c.displayName() + ": §f" + String.format(Locale.ROOT, "%,d", runs);
				g.text(font, line, x + 6, y, WHITE, false);
				y += 10;
			}
			y += 3;
		}

		if (dungeonPlan.runsToCata50() == 0 && plan.totalRuns() == 0) {
			g.text(font, "§aCata 50 and class average 50 done!", x, y, WHITE, false);
			y += 13;
		}

		XpBoosts b = dungeonPlan.boosts();
		List<String> boostLines = new ArrayList<>();
		boostLines.add("§7Hecatomb " + ROMAN[Math.min(b.hecatombLevel(), 10)] + (b.expertRing() ? " · Expert Ring" : ""));
		boostLines.add("§7Scarf +" + percent(b.scarfBonus()) + " · Graduate " + ROMAN[b.graduateLevel()]);
		if (b.explorerLevel() > 0 || b.mayorMultiplier() > 1) {
			boostLines.add("§7" + (b.explorerLevel() > 0 ? "Explorer " + ROMAN[b.explorerLevel()] : "")
					+ (b.explorerLevel() > 0 && b.mayorMultiplier() > 1 ? " · " : "") + (b.mayorMultiplier() > 1 ? "Derpy" : ""));
		}
		boostLines.add("§8Hover for details");
		drawLines(g, boostLines, x, y);
		y += boostLines.size() * 11;

		if (mouseX >= x && mouseX < left + PANEL_WIDTH && mouseY >= startY && mouseY < y) {
			hoveredText = calculatorTooltip(b);
		}
	}

	private List<Component> calculatorTooltip(XpBoosts b) {
		List<Component> lines = new ArrayList<>();
		lines.add(Component.literal("§6How this is calculated"));
		lines.add(Component.literal("§7S+ M7 runs (300k base XP), always playing"));
		lines.add(Component.literal("§7the class furthest from 50. Classes you"));
		lines.add(Component.literal("§7don't play get " + percent(b.teamShare()) + " of their XP."));
		lines.add(Component.literal(""));
		lines.add(Component.literal("§eBoosts found on this profile:"));
		lines.add(Component.literal("§7Hecatomb: §f" + ROMAN[Math.min(b.hecatombLevel(), 10)] + " §7(+" + percent(b.hecatomb()) + ")"));
		lines.add(Component.literal("§7Catacombs Expert Ring: §f" + (b.expertRing() ? "yes" : "no")));
		lines.add(Component.literal("§7Scarf accessory: §f+" + percent(b.scarfBonus()) + " class XP"));
		lines.add(Component.literal("§7Catacombs Graduate: §f" + ROMAN[b.graduateLevel()] + " §7(+" + percent(b.graduate()) + " class XP)"));
		lines.add(Component.literal("§7Catacombs Explorer: §f" + ROMAN[b.explorerLevel()] + " §7(+" + percent(b.explorer()) + " Cata XP)"));
		for (DungeonClass c : DungeonClass.values()) {
			lines.add(Component.literal("§7" + c.displayName() + " perk: §f+" + percent(b.classPerk(c))));
		}
		lines.add(Component.literal("§7Mayor: §f" + (lookup.mayor() == null ? "unknown" : lookup.mayor())
				+ (b.mayorMultiplier() > 1 ? " (+" + percent(b.mayorMultiplier() - 1) + ")" : "")));
		if (config.dungeonGraduateLevel >= 0 || config.dungeonExplorerLevel >= 0) {
			lines.add(Component.literal("§8Shard levels set in your config."));
		} else {
			lines.add(Component.literal("§8Shard levels wrong? Set dungeonGraduateLevel"));
			lines.add(Component.literal("§8and dungeonExplorerLevel in the config."));
		}
		if (b.globalMultiplier() > 1 || b.extraClassBonus() > 0) {
			lines.add(Component.literal("§7From your config: §f+" + percent(b.globalMultiplier() - 1) + " global, +"
					+ percent(b.extraClassBonus()) + " class"));
		}
		return lines;
	}

	private void drawInventory(GuiGraphicsExtractor g, int y, int mouseX, int mouseY) {
		Inventories inv = profile.member().inventories();
		if (!inv.apiEnabled()) {
			g.text(font, "§7Inventory API is turned off for this player.", left + 8, y, WHITE, false);
			return;
		}
		int armorX = left + 8;
		g.text(font, "§7Armor", armorX, y, WHITE, false);
		drawGrid(g, inv.armor(), armorX, y + 10, 1, mouseX, mouseY);
		int equipX = armorX + Math.max(SLOT, font.width("Armor")) + 10;
		g.text(font, "§7Equip", equipX, y, WHITE, false);
		drawGrid(g, inv.equipment(), equipX, y + 10, 1, mouseX, mouseY);

		// The API lists the hotbar first; show it last, under the main inventory, like the game.
		List<SkyblockItem> items = inv.inventory();
		int gridX = equipX + Math.max(SLOT, font.width("Equip")) + 16;
		g.text(font, "§7Inventory", gridX, y, WHITE, false);
		if (items.size() > 9) {
			drawGrid(g, items.subList(9, items.size()), gridX, y + 10, 9, mouseX, mouseY);
		}
		drawGrid(g, items.subList(0, Math.min(9, items.size())), gridX, y + 10 + 3 * SLOT + 4, 9, mouseX, mouseY);
	}

	private void drawStorage(GuiGraphicsExtractor g, int y, int mouseX, int mouseY) {
		if (storagePages.isEmpty()) {
			g.text(font, "§7No ender chest or backpacks visible (inventory API off?).", left + 8, y, WHITE, false);
			return;
		}
		StoragePage current = storagePages.get(Math.min(page, storagePages.size() - 1));
		g.text(font, "§e" + current.title(), left + 8, y, WHITE, false);
		drawGrid(g, current.items(), left + 8, y + 11, 9, mouseX, mouseY);
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
		g.text(font, "§cBronze §7Silver §6Gold §bDiamond", left + 8, y + 11, WHITE, false);
		y += 24;
		String[] tierColors = {"§c", "§7", "§6", "§b"};
		int columnWidth = (PANEL_WIDTH - 16) / 2;
		int col = 0;
		int rowY = y;
		for (TrophyFish fish : TrophyFish.values()) {
			int x = left + 8 + col * columnWidth;
			TrophyFish.Tier best = tf.bestTier(fish);
			StringBuilder counts = new StringBuilder();
			for (TrophyFish.Tier tier : TrophyFish.Tier.values()) {
				if (counts.length() > 0) counts.append(' ');
				counts.append(tierColors[tier.ordinal()]).append(tf.count(fish, tier));
			}
			// Counts sit flush right in the column; the name gets whatever room is left.
			int countsWidth = font.width(counts.toString());
			int countsX = x + columnWidth - 8 - countsWidth;
			String name = fit(fish.displayName, countsX - x - 4);
			g.text(font, (best == null ? "§8" : tierColors[best.ordinal()]) + name, x, rowY, WHITE, false);
			g.text(font, counts.toString(), countsX, rowY, WHITE, false);
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
			ItemStack stack = stacks.get(item);
			g.item(stack, sx + 1, sy + 1);
			g.itemDecorations(font, stack, sx + 1, sy + 1);
			if (mouseX >= sx && mouseX < sx + SLOT && mouseY >= sy && mouseY < sy + SLOT) {
				g.fill(sx, sy, sx + SLOT - 1, sy + SLOT - 1, 0x80FFFFFF);
				hoveredStack = stack;
			}
		}
	}

	private void drawBar(GuiGraphicsExtractor g, int x, int y, int width, double progress, boolean maxed) {
		g.fill(x, y, x + width, y + 4, 0xFF333333);
		g.fill(x, y, x + (int) (width * Math.max(0, Math.min(1, progress))), y + 4, maxed ? 0xFFFFAA00 : 0xFF55FF55);
	}

	private void drawLines(GuiGraphicsExtractor g, List<String> lines, int x, int y) {
		for (String line : lines) {
			g.text(font, line, x, y, WHITE, false);
			y += 11;
		}
	}

	/** Width of the widest line, ignoring § colour codes (Font.width already skips them). */
	private int widest(List<String> lines) {
		return lines.stream().mapToInt(font::width).max().orElse(0);
	}

	/** Shortens text with an ellipsis until it fits in {@code maxWidth} pixels. */
	private String fit(String text, int maxWidth) {
		if (font.width(text) <= maxWidth) return text;
		String shortened = text;
		while (!shortened.isEmpty() && font.width(shortened + "…") > maxWidth) {
			shortened = shortened.substring(0, shortened.length() - 1);
		}
		return shortened + "…";
	}

	private int pageCount() {
		if (profile == null) return 1;
		Inventories inv = profile.member().inventories();
		return switch (tab) {
			case STORAGE -> Math.max(1, storagePages.size());
			case ACCESSORIES -> inv.accessoryBag() == null ? 1
					: Math.max(1, (inv.accessoryBag().size() + ACCESSORIES_PER_PAGE - 1) / ACCESSORIES_PER_PAGE);
			default -> 1;
		};
	}

	private List<SkillLevel> skills() {
		List<SkillLevel> levels = new ArrayList<>();
		for (Map.Entry<String, Double> e : profile.member().skillXp().entrySet()) {
			if (lookup.skills().knows(e.getKey())) levels.add(lookup.skills().level(e.getKey(), e.getValue()));
		}
		return levels;
	}

	private static String coins(double amount) {
		return String.format(Locale.ROOT, "%,.0f", amount);
	}

	private static String decimal(double value) {
		return String.format(Locale.ROOT, "%.2f", value);
	}

	private static String percent(double fraction) {
		return String.format(Locale.ROOT, "%.4g", fraction * 100).replaceAll("\\.?0+$", "") + "%";
	}

	private static String compact(long value) {
		if (value >= 1_000_000) return String.format(Locale.ROOT, "%.1fM", value / 1_000_000.0);
		if (value >= 1_000) return String.format(Locale.ROOT, "%.1fk", value / 1_000.0);
		return Long.toString(value);
	}

	private static String time(long millis) {
		long seconds = millis / 1000;
		return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
