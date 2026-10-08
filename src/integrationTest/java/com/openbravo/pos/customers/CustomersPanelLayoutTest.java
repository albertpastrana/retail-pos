package com.openbravo.pos.customers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.openbravo.pos.theme.RetailPOSColors;
import com.openbravo.pos.theme.RetailPOSTheme;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Field;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.junit.jupiter.api.Test;

class CustomersPanelLayoutTest {
	@Test
	void customerFieldsFitAtTillSizeInBothThemes() throws Exception {
		RetailPOSTheme.registerDefaultsSource();
		for (boolean dark : new boolean[]{false, true}) {
			UIManager.setLookAndFeel(dark ? new FlatDarkLaf() : new FlatLightLaf());
			RetailPOSTheme.applyFonts();
			for (int[] size : new int[][]{{1024, 768}, {1600, 900}}) {
				SwingUtilities.invokeAndWait(() -> {
					CustomersPanel panel = new CustomersPanel();
					panel.setSize(size[0], size[1]);
					layoutTree(panel);
					try {
						JTextField name = field(panel, "name", JTextField.class);
						JTextField maxDebt = field(panel, "maxDebt", JTextField.class);
						JTextArea notes = field(panel, "notes", JTextArea.class);
						notes.setEnabled(true);
						name.setEnabled(true);
						maxDebt.setEnabled(true);
						JScrollPane notesScroll = (JScrollPane) notes.getParent().getParent();
						assertEquals(name.getHeight(), maxDebt.getHeight());
						assertTrue(maxDebt.getY() - notesScroll.getY() - notesScroll.getHeight() < 55);
						assertTrue(notesScroll.getHeight() >= notes.getFontMetrics(notes.getFont()).getHeight() * 5);
						assertFalse(notesScroll.getVerticalScrollBar().isVisible());
						assertEquals(RetailPOSColors.surface200(), notes.getBackground());
					} catch (ReflectiveOperationException e) {
						throw new RuntimeException(e);
					}
				});
			}
		}
	}

	private static <T> T field(CustomersPanel panel, String name, Class<T> type) throws ReflectiveOperationException {
		Field field = CustomersPanel.class.getDeclaredField(name);
		field.setAccessible(true);
		return type.cast(field.get(panel));
	}

	private static void layoutTree(Component component) {
		if (component instanceof Container container) {
			container.doLayout();
			for (Component child : container.getComponents()) {
				layoutTree(child);
			}
		}
	}
}
