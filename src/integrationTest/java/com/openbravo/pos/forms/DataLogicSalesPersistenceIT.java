package com.openbravo.pos.forms;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;

import org.junit.jupiter.api.Test;

import com.openbravo.data.loader.Session;
import com.openbravo.pos.payment.PaymentInfoCash;
import com.openbravo.pos.payment.PaymentInfoExternalVoucher;
import com.openbravo.pos.payment.PaymentInfoTicket;
import com.openbravo.pos.reports.PaymentSalesRepository;
import com.openbravo.pos.reports.SalesSummaryParameters;
import com.openbravo.pos.ticket.FindTicketsInfo;
import com.openbravo.pos.ticket.TaxInfo;
import com.openbravo.pos.ticket.TicketInfo;
import com.openbravo.pos.ticket.TicketLineInfo;
import com.openbravo.pos.ticket.UserInfo;

import static org.assertj.core.api.Assertions.assertThat;

public class DataLogicSalesPersistenceIT {
	@Test
	public void externalVoucherNameSurvivesConfigChangesInSalesAndClosing() throws Exception {
		String url = "jdbc:derby:memory:externalVoucherIT;create=true";
		DatabaseMigrator.migrate(url, null, null);
		Session session = new Session(url, null, null);
		try {
			DataLogicSystem system = new DataLogicSystem();
			system.init(session);
			assertThat(system.getExternalVoucherName()).isEmpty();
			system.setExternalVoucherName("iBons");
			Session secondTill = new Session(url, null, null);
			try {
				DataLogicSystem shared = new DataLogicSystem();
				shared.init(secondTill);
				assertThat(shared.getExternalVoucherName()).isEqualTo("iBons");
			} finally {
				secondTill.close();
			}
			DataLogicSales sales = new DataLogicSales();
			sales.init(session);
			try (Statement statement = session.getConnection().createStatement()) {
				statement.executeUpdate("INSERT INTO CLOSEDCASH VALUES ('voucher-cash', 'test-host', 1, "
						+ "TIMESTAMP('2026-09-24 10:00:00'), NULL)");
			}
			TaxInfo tax = new TaxInfo("000", "Tax Exempt", "000", new Date(0L), null, null, 0.0, false, null);
			TicketInfo ticket = new TicketInfo();
			ticket.setUser(new UserInfo("0", "Administrator"));
			ticket.setActiveCash("voucher-cash");
			ticket.addLine(new TicketLineInfo("gift-voucher-10", "Test product", "000", 2.0, 10.0, tax));
			ticket.getPayments().add(new PaymentInfoExternalVoucher(8.0, system.getExternalVoucherName()));
			ticket.getPayments().add(new PaymentInfoCash(12.0, 12.0));
			sales.saveTicket(ticket, "0");
			system.setExternalVoucherName("Other campaign");
			TicketInfo later = new TicketInfo();
			later.setUser(new UserInfo("0", "Administrator"));
			later.setActiveCash("voucher-cash");
			later.addLine(new TicketLineInfo("gift-voucher-10", "Test product", "000", 1.0, 10.0, tax));
			later.getPayments().add(new PaymentInfoExternalVoucher(10.0, system.getExternalVoucherName()));
			sales.saveTicket(later, "0");

			TicketInfo loaded = sales.loadTicket(TicketInfo.RECEIPT_NORMAL, ticket.getTicketId());
			assertThat(((PaymentInfoTicket) loaded.getPayments().get(0)).getLabel()).isEqualTo("iBons");
			assertThat(sales.getRecentTickets(TicketInfo.RECEIPT_NORMAL, 10))
					.extracting(row -> ((FindTicketsInfo) row).getPaymentLabel())
					.containsExactlyInAnyOrder("iBons", "Other campaign");
			assertThat(new PaymentSalesRepository().load(session.getConnection(),
					new SalesSummaryParameters(new Date(0L), new Date(System.currentTimeMillis() + 86_400_000L))))
					.filteredOn(row -> "externalvoucher".equals(row.getPaymentType())).extracting(row -> row.getLabel())
					.containsExactlyInAnyOrder("iBons", "Other campaign");
			try (Statement statement = session.getConnection().createStatement();
					ResultSet result = statement.executeQuery("SELECT PAYMENTLABEL, TOTAL FROM PAYMENTS "
							+ "WHERE RECEIPT = '" + ticket.getId() + "' AND PAYMENT = 'externalvoucher'")) {
				assertThat(result.next()).isTrue();
				assertThat(result.getString(1)).isEqualTo("iBons");
				assertThat(result.getDouble(2)).isEqualTo(8.0);
			}
			assertThat(sales.getCashTotal("voucher-cash")).isEqualTo(12.0);
		} finally {
			session.close();
		}
	}

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
	public void barcodeLookupTreatsMalformedAndInjectionShapedInputAsData() throws Exception {
		String url = "jdbc:derby:memory:barcodeLookupPersistenceIT;create=true";
		DatabaseMigrator.migrate(url, null, null);
		Session session = new Session(url, null, null);
		try {
			DataLogicSales sales = new DataLogicSales();
			sales.init(session);
			try (Statement statement = session.getConnection().createStatement()) {
				statement.executeUpdate("INSERT INTO BARCODE_TABLE (PID, CODE) VALUES ('gift-voucher-10', 'ALT-10')");
			}

			assertThat(sales.getProductInfoByCode("VAL10").getID()).isEqualTo("gift-voucher-10");
			assertThat(sales.getProductInfoByCode("ALT-10").getID()).isEqualTo("gift-voucher-10");
			assertThat(sales.getProductInfoByCode("' OR '1'='1")).isNull();
			assertThat(sales.getProductInfoByCode("[.*]")).isNull();
			assertThat(sales.getProductInfoByCode(null)).isNull();
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
	@Test
	public void deletingSaleRemovesReceiptAndReversesStock() throws Exception {
		String url = "jdbc:derby:memory:voidPersistenceIT;create=true";
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
			ticket.getPayments().add(new PaymentInfoCash(20.0, 20.0));
			sales.saveTicket(ticket, "0");

			sales.deleteTicket(ticket, "0");

			assertThat(sales.loadTicket(TicketInfo.RECEIPT_NORMAL, ticket.getTicketId())).isNull();
			try (Statement statement = session.getConnection().createStatement();
					ResultSet result = statement.executeQuery(
							"SELECT COUNT(*), COALESCE(SUM(UNITS), 0) FROM STOCKDIARY WHERE PRODUCT = 'gift-voucher-10'")) {
				assertThat(result.next()).isTrue();
				assertThat(result.getInt(1)).isEqualTo(2);
				assertThat(result.getDouble(2)).isEqualTo(0.0);
			}
		} finally {
			session.close();
		}
	}
}
