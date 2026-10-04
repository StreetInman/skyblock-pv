package io.github.streetinman.skyblockpv.core.model;

/**
 * One SkyBlock profile as seen from the viewed player.
 *
 * @param cuteName    fruit name, e.g. "Mango"
 * @param gameMode    null for normal, else "ironman", "island" (stranded) or "bingo"
 * @param bankBalance null when the banking API is off or the profile has no bank
 */
public record Profile(String profileId, String cuteName, String gameMode, boolean selected, Double bankBalance, MemberData member) {
}
