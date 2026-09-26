package com.openbravo.pos.forms;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.Action;
import org.junit.jupiter.api.Test;

public class WorkflowCatalogueTest {

	@Test
	public void containsEveryLiveWorkflowDestination() throws Exception {
		assertThat(JPanelWelcome.workflowTaskNames()).hasSize(29).contains(
				"com.openbravo.pos.forms.MenuSalesManagement", "com.openbravo.pos.forms.MenuStockManagement",
				"com.openbravo.pos.reports.JPanelSalesSummary");
		for (String taskName : JPanelWelcome.workflowTaskNames()) {
			if (!taskName.startsWith("Menu.") && !"com.openbravo.pos.forms.MenuSalesManagement".equals(taskName)
					&& !"com.openbravo.pos.forms.MenuStockManagement".equals(taskName)) {
				assertThat(Class.forName(taskName)).as(taskName).isNotNull();
			}
		}
	}

	@Test
	public void homeMenuActionUsesAFullSizedHouseIcon() {
		HomeMenuIcon icon = new HomeMenuIcon();
		assertThat(icon.getIconWidth()).isEqualTo(24);
		assertThat(icon.getIconHeight()).isEqualTo(24);
		BufferedImage image = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		try {
			icon.paintIcon(null, graphics, 0, 0);
		} finally {
			graphics.dispose();
		}
		assertThat(image.getRGB(12, 3) >>> 24).isGreaterThan(0);
		assertThat(image.getRGB(5, 15) >>> 24).isGreaterThan(0);
		MenuPanelAction action = new MenuPanelAction(null, icon, "Menu.Home", "com.openbravo.pos.forms.JPanelWelcome",
				JPanelWelcome.class);
		assertThat(action.getValue(Action.SMALL_ICON)).isSameAs(icon);
	}
}
