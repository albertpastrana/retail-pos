package com.openbravo.pos.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.openbravo.data.loader.Session;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReplenishmentExportOrderTest {
	@Test
	void exportSortsFilteredEntriesByCatalogueBrandAndFamilyWithoutChangingListOrder() throws Exception {
		Session session = new Session("jdbc:derby:memory:replenishmentExport" + UUID.randomUUID() + ";create=true",
				null, null);
		try (Statement sql = session.getConnection().createStatement()) {
			sql.execute("CREATE TABLE PEOPLE (ID VARCHAR(40), NAME VARCHAR(80))");
			sql.execute("CREATE TABLE CUSTOMERS (ID VARCHAR(40), PHONE VARCHAR(40))");
			sql.execute("CREATE TABLE PRODUCTS (ID VARCHAR(40), BRAND VARCHAR(80), FAMILY VARCHAR(80))");
			sql.execute("CREATE TABLE REPLENISHMENT_ENTRIES (ID VARCHAR(40), PRODUCT_ID VARCHAR(40), "
					+ "PRODUCT_REFERENCE VARCHAR(40), PRODUCT_NAME VARCHAR(80), PRODUCT_EAN VARCHAR(40), "
					+ "VARIANT_SIZE VARCHAR(40), VARIANT_COLOUR VARCHAR(40), MANUAL_DESCRIPTION VARCHAR(80), "
					+ "MANUAL_EAN VARCHAR(40), NOTE VARCHAR(80), CUSTOMER_ID VARCHAR(40), CUSTOMER_NAME VARCHAR(80), "
					+ "STATUS VARCHAR(20), CREATED_AT TIMESTAMP, UPDATED_AT TIMESTAMP, CREATED_BY VARCHAR(40), UPDATED_BY VARCHAR(40), SUPPLIER_ID VARCHAR(40))");
			sql.execute("INSERT INTO PRODUCTS VALUES ('alpha2', 'Alpha', 'Alpha|Two'), "
					+ "('beta', 'Beta', 'Beta|One'), ('alpha1', 'Alpha', 'Alpha|One'), "
					+ "('alpha1b', 'Alpha', 'Alpha|One'), ('noFamily', 'Alpha', NULL), ('noBrand', NULL, NULL)");
			sql.execute("INSERT INTO REPLENISHMENT_ENTRIES "
					+ "(ID, PRODUCT_ID, PRODUCT_NAME, CUSTOMER_ID, CUSTOMER_NAME, STATUS, CREATED_AT) VALUES "
					+ "('manual', NULL, 'Manual', 'c', 'Ana', 'PENDING', '2026-10-24 12:10:00'), "
					+ "('noBrand', 'noBrand', 'No brand', 'c', 'Ana', 'PENDING', '2026-10-24 12:09:00'), "
					+ "('alpha2', 'alpha2', 'Alpha Two', 'c', 'Ana', 'PENDING', '2026-10-24 12:08:00'), "
					+ "('alpha1', 'alpha1', 'Alpha One', 'c', 'Ana', 'PENDING', '2026-10-24 12:07:00'), "
					+ "('alpha1b', 'alpha1b', 'Alpha One', 'c', 'Ana', 'PENDING', '2026-10-24 12:07:00'), "
					+ "('noFamily', 'noFamily', 'No family', 'c', 'Ana', 'PENDING', '2026-10-24 12:06:00'), "
					+ "('beta', 'beta', 'Beta One', 'c', 'Ana', 'ORDERED', '2026-10-24 12:05:00'), "
					+ "('ordinary', 'beta', 'Not a customer order', NULL, NULL, 'PENDING', '2026-10-24 12:11:00'), "
					+ "('received', 'beta', 'Received', 'c', 'Ana', 'RECEIVED', '2026-10-24 12:12:00')");
			DataLogicReplenishment data = new DataLogicReplenishment();
			data.init(session);
			assertEquals("manual", data.list("", "ENCARGO", "OPEN").get(0).id);
			assertEquals(List.of("alpha1", "alpha1b", "alpha2", "noFamily", "beta", "manual", "noBrand"),
					data.listForExport("", "ENCARGO", "OPEN").stream().map(e -> e.id).toList());
			assertEquals(List.of("alpha1", "alpha1b"),
					data.listForExport("Alpha One", "ENCARGO", "PENDING").stream().map(e -> e.id).toList());
			String text = ReplenishmentOrderText.format(data.listForExport("", "ENCARGO", "OPEN"));
			assertTrue(text.indexOf("Alpha · Alpha|One") < text.indexOf("Alpha · Alpha|Two"));
			assertTrue(text.indexOf("Alpha · Alpha|Two") < text.indexOf("Beta · Beta|One"));
			assertFalse(text.contains("Ana"));
			assertFalse(text.contains("Customer"));
			sql.executeUpdate("UPDATE REPLENISHMENT_ENTRIES SET SUPPLIER_ID='supplier' "
					+ "WHERE ID IN ('beta', 'alpha2', 'alpha1')");
			assertEquals(List.of("alpha1", "alpha2", "beta"),
					data.list("", "ALL", "ALL", "supplier").stream().map(e -> e.id).toList());
			assertEquals("received", data.list("", "ALL", "ALL", "NONE").get(0).id);
		} finally {
			session.close();
		}
	}

	@Test
	void outgoingTextContainsReferenceAndNameButNeverCustomerDetails() {
		ReplenishmentEntry entry = new ReplenishmentEntry();
		entry.brand = "Brand";
		entry.family = "Family";
		entry.productId = "product";
		entry.reference = "REF-1";
		entry.name = "Jumper";
		entry.customerName = "Private customer";
		entry.customerPhone = "Private phone";
		entry.note = "Private note";
		assertEquals("Brand · Family\nREF-1 - Jumper\n", ReplenishmentOrderText.format(List.of(entry)));
		assertFalse(ReplenishmentOrderText.format(List.of(entry)).contains("Private phone"));
		entry.id = "one";
		ReplenishmentEntry other = new ReplenishmentEntry();
		other.id = "two";
		other.productId = "other";
		other.name = "Second";
		ReplenishmentEntry notDisplayed = new ReplenishmentEntry();
		notDisplayed.id = "three";
		assertEquals(List.of(entry, other), ReplenishmentOrderText.scope(List.of(entry, other, notDisplayed),
				List.of(entry, other), Set.of(entry.id), false));
		assertEquals(List.of(entry), ReplenishmentOrderText.scope(List.of(entry, other, notDisplayed),
				List.of(entry, other), Set.of(entry.id), true));
		entry.status = "PENDING";
		other.status = "ORDERED";
		assertEquals(List.of(entry.id), ReplenishmentOrderText.selectedPendingIds(List.of(entry, other)));
		ReplenishmentEntry manual = new ReplenishmentEntry();
		manual.manualDescription = "Item 2";
		assertEquals("Manual - Item 2\n", ReplenishmentOrderText.format(List.of(manual)));
	}
	@Test
	void catalogueSnapshotsDoNotRepeatReferencesOrExposeFamilyCodes() {
		ReplenishmentEntry entry = new ReplenishmentEntry();
		entry.productId = "catalogue";
		entry.brand = "Cotó";
		entry.family = "A0256P1";
		entry.reference = "A0256P1-001 M/48";
		entry.name = "Samarreta — M/48 [A0256P1-001 M/48]";
		assertEquals("Cotó\nA0256P1-001 - Samarreta — M/48\n", ReplenishmentOrderText.format(List.of(entry)));
	}
}
