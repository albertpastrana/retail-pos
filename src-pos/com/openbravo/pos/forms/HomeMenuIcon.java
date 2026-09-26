package com.openbravo.pos.forms;

import com.openbravo.pos.theme.RetailPOSColors;
import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import javax.swing.Icon;

/**
 * A theme-aware home icon at the same on-screen size as the other menu icons.
 */
final class HomeMenuIcon implements Icon {
	private static final int SIZE = 24;

	@Override
	public int getIconWidth() {
		return SIZE;
	}

	@Override
	public int getIconHeight() {
		return SIZE;
	}

	@Override
	public void paintIcon(Component component, Graphics graphics, int x, int y) {
		Graphics2D g = (Graphics2D) graphics.create();
		try {
			g.translate(x, y);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(RetailPOSColors.ink());
			g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			Path2D roof = new Path2D.Float();
			roof.moveTo(3, 10);
			roof.lineTo(12, 3);
			roof.lineTo(21, 10);
			g.draw(roof);
			Path2D house = new Path2D.Float();
			house.moveTo(5, 9);
			house.lineTo(5, 21);
			house.lineTo(19, 21);
			house.lineTo(19, 9);
			g.draw(house);
			Path2D door = new Path2D.Float();
			door.moveTo(10, 21);
			door.lineTo(10, 14);
			door.lineTo(14, 14);
			door.lineTo(14, 21);
			g.draw(door);
		} finally {
			g.dispose();
		}
	}
}
