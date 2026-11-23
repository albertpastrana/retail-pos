package com.openbravo.pos.forms;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.openbravo.basic.BasicException;
import com.openbravo.pos.payment.PaymentInfoCash;
import com.openbravo.pos.payment.PaymentInfoExternalVoucher;
import com.openbravo.pos.payment.PaymentInfoMagcard;
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

	@Test
	public void rejectsUnderpayment() {
		TicketInfo ticket = new TicketInfo() {
			@Override
			public double getTotal() {
				return 89.60;
			}
		};
		ticket.getPayments().add(new PaymentInfoCash(80.00, 80.00));

		assertThatThrownBy(() -> DataLogicSales.validatePaymentTotals(ticket)).isInstanceOf(BasicException.class);
	}

	@Test
	public void acceptsMixedCashAndCardPayment() {
		TicketInfo ticket = new TicketInfo() {
			@Override
			public double getTotal() {
				return 89.60;
			}
		};
		ticket.getPayments().add(new PaymentInfoCash(20.00, 20.00));
		ticket.getPayments().add(new PaymentInfoMagcard("transaction-1", 69.60));

		assertThatCode(() -> DataLogicSales.validatePaymentTotals(ticket)).doesNotThrowAnyException();
	}

	@Test
	public void acceptsRefundWithNegativePaymentTotal() {
		TicketInfo ticket = new TicketInfo() {
			@Override
			public double getTotal() {
				return -20.00;
			}
		};
		ticket.setTicketType(TicketInfo.RECEIPT_REFUND);
		ticket.getPayments().add(new PaymentInfoCash(-20.00, -20.00));

		assertThatCode(() -> DataLogicSales.validatePaymentTotals(ticket)).doesNotThrowAnyException();
	}

	@Test
	public void acceptsMixedExternalVoucherAndCardAndPreservesNameOnCopy() {
		TicketInfo ticket = new TicketInfo() {
			@Override
			public double getTotal() {
				return 89.60;
			}
		};
		PaymentInfoExternalVoucher voucher = new PaymentInfoExternalVoucher(20.00, "iBons");
		ticket.getPayments().add(voucher);
		ticket.getPayments().add(new PaymentInfoMagcard("terminal", 69.60));
		assertThatCode(() -> DataLogicSales.validatePaymentTotals(ticket)).doesNotThrowAnyException();
		org.assertj.core.api.Assertions.assertThat(((PaymentInfoExternalVoucher) voucher.copyPayment()).getLabel())
				.isEqualTo("iBons");
	}

	@Test
	public void rejectsVoucherRefundAndAmountsOutsideSale() {
		TicketInfo ticket = new TicketInfo() {
			@Override
			public double getTotal() {
				return 20.0;
			}
		};
		for (double amount : new double[]{0.0, -1.0, 21.0, Double.NaN}) {
			ticket.getPayments().clear();
			ticket.getPayments().add(new PaymentInfoExternalVoucher(amount, "Voucher"));
			assertThatThrownBy(() -> DataLogicSales.validatePaymentTotals(ticket)).isInstanceOf(BasicException.class)
					.hasMessage(AppLocal.getIntString("message.invalidexternalvoucher"));
		}
		ticket.getPayments().clear();
		ticket.setTicketType(TicketInfo.RECEIPT_REFUND);
		ticket.getPayments().add(new PaymentInfoExternalVoucher(20.0, "Voucher"));
		assertThatThrownBy(() -> DataLogicSales.validatePaymentTotals(ticket)).isInstanceOf(BasicException.class)
				.hasMessage(AppLocal.getIntString("message.invalidexternalvoucher"));
	}
}
