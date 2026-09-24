package com.openbravo.pos.forms;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.openbravo.basic.BasicException;
import com.openbravo.pos.payment.PaymentInfoCash;
import com.openbravo.pos.ticket.TicketInfo;
import org.junit.jupiter.api.Test;

public class DataLogicSalesPaymentValidationTest {

	@Test
	public void rejectsZeroCashForPositiveSale() {
		TicketInfo ticket = new TicketInfo() {
			@Override
			public double getTotal() {
				return 89.60;
			}
		};
		ticket.getPayments().add(new PaymentInfoCash(0.0, 0.0));

		assertThatThrownBy(() -> DataLogicSales.validatePaymentTotals(ticket)).isInstanceOf(BasicException.class);
	}

	@Test
	public void acceptsPaymentTotalMatchingSale() {
		TicketInfo ticket = new TicketInfo() {
			@Override
			public double getTotal() {
				return 89.60;
			}
		};
		ticket.getPayments().add(new PaymentInfoCash(89.60, 100.0));

		assertThatCode(() -> DataLogicSales.validatePaymentTotals(ticket)).doesNotThrowAnyException();
	}
}
