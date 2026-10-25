package com.openbravo.pos.reports;

import com.openbravo.pos.theme.RetailPOSColors;
import com.openbravo.pos.theme.RetailPOSTheme;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.text.NumberFormat;
import java.util.Locale;
import javax.swing.JComponent;

/** Token-coloured paired bars through the last closed day. */
final class SalesTrendChart extends JComponent {
	private SalesTrend trend;

	SalesTrendChart() {
		setPreferredSize(new Dimension(400, 188));
	}

	void setTrend(SalesTrend trend) {
		this.trend = trend;
		repaint();
	}

	static String axisLabel(double value) {
		return NumberFormat.getIntegerInstance(Locale.getDefault()).format(Math.round(value));
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		if (trend == null) {
			return;
		}
		Graphics2D g = (Graphics2D) graphics.create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		int right = getWidth() - 8;
		int bottom = getHeight() - 28;
		int top = 12;
		int height = Math.max(1, bottom - top);
		double maximum = 1;
		for (int i = 0; i < trend.labels.length; i++) {
			maximum = Math.max(maximum, Math.max(trend.current[i], trend.previous[i]));
		}
		g.setFont(RetailPOSTheme.PLEX_MONO_REGULAR.deriveFont(11f));
		int left = 38;
		for (int line = 0; line <= 3; line++) {
			left = Math.max(left, g.getFontMetrics().stringWidth(axisLabel(maximum * line / 3)) + 8);
		}
		g.setColor(RetailPOSColors.inkMuted());
		for (int line = 0; line <= 3; line++) {
			int y = bottom - line * height / 3;
			g.drawString(axisLabel(maximum * line / 3), 0, y + 4);
			g.setColor(RetailPOSColors.border());
			g.drawLine(left, y, right, y);
			g.setColor(RetailPOSColors.inkMuted());
		}
		int cell = Math.max(1, (right - left) / trend.labels.length);
		int bar = Math.max(2, Math.min(18, cell / 3));
		g.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(12f));
		for (int i = 0; i < trend.labels.length; i++) {
			int x = left + cell * i + cell / 2;
			int previousHeight = (int) Math.round(height * Math.max(0, trend.previous[i]) / maximum);
			g.setColor(RetailPOSColors.borderStrong());
			if (i == trend.partialMonthIndex) {
				int elapsedHeight = Math.min(previousHeight,
						(int) Math.round(height * Math.max(0, trend.previousElapsed[i]) / maximum));
				if (elapsedHeight > 0) {
					g.setStroke(new BasicStroke(1f));
					g.drawRoundRect(x + 1, bottom - elapsedHeight, bar, elapsedHeight, 4, 4);
				}
				if (previousHeight > elapsedHeight) {
					g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f,
							new float[]{3f, 3f}, 0f));
					g.drawRect(x + 1, bottom - previousHeight, bar, previousHeight - elapsedHeight);
				}
			} else {
				g.setStroke(trend.reached[i]
						? new BasicStroke(1f)
						: new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[]{3f, 3f},
								0f));
				g.drawRoundRect(x + 1, bottom - previousHeight, bar, previousHeight, 4, 4);
			}
			g.setStroke(new BasicStroke(1f));
			if (trend.reached[i]) {
				int currentHeight = (int) Math.round(height * Math.max(0, trend.current[i]) / maximum);
				g.setColor(RetailPOSColors.brand());
				g.fillRoundRect(x - bar - 1, bottom - currentHeight, bar, currentHeight, 4, 4);
			}
			g.setColor(RetailPOSColors.inkMuted());
			String text = trend.labels[i];
			g.drawString(text, x - g.getFontMetrics().stringWidth(text) / 2, getHeight() - 8);
		}
		g.dispose();
	}

}
