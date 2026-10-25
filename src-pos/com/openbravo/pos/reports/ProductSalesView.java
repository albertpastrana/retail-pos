package com.openbravo.pos.reports;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Filters and aggregates the loaded period without further database calls. */
final class ProductSalesView {
	enum Grouping {
		NONE, CATEGORY, BRAND
	}

	static final class Line {
		final ProductSalesRow product;
		final String title;
		final double units;
		final double amount;
		final double share;
		final boolean group;
		final boolean total;
		final int productCount;
		final String groupKey;
		final int depth;

		Line(ProductSalesRow product, String title, double units, double amount, double share, boolean group,
				boolean total, int productCount) {
			this(product, title, units, amount, share, group, total, productCount, title, 0);
		}

		Line(ProductSalesRow product, String title, double units, double amount, double share, boolean group,
				boolean total, int productCount, String groupKey, int depth) {
			this.product = product;
			this.title = title;
			this.units = units;
			this.amount = amount;
			this.share = share;
			this.group = group;
			this.total = total;
			this.productCount = productCount;
			this.groupKey = groupKey;
			this.depth = depth;
		}
	}

	private static final class CategoryNode {
		final String id;
		final String path;
		final int depth;
		final List<CategoryNode> children = new ArrayList<>();
		final List<ProductSalesRow> directProducts = new ArrayList<>();
		double units;
		double amount;
		int productCount;

		CategoryNode(String id, String path, int depth) {
			this.id = id;
			this.path = path;
			this.depth = depth;
		}

		String key() {
			return "category:id:" + id;
		}
	}

	final List<Line> lines;
	final double units;
	final double amount;
	final double manualUnits;
	final double manualAmount;
	final long manualLines;
	final int productCount;
	final List<String> groupKeys;

	private ProductSalesView(List<Line> lines, double units, double amount, double manualUnits, double manualAmount,
			long manualLines, int productCount, List<String> groupKeys) {
		this.lines = lines;
		this.units = units;
		this.amount = amount;
		this.manualUnits = manualUnits;
		this.manualAmount = manualAmount;
		this.manualLines = manualLines;
		this.productCount = productCount;
		this.groupKeys = groupKeys;
	}

	static ProductSalesView build(List<ProductSalesRow> rows, Map<String, ProductSalesRepository.Category> categories,
			String search, Grouping grouping, Map<String, Boolean> collapsed, String uncategorized, String unbranded,
			String manual, String total) {
		String query = search.trim().toLowerCase(Locale.ROOT);
		List<ProductSalesRow> matching = new ArrayList<>();
		double units = 0, amount = 0, manualUnits = 0, manualAmount = 0;
		int count = 0;
		long manualLines = 0;
		for (ProductSalesRow row : rows) {
			String path = ProductSalesRepository.categoryPath(row.getCategoryId(), categories);
			if (!query.isEmpty() && !(row.getReference() + " " + row.getProductName() + " " + path + " "
					+ row.getBrand() + " " + (row.isManual() ? manual : "")).toLowerCase(Locale.ROOT).contains(query)) {
				continue;
			}
			matching.add(row);
			units += row.getUnits();
			amount += row.getAmount();
			if (row.isManual()) {
				manualLines += row.getLineCount();
				manualUnits += row.getUnits();
				manualAmount += row.getAmount();
			} else {
				count++;
			}
		}
		List<Line> lines = new ArrayList<>();
		List<String> groupKeys = new ArrayList<>();
		if (grouping == Grouping.NONE) {
			for (ProductSalesRow row : matching) {
				if (!row.isManual()) {
					lines.add(productLine(row));
				}
			}
			for (ProductSalesRow row : matching) {
				if (row.isManual()) {
					lines.add(productLine(row));
				}
			}
		} else if (grouping == Grouping.CATEGORY) {
			appendCategories(matching, categories, collapsed, uncategorized, amount, lines, groupKeys);
		} else {
			Map<String, List<ProductSalesRow>> groups = new LinkedHashMap<>();
			for (ProductSalesRow row : matching) {
				if (row.isManual()) {
					continue;
				}
				String key = row.getBrand();
				if (key.isEmpty()) {
					key = unbranded;
				}
				groups.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
			}
			List<String> keys = new ArrayList<>(groups.keySet());
			keys.sort(Comparator
					.comparingDouble(
							(String key) -> groups.get(key).stream().mapToDouble(ProductSalesRow::getAmount).sum())
					.reversed());
			for (String key : keys) {
				groupKeys.add(key);
				List<ProductSalesRow> members = groups.get(key);
				double groupUnits = members.stream().mapToDouble(ProductSalesRow::getUnits).sum();
				double groupAmount = members.stream().mapToDouble(ProductSalesRow::getAmount).sum();
				lines.add(new Line(null, key, groupUnits, groupAmount, amount == 0 ? 0 : groupAmount / amount * 100,
						true, false, members.size()));
				if (!collapsed.getOrDefault(key, true)) {
					for (ProductSalesRow row : members) {
						lines.add(productLine(row));
					}
				}
			}
		}
		if (grouping != Grouping.NONE && manualLines > 0) {
			String manualKey = "manual:lines";
			groupKeys.add(manualKey);
			lines.add(new Line(null, manual, manualUnits, manualAmount, amount == 0 ? 0 : manualAmount / amount * 100,
					true, false, 0, manualKey, 0));
			if (!collapsed.getOrDefault(manualKey, true)) {
				for (ProductSalesRow row : matching) {
					if (row.isManual()) {
						lines.add(productLine(row));
					}
				}
			}
		}
		lines.add(new Line(null, total, units, amount, 0, false, true, count));
		return new ProductSalesView(lines, units, amount, manualUnits, manualAmount, manualLines, count, groupKeys);
	}

	private static void appendCategories(List<ProductSalesRow> matching,
			Map<String, ProductSalesRepository.Category> categories, Map<String, Boolean> collapsed,
			String uncategorized, double total, List<Line> lines, List<String> groupKeys) {
		Map<String, CategoryNode> nodes = new HashMap<>();
		List<CategoryNode> roots = new ArrayList<>();
		List<ProductSalesRow> unassigned = new ArrayList<>();
		for (ProductSalesRow row : matching) {
			if (row.isManual()) {
				continue;
			}
			List<ProductSalesRepository.Category> ancestors = new ArrayList<>();
			Set<String> visited = new HashSet<>();
			String id = row.getCategoryId();
			while (id != null && visited.add(id) && categories.containsKey(id)) {
				ProductSalesRepository.Category category = categories.get(id);
				ancestors.add(category);
				id = category.parentId;
			}
			if (ancestors.isEmpty()) {
				unassigned.add(row);
				continue;
			}
			CategoryNode parent = null;
			for (int i = ancestors.size() - 1; i >= 0; i--) {
				ProductSalesRepository.Category category = ancestors.get(i);
				CategoryNode node = nodes.get(category.id);
				if (node == null) {
					node = new CategoryNode(category.id,
							parent == null ? category.name : parent.path + " › " + category.name,
							parent == null ? 0 : parent.depth + 1);
					nodes.put(category.id, node);
					groupKeys.add(node.key());
					if (parent == null) {
						roots.add(node);
					} else {
						parent.children.add(node);
					}
				}
				node.units += row.getUnits();
				node.amount += row.getAmount();
				node.productCount++;
				parent = node;
			}
			parent.directProducts.add(row);
		}
		roots.sort(Comparator.comparingDouble((CategoryNode node) -> node.amount).reversed()
				.thenComparing(node -> node.path, String.CASE_INSENSITIVE_ORDER));
		for (CategoryNode root : roots) {
			appendCategory(root, collapsed, total, lines);
		}
		if (!unassigned.isEmpty()) {
			String key = "category:unassigned";
			groupKeys.add(key);
			unassigned.sort(amountOrder());
			double units = unassigned.stream().mapToDouble(ProductSalesRow::getUnits).sum();
			double amount = unassigned.stream().mapToDouble(ProductSalesRow::getAmount).sum();
			lines.add(new Line(null, uncategorized, units, amount, total == 0 ? 0 : amount / total * 100, true, false,
					unassigned.size(), key, 0));
			if (!collapsed.getOrDefault(key, true)) {
				for (ProductSalesRow row : unassigned) {
					lines.add(productLine(row, 1));
				}
			}
		}
	}

	private static void appendCategory(CategoryNode node, Map<String, Boolean> collapsed, double total,
			List<Line> lines) {
		lines.add(new Line(null, node.path, node.units, node.amount, total == 0 ? 0 : node.amount / total * 100, true,
				false, node.productCount, node.key(), node.depth));
		if (collapsed.getOrDefault(node.key(), true)) {
			return;
		}
		node.directProducts.sort(amountOrder());
		node.children.sort(Comparator.comparingDouble((CategoryNode child) -> child.amount).reversed()
				.thenComparing(child -> child.path, String.CASE_INSENSITIVE_ORDER));
		int productIndex = 0;
		int childIndex = 0;
		while (productIndex < node.directProducts.size() || childIndex < node.children.size()) {
			if (childIndex < node.children.size() && (productIndex == node.directProducts.size()
					|| node.children.get(childIndex).amount >= node.directProducts.get(productIndex).getAmount())) {
				appendCategory(node.children.get(childIndex++), collapsed, total, lines);
			} else {
				lines.add(productLine(node.directProducts.get(productIndex++), node.depth + 1));
			}
		}
	}

	private static Line productLine(ProductSalesRow row) {
		return productLine(row, 0);
	}

	private static Line productLine(ProductSalesRow row, int depth) {
		return new Line(row, "", row.getUnits(), row.getAmount(), 0, false, false, 1, "", depth);
	}

	static Comparator<ProductSalesRow> productNameOrder() {
		return Comparator.comparing(ProductSalesView::displayName, String.CASE_INSENSITIVE_ORDER)
				.thenComparing(ProductSalesRow::getReference, String.CASE_INSENSITIVE_ORDER);
	}

	static Comparator<ProductSalesRow> amountOrder() {
		return Comparator.comparingDouble(ProductSalesRow::getAmount).reversed().thenComparing(productNameOrder());
	}

	static Comparator<ProductSalesRow> categoryProductOrder(Map<String, ProductSalesRepository.Category> categories) {
		return Comparator.comparing(
				(ProductSalesRow row) -> ProductSalesRepository.categoryPath(row.getCategoryId(), categories).isEmpty())
				.thenComparing(
						(ProductSalesRow row) -> ProductSalesRepository.categoryPath(row.getCategoryId(), categories),
						String.CASE_INSENSITIVE_ORDER)
				.thenComparing(productNameOrder());
	}

	static String displayName(ProductSalesRow row) {
		String name = row.getProductName();
		int opening = name.lastIndexOf(" [");
		if (opening < 0 || !name.endsWith("]")) {
			return name;
		}
		String suffix = name.substring(opening + 2, name.length() - 1);
		String reference = row.getReference();
		return !suffix.isEmpty() && (reference.equals(suffix) || reference.startsWith(suffix + " "))
				? name.substring(0, opening)
				: name;
	}
}
