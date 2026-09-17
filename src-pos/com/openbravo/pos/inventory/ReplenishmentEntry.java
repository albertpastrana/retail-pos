package com.openbravo.pos.inventory;

import java.util.Date;

public class ReplenishmentEntry {
	public String id, productId, reference, name, ean, size, colour, manualDescription, manualEan, note;
	public String customerId, customerName, status, createdBy, updatedBy, openProductId;
	public Date createdAt, updatedAt;

	public String displayName() {
		if (productId == null)
			return "Manual: " + (manualDescription == null ? "" : manualDescription);
		return (reference == null ? "" : reference) + " " + (name == null ? "" : name);
	}
}
