package com.openbravo.pos.reports;

import com.openbravo.format.Formats;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.theme.RetailPOSColors;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Reads only actionable cash, top-seller stock and debt conditions. */
final class ReportAttentionRepository {
	List<ReportAttention> load(Connection connection, SalesSummaryParameters period, Date now) throws SQLException {
		List<ReportAttention> notices = new ArrayList<>();
		try (PreparedStatement statement = connection
				.prepareStatement("SELECT MIN(DATESTART) FROM CLOSEDCASH WHERE DATEEND IS NULL");
				ResultSet result = statement.executeQuery()) {
			if (result.next()) {
				Timestamp opened = result.getTimestamp(1);
				if (opened != null) {
					long days = Math.max(0, Duration.between(opened.toInstant(), now.toInstant()).toDays());
					notices.add(new ReportAttention(AppLocal.getIntString("reports.welcome.cashOpen", days),
							"com.openbravo.pos.panels.JPanelCloseMoney", RetailPOSColors.warning()));
				}
			}
		}
		List<ProductSalesRow> sellers = new ProductSalesRepository().load(connection, period);
		Set<String> top = new HashSet<>();
		for (ProductSalesRow row : sellers) {
			if (!row.getReference().isEmpty()) {
				top.add(row.getReference());
			}
			if (top.size() == 5) {
				break;
			}
		}
		int lowCount = 0;
		Set<String> counted = new HashSet<>();
		for (LowStockRow row : new LowStockRepository().load(connection)) {
			if (top.contains(row.getReference()) && counted.add(row.getReference())) {
				lowCount++;
			}
		}
		if (lowCount > 0) {
			notices.add(new ReportAttention(AppLocal.getIntString("reports.welcome.lowStock", lowCount),
					"com.openbravo.pos.reports.JPanelLowStock", RetailPOSColors.warning()));
		}
		double outstanding = 0;
		int owing = 0;
		for (CustomerDebtRow row : new CustomerDebtRepository().load(connection)) {
			if (row.getCurrentDebt() > 0) {
				outstanding += row.getCurrentDebt();
				owing++;
			}
		}
		if (owing > 0) {
			notices.add(new ReportAttention(
					AppLocal.getIntString("reports.welcome.debt", Formats.CURRENCY.formatValue(outstanding), owing),
					"com.openbravo.pos.reports.JPanelCustomerDebt", RetailPOSColors.info()));
		}
		return notices;
	}
}
