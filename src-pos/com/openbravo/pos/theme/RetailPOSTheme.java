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

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.UIManager;
import com.formdev.flatlaf.FlatLaf;

/**
 * Bridges the Retail POS design system (design-system/tokens.json) onto the
 * app's existing FlatLaf setup. See design-system/swing-development.md for the
 * full mapping this class implements.
 *
 * @author Retail POS design system
 */
public final class RetailPOSTheme {

	private static final Logger logger = Logger.getLogger(RetailPOSTheme.class.getName());

	private static boolean defaultsSourceRegistered = false;

	// Manrope carries every UI surface; IBM Plex Mono is reserved for figures
	// (prices, totals, barcodes) so a glance tells "word" from "number" before
	// it's even read. Held as Font objects, never re-resolved by family name:
	// the Manrope files carry the internal family name "Manrope ExtraLight",
	// with each weight appending its own style (e.g. "ExtraBold").
	public static final Font MANROPE_REGULAR = loadFont("Manrope-Regular.ttf", 15f);
	public static final Font MANROPE_MEDIUM = loadFont("Manrope-Medium.ttf", 15f);
	public static final Font MANROPE_SEMIBOLD = loadFont("Manrope-SemiBold.ttf", 15f);
	public static final Font MANROPE_BOLD = loadFont("Manrope-Bold.ttf", 20f);
	public static final Font MANROPE_EXTRABOLD = loadFont("Manrope-ExtraBold.ttf", 40f);
	public static final Font PLEX_MONO_REGULAR = loadFont("IBMPlexMono-Regular.ttf", 15f);
	public static final Font PLEX_MONO_MEDIUM = loadFont("IBMPlexMono-Medium.ttf", 15f);
	public static final Font PLEX_MONO_SEMIBOLD = loadFont("IBMPlexMono-SemiBold.ttf", 17f);

	private RetailPOSTheme() {
	}

	/**
	 * Registers this package as a source of per-LookAndFeel custom defaults.
	 * FlatLaf then looks up "FlatLightLaf.properties" / "FlatDarkLaf.properties"
	 * next to this class whenever that look and feel is installed, and layers their
	 * values (the design system's tokens) on top of its own defaults. Call once,
	 * before UIManager.setLookAndFeel(...).
	 */
	public static synchronized void registerDefaultsSource() {
		if (!defaultsSourceRegistered) {
			FlatLaf.registerCustomDefaultsSource(RetailPOSTheme.class.getPackage().getName());
			defaultsSourceRegistered = true;
		}
	}

	/**
	 * Applies the design system's type ramp to the UIManager defaults. Call after
	 * the look and feel is installed and before any component is created, same as
	 * the app's own font scaling pass.
	 */
	public static void applyFonts() {
		UIManager.put("defaultFont", MANROPE_MEDIUM);
		UIManager.put("Table.font", PLEX_MONO_REGULAR);
		UIManager.put("Label.font", MANROPE_MEDIUM);
		UIManager.put("TitledBorder.font", MANROPE_SEMIBOLD);
	}

	private static Font loadFont(String fileName, float size) {
		try (InputStream in = RetailPOSTheme.class.getResourceAsStream("fonts/" + fileName)) {
			Font base = Font.createFont(Font.TRUETYPE_FONT, in);
			GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(base);
			return base.deriveFont(size);
		} catch (Exception e) {
			logger.log(Level.WARNING, "Cannot load Retail POS theme font " + fileName, e);
			return new Font(Font.SANS_SERIF, Font.PLAIN, (int) size);
		}
	}
}
