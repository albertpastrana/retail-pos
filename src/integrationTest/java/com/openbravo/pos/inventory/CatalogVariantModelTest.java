package com.openbravo.pos.inventory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.openbravo.pos.forms.DataLogicSales;
import com.openbravo.pos.ticket.ProductInfoExt;

public class CatalogVariantModelTest {

	private static final String PRODUCTS = "src/integrationTest/fixtures/catalog/import-products.tsv";
	private static final String CATEGORIES = "src/integrationTest/fixtures/catalog/import-categories.tsv";

	@Test
	public void groupsSupplierReferencesByModel() {
		assertEquals("3267", CatalogVariantModel.fromReference("3267-358-E", "Avetset"));
		assertEquals("P761237", CatalogVariantModel.fromReference("P761237-R64-3XL", "Massana"));
		assertEquals("26211064", CatalogVariantModel.fromReference("26211064-178-M", "Petrus"));
		assertEquals("26512", CatalogVariantModel.fromReference("26512-11-50", "Dusen"));
		assertEquals("262102", CatalogVariantModel.fromReference("262102-13-XL", "Señoretta"));
		assertEquals("262106", CatalogVariantModel.fromReference("262106-08-M", "Egatex"));
		assertEquals("262201", CatalogVariantModel.fromReference("262201-01-L", "Soy"));
		assertEquals("265652", CatalogVariantModel.fromReference("265652-000017-M", "Muslher"));
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

		List<ProductInfoExt> massana = sales.getCatalogProductFamily("9990000000101", PRODUCTS, CATEGORIES);
		assertEquals(4, massana.size());
		assertTrue(containsCode(massana, "9990000000104"));
		// P761238 is another Massana model, and the Gisela row splits to the same
		// P761237 model string under a different brand.
		assertFalse(containsCode(massana, "9990000000110"));
		assertFalse(containsCode(massana, "9990000000301"));

		List<ProductInfoExt> avet = sales.getCatalogProductFamily("9990000000201", PRODUCTS, CATEGORIES);
		assertEquals(3, avet.size());
		assertTrue(containsCode(avet, "9990000000203"));
		assertFalse(containsCode(avet, "9990000000210"));
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
