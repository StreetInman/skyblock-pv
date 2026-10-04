package io.github.streetinman.skyblockpv.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/** Turns "§6§lLEGENDARY" style strings into real text components. */
public final class LegacyText {
	private LegacyText() {
	}

	/**
	 * @return the text with colours and formats applied. Italic is switched off explicitly,
	 *         because item names and lore are otherwise drawn italic.
	 */
	public static MutableComponent parse(String legacy) {
		MutableComponent result = Component.empty();
		StringBuilder run = new StringBuilder();
		Style style = Style.EMPTY.withItalic(false);

		for (int i = 0; i < legacy.length(); i++) {
			char c = legacy.charAt(i);
			if (c == '§' && i + 1 < legacy.length()) {
				ChatFormatting format = ChatFormatting.getByCode(Character.toLowerCase(legacy.charAt(i + 1)));
				if (format != null) {
					if (!run.isEmpty()) {
						result.append(Component.literal(run.toString()).setStyle(style));
						run.setLength(0);
					}
					style = apply(style, format);
					i++;
					continue;
				}
			}
			run.append(c);
		}
		if (!run.isEmpty()) {
			result.append(Component.literal(run.toString()).setStyle(style));
		}
		return result;
	}

	private static Style apply(Style style, ChatFormatting format) {
		// A colour code or §r resets bold/italic/etc., as in the 1.8 client.
		if (format.isColor()) return Style.EMPTY.withItalic(false).withColor(format);
		return switch (format) {
			case BOLD -> style.withBold(true);
			case ITALIC -> style.withItalic(true);
			case UNDERLINE -> style.withUnderlined(true);
			case STRIKETHROUGH -> style.withStrikethrough(true);
			case OBFUSCATED -> style.withObfuscated(true);
			default -> Style.EMPTY.withItalic(false);
		};
	}
}
