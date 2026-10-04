package com.openbravo.pos.ticket;

import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import javax.swing.JComboBox;

import org.junit.jupiter.api.Test;

import com.openbravo.data.loader.DataWriteUtils;
import com.openbravo.data.loader.QBFBuilder;
import com.openbravo.data.loader.QBFCompareEnum;
import com.openbravo.data.loader.SerializerWriteBasic;
import com.openbravo.data.loader.Datas;

import static org.assertj.core.api.Assertions.assertThat;

class ProductFilterTest {

	@Test
	void categoryFilterIncludesAllNestedDescendants() {
		CategoryInfo root = category("pijames", null);
		CategoryInfo child = category("hivern", "pijames");
		CategoryInfo grandchild = category("classics", "hivern");
		CategoryInfo sibling = category("samarretes", null);

		String selected = ProductFilter.categoryIdsIncludingDescendants(Arrays.asList(root, child, grandchild, sibling),
				"pijames");

		assertThat(new HashSet<String>(Arrays.asList(selected.split(","))))
				.isEqualTo(new HashSet<String>(Arrays.asList("pijames", "hivern", "classics")));
	}

	@Test
	void categoryFilterKeepsLeafCategoriesNarrow() {
		String selected = ProductFilter.categoryIdsIncludingDescendants(
				Arrays.asList(category("pijames", null), category("hivern", "pijames")), "hivern");

		assertThat(selected).isEqualTo("hivern");
	}

	@Test
	void categoryIdsAreRenderedAsSqlValues() {
		assertThat(QBFCompareEnum.COMP_IN.getExpression("P.CATEGORY", DataWriteUtils.getSQLValue("pijames,hivern")))
				.isEqualTo("P.CATEGORY IN ('pijames', 'hivern')");
	}

	@Test
	void productListCategoryFilterUsesProductCategoryId() throws Exception {
		QBFBuilder builder = new QBFBuilder("SELECT * FROM PRODUCTS P WHERE ?(QBF_FILTER)",
				new String[]{"P.NAME", "P.CATEGORY", "P.BRAND", "P.FAMILY"});
		Object[] filter = new Object[]{QBFCompareEnum.COMP_NONE, null, QBFCompareEnum.COMP_IN, "pijames,hivern",
				QBFCompareEnum.COMP_NONE, null, QBFCompareEnum.COMP_NONE, null};

		String sql = builder.getSQL(new SerializerWriteBasic(new Datas[]{Datas.OBJECT, Datas.STRING, Datas.OBJECT,
				Datas.STRING, Datas.OBJECT, Datas.STRING, Datas.OBJECT, Datas.STRING}), filter);

		assertThat(sql).contains("P.CATEGORY IN ('pijames', 'hivern')");
	}

	@Test
	void containsFilterRequiresEverySearchTerm() {
		String sql = QBFCompareEnum.COMP_CONTAINS.getExpression("P.NAME", DataWriteUtils.getSQLValue("suje blan"));

		assertThat(sql).isEqualTo("UPPER(P.NAME) LIKE UPPER('%suje%') AND UPPER(P.NAME) LIKE UPPER('%blan%')");
	}

	@Test
	void salesProductFinderUsesDescendantsForSelectedCategory() throws Exception {
		ProductFilterSales filter = new ProductFilterSales();
		List<CategoryInfo> categories = Arrays.asList(category("pijames", null), category("hivern", "pijames"));
		filter.setCategories(categories);
		JComboBox<?> category = (JComboBox<?>) field(filter, "m_jCategory").get(filter);
		category.setSelectedIndex(1);

		Object[] value = (Object[]) filter.createValue();

		assertThat(new HashSet<String>(Arrays.asList(value[3].toString().split(","))))
				.isEqualTo(new HashSet<String>(Arrays.asList("pijames", "hivern")));
	}

	private static Field field(Object target, String name) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field;
	}

	private static CategoryInfo category(String id, String parent) {
		return new CategoryInfo(id, id, parent, (BufferedImage) null);
	}
}
