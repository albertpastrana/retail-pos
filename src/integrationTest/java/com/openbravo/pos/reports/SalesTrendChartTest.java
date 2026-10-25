package com.openbravo.pos.reports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.openbravo.pos.theme.RetailPOSColors;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Locale;
import com.openbravo.pos.forms.AppLocal;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class SalesTrendChartTest {
	@Test
	void monthLabelsEveryDayAtMinimumChartWidth() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			String[] days = new String[31];
			for (int i = 0; i < days.length; i++) {
				days[i] = Integer.toString(i + 1);
			}
			SalesTrendChart chart = new SalesTrendChart();
			chart.setSize(880, 188);
			chart.setTrend(new SalesTrend(days));
			BufferedImage image = new BufferedImage(880, 188, BufferedImage.TYPE_INT_ARGB);
			Graphics2D graphics = image.createGraphics();
			chart.paint(graphics);
			graphics.dispose();
			int cell = (880 - 8 - 38) / days.length;
			for (int i = 0; i < days.length; i++) {
				int center = 38 + cell * i + cell / 2;
				boolean printed = false;
				for (int x = center - 9; x <= center + 9; x++) {
					for (int y = 166; y <= 181; y++) {
						printed |= (image.getRGB(x, y) >>> 24) != 0;
					}
				}
				assertTrue(printed, "missing day " + (i + 1));
			}
		});
	}

	@Test
	void axisUsesLocaleGroupingAndPartialPreviousMonthHasSolidAndDashedSections() throws Exception {
		Locale original = Locale.getDefault();
		try {
			AppLocal.setLocale(new Locale("ca", "ES"));
			assertEquals("48.391", SalesTrendChart.axisLabel(48391));
			SwingUtilities.invokeAndWait(() -> {
				SalesTrend trend = new SalesTrend(new String[]{"nov.", "des.", "gen.", "feb.", "març", "abr.", "maig",
						"juny", "jul.", "ag.", "set.", "oct. (1–24)"});
				trend.partialMonthIndex = 11;
				trend.reached[11] = true;
				trend.current[11] = 80;
				trend.previousElapsed[11] = 60;
				trend.previous[11] = 100;
				SalesTrendChart chart = new SalesTrendChart();
				chart.setSize(900, 188);
				chart.setTrend(trend);
				BufferedImage image = new BufferedImage(900, 188, BufferedImage.TYPE_INT_ARGB);
				Graphics2D graphics = image.createGraphics();
				chart.paint(graphics);
				graphics.dispose();
				int cell = (900 - 8 - 38) / 12;
				int x = 38 + cell * 11 + cell / 2;
				assertEquals(RetailPOSColors.brand().getRGB(), image.getRGB(x - 16, 100));
				assertEquals(RetailPOSColors.borderStrong().getRGB(), image.getRGB(x + 1, 100));
				int dashedPixels = 0;
				for (int y = 20; y < 65; y++) {
					if (image.getRGB(x + 1, y) == RetailPOSColors.borderStrong().getRGB()) {
						dashedPixels++;
					}
				}
				assertTrue(dashedPixels > 0 && dashedPixels < 45, "upper target must be dashed");
			});
			AppLocal.setLocale(Locale.ENGLISH);
			assertEquals("48,391", SalesTrendChart.axisLabel(48391));
		} finally {
			AppLocal.setLocale(original);
		}
	}

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
