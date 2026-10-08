package io.github.streetinman.skyblockpv.gui;

import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import io.github.streetinman.skyblockpv.config.PvConfig;

/**
 * {@code /skyblockpv}: the mod's settings. Every option here is cosmetic and client-side; changes
 * apply immediately and are saved when the screen closes.
 */
public class SettingsScreen extends Screen {
	private static final int COLUMN = 150;
	private static final int GAP = 10;
	private static final int ROW = 22;
	private static final int WHITE = 0xFFFFFFFF;

	private final PvConfig config;
	private final Runnable save;
	private int top;
	private int leftX;
	private int rightX;

	public SettingsScreen(PvConfig config, Runnable save) {
		super(Component.literal("SkyBlock PV Settings"));
		this.config = config;
		this.save = save;
	}

	@Override
	protected void init() {
		PvConfig.PlayerModel model = config.playerModel;
		PvConfig.ItemAnimations anim = config.itemAnimations;
		top = height / 2 - 105;
		leftX = width / 2 - COLUMN - GAP / 2;
		rightX = width / 2 + GAP / 2;

		int y = top + 12;
		addRenderableWidget(new Slider(leftX, y, "Player size", 0.25, 2.0, 0.05, () -> model.scale, v -> model.scale = v, "%.2fx"));
		addRenderableWidget(toggle(leftX, y + ROW, "Other players too", () -> model.includeOtherPlayers,
				() -> model.includeOtherPlayers = !model.includeOtherPlayers));

		y = top + 72;
		addRenderableWidget(toggle(leftX, y, "Item animations", () -> anim.enabled, () -> anim.enabled = !anim.enabled));
		addRenderableWidget(new Slider(leftX, y + ROW, "Item size", 0.25, 2.0, 0.05, () -> anim.scale, v -> anim.scale = v, "%.2fx"));
		addRenderableWidget(new Slider(leftX, y + 2 * ROW, "Swing speed", 0.25, 3.0, 0.05, () -> anim.swingSpeed, v -> anim.swingSpeed = v, "%.2fx"));
		addRenderableWidget(toggle(leftX, y + 3 * ROW, "Skip equip animation", () -> anim.noEquipAnimation,
				() -> anim.noEquipAnimation = !anim.noEquipAnimation));

		y = top + 12;
		addRenderableWidget(new Slider(rightX, y, "Offset X", -1.5, 1.5, 0.05, () -> anim.offsetX, v -> anim.offsetX = v, "%.2f"));
		addRenderableWidget(new Slider(rightX, y + ROW, "Offset Y", -1.5, 1.5, 0.05, () -> anim.offsetY, v -> anim.offsetY = v, "%.2f"));
		addRenderableWidget(new Slider(rightX, y + 2 * ROW, "Offset Z", -1.5, 1.5, 0.05, () -> anim.offsetZ, v -> anim.offsetZ = v, "%.2f"));
		addRenderableWidget(new Slider(rightX, y + 3 * ROW, "Rotate X", -180, 180, 5, () -> anim.rotationX, v -> anim.rotationX = v, "%.0f°"));
		addRenderableWidget(new Slider(rightX, y + 4 * ROW, "Rotate Y", -180, 180, 5, () -> anim.rotationY, v -> anim.rotationY = v, "%.0f°"));
		addRenderableWidget(new Slider(rightX, y + 5 * ROW, "Rotate Z", -180, 180, 5, () -> anim.rotationZ, v -> anim.rotationZ = v, "%.0f°"));
		addRenderableWidget(Button.builder(Component.literal("Reset animations"), b -> {
			anim.reset();
			rebuildWidgets();
		}).bounds(rightX, y + 6 * ROW, COLUMN, 20).build());

		addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
				.bounds(width / 2 - 75, top + 190, 150, 20).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		g.centeredText(font, "§6§lSkyBlock PV Settings", width / 2, top - 14, WHITE);
		g.text(font, "§ePlayer Model", leftX, top, WHITE, true);
		g.text(font, "§eFirst-Person Items", leftX, top + 60, WHITE, true);
		g.text(font, "§eItem Position", rightX, top, WHITE, true);
		g.centeredText(font, "§7Visual only: nothing here changes what the server sees.", width / 2, top + 176, WHITE);
	}

	@Override
	public void removed() {
		save.run();
		super.removed();
	}

	private Button toggle(int x, int y, String label, java.util.function.BooleanSupplier get, Runnable flip) {
		return Button.builder(toggleLabel(label, get.getAsBoolean()), b -> {
			flip.run();
			b.setMessage(toggleLabel(label, get.getAsBoolean()));
		}).bounds(x, y, COLUMN, 20).build();
	}

	private static Component toggleLabel(String label, boolean on) {
		return Component.literal(label + ": " + (on ? "§aON" : "§cOFF"));
	}

	/** A slider over [min, max] that snaps to {@code step} and writes straight into the config. */
	private static final class Slider extends AbstractSliderButton {
		private final String label;
		private final double min;
		private final double max;
		private final double step;
		private final DoubleConsumer setter;
		private final String format;

		Slider(int x, int y, String label, double min, double max, double step, DoubleSupplier getter, DoubleConsumer setter, String format) {
			super(x, y, COLUMN, 20, Component.empty(), (Math.clamp(getter.getAsDouble(), min, max) - min) / (max - min));
			this.label = label;
			this.min = min;
			this.max = max;
			this.step = step;
			this.setter = setter;
			this.format = format;
			updateMessage();
		}

		private double current() {
			double raw = min + value * (max - min);
			return Math.clamp(Math.round(raw / step) * step, min, max);
		}

		@Override
		protected void updateMessage() {
			setMessage(Component.literal(label + ": " + String.format(Locale.ROOT, format, current())));
		}

		@Override
		protected void applyValue() {
			setter.accept(current());
		}
	}
}
