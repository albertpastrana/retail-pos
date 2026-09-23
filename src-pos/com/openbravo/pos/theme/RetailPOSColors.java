//    Openbravo POS is a point of sales application designed for touch screens.
//    Copyright (C) 2007-2009 Openbravo, S.L.
//    http://www.openbravo.com/product/pos
//
//    This file is part of Openbravo POS.
//
//    Openbravo POS is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    Openbravo POS is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with Openbravo POS.  If not, see <http://www.gnu.org/licenses/>.

package com.openbravo.pos.theme;

import java.awt.Color;
import javax.swing.AbstractButton;
import javax.swing.UIManager;

/**
 * The Retail POS design system's colour tokens (design-system/tokens.json), one
 * constant per token. Read live from the active FlatLaf theme's
 * FlatLightLaf.properties / FlatDarkLaf.properties (retailpos.* keys), so a
 * value here always tracks Configuration → General's Light/Dark choice — a
 * screen never has to know which one is active.
 *
 * A hardcoded java.awt.Color bypasses this: it never sees a theme switch, and
 * that's exactly what left buttons in Retail POS painted in a stray blue that
 * matched neither theme. Pull the colour from here instead, e.g.
 * {@code button.setBackground(RetailPOSColors.brand());}, or use
 * {@link #primaryButton(AbstractButton)} for a screen's one primary action.
 *
 * Falls back to the light theme's own values if a token is ever missing from
 * the active look and feel (e.g. a non-FlatLaf skin from "Installed skins" in
 * Configuration → General), so a screen never ends up with a null colour.
 *
 * @author Retail POS design system
 */
public final class RetailPOSColors {

	private RetailPOSColors() {
	}

	/** Page background behind every screen. */
	public static Color surface0() {
		return token("retailpos.surface0", 0xf5f1ea);
	}

	/** Raised surfaces: cards, panels, the ticket list, dialogs. */
	public static Color surface100() {
		return token("retailpos.surface100", 0xfffdf9);
	}

	/** Sunken fields: the numeric-pad well, input backgrounds, table stripes. */
	public static Color surface200() {
		return token("retailpos.surface200", 0xe5dac5);
	}

	/** Primary text and icons on surface0/surface100/surface200. */
	public static Color ink() {
		return token("retailpos.ink", 0x241c14);
	}

	/** Secondary text: captions, helper text, timestamps, placeholder text. */
	public static Color inkMuted() {
		return token("retailpos.inkMuted", 0x6b6154);
	}

	/**
	 * Hairline dividers and input/control outlines that must stay visible on their
	 * own.
	 */
	public static Color border() {
		return token("retailpos.border", 0x96896f);
	}

	/** Focus rings and the outline of the selected keypad key or list row. */
	public static Color borderStrong() {
		return token("retailpos.borderStrong", 0x6b5f47);
	}

	/**
	 * The one accent: a screen's single primary action (the sales screen's Charge
	 * button, a dialog's confirm button) and the active nav item. Never decoration
	 * — a screen with three brand-coloured elements has no primary action left.
	 */
	public static Color brand() {
		return token("retailpos.brand", 0xb8481f);
	}

	/** Hover/pressed state of brand-filled controls. */
	public static Color brandStrong() {
		return token("retailpos.brandStrong", 0x8f3216);
	}

	/** Low-intensity brand surface for selected list and table rows. */
	public static Color brandSubtle() {
		return token("retailpos.brandSubtle", 0xe7b8a4);
	}

	/** Text and icons on a brand-filled surface. */
	public static Color onBrand() {
		return token("retailpos.onBrand", 0xffffff);
	}

	/**
	 * Paid, in-stock, completed. Pair with the word or a check icon, never colour
	 * alone.
	 */
	public static Color success() {
		return token("retailpos.success", 0x1f6b5c);
	}

	/**
	 * Low stock, held ticket, price override in effect. Pair with the word or an
	 * icon.
	 */
	public static Color warning() {
		return token("retailpos.warning", 0x7a5300);
	}

	/** Void, refund, error, till closed. Pair with the word or an icon. */
	public static Color danger() {
		return token("retailpos.danger", 0x8a2a22);
	}

	/** Neutral notices: scan-to-import lookup, update available. Used sparingly. */
	public static Color info() {
		return token("retailpos.info", 0x3c6e8f);
	}

	/**
	 * Styles {@code button} as the screen's one primary action: {@link #brand()}
	 * background, {@link #onBrand()} text. There should be at most one of these per
	 * screen — see the design system's "Colour" rule in design-system/README.md.
	 */
	public static void primaryButton(AbstractButton button) {
		button.setOpaque(true);
		button.setBackground(brand());
		button.setForeground(onBrand());
		button.putClientProperty("FlatLaf.style",
				"background: " + toHex(brand()) + "; foreground: " + toHex(onBrand()) + "; focusedBackground: "
						+ toHex(brand()) + "; focusedForeground: " + toHex(onBrand()) + "; hoverBackground: "
						+ toHex(brandStrong()) + "; hoverForeground: " + toHex(onBrand()) + "; pressedBackground: "
						+ toHex(brandStrong()) + "; pressedForeground: " + toHex(onBrand()));
	}

	private static String toHex(Color color) {
		return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
	}

	private static Color token(String key, int lightRgbFallback) {
		Color color = UIManager.getColor(key);
		return color != null ? color : new Color(lightRgbFallback);
	}
}
