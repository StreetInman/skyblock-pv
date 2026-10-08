package io.github.streetinman.skyblockpv.gui;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import io.github.streetinman.skyblockpv.SkyblockPvClient;
import io.github.streetinman.skyblockpv.compat.Compat;
import io.github.streetinman.skyblockpv.core.items.ItemRepository;
import io.github.streetinman.skyblockpv.core.items.Recipe;
import io.github.streetinman.skyblockpv.core.items.RepoItem;
import io.github.streetinman.skyblockpv.core.networth.Prices;
import io.github.streetinman.skyblockpv.item.ItemCatalog;

/**
 * {@code /sbitems}: every SkyBlock item, a page at a time, with a search box. Clicking an item
 * shows each way to get it (crafting grid, forge, shops, drops…) and what it's used in; clicking
 * an ingredient jumps to that item.
 */
public class ItemBrowserScreen extends Screen {
	private static final int PANEL_WIDTH = 372;
	private static final int PANEL_HEIGHT = 222;
	private static final int SLOT = 18;
	private static final int COLUMNS = 9;
	private static final int ROWS = 9;
	private static final int PER_PAGE = COLUMNS * ROWS;
	private static final int WHITE = 0xFFFFFFFF;
	private static final Map<String, String> TYPE_LABELS = Map.of("crafting", "Crafting", "forge", "Forge", "npc_shop", "NPC shop",
			"trade", "Trade", "drops", "Mob drop", "katgrade", "Kat upgrade");

	private final Screen parent;
	private String query;
	private ItemRepository repo;
	private String status = "§7Loading items…";
	private List<RepoItem> shown = List.of();
	private RepoItem usedInOf;
	private int page;
	private RepoItem selected;
	private int recipeIndex;
	private final Deque<RepoItem> history = new ArrayDeque<>();

	private int left;
	private int top;
	private EditBox searchBox;
	private final List<Hit> hits = new ArrayList<>();
	private ItemStack hovered;
	private Prices prices;

	private record Hit(int x, int y, int w, int h, Runnable action) {
		boolean contains(double mx, double my) {
			return mx >= x && mx < x + w && my >= y && my < y + h;
		}
	}

	public ItemBrowserScreen(Screen parent, String query) {
		super(Component.literal("SkyBlock Items"));
		this.parent = parent;
		this.query = query == null ? "" : query;
		ItemCatalog.repository().whenComplete((loaded, error) -> minecraft().execute(() -> {
			if (error != null) {
				status = "§cCouldn't load items: " + SkyblockPvClient.rootMessage(error);
				return;
			}
			repo = loaded;
			refilter();
		}));
		SkyblockPvClient.prices().prices().thenAccept(p -> minecraft().execute(() -> prices = p));
	}

	/** True once items are loaded (or failed to). Used by the launch self-test. */
	public boolean loaded() {
		return repo != null || (status != null && status.startsWith("§c"));
	}

	public String status() {
		return status;
	}

	/** Selects the n-th shown item, as clicking it would. */
	public void selectShown(int index) {
		if (index < shown.size()) select(shown.get(index), true);
	}

	private static net.minecraft.client.Minecraft minecraft() {
		return net.minecraft.client.Minecraft.getInstance();
	}

	@Override
	protected void init() {
		left = (width - PANEL_WIDTH) / 2;
		top = (height - PANEL_HEIGHT) / 2;
		searchBox = new EditBox(font, left + 6 + COLUMNS * SLOT - 110, top + 5, 110, 14, Component.literal("Search items"));
		searchBox.setHint(Component.literal("§8Search items…"));
		searchBox.setValue(query);
		searchBox.setResponder(text -> {
			if (text.equals(query)) return;
			query = text;
			usedInOf = null;
			refilter();
		});
		addRenderableWidget(searchBox);
		setInitialFocus(searchBox);
		int gridBottom = top + 24 + ROWS * SLOT;
		addRenderableWidget(Button.builder(Component.literal("<"), b -> turn(-1)).bounds(left + 6, gridBottom + 4, 20, 16).build());
		addRenderableWidget(Button.builder(Component.literal(">"), b -> turn(1)).bounds(left + 6 + COLUMNS * SLOT - 20, gridBottom + 4, 20, 16).build());
		if (parent != null) {
			addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose()).bounds(width / 2 - 40, top + PANEL_HEIGHT + 4, 80, 18).build());
		}
	}

	@Override
	public void onClose() {
		Compat.setScreen(parent);
	}

	private void refilter() {
		if (repo == null) return;
		if (usedInOf != null) shown = repo.usedIn(usedInOf.id());
		else shown = query.isBlank() ? repo.all() : repo.search(query);
		page = 0;
		status = shown.isEmpty() ? "§7No items match." : null;
	}

	private int pages() {
		return Math.max(1, (shown.size() + PER_PAGE - 1) / PER_PAGE);
	}

	private void turn(int delta) {
		page = Math.floorMod(page + delta, pages());
	}

	private void select(RepoItem item, boolean remember) {
		if (item == null || item == selected) return;
		if (remember && selected != null) history.push(selected);
		selected = item;
		recipeIndex = 0;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		hits.clear();
		hovered = null;
		g.fill(left - 1, top - 1, left + PANEL_WIDTH + 1, top + PANEL_HEIGHT + 1, 0xFF3A3A5C);
		g.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xE0101018);
		g.fill(left, top, left + PANEL_WIDTH, top + 1, 0xFF6C6CFF);
		int detailX = left + 14 + COLUMNS * SLOT;
		g.fill(detailX - 5, top + 6, detailX - 4, top + PANEL_HEIGHT - 6, 0xFF3A3A5C);
		super.extractRenderState(g, mouseX, mouseY, delta);

		String heading = usedInOf != null ? "§6Used in: §r" + usedInOf.name() : "§6§lSkyBlock Items";
		g.text(font, font.plainSubstrByWidth(heading, COLUMNS * SLOT - 116), left + 6, top + 9, WHITE, true);

		int gridY = top + 24;
		if (status != null) {
			g.centeredText(font, status, left + 6 + COLUMNS * SLOT / 2, gridY + 60, WHITE);
		} else {
			int start = page * PER_PAGE;
			for (int i = 0; i < PER_PAGE && start + i < shown.size(); i++) {
				RepoItem item = shown.get(start + i);
				int sx = left + 6 + (i % COLUMNS) * SLOT;
				int sy = gridY + (i / COLUMNS) * SLOT;
				slot(g, ItemCatalog.icon(item), sx, sy, mouseX, mouseY, item == selected, () -> select(item, true));
			}
		}
		int gridBottom = gridY + ROWS * SLOT;
		g.centeredText(font, "§7" + (page + 1) + " / " + pages() + "  §8(" + shown.size() + ")", left + 6 + COLUMNS * SLOT / 2, gridBottom + 8, WHITE);

		drawDetail(g, detailX, mouseX, mouseY);

		if (hovered != null) g.setTooltipForNextFrame(font, hovered, mouseX, mouseY);
	}

	private void drawDetail(GuiGraphicsExtractor g, int x, int mouseX, int mouseY) {
		int right = left + PANEL_WIDTH - 6;
		int w = right - x;
		if (selected == null) {
			int y = top + 30;
			for (String line : List.of("§7Click an item to see", "§7its recipes and uses.", "", "§7Click an ingredient to", "§7jump to it. Scroll to",
					"§7change pages.", "", "§8Data: NotEnoughUpdates repo")) {
				g.text(font, line, x, y, WHITE, false);
				y += 11;
			}
			return;
		}
		int y = top + 8;
		if (!history.isEmpty()) {
			link(g, "§b< Back", x, y, mouseX, mouseY, () -> {
				selected = history.pop();
				recipeIndex = 0;
			});
		}
		y += 12;
		slot(g, ItemCatalog.icon(selected), x, y, mouseX, mouseY, false, null);
		g.text(font, font.plainSubstrByWidth(selected.name(), w - 22), x + 21, y + 1, WHITE, true);
		g.text(font, "§8" + font.plainSubstrByWidth(selected.id(), w - 22), x + 21, y + 10, WHITE, false);
		y += 24;

		List<Recipe> recipes = selected.recipes();
		if (recipes.isEmpty()) {
			g.text(font, "§7No recipe known.", x, y, WHITE, false);
			y += 12;
		} else {
			recipeIndex = Math.floorMod(recipeIndex, recipes.size());
			Recipe recipe = recipes.get(recipeIndex);
			String label = "§e" + TYPE_LABELS.getOrDefault(recipe.type(), capitalize(recipe.type()));
			g.text(font, label, x, y, WHITE, false);
			if (recipes.size() > 1) {
				String pager = (recipeIndex + 1) + "/" + recipes.size();
				int px = right - font.width(pager) - 24;
				link(g, "§b<", px, y, mouseX, mouseY, () -> recipeIndex--);
				g.text(font, "§7" + pager, px + 9, y, WHITE, false);
				link(g, "§b>", right - 6, y, mouseX, mouseY, () -> recipeIndex++);
			}
			y += 12;
			y = drawRecipe(g, recipe, x, y, w, mouseX, mouseY);
		}

		y += 4;
		if (repo != null) {
			int uses = repo.usedIn(selected.id()).size();
			if (uses > 0) {
				RepoItem of = selected;
				link(g, "§bUsed in " + uses + " recipe" + (uses == 1 ? "" : "s") + " >", x, y, mouseX, mouseY, () -> {
					usedInOf = of;
					refilter();
				});
			} else {
				g.text(font, "§8Not used in any recipe.", x, y, WHITE, false);
			}
			y += 12;
		}
		if (prices != null && prices.has(selected.id())) {
			g.text(font, "§7Value: §6" + compact(prices.of(selected.id())) + " coins", x, y, WHITE, false);
		}
	}

	/** @return the y below what was drawn */
	private int drawRecipe(GuiGraphicsExtractor g, Recipe recipe, int x, int y, int w, int mouseX, int mouseY) {
		if (recipe.isCrafting()) {
			for (int i = 0; i < 9; i++) {
				Recipe.Ingredient in = i < recipe.inputs().size() ? recipe.inputs().get(i) : null;
				ingredient(g, in, x + (i % 3) * SLOT, y + (i / 3) * SLOT, mouseX, mouseY);
			}
			g.text(font, "§7→", x + 3 * SLOT + 6, y + SLOT + 5, WHITE, false);
			ingredient(g, recipe.output(), x + 3 * SLOT + 20, y + SLOT, mouseX, mouseY);
			return y + 3 * SLOT + 4;
		}
		int perRow = Math.max(1, w / SLOT);
		List<Recipe.Ingredient> inputs = recipe.inputs();
		for (int i = 0; i < inputs.size(); i++) {
			ingredient(g, inputs.get(i), x + (i % perRow) * SLOT, y + (i / perRow) * SLOT, mouseX, mouseY);
		}
		if (!inputs.isEmpty()) y += ((inputs.size() + perRow - 1) / perRow) * SLOT + 2;
		if (recipe.output() != null && !recipe.type().equals("drops")) {
			g.text(font, "§7Gives:", x, y + 5, WHITE, false);
			ingredient(g, recipe.output(), x + font.width("Gives:") + 4, y, mouseX, mouseY);
			y += SLOT + 2;
		}
		if (recipe.note() != null) {
			for (var line : font.split(Component.literal("§7" + recipe.note()), w)) {
				g.text(font, line, x, y, WHITE, false);
				y += 10;
			}
		}
		return y;
	}

	private void ingredient(GuiGraphicsExtractor g, Recipe.Ingredient in, int x, int y, int mouseX, int mouseY) {
		if (in == null) {
			g.fill(x, y, x + SLOT - 1, y + SLOT - 1, 0xFF373737);
			return;
		}
		RepoItem item = repo == null ? null : repo.get(in.id());
		ItemStack stack = item != null ? ItemCatalog.icon(item) : ItemCatalog.missing(in.id());
		if (in.count() != 1) stack = stack.copyWithCount(in.count());
		slot(g, stack, x, y, mouseX, mouseY, false, item == null ? null : () -> select(item, true));
	}

	private void slot(GuiGraphicsExtractor g, ItemStack stack, int x, int y, int mouseX, int mouseY, boolean highlight, Runnable click) {
		g.fill(x, y, x + SLOT - 1, y + SLOT - 1, highlight ? 0xFF6C6CFF : 0xFF373737);
		g.item(stack, x + 1, y + 1);
		g.itemDecorations(font, stack, x + 1, y + 1);
		if (mouseX >= x && mouseX < x + SLOT && mouseY >= y && mouseY < y + SLOT) {
			g.fill(x, y, x + SLOT - 1, y + SLOT - 1, 0x80FFFFFF);
			hovered = stack;
		}
		if (click != null) hits.add(new Hit(x, y, SLOT, SLOT, click));
	}

	private void link(GuiGraphicsExtractor g, String text, int x, int y, int mouseX, int mouseY, Runnable click) {
		int w = font.width(text);
		boolean over = mouseX >= x && mouseX < x + w && mouseY >= y - 1 && mouseY < y + 9;
		g.text(font, over ? text.replace("§b", "§f") : text, x, y, WHITE, false);
		hits.add(new Hit(x, y - 1, w, 10, click));
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) return true;
		if (event.button() != 0) return false;
		for (Hit hit : List.copyOf(hits)) {
			if (hit.contains(event.x(), event.y())) {
				hit.action().run();
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (scrollY != 0 && mouseX < left + 6 + COLUMNS * SLOT) {
			turn(scrollY > 0 ? -1 : 1);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	private static String compact(double value) {
		if (value >= 1e9) return String.format(java.util.Locale.ROOT, "%.2fB", value / 1e9);
		if (value >= 1e6) return String.format(java.util.Locale.ROOT, "%.2fM", value / 1e6);
		if (value >= 1e3) return String.format(java.util.Locale.ROOT, "%.1fk", value / 1e3);
		return String.format(java.util.Locale.ROOT, "%.0f", value);
	}

	private static String capitalize(String s) {
		if (s == null || s.isEmpty()) return "Recipe";
		String spaced = s.replace('_', ' ');
		return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
	}
}
