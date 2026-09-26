package com.openbravo.pos.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.openbravo.data.user.DirtyManager;
import com.openbravo.pos.forms.AppLocal;
import java.awt.Component;
import java.awt.Container;
import java.util.List;
import java.util.Locale;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class AdminEditorLayoutTest {
	@Test
	void primaryActionsFitAtMinimumScreenWidthInThreeLanguages() throws Exception {
		Locale previous = Locale.getDefault();
		try {
			for (Locale locale : List.of(Locale.ENGLISH, new Locale("es"), new Locale("ca"))) {
				AppLocal.setLocale(locale);
				SwingUtilities.invokeAndWait(() -> {
					// A third of the 1024px content area belongs to the list.
					for (JPanel editor : List.of(new PeopleView(new DataLogicAdmin(), new DirtyManager()),
							new RolesView(new DirtyManager()), new ResourcesView(new DirtyManager()))) {
						editor.setSize(650, 600);
						layoutRecursively(editor);
						assertThat(editor.getPreferredSize().width).as(editor.getClass().getSimpleName() + locale)
								.isLessThanOrEqualTo(650);
						assertThat(hasButton(editor,
								AppLocal.getIntString(editor instanceof PeopleView
										? "Admin.SaveUser"
										: editor instanceof RolesView ? "Admin.SaveRole" : "Admin.SaveResource")))
								.isTrue();
					}
				});
			}
		} finally {
			AppLocal.setLocale(previous);
		}
	}

	private static void layoutRecursively(Container container) {
		container.doLayout();
		for (Component child : container.getComponents()) {
			if (child instanceof Container nested)
				layoutRecursively(nested);
		}
	}

	private static boolean hasButton(Container container, String caption) {
		for (Component child : container.getComponents()) {
			if (child instanceof JButton button && caption.equals(button.getText()))
				return true;
			if (child instanceof Container nested && hasButton(nested, caption))
				return true;
		}
		return false;
	}
}
