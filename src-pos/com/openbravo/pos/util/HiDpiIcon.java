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

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.net.URL;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * An icon whose image resource is authored at twice the size it displays at.
 * The image is scaled down when painted rather than when loaded, so on a 2x
 * display Java2D maps it onto the device pixels one to one and the icon stays
 * sharp instead of being interpolated up from the logical size.
 */
public class HiDpiIcon implements Icon {

	private static final int SOURCE_SCALE = 2;

	private Image image;
	private int iconwidth;
	private int iconheight;

	public HiDpiIcon(URL resource) {
		this(new ImageIcon(resource).getImage());
	}

	public HiDpiIcon(Image image) {

		this.image = image;
		iconwidth = image.getWidth(null) / SOURCE_SCALE;
		iconheight = image.getHeight(null) / SOURCE_SCALE;
	}

	public void paintIcon(Component c, Graphics g, int x, int y) {

		Graphics2D g2d = (Graphics2D) g.create();

		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2d.drawImage(image, x, y, iconwidth, iconheight, c);

		g2d.dispose();
	}

	public int getIconWidth() {
		return iconwidth;
	}

	public int getIconHeight() {
		return iconheight;
	}
}
