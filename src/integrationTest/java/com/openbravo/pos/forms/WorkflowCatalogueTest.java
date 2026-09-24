package com.openbravo.pos.forms;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class WorkflowCatalogueTest {

	@Test
	public void containsEveryLiveWorkflowDestination() throws Exception {
		assertThat(JPanelWelcome.workflowTaskNames()).hasSize(27);
		for (String taskName : JPanelWelcome.workflowTaskNames()) {
			if (!taskName.startsWith("Menu.")) {
				assertThat(Class.forName(taskName)).as(taskName).isNotNull();
			}
		}
	}
}
