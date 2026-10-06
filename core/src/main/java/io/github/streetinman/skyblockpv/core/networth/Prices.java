package io.github.streetinman.skyblockpv.core.networth;

import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Item prices in coins.
 *
 * @param bazaar    bazaar product ID → instant-sell price
 * @param lowestBin item ID → lowest Buy It Now auction price. Pets use keys like {@code ENDER_DRAGON;4}.
 */
public record Prices(Map<String, Double> bazaar, Map<String, Double> lowestBin) {

	/** Bazaar price if the item is sold there, else lowest BIN, else 0. */
	public double of(String id) {
		if (id == null) return 0;
		Double price = bazaar.get(id);
		if (price == null || price <= 0) price = lowestBin.get(id);
		return price == null ? 0 : price;
	}

	public boolean has(String id) {
		return id != null && (bazaar.containsKey(id) || lowestBin.containsKey(id));
	}

	/** Reads {@code /v2/skyblock/bazaar}: {@code products.<id>.quick_status.sellPrice}. */
	public static Map<String, Double> parseBazaar(JsonObject response) {
		Map<String, Double> prices = new java.util.HashMap<>();
		JsonObject products = response.has("products") ? response.getAsJsonObject("products") : new JsonObject();
		for (Map.Entry<String, JsonElement> e : products.entrySet()) {
			JsonObject quick = e.getValue().getAsJsonObject().getAsJsonObject("quick_status");
			if (quick != null && quick.has("sellPrice")) prices.put(e.getKey(), quick.get("sellPrice").getAsDouble());
		}
		return Map.copyOf(prices);
	}

	/** Reads a flat {@code {"ITEM_ID": price}} lowest-BIN list (the NEU format). */
	public static Map<String, Double> parseLowestBin(JsonObject response) {
		Map<String, Double> prices = new java.util.HashMap<>();
		for (Map.Entry<String, JsonElement> e : response.entrySet()) {
			if (e.getValue().isJsonPrimitive() && e.getValue().getAsJsonPrimitive().isNumber()) {
				prices.put(e.getKey(), e.getValue().getAsDouble());
			}
		}
		return Map.copyOf(prices);
	}
}
