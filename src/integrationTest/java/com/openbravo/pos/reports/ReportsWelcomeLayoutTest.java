package com.openbravo.pos.reports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.openbravo.pos.forms.MenuDefinition;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.format.Formats;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
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
				assertEquals(3, detail.getComponentCount());
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
						Container content = (Container) ((Container) button).getComponent(1);
						for (Component label : content.getComponents()) {
							if (label instanceof JLabel && !((JLabel) label).getText().isBlank()) {
								assertTrue(label.getPreferredSize().width <= label.getWidth(),
										() -> locale + ": " + ((JLabel) label).getText());
							}
						}
					}
					JPanel detail = (JPanel) body.getComponent(1);
					JPanel top = (JPanel) detail.getComponent(0);
					JPanel track = (JPanel) top.getComponent(0);
					assertEquals(4, track.getComponentCount());
					for (Component segment : track.getComponents()) {
						assertTrue(segment instanceof JToggleButton);
						assertTrue(segment.getHeight() >= 44);
						assertTrue(((JToggleButton) segment).getText().length() > 0);
					}
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
					assertEquals(1, ((JPanel) detail.getComponent(2)).getComponentCount());
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
					JPanel figures = (JPanel) detail.getComponent(1);
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
