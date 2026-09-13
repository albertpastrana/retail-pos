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

package com.openbravo.pos.util;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Insets;
import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.SwingConstants;

/**
 * The shared look of the till buttons: every icon shows at the size of the
 * staff avatars, and a button that stands on its own carries its name under the
 * icon in a fixed square, so a longer word in another language cannot stretch
 * the row it sits in.
 */
public class TillButtons {

	public static final int ICON_SIZE = 32;

	// Icons are built at twice the size they show at, so HiDpiIcon can map them
	// onto the device pixels of a 2x display instead of interpolating them up.
	public static final int ICON_SOURCE_SIZE = 2 * ICON_SIZE;

	private static final int BUTTON_SIZE = 56;

	private static final int TEXT_MARGIN = 6;

	private static final ThumbNailBuilder ICONS = new ThumbNailBuilder(ICON_SOURCE_SIZE, ICON_SOURCE_SIZE);

	private TillButtons() {
	}

	public static Icon icon(String resource) {
		Image image = new ImageIcon(TillButtons.class.getResource(resource)).getImage();
		return new HiDpiIcon(ICONS.getThumbNail(image));
	}

	public static void labelUnderIcon(AbstractButton button, String text) {
		Font font = new Font("Dialog", Font.PLAIN, 10);
		button.setText(text);
		button.setFont(font);
		button.setVerticalTextPosition(SwingConstants.BOTTOM);
		button.setHorizontalTextPosition(SwingConstants.CENTER);
		button.setIconTextGap(2);
		button.setMargin(new Insets(2, 2, 2, 2));

		// A square, unless the name needs more room than the icon does.
		int width = text == null ? BUTTON_SIZE
				: Math.max(BUTTON_SIZE, button.getFontMetrics(font).stringWidth(text) + 2 * TEXT_MARGIN);
		button.setPreferredSize(new Dimension(width, BUTTON_SIZE));
	}
}
