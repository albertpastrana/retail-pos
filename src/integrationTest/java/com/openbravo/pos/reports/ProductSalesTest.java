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
import java.lang.reflect.Field;
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
import javax.swing.JTextField;
import javax.swing.JLabel;
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
	public void formatsProductUnitsForTheCatalanCaption() {
		Locale original = Locale.getDefault();
		try {
			AppLocal.setLocale(new Locale("ca", "ES"));
			assertEquals("7 u.", JPanelReportsWelcome.formatUnits(7));
			assertEquals("1,5 u.", JPanelReportsWelcome.formatUnits(1.5));
		} finally {
			AppLocal.setLocale(original);
		}
	}

	@Test
	public void formatsSharesAsPercentagesForTheCatalanLocale() {
		Locale original = Locale.getDefault();
		try {
			AppLocal.setLocale(new Locale("ca", "ES"));
			assertEquals("76,8 %", JPanelReportsWelcome.formatShare(0.768));
		} finally {
			AppLocal.setLocale(original);
		}
	}

	@Test
	public void searchesReferencesNamesCategoryPathsAndBrandsWithoutChangingTotals() {
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
		Map<String, Boolean> collapsedByDefault = Collections.emptyMap();
		ProductSalesView all = view(rows, categories, "", ProductSalesView.Grouping.NONE, collapsedByDefault);
		assertEquals(125, all.amount, 0.000001);
		assertEquals(4, all.productCount);
		assertEquals(20, all.manualAmount, 0.000001);
		assertTrue(all.lines.get(all.lines.size() - 2).product.isManual());
		assertEquals("Home › Pyjamas › Cotton", ProductSalesRepository.categoryPath("cotton", categories));
		assertEquals(100, view(rows, categories, "home", ProductSalesView.Grouping.NONE, collapsedByDefault).amount);
		assertEquals(90, view(rows, categories, "Pyjamas", ProductSalesView.Grouping.NONE, collapsedByDefault).amount);
		assertEquals(30, view(rows, categories, "Cotton", ProductSalesView.Grouping.NONE, collapsedByDefault).amount);
		assertEquals(30, view(rows, categories, "C-1", ProductSalesView.Grouping.NONE, collapsedByDefault).amount);
		assertEquals(10, view(rows, categories, "Slippers", ProductSalesView.Grouping.NONE, collapsedByDefault).amount);
		assertEquals(40, view(rows, categories, "Acme", ProductSalesView.Grouping.NONE, collapsedByDefault).amount);
		assertEquals(0,
				view(rows, categories, "not found", ProductSalesView.Grouping.NONE, collapsedByDefault).productCount);
		for (ProductSalesView.Grouping grouping : ProductSalesView.Grouping.values()) {
			ProductSalesView grouped = view(rows, categories, "", grouping, collapsedByDefault);
			assertEquals(all.amount, grouped.amount, 0.000001);
			assertEquals(all.units, grouped.units, 0.000001);
			if (grouping != ProductSalesView.Grouping.NONE) {
				assertEquals(all.amount, grouped.lines.stream().filter(line -> line.group && line.depth == 0)
						.mapToDouble(line -> line.amount).sum(), 0.000001);
				assertTrue(grouped.lines.stream().anyMatch(line -> line.title.equals("Manual lines") && line.group));
				assertTrue(grouped.lines.stream()
						.anyMatch(line -> line.title.equals("No brand") || line.title.equals("No category")));
				assertTrue(grouped.lines.get(grouped.lines.size() - 2).title.equals("Manual lines"));
				List<ProductSalesView.Line> productGroups = grouped.lines.stream()
						.filter(line -> line.group && line.depth == 0 && !line.title.equals("Manual lines"))
						.collect(java.util.stream.Collectors.toList());
				for (int i = 1; i < productGroups.size(); i++) {
					assertTrue(productGroups.get(i - 1).amount >= productGroups.get(i).amount
							|| productGroups.get(i).title.equals("No category"));
				}
				assertEquals(all.productCount, productGroups.stream().mapToInt(line -> line.productCount).sum());
				assertTrue(grouped.lines.get(grouped.lines.size() - 1).total);
			}
		}
		ProductSalesView brandDefault = view(rows, categories, "", ProductSalesView.Grouping.BRAND,
				Collections.emptyMap());
		assertFalse(brandDefault.lines.stream()
				.anyMatch(line -> line.product != null && "C-1".equals(line.product.getReference())));
		assertEquals(all.amount, brandDefault.amount, 0.000001);
		ProductSalesView acmeOpen = view(rows, categories, "", ProductSalesView.Grouping.BRAND,
				Collections.singletonMap("Acme", false));
		assertTrue(acmeOpen.lines.stream()
				.anyMatch(line -> line.product != null && "C-1".equals(line.product.getReference())));
	}

	@Test
	public void categoryAndProductOrderUsesAmountAndLeavesManualLinesLast() {
		Map<String, ProductSalesRepository.Category> categories = Map.of("a",
				new ProductSalesRepository.Category("a", "Aprons", null), "b",
				new ProductSalesRepository.Category("b", "Bed linen", null));
		List<ProductSalesRow> rows = Arrays.asList(new ProductSalesRow("b", "Zipper cover", "", "b", "", false, 2, 100),
				new ProductSalesRow("a2", "Zebra apron", "", "a", "", false, 1, 50),
				new ProductSalesRow("a1", "Amber apron [a1]", "", "a", "", false, 1, 3),
				new ProductSalesRow("u", "Unassigned", "", null, "", false, 1, 5),
				new ProductSalesRow("", "", "", null, "", true, 1, 20));
		ProductSalesView grouped = view(rows, categories, "", ProductSalesView.Grouping.CATEGORY,
				Collections.emptyMap());
		assertEquals("Bed linen", grouped.lines.get(0).title);
		assertEquals("Aprons", grouped.lines.get(1).title);
		assertEquals("No category", grouped.lines.get(2).title);
		assertEquals("Manual lines", grouped.lines.get(3).title);
		assertFalse(grouped.lines.stream().anyMatch(line -> line.product != null));
		assertEquals(178, grouped.lines.get(grouped.lines.size() - 1).amount, 0.000001);
		assertEquals(178, grouped.lines.stream().filter(line -> line.group).mapToDouble(line -> line.amount).sum(),
				0.000001);
		ProductSalesView expanded = view(rows, categories, "", ProductSalesView.Grouping.CATEGORY,
				expandedGroups(grouped));
		assertEquals("Bed linen", expanded.lines.get(0).title);
		assertEquals("Zipper cover", ProductSalesView.displayName(expanded.lines.get(1).product));
		assertEquals("Aprons", expanded.lines.get(2).title);
		assertEquals("Zebra apron", ProductSalesView.displayName(expanded.lines.get(3).product));
		assertEquals("Amber apron", ProductSalesView.displayName(expanded.lines.get(4).product));
		assertEquals("No category", expanded.lines.get(5).title);
		assertEquals("Manual lines", expanded.lines.get(7).title);
		List<ProductSalesRow> ordered = new java.util.ArrayList<>(rows.subList(0, 4));
		ordered.sort(ProductSalesView.amountOrder());
		assertEquals("Zipper cover", ProductSalesView.displayName(ordered.get(0)));
		assertEquals("Zebra apron", ProductSalesView.displayName(ordered.get(1)));
		assertEquals("Unassigned", ProductSalesView.displayName(ordered.get(2)));
		assertEquals("Amber apron", ProductSalesView.displayName(ordered.get(3)));
	}

	@Test
	public void categoryHierarchyRollsUpSubcategoriesWithoutCountingProductsTwice() {
		Map<String, ProductSalesRepository.Category> categories = Map.of("clothes",
				new ProductSalesRepository.Category("clothes", "Clothing", null), "pyjamas",
				new ProductSalesRepository.Category("pyjamas", "Pyjamas", "clothes"), "cotton",
				new ProductSalesRepository.Category("cotton", "Cotton", "pyjamas"), "home",
				new ProductSalesRepository.Category("home", "Home", null));
		List<ProductSalesRow> rows = Arrays.asList(
				new ProductSalesRow("c2", "Zebra cotton", "", "cotton", "A", false, 2, 30),
				new ProductSalesRow("h", "Towel", "", "home", "B", false, 1, 5),
				new ProductSalesRow("p", "Silk", "", "pyjamas", "A", false, 3, 60),
				new ProductSalesRow("r", "Cap", "", "clothes", "B", false, 1, 9),
				new ProductSalesRow("c1", "Amber cotton", "", "cotton", "A", false, 1, 1),
				new ProductSalesRow("u", "Misc", "", null, "", false, 1, 5),
				new ProductSalesRow("", "", "", null, "", true, 2, 20));
		ProductSalesView none = view(rows, categories, "", ProductSalesView.Grouping.NONE, Collections.emptyMap());
		ProductSalesView grouped = view(rows, categories, "", ProductSalesView.Grouping.CATEGORY,
				Collections.emptyMap());
		assertEquals(130, none.amount, 0.000001);
		assertEquals(none.amount, grouped.amount, 0.000001);
		assertEquals(none.units, grouped.units, 0.000001);
		assertEquals(none.amount, grouped.lines.stream().filter(line -> line.group && line.depth == 0)
				.mapToDouble(line -> line.amount).sum(), 0.000001);
		assertFalse(grouped.lines.stream().anyMatch(line -> line.product != null));
		assertEquals("Clothing", grouped.lines.get(0).title);
		assertEquals(100, grouped.lines.get(0).amount, 0.000001);
		assertEquals(4, grouped.lines.get(0).productCount);
		assertEquals("Home", grouped.lines.get(1).title);
		assertEquals("Manual lines", grouped.lines.get(grouped.lines.size() - 2).title);
		assertEquals(130, grouped.lines.get(grouped.lines.size() - 1).amount, 0.000001);
		Map<String, Boolean> expanded = expandedGroups(grouped);
		ProductSalesView open = view(rows, categories, "", ProductSalesView.Grouping.CATEGORY, expanded);
		assertEquals(none.amount,
				open.lines.stream().filter(line -> line.product != null).mapToDouble(line -> line.amount).sum(),
				0.000001);
		assertEquals("Clothing › Pyjamas", open.lines.get(1).title);
		assertEquals(91, open.lines.get(1).amount, 0.000001);
		assertEquals(1, open.lines.get(1).depth);
		assertEquals("Silk", ProductSalesView.displayName(open.lines.get(2).product));
		assertEquals("Clothing › Pyjamas › Cotton", open.lines.get(3).title);
		assertEquals(31, open.lines.get(3).amount, 0.000001);
		assertEquals(2, open.lines.get(3).depth);
		assertEquals("Zebra cotton", ProductSalesView.displayName(open.lines.get(4).product));
		assertEquals("Amber cotton", ProductSalesView.displayName(open.lines.get(5).product));
		assertEquals("Cap", ProductSalesView.displayName(open.lines.get(6).product));
		assertEquals("Manual lines", open.lines.get(open.lines.size() - 3).title);

		Map<String, Boolean> childCollapsed = new HashMap<>(expanded);
		childCollapsed.put("category:id:pyjamas", true);
		ProductSalesView childClosed = view(rows, categories, "", ProductSalesView.Grouping.CATEGORY, childCollapsed);
		assertTrue(childClosed.lines.stream().anyMatch(line -> line.title.equals("Clothing › Pyjamas") && line.group));
		assertFalse(childClosed.lines.stream().anyMatch(line -> line.title.endsWith("Cotton") && line.group));
		assertEquals(130, childClosed.amount, 0.000001);
		assertTrue(childClosed.groupKeys.contains("category:id:cotton"));
		Map<String, Boolean> parentCollapsed = new HashMap<>(expanded);
		parentCollapsed.put("category:id:clothes", true);
		ProductSalesView parentClosed = view(rows, categories, "", ProductSalesView.Grouping.CATEGORY, parentCollapsed);
		assertFalse(parentClosed.lines.stream().anyMatch(line -> line.title.contains("Pyjamas") && line.group));
		assertEquals(130, parentClosed.lines.stream().filter(line -> line.group && line.depth == 0)
				.mapToDouble(line -> line.amount).sum(), 0.000001);
		ProductSalesView filtered = view(rows, categories, "cotton", ProductSalesView.Grouping.CATEGORY,
				Collections.emptyMap());
		assertEquals(31, filtered.amount, 0.000001);
		assertEquals(31, filtered.lines.stream().filter(line -> line.group && line.depth == 0)
				.mapToDouble(line -> line.amount).sum(), 0.000001);
	}

	private ProductSalesView view(List<ProductSalesRow> rows, Map<String, ProductSalesRepository.Category> categories,
			String text, ProductSalesView.Grouping grouping, Map<String, Boolean> collapsed) {
		return ProductSalesView.build(rows, categories, text, grouping, collapsed, "No category", "No brand",
				"Manual lines", "Total");
	}

	private static Map<String, Boolean> expandedGroups(ProductSalesView view) {
		Map<String, Boolean> expanded = new HashMap<>();
		for (String key : view.groupKeys) {
			expanded.put(key, false);
		}
		return expanded;
	}

	@Test
	public void presetSelectionLoadsOnceAndSharedSelectorKeepsManualButtonMode() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			AtomicInteger loads = new AtomicInteger();
			ReportPeriodSelector selector = new ReportPeriodSelector(loads::incrementAndGet);
			assertEquals(0, loads.get());
			javax.swing.JPanel buttons = (javax.swing.JPanel) ((JPanel) selector.getComponent(0)).getComponent(0);
			((JToggleButton) buttons.getComponent(1)).doClick();
			assertEquals(1, loads.get());
			long yesterday = selector.getParameters().getStartInclusive().getTime();
			((JToggleButton) buttons.getComponent(0)).doClick();
			assertEquals(2, loads.get());
			assertTrue(selector.getParameters().getStartInclusive().getTime() > yesterday);
			JToggleButton week = (JToggleButton) buttons.getComponent(2);
			Object enter = week.getInputMap(JToggleButton.WHEN_FOCUSED)
					.get(javax.swing.KeyStroke.getKeyStroke("ENTER"));
			week.getActionMap().get(enter).actionPerformed(null);
			assertTrue(week.isSelected());
			assertEquals(3, loads.get());
			assertTrue(week.getInputMap(JToggleButton.WHEN_FOCUSED)
					.get(javax.swing.KeyStroke.getKeyStroke("pressed SPACE")) != null);
			((JToggleButton) buttons.getComponent(5)).doClick();
			javax.swing.JPanel detail = (javax.swing.JPanel) selector.getComponent(1);
			javax.swing.JPanel fields = (javax.swing.JPanel) detail.getComponent(1);
			javax.swing.JPanel startPicker = (javax.swing.JPanel) fields.getComponent(1);
			((JTextField) startPicker.getComponent(0)).postActionEvent();
			assertEquals(5, loads.get());
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
					for (int width : new int[]{800, 1000, 1280}) {
						JPanelProductSales report = new JPanelProductSales();
						report.setSize(new Dimension(width, 720));
						layout(report);
						JPanel controls = (JPanel) report.getComponent(0);
						ReportPeriodSelector dates = (ReportPeriodSelector) controls.getComponent(0);
						JPanel header = (JPanel) dates.getComponent(0);
						JPanel presets = (JPanel) header.getComponent(0);
						for (Component button : presets.getComponents()) {
							assertTrue(button.getWidth() >= button.getPreferredSize().width, locale.toString());
							assertTrue(button.getHeight() >= 44, locale.toString());
							if (width >= 1000) {
								assertEquals(presets.getComponent(0).getY(), button.getY(), locale.toString());
							}
						}
						JPanel searchSlot = (JPanel) header.getComponent(1);
						JPanel searchWell = (JPanel) searchSlot.getComponent(0);
						JTextField search = (JTextField) searchWell.getComponent(1);
						assertTrue(searchWell.getParent() == searchSlot);
						assertEquals(48, searchWell.getHeight());
						javax.swing.border.CompoundBorder normalBorder = (javax.swing.border.CompoundBorder) searchWell
								.getBorder();
						java.awt.event.FocusEvent gained = new java.awt.event.FocusEvent(search,
								java.awt.event.FocusEvent.FOCUS_GAINED);
						java.awt.event.FocusEvent lost = new java.awt.event.FocusEvent(search,
								java.awt.event.FocusEvent.FOCUS_LOST);
						for (java.awt.event.FocusListener listener : search.getFocusListeners()) {
							listener.focusGained(gained);
						}
						assertEquals(com.openbravo.pos.theme.RetailPOSColors.ink(),
								((javax.swing.border.LineBorder) ((javax.swing.border.CompoundBorder) searchWell
										.getBorder()).getOutsideBorder()).getLineColor());
						for (java.awt.event.FocusListener listener : search.getFocusListeners()) {
							listener.focusLost(lost);
						}
						assertEquals(normalBorder, searchWell.getBorder());
						assertEquals(AppLocal.getIntString("reports.product.searchHint"),
								search.getClientProperty("JTextField.placeholderText"));
						assertFalse(AppLocal.getIntString("reports.product.searchHint").isBlank());
						assertTrue(((JLabel) searchWell.getComponent(0)).getIcon() != null);
						assertTrue(searchSlot.getX() + searchSlot.getWidth() <= header.getWidth(), locale.toString());
						if (width == 800) {
							assertTrue(searchSlot.getY() >= presets.getHeight(), locale.toString());
						} else if (width == 1000) {
							assertTrue(searchSlot.getY() >= presets.getHeight(), locale.toString());
						} else if (width == 1280) {
							assertEquals(presets.getY(), searchSlot.getY(), locale.toString());
						}
						JPanel below = (JPanel) controls.getComponent(1);
						JPanel overview = (JPanel) below.getComponent(0);
						JPanel figures = (JPanel) overview.getComponent(0);
						assertEquals(32, figures.getComponent(1).getWidth());
						assertTrue(figures.getComponent(4).getX() < figures.getWidth() / 2);
						JPanel toolbar = (JPanel) below.getComponent(1);
						JPanel groupButtons = (JPanel) toolbar.getComponent(1);
						JPanel actions = (JPanel) toolbar.getComponent(2);
						assertEquals(48, groupButtons.getHeight());
						assertTrue(actions.isVisible());
						assertTrue(actions.getX() + actions.getWidth() <= toolbar.getWidth());
						for (Component action : actions.getComponents()) {
							assertEquals(48, action.getHeight());
							JButton button = (JButton) action;
							assertFalse(button.isPreferredSizeSet(), locale.toString());
							assertActionFits(button, locale);
							java.awt.Font buttonFont = button.getFont();
							button.setFont(buttonFont.deriveFont(buttonFont.getSize2D() * 1.25f));
							layout(report);
							layout(report);
							assertActionFits(button, locale);
							assertTrue(actions.getX() + actions.getWidth() <= toolbar.getWidth(), locale.toString());
							assertTrue(button.getX() + button.getWidth() <= actions.getWidth(), locale.toString());
							assertTrue(button.getY() + button.getHeight() <= actions.getHeight(), locale.toString());
							button.setFont(buttonFont);
							layout(report);
						}
						JScrollPane scroll = (JScrollPane) report.getComponent(1);
						JTable table = (JTable) scroll.getViewport().getView();
						assertEquals(65, table.getColumnModel().getColumn(3).getPreferredWidth());
						assertEquals(125, table.getColumnModel().getColumn(4).getPreferredWidth());
						assertTrue(table.getColumnModel().getColumn(0).getPreferredWidth() >= 260);
						assertEquals(0, buttonsOfType(report, JButton.class, AppLocal.getIntString("Button.Load")));
						assertEquals(0, buttonsOfType(report, JComboBox.class, null));
						assertEquals(9, buttonsOfType(report, JToggleButton.class, null));
						((JToggleButton) presets.getComponent(5)).doClick();
						layout(report);
						layout(report);
						JPanel detail = (JPanel) dates.getComponent(1);
						assertTrue(detail.getComponent(1).isVisible());
						assertEquals(48, searchWell.getHeight());
						assertTrue(detail.getY() >= header.getY() + header.getHeight(), locale.toString());
					}
				});
			}
		} finally {
			AppLocal.setLocale(original);
		}
	}

	private static void assertActionFits(JButton button, Locale locale) {
		int textWidth = button.getFontMetrics(button.getFont()).stringWidth(button.getText()) + button.getInsets().left
				+ button.getInsets().right + 8;
		assertTrue(button.getWidth() >= textWidth,
				locale + " " + button.getText() + " needs " + textWidth + "px, has " + button.getWidth() + "px");
		assertTrue(button.getPreferredSize().width >= textWidth, locale + " " + button.getText());
	}

	@Test
	public void groupShareUsesOneDecimalAndTheLanguagePercentSuffix() {
		assertEquals("13,5 %", JPanelProductSales.groupPercentage(13.532, new Locale("ca", "ES")));
		assertEquals("13,5 %", JPanelProductSales.groupPercentage(13.532, new Locale("es", "ES")));
		assertEquals("13.5%", JPanelProductSales.groupPercentage(13.532, Locale.ENGLISH));
	}

	@Test
	public void stripsOnlyTheStoredReferenceSuffixInThisReport() {
		assertEquals("Vest — L/52", JPanelProductSales
				.displayName(new ProductSalesRow("A0DGOP1-001", "Vest — L/52 [A0DGOP1-001]", "", 1, 4)));
		assertEquals("Vest [A0DGOP1-001] in stock", JPanelProductSales
				.displayName(new ProductSalesRow("A0DGOP1-001", "Vest [A0DGOP1-001] in stock", "", 1, 4)));
		assertEquals("Vest [other]",
				JPanelProductSales.displayName(new ProductSalesRow("A0DGOP1-001", "Vest [other]", "", 1, 4)));
		assertEquals("Vest — M", JPanelProductSales
				.displayName(new ProductSalesRow("P01BTP1-000 M", "Vest — M [P01BTP1-000]", "", 1, 4)));
		assertEquals("Vest — L/52", JPanelProductSales
				.displayName(new ProductSalesRow("A0300P1-001 L/52", "Vest — L/52 [A0300P1-001]", "", 1, 4)));
		assertEquals("Vest [P01BTP1-000]",
				JPanelProductSales.displayName(new ProductSalesRow("P01BTP1-000X M", "Vest [P01BTP1-000]", "", 1, 4)));
	}

	@Test
	public void reportTableKeepsManualRowsAtEndWithTotalsAndSortIndicators() throws Exception {
		Locale original = Locale.getDefault();
		try {
			AppLocal.setLocale(Locale.ENGLISH);
			SwingUtilities.invokeAndWait(() -> {
				try {
					JPanelProductSales report = new JPanelProductSales();
					Field rows = JPanelProductSales.class.getDeclaredField("loaded");
					rows.setAccessible(true);
					rows.set(report,
							Arrays.asList(
									new ProductSalesRow("REFERENCE-1234567890", "Vest [REFERENCE-1234567890]", "", "c",
											"Acme", false, 2, 20),
									new ProductSalesRow("", "", "", null, "", true, 3, 9, 2)));
					Field categoryMap = JPanelProductSales.class.getDeclaredField("categories");
					categoryMap.setAccessible(true);
					categoryMap.set(report, Map.of("root", new ProductSalesRepository.Category("root", "Home", null),
							"c", new ProductSalesRepository.Category("c", "Clothes", "root")));
					java.lang.reflect.Method refresh = JPanelProductSales.class.getDeclaredMethod("refresh");
					refresh.setAccessible(true);
					refresh.invoke(report);
					report.setSize(1000, 700);
					layout(report);
					JScrollPane scroll = (JScrollPane) report.getComponent(1);
					JTable table = (JTable) scroll.getViewport().getView();
					assertEquals("Product", table.getColumnName(0));
					assertEquals("Reference", table.getColumnName(1));
					assertEquals(3, table.getRowCount());
					assertTrue(table.getValueAt(0, 0).toString().contains("Home"));
					assertTrue(table.getValueAt(0, 0).toString().contains("1 product"));
					assertFalse(table.getValueAt(0, 0).toString().contains("<br>"));
					assertTrue(table.getValueAt(0, 0).toString().contains("%"));
					assertTrue(table.getValueAt(1, 0).toString().contains("No product"));
					assertEquals(56, table.getRowHeight(0));
					assertEquals(56, table.getRowHeight(1));
					assertEquals(0, table.getColumnModel().getColumn(2).getWidth());
					assertTrue(table.getValueAt(2, 0).toString().contains("Total"));
					assertEquals(29, ((ProductSalesView) getView(report)).amount, 0.000001);
					assertEquals(29, ((ProductSalesView) getView(report)).lines.stream()
							.filter(line -> line.group && line.depth == 0).mapToDouble(line -> line.amount).sum(),
							0.000001);
					JPanel controls = (JPanel) report.getComponent(0);
					JPanel below = (JPanel) controls.getComponent(1);
					JPanel overview = (JPanel) below.getComponent(0);
					JPanel toolbar = (JPanel) below.getComponent(1);
					JPanel groupButtons = (JPanel) toolbar.getComponent(1);
					JPanel actions = (JPanel) toolbar.getComponent(2);
					Field searchField = JPanelProductSales.class.getDeclaredField("search");
					searchField.setAccessible(true);
					JTextField search = (JTextField) searchField.get(report);
					assertTrue(((JToggleButton) groupButtons.getComponent(1)).isSelected());
					((JButton) actions.getComponent(0)).doClick();
					assertEquals(6, table.getRowCount());
					assertTrue(table.getValueAt(1, 0).toString().contains("<b>Clothes</b>"));
					assertFalse(table.getValueAt(1, 0).toString().contains("Home ›"));
					javax.swing.JComponent rootHeader = (javax.swing.JComponent) table.getCellRenderer(0, 0)
							.getTableCellRendererComponent(table, table.getValueAt(0, 0), false, false, 0, 0);
					int rootInset = rootHeader.getBorder().getBorderInsets(rootHeader).left;
					javax.swing.JComponent childHeader = (javax.swing.JComponent) table.getCellRenderer(1, 0)
							.getTableCellRendererComponent(table, table.getValueAt(1, 0), false, false, 1, 0);
					assertEquals("Home › Clothes", childHeader.getToolTipText());
					assertTrue(childHeader.getBorder().getBorderInsets(childHeader).left > rootInset);
					assertEquals("Vest", table.getValueAt(2, 0));
					assertEquals("REFERENCE-1234567890", table.getValueAt(2, 1));
					assertTrue(table.getValueAt(3, 0).toString().contains("No product"));
					assertTrue(table.getValueAt(5, 0).toString().contains("Total"));
					for (String term : new String[]{"REFERENCE-1234567890", "Vest", "Home", "Clothes", "Acme"}) {
						search.setText(term);
						assertEquals(4, table.getRowCount(), term);
						assertEquals(20, ((ProductSalesView) getView(report)).amount, 0.000001, term);
					}
					search.setText("unknown product");
					assertEquals(1, table.getRowCount());
					assertEquals(0, ((ProductSalesView) getView(report)).amount, 0.000001);
					assertEquals(AppLocal.getIntString("reports.product.noResults"),
							((JLabel) overview.getComponent(1)).getText());
					search.setText("");
					java.awt.Rectangle groupRow = table.getCellRect(1, 0, true);
					table.dispatchEvent(new java.awt.event.MouseEvent(table, java.awt.event.MouseEvent.MOUSE_CLICKED,
							System.currentTimeMillis(), 0, groupRow.x + 20, groupRow.y + 20, 1, false));
					assertEquals(5, table.getRowCount());
					((JButton) actions.getComponent(0)).doClick();
					assertEquals(6, table.getRowCount());
					((JButton) actions.getComponent(1)).doClick();
					assertEquals(3, table.getRowCount());
					table.setRowSelectionInterval(0, 0);
					table.getActionMap().get("toggleGroup").actionPerformed(null);
					assertEquals(4, table.getRowCount());
					assertEquals(0, table.getSelectedRow());
					assertEquals("toggleGroup",
							table.getInputMap(JTable.WHEN_FOCUSED).get(javax.swing.KeyStroke.getKeyStroke("SPACE")));
					assertEquals("toggleGroup",
							table.getInputMap(JTable.WHEN_FOCUSED).get(javax.swing.KeyStroke.getKeyStroke("ENTER")));
					table.setRowSelectionInterval(1, 1);
					table.getActionMap().get("toggleGroup").actionPerformed(null);
					assertEquals(5, table.getRowCount());
					assertEquals(1, table.getSelectedRow());
					table.getActionMap().get("toggleGroup").actionPerformed(null);
					assertEquals(4, table.getRowCount());
					((JButton) actions.getComponent(0)).doClick();
					assertEquals(6, table.getRowCount());
					((JToggleButton) groupButtons.getComponent(0)).doClick();
					assertEquals(3, table.getRowCount());
					assertTrue(table.getColumnModel().getColumn(2).getWidth() > 0);
					JLabel categoryHeader = (JLabel) table.getTableHeader().getDefaultRenderer()
							.getTableCellRendererComponent(table, table.getColumnName(2), false, false, 0, 2);
					assertTrue(categoryHeader.getText().equals(table.getColumnName(2)));
					search.setText("Vest");
					assertEquals(2, table.getRowCount());
					assertEquals(20, ((ProductSalesView) getView(report)).amount, 0.000001);
					search.setText("");
					((JToggleButton) groupButtons.getComponent(2)).doClick();
					assertEquals(3, table.getRowCount());
					assertEquals(29, ((ProductSalesView) getView(report)).lines.stream().filter(line -> line.group)
							.mapToDouble(line -> line.amount).sum(), 0.000001);
					for (int width : new int[]{800, 1000, 1280}) {
						report.setSize(width, 700);
						layout(report);
						layout(report);
						assertTrue(actions.getX() + actions.getWidth() <= toolbar.getWidth());
						assertTrue(toolbar.getY() >= overview.getY() + overview.getHeight());
						assertTrue(table.getColumnModel().getTotalColumnWidth() <= scroll.getViewport().getWidth());
					}
					assertTrue(table.getColumnModel().getColumn(1).getWidth() > 120);
					JLabel amountHeader = (JLabel) table.getTableHeader().getDefaultRenderer()
							.getTableCellRendererComponent(table, table.getColumnName(4), false, false, 0, 4);
					assertTrue(amountHeader.getText().contains("↓"));
					assertEquals(javax.swing.SwingConstants.RIGHT, amountHeader.getHorizontalAlignment());
					JLabel productHeader = (JLabel) table.getTableHeader().getDefaultRenderer()
							.getTableCellRendererComponent(table, table.getColumnName(0), false, false, 0, 0);
					assertEquals(javax.swing.SwingConstants.LEFT, productHeader.getHorizontalAlignment());
				} catch (ReflectiveOperationException e) {
					throw new AssertionError(e);
				}
			});
		} finally {
			AppLocal.setLocale(original);
		}
	}

	private Object getView(JPanelProductSales report) throws ReflectiveOperationException {
		Field model = JPanelProductSales.class.getDeclaredField("model");
		model.setAccessible(true);
		Object value = model.get(report);
		Field view = value.getClass().getDeclaredField("view");
		view.setAccessible(true);
		return view.get(value);
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
