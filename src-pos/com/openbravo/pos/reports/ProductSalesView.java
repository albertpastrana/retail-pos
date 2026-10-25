package com.openbravo.pos.reports;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

		Line(ProductSalesRow product, String title, double units, double amount, double share, boolean group,
				boolean total) {
			this.product = product;
			this.title = title;
			this.units = units;
			this.amount = amount;
			this.share = share;
			this.group = group;
			this.total = total;
		}
	}

	final List<Line> lines;
	final double units;
	final double amount;
	final double manualUnits;
	final double manualAmount;
	final long manualLines;
	final int productCount;

	private ProductSalesView(List<Line> lines, double units, double amount, double manualUnits, double manualAmount,
			long manualLines, int productCount) {
		this.lines = lines;
		this.units = units;
		this.amount = amount;
		this.manualUnits = manualUnits;
		this.manualAmount = manualAmount;
		this.manualLines = manualLines;
		this.productCount = productCount;
	}

	static ProductSalesView build(List<ProductSalesRow> rows, Map<String, ProductSalesRepository.Category> categories,
			String categoryId, String brand, String search, Grouping grouping, Map<String, Boolean> collapsed,
			String uncategorized, String unbranded, String manual, String total) {
		String query = search.trim().toLowerCase(Locale.ROOT);
		List<ProductSalesRow> matching = new ArrayList<>();
		double units = 0, amount = 0, manualUnits = 0, manualAmount = 0;
		int count = 0;
		long manualLines = 0;
		for (ProductSalesRow row : rows) {
			String path = ProductSalesRepository.categoryPath(row.getCategoryId(), categories);
			if (categoryId != null && !ProductSalesRepository.isDescendant(row.getCategoryId(), categoryId, categories)
					|| brand != null && !brand.equals(row.getBrand())
					|| !query.isEmpty()
							&& !(row.getReference() + " " + row.getProductName() + " " + path + " " + row.getBrand()
									+ " " + (row.isManual() ? manual : "")).toLowerCase(Locale.ROOT).contains(query)) {
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
		} else {
			Map<String, List<ProductSalesRow>> groups = new LinkedHashMap<>();
			for (ProductSalesRow row : matching) {
				if (row.isManual()) {
					continue;
				}
				String key = grouping == Grouping.CATEGORY
						? ProductSalesRepository.categoryPath(row.getCategoryId(), categories)
						: row.getBrand();
				if (key.isEmpty()) {
					key = grouping == Grouping.CATEGORY ? uncategorized : unbranded;
				}
				groups.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
			}
			List<String> keys = new ArrayList<>(groups.keySet());
			Collections.sort(keys,
					Comparator.comparingDouble(
							(String key) -> groups.get(key).stream().mapToDouble(ProductSalesRow::getAmount).sum())
							.reversed());
			for (String key : keys) {
				List<ProductSalesRow> members = groups.get(key);
				double groupUnits = members.stream().mapToDouble(ProductSalesRow::getUnits).sum();
				double groupAmount = members.stream().mapToDouble(ProductSalesRow::getAmount).sum();
				lines.add(new Line(null, key, groupUnits, groupAmount, amount == 0 ? 0 : groupAmount / amount * 100,
						true, false));
				if (!collapsed.getOrDefault(key, false)) {
					for (ProductSalesRow row : members) {
						lines.add(productLine(row));
					}
				}
			}
			if (matching.stream().anyMatch(ProductSalesRow::isManual)) {
				lines.add(new Line(null, manual, manualUnits, manualAmount,
						amount == 0 ? 0 : manualAmount / amount * 100, true, false));
				if (!collapsed.getOrDefault(manual, false)) {
					for (ProductSalesRow row : matching) {
						if (row.isManual()) {
							lines.add(productLine(row));
						}
					}
				}
			}
		}
		lines.add(new Line(null, total, units, amount, 0, false, true));
		return new ProductSalesView(lines, units, amount, manualUnits, manualAmount, manualLines, count);
	}

	private static Line productLine(ProductSalesRow row) {
		return new Line(row, "", row.getUnits(), row.getAmount(), 0, false, false);
	}
}
