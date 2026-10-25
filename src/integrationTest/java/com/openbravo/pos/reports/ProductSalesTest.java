package com.openbravo.pos.reports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import com.openbravo.pos.forms.AppLocal;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

public class ProductSalesTest {

	@Test
	public void keepsRefundsAsNegativeProductSales() {
		ProductSalesRow row = new ProductSalesRow("SKU-1", "Coffee", "Drinks", -1.0, -2.5);

		assertEquals("SKU-1", row.getReference());
		assertEquals(-1.0, row.getUnits(), 0.000001);
		assertEquals(-2.5, row.getAmount(), 0.000001);
	}

	@Test
	public void filtersCategorySubtreesBrandsAndTextAndBalancesGroupedTotals() {
		Map<String, ProductSalesRepository.Category> categories = new HashMap<>();
		categories.put("root", new ProductSalesRepository.Category("root", "Home", null));
		categories.put("sleep", new ProductSalesRepository.Category("sleep", "Pyjamas", "root"));
		categories.put("cotton", new ProductSalesRepository.Category("cotton", "Cotton", "sleep"));
		categories.put("other", new ProductSalesRepository.Category("other", "Shoes", "root"));
		List<ProductSalesRow> rows = Arrays.asList(
				new ProductSalesRow("C-1", "Cotton pyjamas", "", "cotton", "Acme", false, 2, 30),
				new ProductSalesRow("P-2", "Silk pyjamas", "", "sleep", "Other", false, 3, 60),
				new ProductSalesRow("S-3", "Slippers", "", "other", "Acme", false, 1, 10),
				new ProductSalesRow("U-4", "Misc", "", null, "", false, 1, 5),
				new ProductSalesRow("", "", "", null, "", true, 4, 20));
		Map<String, Boolean> expanded = Collections.emptyMap();
		ProductSalesView all = view(rows, categories, null, null, "", ProductSalesView.Grouping.NONE, expanded);
		assertEquals(125, all.amount, 0.000001);
		assertEquals(4, all.productCount);
		assertEquals(20, all.manualAmount, 0.000001);
		assertTrue(all.lines.get(all.lines.size() - 2).product.isManual());
		assertEquals("Home › Pyjamas › Cotton", ProductSalesRepository.categoryPath("cotton", categories));
		assertEquals(90, view(rows, categories, "sleep", null, "", ProductSalesView.Grouping.NONE, expanded).amount);
		assertEquals(30, view(rows, categories, "cotton", null, "", ProductSalesView.Grouping.NONE, expanded).amount);
		assertEquals(30,
				view(rows, categories, "sleep", "Acme", "C-1", ProductSalesView.Grouping.NONE, expanded).amount);
		assertEquals(0, view(rows, categories, "sleep", "Other", "Slippers", ProductSalesView.Grouping.NONE,
				expanded).productCount);
		for (ProductSalesView.Grouping grouping : ProductSalesView.Grouping.values()) {
			ProductSalesView grouped = view(rows, categories, null, null, "", grouping, expanded);
			assertEquals(all.amount, grouped.amount, 0.000001);
			assertEquals(all.units, grouped.units, 0.000001);
			if (grouping != ProductSalesView.Grouping.NONE) {
				assertEquals(all.amount,
						grouped.lines.stream().filter(line -> line.group).mapToDouble(line -> line.amount).sum(),
						0.000001);
				assertTrue(grouped.lines.stream().anyMatch(line -> line.title.equals("Manual lines") && line.group));
				assertTrue(grouped.lines.stream()
						.anyMatch(line -> line.title.equals("No brand") || line.title.equals("No category")));
			}
		}
		ProductSalesView collapsed = view(rows, categories, null, null, "", ProductSalesView.Grouping.BRAND,
				Collections.singletonMap("Acme", true));
		assertFalse(collapsed.lines.stream()
				.anyMatch(line -> line.product != null && "C-1".equals(line.product.getReference())));
		assertEquals(all.amount, collapsed.amount, 0.000001);
	}

	private ProductSalesView view(List<ProductSalesRow> rows, Map<String, ProductSalesRepository.Category> categories,
			String category, String brand, String text, ProductSalesView.Grouping grouping,
			Map<String, Boolean> collapsed) {
		return ProductSalesView.build(rows, categories, category, brand, text, grouping, collapsed, "No category",
				"No brand", "Manual lines", "Total");
	}

	@Test
	public void presetSelectionLoadsOnceAndSharedSelectorKeepsManualButtonMode() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			AtomicInteger loads = new AtomicInteger();
			ReportPeriodSelector selector = new ReportPeriodSelector(loads::incrementAndGet);
			assertEquals(0, loads.get());
			javax.swing.JPanel buttons = (javax.swing.JPanel) selector.getComponent(0);
			((JToggleButton) buttons.getComponent(1)).doClick();
			assertEquals(1, loads.get());
			long yesterday = selector.getParameters().getStartInclusive().getTime();
			((JToggleButton) buttons.getComponent(0)).doClick();
			assertEquals(2, loads.get());
			assertTrue(selector.getParameters().getStartInclusive().getTime() > yesterday);
			assertEquals(1, new ReportPeriodSelector().getComponentCount());
		});
	}

	@Test
	public void reportControlsAndAmountsFitMinimumWidthInAllLanguages() throws Exception {
		Locale original = Locale.getDefault();
		try {
			for (Locale locale : new Locale[]{Locale.ENGLISH, new Locale("es", "ES"), new Locale("ca", "ES")}) {
				AppLocal.setLocale(locale);
				SwingUtilities.invokeAndWait(() -> {
					JPanelProductSales report = new JPanelProductSales();
					report.setSize(new Dimension(1000, 720));
					layout(report);
					JPanel controls = (JPanel) report.getComponent(0);
					ReportPeriodSelector dates = (ReportPeriodSelector) controls.getComponent(0);
					JPanel presets = (JPanel) dates.getComponent(0);
					for (Component button : presets.getComponents()) {
						assertTrue(button.getWidth() >= button.getPreferredSize().width, locale.toString());
					}
					JScrollPane scroll = (JScrollPane) report.getComponent(1);
					JTable table = (JTable) scroll.getViewport().getView();
					assertEquals(65, table.getColumnModel().getColumn(3).getPreferredWidth());
					assertEquals(125, table.getColumnModel().getColumn(4).getPreferredWidth());
					assertTrue(table.getColumnModel().getColumn(1).getPreferredWidth() >= 260);
					assertEquals(0, buttonsOfType(report, JButton.class, AppLocal.getIntString("Button.Load")));
					assertTrue(buttonsOfType(report, JComboBox.class, null) >= 3);
				});
			}
		} finally {
			AppLocal.setLocale(original);
		}
	}

	@Test
	public void repositoryLoadsManualLineCountsPathsBrandsAndPeriodBoundaries() throws Exception {
		try (Connection connection = DriverManager
				.getConnection("jdbc:derby:memory:productSales" + UUID.randomUUID() + ";create=true");
				Statement statement = connection.createStatement()) {
			statement.executeUpdate(
					"CREATE TABLE CATEGORIES (ID VARCHAR(255), NAME VARCHAR(255), PARENTID VARCHAR(255))");
			statement.executeUpdate(
					"CREATE TABLE PRODUCTS (ID VARCHAR(255), REFERENCE VARCHAR(255), NAME VARCHAR(255), CATEGORY VARCHAR(255), BRAND VARCHAR(255))");
			statement.executeUpdate(
					"CREATE TABLE TICKETLINES (TICKET VARCHAR(255), PRODUCT VARCHAR(255), UNITS DOUBLE, PRICE DOUBLE)");
			statement.executeUpdate("CREATE TABLE TICKETS (ID VARCHAR(255), TICKETTYPE INTEGER)");
			statement.executeUpdate("CREATE TABLE RECEIPTS (ID VARCHAR(255), DATENEW TIMESTAMP)");
			statement.executeUpdate(
					"INSERT INTO CATEGORIES VALUES ('root', 'Pyjamas', NULL), ('child', 'Cotton', 'root')");
			statement.executeUpdate("INSERT INTO PRODUCTS VALUES ('p', 'P-1', 'Cotton pyjamas', 'child', 'Acme')");
			statement.executeUpdate("INSERT INTO TICKETS VALUES ('t', 0), ('later', 0), ('refund', 1)");
			statement.executeUpdate(
					"INSERT INTO RECEIPTS VALUES ('t', '2026-10-25 14:00:00'), ('later', '2026-10-26 00:00:00'), ('refund', '2026-10-25 16:00:00')");
			statement.executeUpdate(
					"INSERT INTO TICKETLINES VALUES ('t', 'p', 2, 15), ('t', NULL, 2, 3), ('t', NULL, 1, 4), ('later', 'p', 100, 15), ('refund', 'p', -1, 15)");
			ProductSalesRepository repository = new ProductSalesRepository();
			Date start = Date.from(LocalDate.of(2026, 10, 25).atStartOfDay(ZoneId.systemDefault()).toInstant());
			Date end = Date.from(LocalDate.of(2026, 10, 26).atStartOfDay(ZoneId.systemDefault()).toInstant());
			List<ProductSalesRow> rows = repository.load(connection, new SalesSummaryParameters(start, end));
			assertEquals(2, rows.size());
			ProductSalesRow manual = rows.stream().filter(ProductSalesRow::isManual).findFirst().orElseThrow();
			assertEquals(2, manual.getLineCount());
			assertEquals(10, manual.getAmount(), 0.000001);
			assertEquals(1, rows.stream().filter(row -> !row.isManual()).findFirst().orElseThrow().getUnits(),
					0.000001);
			assertEquals("Pyjamas › Cotton",
					ProductSalesRepository.categoryPath("child", repository.categories(connection)));
			assertEquals(Collections.singletonList("Acme"), repository.brands(connection));
		}
	}

	private static void layout(Component component) {
		if (component instanceof Container) {
			component.doLayout();
			for (Component child : ((Container) component).getComponents()) {
				layout(child);
			}
		}
	}

	private static int buttonsOfType(Component component, Class<?> type, String key) {
		int count = type.isInstance(component) && (key == null || ((JButton) component).getText().equals(key)) ? 1 : 0;
		if (component instanceof Container) {
			for (Component child : ((Container) component).getComponents()) {
				count += buttonsOfType(child, type, key);
			}
		}
		return count;
	}
}
