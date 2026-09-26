package com.openbravo.pos.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.openbravo.data.user.DirtyManager;
import org.junit.jupiter.api.Test;

class ResourceOriginalTest {
	@Test
	void onlyShippedResourcesCanBeMarkedAndRestored() {
		byte[] shipped = ResourceOriginal.bytes("Printer.Ticket", 0);
		assertThat(shipped).isNotEmpty();
		assertThat(ResourceOriginal.differs(new Object[]{"1", "Printer.Ticket", 0, shipped})).isFalse();
		assertThat(ResourceOriginal.differs(new Object[]{"1", "Printer.Ticket", 0, "changed".getBytes()})).isTrue();
		assertThat(ResourceOriginal.bytes("Custom.Template", 0)).isNull();
		assertThat(ResourceOriginal.differs(new Object[]{"2", "Custom.Template", 0, "changed".getBytes()})).isFalse();
	}

	@Test
	void openingAnImageAndSavingWithoutEditingPreservesItsOriginalBytes() throws Exception {
		byte[] shipped = ResourceOriginal.bytes("Printer.Ticket.Logo", 1);
		assertThat(shipped).isNotEmpty();
		ResourcesView editor = new ResourcesView(new DirtyManager());
		editor.writeValueEdit(new Object[]{"1", "Printer.Ticket.Logo", 1, shipped});
		assertThat((byte[]) ((Object[]) editor.createValue())[3]).isEqualTo(shipped);
	}
}
