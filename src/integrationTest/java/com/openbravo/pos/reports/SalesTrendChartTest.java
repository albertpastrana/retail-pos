package com.openbravo.pos.reports;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.openbravo.pos.theme.RetailPOSColors;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class SalesTrendChartTest {
	@Test
	void lastClosedDayBarIsFullyOpaqueAndFutureDayHasNoCurrentBar() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			SalesTrend trend = new SalesTrend(
					new String[]{"Mon 19", "Tue 20", "Wed 21", "Thu 22", "Fri 23", "Sat 24", "Sun 25"});
			for (int i = 0; i < 6; i++) {
				trend.reached[i] = true;
			}
			trend.current[5] = 25;
			trend.previous[6] = 25;
			SalesTrendChart chart = new SalesTrendChart();
			chart.setSize(700, 188);
			chart.setTrend(trend);
			BufferedImage image = new BufferedImage(700, 188, BufferedImage.TYPE_INT_ARGB);
			Graphics2D graphics = image.createGraphics();
			chart.paint(graphics);
			graphics.dispose();
			// The Saturday bar is the last closed day and must not be translucent.
			int bar = Math.min(18, ((700 - 8 - 38) / 7) / 3);
			int saturday = 38 + ((700 - 8 - 38) / 7) * 5 + ((700 - 8 - 38) / 7) / 2;
			assertEquals(RetailPOSColors.brand().getRGB(), image.getRGB(saturday - bar, 80));
			int sunday = 38 + ((700 - 8 - 38) / 7) * 6 + ((700 - 8 - 38) / 7) / 2;
			assertEquals(0, image.getRGB(sunday - bar, 80));
		});
	}
}
