package io.github.streetinman.skyblockpv.core.model;

import java.util.Map;

/**
 * Accessory bag power settings from {@code accessory_bag_storage}.
 *
 * @param magicalPower   highest magical power the bag has reached
 * @param selectedPower  power stone name, e.g. {@code silky}, or null if none picked
 * @param tuning         stat key (e.g. {@code strength}) → tuning points in the active slot, non-zero only
 * @param unlockedPowers how many power stones have been learned
 */
public record AccessoryPower(int magicalPower, String selectedPower, Map<String, Integer> tuning, int unlockedPowers) {
	public static final AccessoryPower NONE = new AccessoryPower(0, null, Map.of(), 0);
}
