package com.openbravo.pos.forms;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;

import org.junit.jupiter.api.Test;

import com.openbravo.data.loader.Session;
import com.openbravo.pos.payment.PaymentInfoCash;
import com.openbravo.pos.ticket.TaxInfo;
import com.openbravo.pos.ticket.TicketInfo;
import com.openbravo.pos.ticket.TicketLineInfo;
import com.openbravo.pos.ticket.UserInfo;

import static org.assertj.core.api.Assertions.assertThat;

public class DataLogicSalesPersistenceIT {

	@Test
	public void savesAndLoadsSaleWithStockMovement() throws Exception {
		String url = "jdbc:derby:memory:salePersistenceIT;create=true";
		DatabaseMigrator.migrate(url, null, null);
		Session session = new Session(url, null, null);
		try {
			DataLogicSales sales = new DataLogicSales();
			sales.init(session);
			try (Statement statement = session.getConnection().createStatement()) {
				statement.executeUpdate("INSERT INTO CLOSEDCASH VALUES ('cash-1', 'test-host', 1, "
						+ "TIMESTAMP('2026-09-24 10:00:00'), NULL)");
			}

			TaxInfo tax = new TaxInfo("000", "Tax Exempt", "000", new Date(0L), null, null, 0.0, false, null);
			TicketInfo ticket = new TicketInfo();
			ticket.setUser(new UserInfo("0", "Administrator"));
			ticket.setActiveCash("cash-1");
			ticket.addLine(new TicketLineInfo("gift-voucher-10", "Test product", "000", 2.0, 10.0, tax));
			ticket.getPayments().add(new PaymentInfoCash(ticket.getTotal(), ticket.getTotal()));

			sales.saveTicket(ticket, "0");

			TicketInfo loaded = sales.loadTicket(TicketInfo.RECEIPT_NORMAL, ticket.getTicketId());
			assertThat(loaded).isNotNull();
			assertThat(loaded.getId()).isEqualTo(ticket.getId());
			assertThat(loaded.getLines()).hasSize(1);
			assertThat(loaded.getLine(0).getProductID()).isEqualTo("gift-voucher-10");
			assertThat(loaded.getLine(0).getMultiply()).isEqualTo(2.0);
			assertThat(loaded.getTotalPaid()).isEqualTo(20.0);

			try (Statement statement = session.getConnection().createStatement();
					ResultSet result = statement
							.executeQuery("SELECT REASON, UNITS FROM STOCKDIARY WHERE PRODUCT = 'gift-voucher-10'")) {
				assertThat(result.next()).isTrue();
				assertThat(result.getInt("REASON")).isEqualTo(-1);
				assertThat(result.getDouble("UNITS")).isEqualTo(-2.0);
				assertThat(result.next()).isFalse();
			}
		} finally {
			session.close();
		}
	}

	@Test
	public void savesRefundWithInboundStockMovement() throws Exception {
		String url = "jdbc:derby:memory:refundPersistenceIT;create=true";
		DatabaseMigrator.migrate(url, null, null);
		Session session = new Session(url, null, null);
		try {
			DataLogicSales sales = new DataLogicSales();
			sales.init(session);
			try (Statement statement = session.getConnection().createStatement()) {
				statement.executeUpdate("INSERT INTO CLOSEDCASH VALUES ('cash-1', 'test-host', 1, "
						+ "TIMESTAMP('2026-09-24 10:00:00'), NULL)");
			}

			TaxInfo tax = new TaxInfo("000", "Tax Exempt", "000", new Date(0L), null, null, 0.0, false, null);
			TicketInfo ticket = new TicketInfo();
			ticket.setTicketType(TicketInfo.RECEIPT_REFUND);
			ticket.setUser(new UserInfo("0", "Administrator"));
			ticket.setActiveCash("cash-1");
			ticket.addLine(new TicketLineInfo("gift-voucher-10", "Test product", "000", -1.0, 10.0, tax));
			ticket.getPayments().add(new PaymentInfoCash(-10.0, -10.0));

			sales.saveTicket(ticket, "0");

			try (Statement statement = session.getConnection().createStatement();
					ResultSet result = statement
							.executeQuery("SELECT REASON, UNITS FROM STOCKDIARY WHERE PRODUCT = 'gift-voucher-10'")) {
				assertThat(result.next()).isTrue();
				assertThat(result.getInt("REASON")).isEqualTo(2);
				assertThat(result.getDouble("UNITS")).isEqualTo(1.0);
				assertThat(result.next()).isFalse();
			}
		} finally {
			session.close();
		}
	}
}
