package com.openbravo.pos.forms;

import static org.assertj.core.api.Assertions.assertThat;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.openbravo.pos.theme.RetailPOSColors;
import com.openbravo.pos.theme.RetailPOSTheme;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.junit.jupiter.api.Test;

class InitialScreenLayoutTest {

	@Test
	void rendersBothStepsAtTillSizeInEveryLocaleAndTheme() throws Exception {
		RetailPOSTheme.registerDefaultsSource();
		for (boolean dark : new boolean[]{false, true}) {
			UIManager.setLookAndFeel(dark ? new FlatDarkLaf() : new FlatLightLaf());
			RetailPOSTheme.applyFonts();
			for (Locale locale : new Locale[]{Locale.ENGLISH, Locale.forLanguageTag("es"),
					Locale.forLanguageTag("ca")}) {
				AppLocal.setLocale(locale);
				SwingUtilities.invokeAndWait(() -> {
					try {
						render(dark, locale);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				});
			}
		}
	}

	private void render(boolean dark, Locale locale) throws Exception {
		JRootApp app = new JRootApp();
		field(app, "m_jLblSubTitle", JLabel.class).setText("Botiga Sant Antoni");
		app.renderCashStatus("1", !dark);
		assertThat(field(app, "tillLabel", JLabel.class).getText())
				.contains(AppLocal.getIntString(dark ? "Label.CashClosed" : "Label.CashOpen"));
		String theme = dark ? "dark" : "light";
		String prefix = theme + "-" + locale.getLanguage();
		JButton sales = field(app, "m_jSalesMode", JButton.class);
		assertThat(sales.isFocusable()).isTrue();
		assertThat(field(app, "m_jAdminMode", JButton.class).isFocusable()).isTrue();
		JButton close = field(app, "m_jClose", JButton.class);
		assertThat(close.isFocusable()).isTrue();
		assertThat(close.getIcon()).isNotNull();
		assertThat(field(app, "m_jAbout", JButton.class).isContentAreaFilled()).isFalse();
		assertThat(RetailPOSColors.dangerText()).isEqualTo(dark ? RetailPOSColors.ink() : RetailPOSColors.danger());
		for (FocusListener listener : sales.getFocusListeners()) {
			listener.focusGained(new FocusEvent(sales, FocusEvent.FOCUS_GAINED));
		}
		snapshot(app, prefix + "-choice", 1024, 768);
		if (!dark && locale.getLanguage().equals("ca")) {
			snapshot(app, prefix + "-choice-wide", 1600, 900);
		}

		JPanel grid = field(app, "peopleGrid", JPanel.class);
		String[] names = {"Marta Puig", "Marta Soler", "Jordi Bosch", "Laia Ferrer", "Rosa Miralles", "Pau Rovira"};
		for (int i = 0; i < names.length; i++) {
			String role = i % 2 == 0 ? AppUser.ROLE_ADMINISTRATOR : AppUser.ROLE_MANAGER;
			JButton button = app.administratorButton(new AppUser("" + i, names[i], null, null, role, null));
			assertThat(button.getText())
					.contains(AppLocal.getIntString(i % 2 == 0 ? "Label.AdministratorRole" : "Label.ManagerRole"));
			assertThat(button.isFocusable()).isTrue();
			grid.add(button);
		}
		JButton first = (JButton) grid.getComponent(0);
		for (FocusListener listener : first.getFocusListeners()) {
			listener.focusGained(new FocusEvent(first, FocusEvent.FOCUS_GAINED));
		}
		JPanel steps = field(app, "loginSteps", JPanel.class);
		JButton back = field(app, "backButton", JButton.class);
		assertThat(back.getIcon()).isNotNull();
		((CardLayout) steps.getLayout()).show(steps, "administrators");
		var reflow = JRootApp.class.getDeclaredMethod("reflowPeople");
		reflow.setAccessible(true);
		reflow.invoke(app);
		snapshot(app, prefix + "-administrators", 1024, 768);
		assertThat(((GridLayout) grid.getLayout()).getColumns()).isEqualTo(2);
		JPanel header = field(app, "pickerHeader", JPanel.class);
		int gridX = SwingUtilities.convertPoint(grid, 0, 0, app).x;
		assertThat(SwingUtilities.convertPoint(header, 0, 0, app).x).isEqualTo(gridX);
		assertThat(header.getWidth()).isEqualTo(grid.getWidth());
		assertThat(gridX).isGreaterThanOrEqualTo(128);
		assertThat(1024 - gridX - grid.getWidth()).isGreaterThanOrEqualTo(128);
		assertThat(grid.getHeight()).isLessThan(768 - 64);
		if (!dark && locale.getLanguage().equals("ca")) {
			snapshot(app, prefix + "-administrators-wide", 1600, 900);
			assertThat(((GridLayout) grid.getLayout()).getColumns()).isEqualTo(2);
			assertThat(grid.getWidth()).isLessThanOrEqualTo(720);
			assertThat(SwingUtilities.convertPoint(header, 0, 0, app).x)
					.isEqualTo(SwingUtilities.convertPoint(grid, 0, 0, app).x);
			snapshot(app, prefix + "-administrators-narrow", 800, 768);
			reflow.invoke(app);
			snapshot(app, prefix + "-administrators-narrow", 800, 768);
			assertThat(((GridLayout) grid.getLayout()).getColumns()).isEqualTo(2);
			assertThat(grid.getWidth())
					.as("pref=%s max=%s parent=%s login=%s", grid.getPreferredSize(), grid.getMaximumSize(),
							grid.getParent().getWidth(), field(app, "m_jPanelLogin", JPanel.class).getWidth())
					.isLessThanOrEqualTo(608);
			assertThat(SwingUtilities.convertPoint(header, 0, 0, app).x)
					.isEqualTo(SwingUtilities.convertPoint(grid, 0, 0, app).x);
		}
	}

	private void snapshot(JRootApp app, String name, int width, int height) throws Exception {
		app.setSize(width, height);
		app.doLayout();
		layoutTree(app);
		app.sizeModeButtons();
		layoutTree(app);
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics();
		app.printAll(graphics);
		graphics.dispose();
		Path destination = Path.of("build/reports/initial-screen", name + ".png");
		Files.createDirectories(destination.getParent());
		ImageIO.write(image, "png", destination.toFile());
	}

	private void layoutTree(Container container) {
		container.doLayout();
		for (Component child : container.getComponents()) {
			if (child instanceof Container nested) {
				layoutTree(nested);
			}
		}
	}

	private <T> T field(JRootApp app, String name, Class<T> type) throws Exception {
		Field field = JRootApp.class.getDeclaredField(name);
		field.setAccessible(true);
		return type.cast(field.get(app));
	}
}
