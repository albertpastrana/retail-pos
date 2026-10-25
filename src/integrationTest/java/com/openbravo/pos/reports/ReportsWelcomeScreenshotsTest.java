package com.openbravo.pos.reports;

import com.openbravo.pos.forms.AppLocal;
import com.openbravo.format.Formats;
import com.openbravo.pos.forms.MenuDefinition;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import static org.junit.jupiter.api.Assertions.assertTrue;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

/** Reproducible Catalan visual-review samples, using example data only. */
class ReportsWelcomeScreenshotsTest {
	@Test
	void capturesCurrentYearRollingYearAndPreviousMonth() throws Exception {
		Locale original = Locale.getDefault();
		Path output = Path.of("build", "reports", "reports-welcome", "screenshots");
		Files.createDirectories(output);
		try (Connection connection = DriverManager
				.getConnection("jdbc:derby:memory:reportsScreenshotsIT;create=true")) {
			seed(connection);
			AppLocal.setLocale(new Locale("ca", "ES"));
			Formats.setCurrencyPattern(null);
			capture(connection, output.resolve("reports-any-ca.png"), ReportsWelcomePeriod.YEAR, date(2026, 10, 24));
			capture(connection, output.resolve("reports-darrers-12-mesos-ca.png"), ReportsWelcomePeriod.ROLLING_YEAR,
					date(2026, 10, 24));
			capture(connection, output.resolve("reports-mes-anterior-ca.png"), ReportsWelcomePeriod.MONTH,
					date(2026, 9, 30));
		} finally {
			AppLocal.setLocale(original);
			Formats.setCurrencyPattern(null);
		}
	}

	private static void seed(Connection connection) throws Exception {
		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate("CREATE TABLE RECEIPTS (ID VARCHAR(40), DATENEW TIMESTAMP)");
			statement.executeUpdate("CREATE TABLE TICKETS (ID VARCHAR(40), TICKETTYPE INTEGER)");
			statement.executeUpdate("CREATE TABLE PAYMENTS (RECEIPT VARCHAR(40), TOTAL DOUBLE)");
			for (int year = 2024; year <= 2026; year++) {
				for (int month = 1; month <= 12; month++) {
					if (year == 2024 && month < 11 || year == 2026 && month > 10) {
						continue;
					}
					String id = year + "-" + month;
					insert(statement, id, year, month, 12, 6000 + (year - 2024) * 1900 + month * 380);
				}
			}
			insert(statement, "october-last-year-rest", 2025, 10, 27, 5000);
			insert(statement, "october-this-year-24", 2026, 10, 24, 4200);
		}
	}

	private static void insert(Statement statement, String id, int year, int month, int day, int amount)
			throws Exception {
		String timestamp = String.format(Locale.ROOT, "%04d-%02d-%02d 12:00:00", year, month, day);
		statement.executeUpdate("INSERT INTO RECEIPTS VALUES ('" + id + "', '" + timestamp + "')");
		statement.executeUpdate("INSERT INTO TICKETS VALUES ('" + id + "', 0)");
		statement.executeUpdate("INSERT INTO PAYMENTS VALUES ('" + id + "', " + amount + ")");
	}

	private static void capture(Connection connection, Path output, ReportsWelcomePeriod period, Date focus)
			throws Exception {
		SalesTrend trend = new SalesTrendRepository().load(connection, period, focus);
		SwingUtilities.invokeAndWait(() -> {
			try {
				JPanelReportsWelcome view = new JPanelReportsWelcome(null, new MenuDefinition("Menu.SalesManagement"));
				set(view, "asOf", date(2026, 10, 24));
				JPanel body = (JPanel) ((JScrollPane) view.getComponent(1)).getViewport().getView();
				JPanel cards = (JPanel) ((JPanel) body.getComponent(0)).getComponent(1);
				((JButton) cards.getComponent(period.ordinal())).doClick();
				set(view, "focusAsOf", focus);
				invoke(view, "updateDates");
				invoke(view, "updatePeriodNavigation");
				Method showCard = JPanelReportsWelcome.class.getDeclaredMethod("showCard", int.class,
						SalesSummaryComparison.class);
				showCard.setAccessible(true);
				for (int i = 0; i < 4; i++) {
					showCard.invoke(view, i, summary(16000 + i * 29000, 13000 + i * 26000));
				}
				Method showDetail = JPanelReportsWelcome.class.getDeclaredMethod("showDetail",
						SalesSummaryComparison.class);
				showDetail.setAccessible(true);
				showDetail.invoke(view,
						period == ReportsWelcomePeriod.MONTH
								? summary(14920, 12400)
								: summary(period == ReportsWelcomePeriod.YEAR ? 151480 : 182760,
										period == ReportsWelcomePeriod.YEAR ? 135600 : 159300));
				Method showTrend = JPanelReportsWelcome.class.getDeclaredMethod("showTrend", SalesTrend.class,
						Date.class);
				showTrend.setAccessible(true);
				showTrend.invoke(view, trend, focus);
				view.setSize(1000, 1080);
				body.invalidate();
				layoutTree(view);
				layoutTree(view);
				body.invalidate();
				body.setSize(body.getWidth(), body.getPreferredSize().height);
				layoutTree(body);
				JPanel detail = (JPanel) body.getComponent(1);
				JPanel overview = (JPanel) detail.getComponent(1);
				SalesTrendChart chart = (SalesTrendChart) overview.getComponent(1);
				assertTrue(chart.getHeight() >= 150,
						"chart=" + chart.getBounds() + " overview=" + overview.getBounds() + " preferred="
								+ overview.getPreferredSize() + " detail=" + detail.getBounds() + " detailPreferred="
								+ detail.getPreferredSize() + " body=" + body.getBounds() + " bodyPreferred="
								+ body.getPreferredSize());
				BufferedImage image = new BufferedImage(1000, 1080, BufferedImage.TYPE_INT_ARGB);
				Graphics2D graphics = image.createGraphics();
				view.printAll(graphics);
				graphics.dispose();
				ImageIO.write(image, "png", output.toFile());
			} catch (Exception ex) {
				throw new AssertionError(ex);
			}
		});
	}

	private static SalesSummaryComparison summary(int current, int previous) {
		return new SalesSummaryComparison(new SalesSummary(212, current, -120, 0, 1),
				new SalesSummary(180, previous, -95, 0, 2));
	}

	private static void set(JPanelReportsWelcome view, String field, Date date) throws Exception {
		Field value = JPanelReportsWelcome.class.getDeclaredField(field);
		value.setAccessible(true);
		value.set(view, date);
	}

	private static void invoke(JPanelReportsWelcome view, String name) throws Exception {
		Method method = JPanelReportsWelcome.class.getDeclaredMethod(name);
		method.setAccessible(true);
		method.invoke(view);
	}

	private static Date date(int year, int month, int day) {
		Calendar calendar = Calendar.getInstance();
		calendar.clear();
		calendar.set(year, month - 1, day);
		return calendar.getTime();
	}

	private static void layoutTree(Component component) {
		if (component instanceof JScrollPane) {
			component.doLayout();
			layoutTree(((JScrollPane) component).getViewport());
		} else if (component instanceof Container) {
			component.doLayout();
			for (Component child : ((Container) component).getComponents()) {
				layoutTree(child);
			}
		}
	}
}
