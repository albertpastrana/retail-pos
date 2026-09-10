package com.openbravo.pos.inventory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.openbravo.pos.forms.DataLogicSales;
import com.openbravo.pos.ticket.ProductInfoExt;

public class CatalogVariantModelTest {

	@Test
	public void groupsSupplierReferencesByModel() {
		assertEquals("3267", CatalogVariantModel.fromReference("3267-358-E", "Avetset"));
		assertEquals("P761237", CatalogVariantModel.fromReference("P761237-R64-3XL", "Massana"));
		assertEquals("1/10011T", CatalogVariantModel.fromReference("1/10011T NGR XL", "Gisela"));
		assertEquals("A5398E2", CatalogVariantModel.fromReference("A5398E2-1SE XXL/60", "Abanderado"));
		assertEquals("P4716P1", CatalogVariantModel.fromReference("P4716P1-001 L", "Playtex"));
		assertEquals("12733", CatalogVariantModel.fromReference("12733UNGR", "Ysabel Mora"));
	}

	@Test
	public void leavesUnknownAndSeleneReferencesAlone() {
		assertEquals("06032", CatalogVariantModel.fromReference("06032", "Selene"));
		assertEquals("ABC", CatalogVariantModel.fromReference("ABC", "Other"));
	}

	@Test
	public void findsEveryVariantFromOneScannedBarcode() throws Exception {
		DataLogicSales sales = new DataLogicSales();
		List<ProductInfoExt> massana = sales.getCatalogProductFamily("8433790116705", "data/import-products.tsv",
				"data/import-categories.tsv");
		assertEquals(12, massana.size());
		assertTrue(containsCode(massana, "8433790120214"));

		List<ProductInfoExt> avet = sales.getCatalogProductFamily("8413092436428", "data/import-products.tsv",
				"data/import-categories.tsv");
		assertEquals(8, avet.size());
		assertTrue(containsCode(avet, "8413092437029"));
	}

	private boolean containsCode(List<ProductInfoExt> products, String code) {
		for (ProductInfoExt product : products) {
			if (code.equals(product.getCode())) {
				return true;
			}
		}
		return false;
	}
}
