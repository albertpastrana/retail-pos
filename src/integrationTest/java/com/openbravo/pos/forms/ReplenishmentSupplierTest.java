package com.openbravo.pos.forms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.openbravo.basic.BasicException;
import com.openbravo.data.loader.Session;
import com.openbravo.pos.inventory.DataLogicReplenishment;
import com.openbravo.pos.inventory.ReplenishmentEntry;
import com.openbravo.pos.inventory.ReplenishmentSupplier;
import com.openbravo.pos.inventory.ReplenishmentSharing;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReplenishmentSupplierTest {
	@Test
	void suppliersSurviveMigrationAndAssignmentAndOnlyIncludedPendingRequestsAdvance(@TempDir Path temporary)
			throws Exception {
		String url = "jdbc:derby:memory:replenishmentSupplier" + UUID.randomUUID() + ";create=true";
		DatabaseMigrator.migrate(url, null, null);
		Session session = new Session(url, null, null);
		try {
			DataLogicReplenishment data = new DataLogicReplenishment();
			data.init(session);
			ReplenishmentSupplier supplier = new ReplenishmentSupplier();
			supplier.name = "A supplier";
			supplier.contactFirstName = "Ada";
			supplier.contactLastName = "Lovelace";
			supplier.whatsapp = "+34 600 000 000";
			supplier.email = "ada@example.com";
			data.saveSupplier(supplier);
			ReplenishmentEntry pending = request("Blue jumper", "Customer private");
			ReplenishmentEntry other = request("Red jumper", "Other customer");
			ReplenishmentEntry unassigned = request("Unassigned", null);
			data.add(pending);
			data.add(other);
			data.add(unassigned);
			data.assignSupplier(List.of(pending.id, other.id), supplier.id, "user");
			data.updateStatus(other.id, "ORDERED", "user");
			assertEquals(Set.of(other.id, pending.id), data.listForExport("", "ALL", "ALL", supplier.id).stream()
					.map(entry -> entry.id).collect(java.util.stream.Collectors.toSet()));
			assertEquals(List.of(unassigned.id),
					data.list("", "ALL", "ALL", "NONE").stream().map(entry -> entry.id).toList());
			assertThrows(BasicException.class,
					() -> data.assignSupplier(List.of(unassigned.id, other.id), supplier.id, "user"));
			assertNull(data.list("", "ALL", "ALL").stream().filter(e -> e.id.equals(unassigned.id)).findFirst()
					.orElseThrow().supplierId);
			List<ReplenishmentEntry> outgoing = data.listForExport("", "ALL", "ALL", supplier.id);
			ReplenishmentSharing.afterWhatsApp(data, outgoing, "user", false);
			assertEquals("PENDING", data.list("Blue jumper", "ALL", "ALL").get(0).status);
			assertThrows(java.io.IOException.class, () -> ReplenishmentSharing.saveTextFile(data, outgoing, "user",
					temporary.resolve("missing").resolve("order.txt"), "Order"));
			assertEquals("PENDING", data.list("Blue jumper", "ALL", "ALL").get(0).status);
			ReplenishmentSharing.saveTextFile(data, outgoing, "user", temporary.resolve("order.txt"), "Order text");
			assertEquals("Order text", Files.readString(temporary.resolve("order.txt")));
			assertEquals(Set.of(pending.id, other.id), data.list("", "ALL", "ORDERED", supplier.id).stream()
					.map(e -> e.id).collect(java.util.stream.Collectors.toSet()));
			assertEquals("PENDING", data.list("", "ALL", "ALL", "NONE").get(0).status);
			supplier.contactLastName = "Byron";
			data.saveSupplier(supplier);
			DatabaseMigrator.migrate(url, null, null);
			assertEquals("Byron", data.suppliers().get(0).contactLastName);
			assertEquals("ada@example.com", data.suppliers().get(0).email);
			assertEquals(2, data.list("", "ALL", "ALL", supplier.id).size());
			data.updateDetailsSupplierAndStatus(unassigned.id, "Check", null, null, supplier.id, "PENDING", "user");
			assertEquals("Check", data.list("", "ALL", "PENDING", supplier.id).get(0).note);
			data.deleteSupplier(supplier.id, "user");
			assertEquals(3, data.list("", "ALL", "ALL", "NONE").size());
			assertEquals(0, data.suppliers().size());
		} finally {
			session.close();
		}
	}

	private static ReplenishmentEntry request(String name, String customer) {
		ReplenishmentEntry entry = new ReplenishmentEntry();
		entry.manualDescription = name;
		entry.createdBy = "user";
		entry.customerId = customer == null ? null : UUID.randomUUID().toString();
		entry.customerName = customer;
		return entry;
	}
}
