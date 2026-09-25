package com.openbravo.pos.reports;

import com.openbravo.pos.theme.RetailPOSColors;
import com.openbravo.pos.theme.RetailPOSTheme;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JComponent;

/**
 * Token-coloured paired bars; the current unfinished day/month is translucent.
 */
final class SalesTrendChart extends JComponent {
	private SalesTrend trend;

	SalesTrendChart() {
		setPreferredSize(new Dimension(400, 188));
	}

	void setTrend(SalesTrend trend) {
		this.trend = trend;
		repaint();
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		if (trend == null) {
			return;
		}
		Graphics2D g = (Graphics2D) graphics.create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		int left = 38;
		int right = getWidth() - 8;
		int bottom = getHeight() - 28;
		int top = 12;
		int height = Math.max(1, bottom - top);
		double maximum = 1;
		for (int i = 0; i < trend.labels.length; i++) {
			maximum = Math.max(maximum, Math.max(trend.current[i], trend.previous[i]));
		}
		g.setFont(RetailPOSTheme.PLEX_MONO_REGULAR.deriveFont(10f));
		g.setColor(RetailPOSColors.inkMuted());
		for (int line = 0; line <= 3; line++) {
			int y = bottom - line * height / 3;
			g.drawString(String.format("%.0f", maximum * line / 3), 0, y + 4);
			g.setColor(RetailPOSColors.border());
			g.drawLine(left, y, right, y);
			g.setColor(RetailPOSColors.inkMuted());
		}
		int cell = Math.max(1, (right - left) / trend.labels.length);
		int bar = Math.max(2, Math.min(18, cell / 3));
		int labelEvery = trend.labels.length > 16 ? 5 : 1;
		g.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(11f));
		for (int i = 0; i < trend.labels.length; i++) {
			int x = left + cell * i + cell / 2;
			int previousHeight = (int) Math.round(height * Math.max(0, trend.previous[i]) / maximum);
			g.setColor(RetailPOSColors.border());
			g.setStroke(trend.reached[i]
					? new BasicStroke(1f)
					: new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[]{3f, 3f}, 0f));
			g.drawRoundRect(x + 1, bottom - previousHeight, bar, previousHeight, 4, 4);
			g.setStroke(new BasicStroke(1f));
			if (trend.reached[i]) {
				int currentHeight = (int) Math.round(height * Math.max(0, trend.current[i]) / maximum);
				java.awt.Color brand = RetailPOSColors.brand();
				g.setColor(i == lastReached()
						? new java.awt.Color(brand.getRed(), brand.getGreen(), brand.getBlue(), 140)
						: brand);
				g.fillRoundRect(x - bar - 1, bottom - currentHeight, bar, currentHeight, 4, 4);
			}
			if (i % labelEvery == 0 || i == trend.labels.length - 1) {
				g.setColor(RetailPOSColors.inkMuted());
				String text = trend.labels[i];
				g.drawString(text, x - g.getFontMetrics().stringWidth(text) / 2, getHeight() - 8);
			}
		}
		g.dispose();
	}

	private int lastReached() {
		for (int i = trend.reached.length - 1; i >= 0; i--) {
			if (trend.reached[i]) {
				return i;
			}
		}
		return -1;
	}
}
