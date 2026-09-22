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

package com.openbravo.pos.printer.ticket;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

public class BasicTicketForScreen extends BasicTicket {

	private static Font BASEFONT = new Font("Monospaced", Font.PLAIN, 12)
			.deriveFont(AffineTransform.getScaleInstance(1.0, 1.40));
	private static int FONTHEIGHT = 20;
	private static double IMAGE_SCALE = 1.0;

	public static int getLineWidth(int columns) {
		BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		FontMetrics normal = graphics.getFontMetrics(BASEFONT);
		FontMetrics doubleWidth = graphics.getFontMetrics(new MyPrinterState(3).getFont(BASEFONT, 0));
		int normalWidth = normal.stringWidth(repeat('0', columns));
		int doubleWidthValue = doubleWidth.stringWidth(repeat('0', columns / 2));
		graphics.dispose();
		return Math.max(normalWidth, doubleWidthValue);
	}

	private static String repeat(char character, int count) {
		StringBuilder result = new StringBuilder(count);
		for (int i = 0; i < count; i++) {
			result.append(character);
		}
		return result.toString();
	}

	@Override
	protected Font getBaseFont() {
		return BASEFONT;
	}

	@Override
	protected int getFontHeight() {
		return FONTHEIGHT;
	}

	@Override
	protected double getImageScale() {
		return IMAGE_SCALE;
	}
}
