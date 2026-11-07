package com.openbravo.pos.inventory;

import com.openbravo.pos.forms.AppLocal;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Supplier-facing text. Never include customer, note or seller data. */
final class ReplenishmentOrderText {
	private ReplenishmentOrderText() {
	}

	static String format(List<ReplenishmentEntry> entries) {
		StringBuilder text = new StringBuilder();
		String brand = null;
		String family = null;
		for (ReplenishmentEntry entry : entries) {
			String nextBrand = clean(entry.brand);
			String nextFamily = clean(entry.family);
			if (nextFamily.matches("(?i)[A-Z]*\\d+[A-Z\\d_-]*"))
				nextFamily = "";
			if (!Objects.equals(brand, nextBrand) || !Objects.equals(family, nextFamily)) {
				if (text.length() > 0)
					text.append('\n');
				if (!nextBrand.isEmpty() || !nextFamily.isEmpty())
					text.append(nextBrand).append(nextBrand.isEmpty() || nextFamily.isEmpty() ? "" : " · ")
							.append(nextFamily).append('\n');
				brand = nextBrand;
				family = nextFamily;
			}
			String reference = clean(entry.productId == null
					? entry.manualEan
					: ReplenishmentPanel.displayReference(entry.name, entry.reference));
			if (entry.productId == null && reference.isEmpty())
				reference = AppLocal.getIntString("Replenishment.Manual");
			String name = clean(entry.productId == null
					? entry.manualDescription
					: ReplenishmentPanel.displayProductName(entry.name, entry.reference));
			text.append(reference);
			if (!reference.isEmpty() && !name.isEmpty())
				text.append(" - ");
			text.append(name).append('\n');
		}
		return text.toString();
	}

	/** Keep catalogue order, but never include an entry that was not displayed. */
	static List<ReplenishmentEntry> scope(List<ReplenishmentEntry> ordered, List<ReplenishmentEntry> visible,
			Set<String> selectedIds, boolean onlySelected) {
		Set<String> visibleIds = visible.stream().map(entry -> entry.id).collect(Collectors.toSet());
		return ordered.stream()
				.filter(entry -> visibleIds.contains(entry.id) && (!onlySelected || selectedIds.contains(entry.id)))
				.toList();
	}

	static List<String> selectedPendingIds(List<ReplenishmentEntry> selection) {
		return selection.stream().filter(entry -> "PENDING".equals(entry.status)).map(entry -> entry.id).toList();
	}

	private static String clean(String value) {
		return value == null ? "" : value.replaceAll("[\\t\\r\\n]+", " ").trim();
	}
}
