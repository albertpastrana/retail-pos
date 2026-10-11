package com.openbravo.pos.forms;

import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;

import org.junit.jupiter.api.Test;

import com.openbravo.data.loader.Session;
import com.openbravo.pos.payment.PaymentInfoCash;
import com.openbravo.pos.reports.CashClosingRepository;
import com.openbravo.pos.reports.CashClosingRow;
import com.openbravo.pos.reports.SalesSummaryParameters;
import com.openbravo.pos.ticket.TaxInfo;
import com.openbravo.pos.ticket.TicketInfo;
import com.openbravo.pos.ticket.TicketLineInfo;
import com.openbravo.pos.ticket.UserInfo;

import static org.assertj.core.api.Assertions.assertThat;

public class ExchangePersistenceIT {

	@Test
	public void exchangesUseBalanceForNumberingStockAndCashClosing() throws Exception {
		String url = "jdbc:derby:memory:exchangePersistenceIT;create=true";
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

			TicketInfo saleExchange = exchange(tax, 40.0);
			saleExchange.getPayments().add(new PaymentInfoCash(10.0, 10.0));
			sales.saveTicket(saleExchange, "0");
			assertThat(saleExchange.getTicketType()).isEqualTo(TicketInfo.RECEIPT_NORMAL);
			assertThat(saleExchange.getTotalPaid()).isEqualTo(10.0);

			TicketInfo refundExchange = exchange(tax, 20.0);
			refundExchange.getPayments().add(new PaymentInfoCash(-10.0, -10.0));
			sales.saveTicket(refundExchange, "0");
			assertThat(refundExchange.getTicketType()).isEqualTo(TicketInfo.RECEIPT_REFUND);

			TicketInfo evenExchange = exchange(tax, 30.0);
			sales.saveTicket(evenExchange, "0");
			assertThat(evenExchange.getTicketType()).isEqualTo(TicketInfo.RECEIPT_NORMAL);
			assertThat(evenExchange.getPayments()).isEmpty();
			assertThat(evenExchange.getTicketId()).isEqualTo(saleExchange.getTicketId() + 1);

			TicketInfo pureReturn = exchange(tax, 0.0);
			pureReturn.getPayments().add(new PaymentInfoCash(-30.0, -30.0));
			sales.saveTicket(pureReturn, "0");
			assertThat(pureReturn.getTicketType()).isEqualTo(TicketInfo.RECEIPT_REFUND);
			assertThat(pureReturn.getTicketId()).isEqualTo(refundExchange.getTicketId() + 1);

			assertThat(sales.loadTicket(TicketInfo.RECEIPT_NORMAL, saleExchange.getTicketId()).getLines()).hasSize(2);
			assertThat(sales.loadTicket(TicketInfo.RECEIPT_NORMAL, evenExchange.getTicketId()).getPayments()).isEmpty();
			assertThat(sales.loadTicket(TicketInfo.RECEIPT_REFUND, refundExchange.getTicketId()).getLines()).hasSize(2);
			try (Statement statement = session.getConnection().createStatement();
					ResultSet result = statement.executeQuery(
							"SELECT REASON, SUM(UNITS) FROM STOCKDIARY WHERE PRODUCT IN ('gift-voucher-10', 'gift-voucher-20') "
									+ "GROUP BY REASON ORDER BY REASON")) {
				assertThat(result.next()).isTrue();
				assertThat(result.getInt(1)).isEqualTo(-1);
				assertThat(result.getDouble(2)).isEqualTo(-3.0);
				assertThat(result.next()).isTrue();
				assertThat(result.getInt(1)).isEqualTo(2);
				assertThat(result.getDouble(2)).isEqualTo(4.0);
				assertThat(result.next()).isFalse();
			}
			CashClosingRow closing = new CashClosingRepository()
					.load(session.getConnection(),
							new SalesSummaryParameters(date("2026-09-24 00:00:00"), date("2026-09-25 00:00:00")))
					.get(0);
			assertThat(closing.getReceiptCount()).isEqualTo(4);
			assertThat(closing.getGrossPayments()).isEqualTo(10.0);
			assertThat(closing.getRefunds()).isEqualTo(-40.0);
		} finally {
			session.close();
		}
	}

	private static TicketInfo exchange(TaxInfo tax, double replacementPrice) {
		TicketInfo ticket = new TicketInfo();
		ticket.setTicketType(TicketInfo.RECEIPT_REFUND);
		ticket.setUser(new UserInfo("0", "Administrator"));
		ticket.setActiveCash("cash-1");
		ticket.setDate(date("2026-09-24 12:00:00"));
		ticket.addLine(new TicketLineInfo("gift-voucher-10", "Returned product", "000", -1.0, 30.0, tax));
		if (replacementPrice > 0.0) {
			ticket.addLine(new TicketLineInfo("gift-voucher-20", "New product", "000", 1.0, replacementPrice, tax));
		}
		ticket.classifyExchangeByBalance();
		return ticket;
	}

	private static Date date(String value) {
		return new Date(Timestamp.valueOf(value).getTime());
	}
}
