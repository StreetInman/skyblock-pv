package io.github.streetinman.skyblockpv.core.items;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Every SkyBlock item and its recipes, read from a zip of the community NotEnoughUpdates item
 * repository (one JSON file per item under {@code items/}). Icons are not built here: the GUI
 * converts only the items on screen, so the whole list never sits in memory as ItemStacks.
 */
public final class ItemRepository {
	private static final String[] ROWS = {"A", "B", "C"};

	private final List<RepoItem> items;
	private final Map<String, RepoItem> byId;
	private final Map<String, List<RepoItem>> usedIn;

	private ItemRepository(List<RepoItem> items) {
		items.sort(Comparator.comparing(RepoItem::plainName, String.CASE_INSENSITIVE_ORDER));
		this.items = List.copyOf(items);
		Map<String, RepoItem> ids = new HashMap<>();
		Map<String, List<RepoItem>> uses = new HashMap<>();
		for (RepoItem item : items) {
			ids.put(item.id(), item);
			for (Recipe recipe : item.recipes()) {
				for (Recipe.Ingredient input : recipe.inputs()) {
					if (input == null) continue;
					List<RepoItem> list = uses.computeIfAbsent(input.id(), k -> new ArrayList<>());
					if (!list.contains(item)) list.add(item);
				}
			}
		}
		this.byId = ids;
		this.usedIn = uses;
	}

	public static ItemRepository load(Path zip) throws IOException {
		List<RepoItem> items = new ArrayList<>();
		try (ZipFile file = new ZipFile(zip.toFile())) {
			Enumeration<? extends ZipEntry> entries = file.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				String name = entry.getName();
				if (entry.isDirectory() || !name.endsWith(".json") || !name.contains("/items/")) continue;
				try (Reader reader = new InputStreamReader(file.getInputStream(entry), StandardCharsets.UTF_8)) {
					RepoItem item = parseItem(JsonParser.parseReader(reader).getAsJsonObject());
					if (item != null) items.add(item);
				} catch (RuntimeException e) {
					// One malformed file shouldn't hide the other few thousand.
				}
			}
		}
		if (items.isEmpty()) throw new IOException("No items found in " + zip.getFileName());
		return new ItemRepository(items);
	}

	/** For tests: build from already-parsed items. */
	static ItemRepository of(List<RepoItem> items) {
		return new ItemRepository(new ArrayList<>(items));
	}

	public List<RepoItem> all() {
		return items;
	}

	public RepoItem get(String id) {
		return byId.get(id);
	}

	/** Items whose name or ID contains every word of the query, in name order. */
	public List<RepoItem> search(String query) {
		String[] words = query.toLowerCase(Locale.ROOT).trim().split("\\s+");
		if (words.length == 1 && words[0].isEmpty()) return items;
		List<RepoItem> result = new ArrayList<>();
		outer:
		for (RepoItem item : items) {
			String text = item.searchText();
			for (String word : words) if (!text.contains(word)) continue outer;
			result.add(item);
		}
		return result;
	}

	/** Items with a recipe that takes this one as an ingredient. */
	public List<RepoItem> usedIn(String id) {
		return usedIn.getOrDefault(id, List.of());
	}

	static RepoItem parseItem(JsonObject json) {
		String id = str(json, "internalname");
		if (id == null) return null;
		List<String> lore = new ArrayList<>();
		if (json.has("lore") && json.get("lore").isJsonArray()) {
			for (JsonElement line : json.getAsJsonArray("lore")) lore.add(line.getAsString());
		}
		List<Recipe> recipes = new ArrayList<>();
		if (json.has("recipe") && json.get("recipe").isJsonObject()) {
			Recipe r = crafting(json.getAsJsonObject("recipe"), id);
			if (r != null) recipes.add(r);
		}
		if (json.has("recipes") && json.get("recipes").isJsonArray()) {
			for (JsonElement e : json.getAsJsonArray("recipes")) {
				if (!e.isJsonObject()) continue;
				Recipe r = recipe(e.getAsJsonObject(), id);
				if (r != null && !recipes.contains(r)) recipes.add(r);
			}
		}
		String name = str(json, "displayname");
		return new RepoItem(id, name == null ? id : name, str(json, "itemid"),
				json.has("damage") ? json.get("damage").getAsInt() : 0, str(json, "nbttag"),
				Collections.unmodifiableList(lore), List.copyOf(recipes));
	}

	private static Recipe recipe(JsonObject r, String id) {
		String type = str(r, "type");
		if (type == null || type.equals("crafting")) return crafting(r, id);
		String outputId = r.has("overrideOutputId") ? str(r, "overrideOutputId") : id;
		return switch (type) {
			case "forge" -> new Recipe("forge", ingredients(r.get("inputs")), new Recipe.Ingredient(outputId, count(r)),
					r.has("duration") ? "Forge time: " + duration(r.get("duration").getAsLong()) : null);
			case "npc_shop" -> new Recipe("npc_shop", ingredients(r.get("cost")), Recipe.Ingredient.parse(str(r, "result")), null);
			case "trade" -> new Recipe("trade", ingredients(r.get("cost")), Recipe.Ingredient.parse(str(r, "result")), null);
			case "drops" -> {
				List<Recipe.Ingredient> drops = new ArrayList<>();
				StringBuilder chances = new StringBuilder();
				if (r.has("drops") && r.get("drops").isJsonArray()) {
					for (JsonElement d : r.getAsJsonArray("drops")) {
						if (!d.isJsonObject()) continue;
						Recipe.Ingredient drop = Recipe.Ingredient.parse(str(d.getAsJsonObject(), "id"));
						if (drop != null && drop.id().equals(id) && d.getAsJsonObject().has("chance")) {
							chances.append(str(d.getAsJsonObject(), "chance"));
						}
						if (drop != null) drops.add(drop);
					}
				}
				String mob = str(r, "name");
				String note = "Dropped by " + (mob == null ? "a mob" : mob.replaceAll("§.", "")) + (chances.isEmpty() ? "" : " (" + chances + ")");
				yield new Recipe("drops", List.of(), new Recipe.Ingredient(id, 1), note);
			}
			case "katgrade" -> new Recipe("katgrade", ingredients(r.get("items")), Recipe.Ingredient.parse(str(r, "output")), "Kat pet upgrade");
			default -> new Recipe(type, ingredients(r.has("inputs") ? r.get("inputs") : r.get("cost")),
					new Recipe.Ingredient(outputId, count(r)), null);
		};
	}

	/** NEU's crafting grid: A1 A2 A3 is the top row, C1 C2 C3 the bottom. */
	private static Recipe crafting(JsonObject r, String id) {
		List<Recipe.Ingredient> grid = new ArrayList<>();
		boolean any = false;
		for (int i = 0; i < 9; i++) {
			Recipe.Ingredient slot = Recipe.Ingredient.parse(str(r, ROWS[i / 3] + (i % 3 + 1)));
			any |= slot != null;
			grid.add(slot);
		}
		if (!any) return null;
		String outputId = r.has("overrideOutputId") ? str(r, "overrideOutputId") : id;
		return new Recipe("crafting", Collections.unmodifiableList(grid), new Recipe.Ingredient(outputId, count(r)), null);
	}

	private static List<Recipe.Ingredient> ingredients(JsonElement e) {
		List<Recipe.Ingredient> list = new ArrayList<>();
		if (e == null) return list;
		if (e.isJsonArray()) {
			for (JsonElement x : (JsonArray) e) {
				Recipe.Ingredient in = x.isJsonPrimitive() ? Recipe.Ingredient.parse(x.getAsString()) : null;
				if (in != null) list.add(in);
			}
		} else if (e.isJsonPrimitive()) {
			Recipe.Ingredient in = Recipe.Ingredient.parse(e.getAsString());
			if (in != null) list.add(in);
		}
		return Collections.unmodifiableList(list);
	}

	private static int count(JsonObject r) {
		try {
			return r.has("count") ? Math.max(1, (int) r.get("count").getAsDouble()) : 1;
		} catch (RuntimeException e) {
			return 1;
		}
	}

	private static String duration(long seconds) {
		if (seconds >= 3600) return String.format(Locale.ROOT, "%dh %02dm", seconds / 3600, seconds % 3600 / 60);
		return String.format(Locale.ROOT, "%dm %02ds", seconds / 60, seconds % 60);
	}

	private static String str(JsonObject o, String key) {
		JsonElement e = o.get(key);
		return e != null && e.isJsonPrimitive() ? e.getAsString() : null;
	}
}
