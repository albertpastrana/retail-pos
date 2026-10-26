package com.openbravo.pos.reports;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregates the below-the-fold sales blocks for the reports landing screen.
 */
public final class WelcomeSalesRepository {

	private final ProductSalesRepository products = new ProductSalesRepository();
	private final PaymentSalesRepository payments = new PaymentSalesRepository();

	public WelcomeSalesData load(Connection connection, SalesSummaryParameters current, SalesSummaryParameters previous)
			throws SQLException {
		Map<String, ProductSalesRepository.Category> categories = products.categories(connection);
		List<ProductSalesRow> currentProducts = products.load(connection, current);
		List<ProductSalesRow> previousProducts = products.load(connection, previous);
		Map<String, Double> previousByProduct = new HashMap<>();
		for (ProductSalesRow row : previousProducts) {
			if (row.getProductId() != null) {
				previousByProduct.put(row.getProductId(), row.getAmount());
			}
		}
		List<ProductInsight> topProducts = new ArrayList<>();
		for (ProductSalesRow row : currentProducts) {
			if (!row.isManual() && row.getProductId() != null) {
				topProducts.add(new ProductInsight(row, previousByProduct.get(row.getProductId()),
						ProductSalesRepository.categoryPath(row.getCategoryId(), categories)));
			}
		}
		topProducts.sort(Comparator.comparingDouble((ProductInsight row) -> row.row.getUnits()).reversed()
				.thenComparing(row -> row.row.getProductName()));
		if (topProducts.size() > 6) {
			topProducts = new ArrayList<>(topProducts.subList(0, 6));
		}
		return new WelcomeSalesData(topProducts, categorySales(currentProducts, previousProducts, categories),
				paymentSales(payments.load(connection, current), payments.load(connection, previous)));
	}

	private List<CategoryInsight> categorySales(List<ProductSalesRow> current, List<ProductSalesRow> previous,
			Map<String, ProductSalesRepository.Category> categories) {
		Map<String, CategoryTotals> roots = new HashMap<>();
		Map<String, CategoryTotals> children = new HashMap<>();
		addCategories(roots, children, current, categories, true);
		addCategories(roots, children, previous, categories, false);
		List<CategoryInsight> result = new ArrayList<>();
		for (Map.Entry<String, CategoryTotals> entry : roots.entrySet()) {
			ProductSalesRepository.Category root = categories.get(entry.getKey());
			CategoryTotals value = entry.getValue();
			List<CategoryInsight> direct = new ArrayList<>();
			for (Map.Entry<String, CategoryTotals> child : children.entrySet()) {
				ProductSalesRepository.Category category = categories.get(child.getKey());
				if (category != null && entry.getKey().equals(category.parentId)) {
					direct.add(new CategoryInsight(category.name, child.getValue().current, child.getValue().previous,
							Collections.emptyList(), child.getValue().hasSalesInBothPeriods()));
				}
			}
			direct.sort(Comparator.comparingDouble(CategoryInsight::getAmount).reversed());
			result.add(new CategoryInsight(root.name, value.current, value.previous, direct,
					value.hasSalesInBothPeriods()));
		}
		result.sort(Comparator.comparingDouble(CategoryInsight::getAmount).reversed());
		return result;
	}

	private void addCategories(Map<String, CategoryTotals> roots, Map<String, CategoryTotals> children,
			List<ProductSalesRow> rows, Map<String, ProductSalesRepository.Category> categories, boolean current) {
		for (ProductSalesRow row : rows) {
			String categoryId = row.getCategoryId();
			if (categoryId == null || !categories.containsKey(categoryId)) {
				continue;
			}
			String rootId = categoryId;
			java.util.Set<String> seen = new java.util.HashSet<>();
			while (categories.get(rootId).parentId != null && seen.add(rootId)) {
				rootId = categories.get(rootId).parentId;
			}
			CategoryTotals root = roots.computeIfAbsent(rootId, ignored -> new CategoryTotals());
			root.add(row.getAmount(), current);
			if (!rootId.equals(categoryId)) {
				CategoryTotals child = children.computeIfAbsent(categoryId, ignored -> new CategoryTotals());
				child.add(row.getAmount(), current);
			}
		}
	}

	private List<PaymentInsight> paymentSales(List<PaymentSalesRow> current, List<PaymentSalesRow> previous) {
		Map<String, Double> values = paymentTotals(current);
		Map<String, Double> oldValues = paymentTotals(previous);
		List<PaymentInsight> result = new ArrayList<>();
		for (String key : new String[]{PaymentSalesRepository.CARD, PaymentSalesRepository.CASH,
				PaymentSalesRepository.OTHER}) {
			result.add(new PaymentInsight(key, values.getOrDefault(key, 0.0), oldValues.getOrDefault(key, 0.0)));
		}
		return result;
	}

	private Map<String, Double> paymentTotals(List<PaymentSalesRow> rows) {
		Map<String, Double> totals = new HashMap<>();
		for (PaymentSalesRow row : rows) {
			String key = PaymentSalesRepository.classify(row.getPaymentType());
			totals.put(key, totals.getOrDefault(key, 0.0) + row.getNet());
		}
		return totals;
	}

	private static final class CategoryTotals {
		double current;
		double previous;
		boolean hasCurrentSales;
		boolean hasPreviousSales;

		void add(double amount, boolean isCurrent) {
			if (isCurrent) {
				current += amount;
				hasCurrentSales |= amount != 0;
			} else {
				previous += amount;
				hasPreviousSales |= amount != 0;
			}
		}

		boolean hasSalesInBothPeriods() {
			return hasCurrentSales && hasPreviousSales;
		}
	}

	public static final class ProductInsight {
		private final ProductSalesRow row;
		private final Double previous;
		private final String categoryPath;

		ProductInsight(ProductSalesRow row, Double previous, String categoryPath) {
			this.row = row;
			this.previous = previous;
			this.categoryPath = categoryPath;
		}

		public ProductSalesRow getRow() {
			return row;
		}
		public Double getPrevious() {
			return previous;
		}
		public String getCategoryPath() {
			return categoryPath;
		}
	}

	public static final class CategoryInsight {
		private final String name;
		private final double amount;
		private final double previous;
		private final List<CategoryInsight> children;
		private final boolean comparable;

		CategoryInsight(String name, double amount, double previous, List<CategoryInsight> children) {
			this(name, amount, previous, children, amount != 0 && previous != 0);
		}

		CategoryInsight(String name, double amount, double previous, List<CategoryInsight> children,
				boolean comparable) {
			this.name = name;
			this.amount = amount;
			this.previous = previous;
			this.children = children;
			this.comparable = comparable;
		}
		public String getName() {
			return name;
		}
		public double getAmount() {
			return amount;
		}
		public double getPrevious() {
			return previous;
		}
		public List<CategoryInsight> getChildren() {
			return children;
		}
		public boolean hasComparablePeriods() {
			return comparable;
		}
	}

	public static final class PaymentInsight {
		private final String category;
		private final double amount;
		private final double previous;

		PaymentInsight(String category, double amount, double previous) {
			this.category = category;
			this.amount = amount;
			this.previous = previous;
		}
		public String getCategory() {
			return category;
		}
		public double getAmount() {
			return amount;
		}
		public double getPrevious() {
			return previous;
		}
	}

	public static final class WelcomeSalesData {
		private final List<ProductInsight> products;
		private final List<CategoryInsight> categories;
		private final List<PaymentInsight> payments;

		WelcomeSalesData(List<ProductInsight> products, List<CategoryInsight> categories,
				List<PaymentInsight> payments) {
			this.products = products;
			this.categories = categories;
			this.payments = payments;
		}
		public List<ProductInsight> getProducts() {
			return products;
		}
		public List<CategoryInsight> getCategories() {
			return categories;
		}
		public List<PaymentInsight> getPayments() {
			return payments;
		}
	}
}
