package com.openbravo.pos.reports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.openbravo.pos.forms.MenuDefinition;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.format.Formats;
import com.openbravo.pos.theme.RetailPOSColors;
import com.openbravo.pos.theme.RetailPOSTheme;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.JToggleButton;
import org.junit.jupiter.api.Test;

public class ReportsWelcomeLayoutTest {

	@Test
	public void sectionsStartAtLeftAndFillViewportWithoutVerticalGap() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			MenuDefinition menu = new MenuDefinition("Menu.SalesManagement");
			for (int i = 0; i < 7; i++) {
				menu.addMenuItem(new AbstractAction("Report " + i) {
					@Override
					public void actionPerformed(ActionEvent event) {
					}
				});
			}
			JPanelReportsWelcome view = new JPanelReportsWelcome(null, menu);
			assertNull(view.getTitle());
			for (int width : new int[]{1000, 1600}) {
				view.setSize(new Dimension(width, 700));
				view.doLayout();
				JPanel header = (JPanel) view.getComponent(0);
				header.doLayout();
				Component heading = header.getComponent(0);
				Component updated = header.getComponent(2);
				Component detailedReports = header.getComponent(4);
				assertTrue(heading.getX() + heading.getWidth() <= updated.getX());
				assertTrue(updated.getX() + updated.getWidth() <= detailedReports.getX());
				assertTrue(detailedReports.getX() + detailedReports.getWidth() <= header.getWidth());
				JScrollPane scroll = (JScrollPane) view.getComponent(1);
				scroll.doLayout();
				scroll.getViewport().doLayout();
				JPanel body = (JPanel) scroll.getViewport().getView();
				body.doLayout();
				JPanel cardsSection = (JPanel) body.getComponent(0);
				JPanel cards = (JPanel) cardsSection.getComponent(1);
				JPanel detail = (JPanel) body.getComponent(1);
				JPanel reports = (JPanel) body.getComponent(2);
				assertEquals(0, cardsSection.getX());
				assertEquals(0, detail.getX());
				assertEquals(0, reports.getX());
				assertEquals(body.getWidth(), cardsSection.getWidth());
				assertEquals(body.getWidth(), detail.getWidth());
				assertEquals(body.getWidth(), reports.getWidth());
				assertEquals(0, cardsSection.getY());
				assertTrue(detail.getY() <= cardsSection.getHeight() + 16);
				assertTrue(reports.getY() > detail.getY());
				assertEquals(2, detail.getComponentCount());
				cardsSection.doLayout();
				cards.doLayout();
				assertTrue(cards.getComponent(0).getWidth() >= 200);
				assertEquals(4, cards.getComponentCount());
			}
		});
	}

	@Test
	public void periodCaptionsFitAtMinimumViewportWidthInAllThreeLanguages() throws Exception {
		Locale original = Locale.getDefault();
		try {
			for (Locale locale : new Locale[]{Locale.ENGLISH, new Locale("es", "ES"), new Locale("ca", "ES")}) {
				AppLocal.setLocale(locale);
				SwingUtilities.invokeAndWait(() -> {
					JPanelReportsWelcome view = new JPanelReportsWelcome(null,
							new MenuDefinition("Menu.SalesManagement"));
					view.setSize(1000, 700);
					layoutTree(view);
					JPanel header = (JPanel) view.getComponent(0);
					Component heading = header.getComponent(0);
					Component updated = header.getComponent(2);
					Component reportsButton = header.getComponent(4);
					assertTrue(heading.getX() + heading.getWidth() <= updated.getX(), locale.toString());
					assertTrue(updated.getX() + updated.getWidth() <= reportsButton.getX(), locale.toString());
					JScrollPane scroll = (JScrollPane) view.getComponent(1);
					JPanel body = (JPanel) scroll.getViewport().getView();
					JPanel cards = (JPanel) ((JPanel) body.getComponent(0)).getComponent(1);
					for (Component button : cards.getComponents()) {
						assertEquals(2, ((Container) button).getComponentCount());
						assertFalse(containsButton((Container) button));
						JPanel cardHeader = (JPanel) ((Container) button).getComponent(0);
						assertEquals(8, cardHeader.getInsets().left);
						JLabel tag = (JLabel) cardHeader.getComponent(1);
						assertEquals(RetailPOSColors.surface100(), tag.getForeground());
						assertEquals(button == cards.getComponent(1), tag.isVisible());
						Container content = (Container) ((Container) button).getComponent(1);
						for (Component label : content.getComponents()) {
							if (label instanceof JLabel && !((JLabel) label).getText().isBlank()) {
								assertTrue(label.getPreferredSize().width <= label.getWidth(),
										() -> locale + ": " + ((JLabel) label).getText());
							}
						}
					}
					JPanel detail = (JPanel) body.getComponent(1);
					JPanel aboveChart = (JPanel) detail.getComponent(0);
					JPanel top = (JPanel) aboveChart.getComponent(0);
					JPanel track = (JPanel) top.getComponent(0);
					assertEquals(RetailPOSColors.surface200(), track.getBackground());
					assertEquals(4, track.getComponentCount());
					for (Component segment : track.getComponents()) {
						assertTrue(segment instanceof JToggleButton);
						assertTrue(segment.getHeight() >= 44);
						assertTrue(((JToggleButton) segment).getText().length() > 0);
						assertFalse(segment.isOpaque());
						assertEquals(segment == track.getComponent(1), ((JToggleButton) segment).isSelected());
						assertFalse(((JToggleButton) segment).isContentAreaFilled());
					}
					BufferedImage segmentPixels = new BufferedImage(track.getWidth(), track.getHeight(),
							BufferedImage.TYPE_INT_ARGB);
					Graphics2D segmentsGraphics = segmentPixels.createGraphics();
					track.paint(segmentsGraphics);
					segmentsGraphics.dispose();
					Component unselected = track.getComponent(0);
					Component selected = track.getComponent(1);
					assertEquals(RetailPOSColors.surface200().getRGB(),
							segmentPixels.getRGB(unselected.getX() + 12, unselected.getY() + 8), locale.toString());
					assertEquals(RetailPOSColors.surface100().getRGB(),
							segmentPixels.getRGB(selected.getX() + 12, selected.getY() + 8), locale.toString());
					JPanel titleRow = (JPanel) top.getComponent(1);
					JPanel navigation = (JPanel) titleRow.getComponent(1);
					JPanel titleBlock = (JPanel) titleRow.getComponent(0);
					JPanel titleAndStatus = (JPanel) titleBlock.getComponent(0);
					for (Component label : titleAndStatus.getComponents()) {
						assertTrue(((JLabel) label).getText().length() > 0, locale.toString());
						assertTrue(label.getX() + label.getWidth() <= titleAndStatus.getWidth(), locale.toString());
					}
					assertTrue(navigation.getWidth() >= navigation.getPreferredSize().width, locale.toString());
					assertTrue(navigation.getComponent(1).getHeight() >= 48);
					assertTrue(navigation.getComponent(2).getHeight() >= 48);
					assertTrue(navigation.getComponent(1).getX() + navigation.getComponent(1).getWidth() <= navigation
							.getComponent(2).getX(), locale.toString());
					assertTrue(navigation.getComponent(2).getX() + navigation.getComponent(2).getWidth() <= navigation
							.getWidth(), locale.toString());
					assertEquals(0, ((JLabel) titleBlock.getComponent(1)).getX());
					assertEquals(titleBlock.getPreferredSize().width + navigation.getPreferredSize().width
							+ 8 > titleRow.getWidth(), navigation.getY() > titleBlock.getY(), locale.toString());
					JPanel figures = (JPanel) aboveChart.getComponent(2);
					assertEquals(5, figures.getComponentCount());
					assertTrue(figures.getY() - (top.getY() + top.getHeight()) <= 8);
					JPanel totalCell = (JPanel) figures.getComponent(0);
					assertEquals(16, totalCell.getInsets().left);
					assertEquals("DeltaBadge", totalCell.getComponent(4).getClass().getSimpleName());
					assertEquals(RetailPOSTheme.PLEX_MONO_REGULAR,
							((JLabel) totalCell.getComponent(6)).getFont().deriveFont(15f));
					JPanel overview = (JPanel) detail.getComponent(1);
					assertEquals(3, overview.getComponentCount());
					JPanel chartHeading = (JPanel) overview.getComponent(0);
					JPanel legend = (JPanel) chartHeading.getComponent(1);
					assertEquals(4, legend.getComponentCount());
					assertTrue(
							legend.getY() > chartHeading.getComponent(0).getY() || chartHeading.getComponent(0).getX()
									+ chartHeading.getComponent(0).getWidth() <= legend.getX(),
							locale.toString());
				});
			}
		} finally {
			AppLocal.setLocale(original);
		}
	}

	@Test
	public void historicalHeaderWrapsOnlyWhenNeededAndNamesBothPeriods() throws Exception {
		Locale original = Locale.getDefault();
		try {
			for (Locale locale : new Locale[]{Locale.ENGLISH, new Locale("es", "ES"), new Locale("ca", "ES")}) {
				AppLocal.setLocale(locale);
				SwingUtilities.invokeAndWait(() -> {
					try {
						JPanelReportsWelcome view = new JPanelReportsWelcome(null,
								new MenuDefinition("Menu.SalesManagement"));
						Field asOf = JPanelReportsWelcome.class.getDeclaredField("asOf");
						Field focus = JPanelReportsWelcome.class.getDeclaredField("focusAsOf");
						Method refresh = JPanelReportsWelcome.class.getDeclaredMethod("updatePeriodNavigation");
						Method showDetail = JPanelReportsWelcome.class.getDeclaredMethod("showDetail",
								SalesSummaryComparison.class);
						asOf.setAccessible(true);
						focus.setAccessible(true);
						refresh.setAccessible(true);
						showDetail.setAccessible(true);
						asOf.set(view, date(2026, 10, 24));
						for (ReportsWelcomePeriod period : new ReportsWelcomePeriod[]{ReportsWelcomePeriod.MONTH,
								ReportsWelcomePeriod.WEEK}) {
							JPanel body = (JPanel) ((JScrollPane) view.getComponent(1)).getViewport().getView();
							JPanel cards = (JPanel) ((JPanel) body.getComponent(0)).getComponent(1);
							((JButton) cards.getComponent(period.ordinal())).doClick();
							focus.set(view, date(2026, period == ReportsWelcomePeriod.MONTH ? 9 : 10,
									period == ReportsWelcomePeriod.MONTH ? 30 : 18));
							refresh.invoke(view);
							SalesSummaryComparison comparison = new SalesSummaryComparison(
									new SalesSummary(10, period == ReportsWelcomePeriod.MONTH ? 1234 : 789, 0, 0),
									new SalesSummary(9, 1000, 0, 0));
							showDetail.invoke(view, comparison);
							for (int width : new int[]{1600, 1000, 1600}) {
								view.setSize(width, 700);
								layoutTree(view);
								layoutTree(view);
								JPanel detail = (JPanel) body.getComponent(1);
								JPanel figures = (JPanel) ((JPanel) detail.getComponent(0)).getComponent(2);
								JPanel total = (JPanel) figures.getComponent(0);
								total.invalidate();
								layoutTree(total);
								assertEquals(Formats.CURRENCY.formatValue(comparison.getCurrent().getNetSales()),
										((JLabel) total.getComponent(3)).getText());
								assertEquals(ReportsWelcomeLabels.focusRange(period, (Date) focus.get(view)),
										((JLabel) total.getComponent(2)).getText());
								for (int index : new int[]{2, 3, 4, 5, 6}) {
									JLabel label = (JLabel) total.getComponent(index);
									assertTrue(label.getPreferredSize().width <= label.getWidth(),
											locale + " " + width + " total: " + label.getText() + " preferred="
													+ label.getPreferredSize().width + " actual=" + label.getWidth()
													+ " max=" + label.getMaximumSize().width + " x=" + label.getX()
													+ " cell=" + total.getWidth() + " figures=" + figures.getWidth());
								}
								JPanel top = (JPanel) ((JPanel) detail.getComponent(0)).getComponent(0);
								JPanel heading = (JPanel) top.getComponent(1);
								JPanel titleBlock = (JPanel) heading.getComponent(0);
								JPanel navigation = (JPanel) heading.getComponent(1);
								JPanel titleRow = (JPanel) titleBlock.getComponent(0);
								JLabel title = (JLabel) titleRow.getComponent(0);
								JLabel status = (JLabel) titleRow.getComponent(1);
								JLabel caption = (JLabel) titleBlock.getComponent(1);
								assertEquals(0, caption.getX(), locale.toString());
								assertTrue(caption.getY() >= titleRow.getHeight(), locale.toString());
								assertTrue(
										title.getText().contains(period == ReportsWelcomePeriod.MONTH ? "2026" : "18"));
								assertTrue(status.getText().contains(AppLocal.getIntString(
										"reports.welcome.closed." + period.name().toLowerCase(Locale.ROOT))));
								assertTrue(navigation.getComponent(0).isVisible(), locale.toString());
								assertTrue(((JButton) navigation.getComponent(0)).getText().length() > 3);
								assertTrue(navigation.getComponent(2).isEnabled(), locale.toString());
								boolean needsWrap = titleBlock.getPreferredSize().width
										+ navigation.getPreferredSize().width + 8 > heading.getWidth();
								assertEquals(needsWrap, navigation.getY() > titleBlock.getY(), locale + " " + width);
								assertTrue(navigation.getX() + navigation.getWidth() <= heading.getWidth(),
										locale.toString());
								JPanel chartHeading = (JPanel) ((JPanel) detail.getComponent(1)).getComponent(0);
								assertTrue(((JLabel) chartHeading.getComponent(0)).getText()
										.contains(period == ReportsWelcomePeriod.MONTH ? "2026" : "18"));
								JPanel legend = (JPanel) chartHeading.getComponent(1);
								for (int index : new int[]{1, 3}) {
									assertFalse(((JLabel) legend.getComponent(index)).getText().isBlank());
									assertTrue(
											legend.getComponent(index).getX()
													+ legend.getComponent(index).getWidth() <= legend.getWidth(),
											locale.toString());
								}
							}
						}
					} catch (ReflectiveOperationException ex) {
						throw new AssertionError(ex);
					}
				});
			}
		} finally {
			AppLocal.setLocale(original);
		}
	}

	private static Date date(int year, int month, int day) {
		Calendar calendar = Calendar.getInstance();
		calendar.clear();
		calendar.set(year, month - 1, day);
		return calendar.getTime();
	}

	@Test
	public void rollingChartLegendRemainsVisibleAtMinimumWidth() throws Exception {
		Locale original = Locale.getDefault();
		try {
			for (Locale locale : new Locale[]{Locale.ENGLISH, new Locale("es", "ES"), new Locale("ca", "ES")}) {
				AppLocal.setLocale(locale);
				SwingUtilities.invokeAndWait(() -> {
					JPanelReportsWelcome view = new JPanelReportsWelcome(null,
							new MenuDefinition("Menu.SalesManagement"));
					JPanel body = (JPanel) ((JScrollPane) view.getComponent(1)).getViewport().getView();
					JPanel cards = (JPanel) ((JPanel) body.getComponent(0)).getComponent(1);
					((JButton) cards.getComponent(3)).doClick();
					view.setSize(1000, 700);
					layoutTree(view);
					layoutTree(view);
					JPanel cardHeader = (JPanel) ((JButton) cards.getComponent(3)).getComponent(0);
					JLabel cardTitle = (JLabel) cardHeader.getComponent(0);
					JLabel tag = (JLabel) cardHeader.getComponent(1);
					assertTrue(cardTitle.getPreferredSize().width <= cardTitle.getWidth(), locale.toString());
					assertTrue(cardTitle.getX() + cardTitle.getWidth() <= tag.getX(), locale.toString());
					assertTrue(tag.getX() + tag.getWidth() <= cardHeader.getWidth(), locale.toString());
					JPanel detail = (JPanel) body.getComponent(1);
					JPanel chartHeading = (JPanel) ((JPanel) detail.getComponent(1)).getComponent(0);
					JPanel legend = (JPanel) chartHeading.getComponent(1);
					assertTrue(
							legend.getY() > chartHeading.getComponent(0).getY() || chartHeading.getComponent(0).getX()
									+ chartHeading.getComponent(0).getWidth() <= legend.getX(),
							locale.toString());
					assertTrue(legend.getX() + legend.getWidth() <= chartHeading.getWidth(), locale.toString());
					for (int index : new int[]{1, 3}) {
						JLabel label = (JLabel) legend.getComponent(index);
						assertTrue(label.getPreferredSize().width <= label.getWidth(), locale.toString());
					}
				});
			}
		} finally {
			AppLocal.setLocale(original);
		}
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

	private static boolean containsButton(Container container) {
		for (Component child : container.getComponents()) {
			if (child instanceof JButton || child instanceof Container && containsButton((Container) child)) {
				return true;
			}
		}
		return false;
	}

	@Test
	public void loyaltyDiscountStaysBesideCountAndReportIconsHaveRealPadding() throws Exception {
		Locale original = Locale.getDefault();
		try {
			for (Locale locale : new Locale[]{Locale.ENGLISH, new Locale("es", "ES"), new Locale("ca", "ES")}) {
				AppLocal.setLocale(locale);
				SwingUtilities.invokeAndWait(() -> {
					MenuDefinition menu = new MenuDefinition("Menu.SalesManagement");
					menu.addMenuItem(new AbstractAction("Report") {
						{
							putValue(Action.SMALL_ICON, new ImageIcon(JPanelReportsWelcome.class
									.getResource("/com/openbravo/images/menu-sales-reports.png")));
						}

						@Override
						public void actionPerformed(ActionEvent event) {
						}
					});
					JPanelReportsWelcome view = new JPanelReportsWelcome(null, menu);
					view.setSize(1000, 700);
					layoutTree(view);
					JPanel body = (JPanel) ((JScrollPane) view.getComponent(1)).getViewport().getView();
					JPanel detail = (JPanel) body.getComponent(1);
					JPanel figures = (JPanel) ((JPanel) detail.getComponent(0)).getComponent(2);
					JPanel loyalty = (JPanel) figures.getComponent(3);
					JPanel amount = (JPanel) loyalty.getComponent(2);
					JLabel count = (JLabel) amount.getComponent(0);
					JLabel description = (JLabel) amount.getComponent(1);
					count.setText("9");
					description.setText(Formats.CURRENCY.formatValue(37.19) + " "
							+ AppLocal.getIntString("reports.welcome.loyaltyValue"));
					layoutTree(detail);
					assertTrue(count.getX() + count.getWidth() <= description.getX(),
							locale + " body=" + body.getWidth() + " detail=" + detail.getWidth() + " figures="
									+ figures.getWidth() + " loyalty=" + loyalty.getWidth() + " amount="
									+ amount.getWidth() + " count=" + count.getBounds() + " description="
									+ description.getBounds());
					assertTrue(description.getPreferredSize().width <= description.getWidth(),
							() -> locale + ": " + description.getText() + " preferred="
									+ description.getPreferredSize().width + " actual=" + description.getWidth()
									+ " row=" + amount.getWidth());
					JPanel reports = (JPanel) body.getComponent(2);
					JPanel entries = (JPanel) reports.getComponent(1);
					JButton button = (JButton) entries.getComponent(0);
					assertNotNull(button.getIcon());
					assertTrue(button.getInsets().left >= 16);
					assertTrue(button.getIconTextGap() >= 8);
				});
			}
		} finally {
			AppLocal.setLocale(original);
		}
	}
}
