package com.openbravo.pos.payment;

/** Captures the name in effect when an external voucher is accepted. */
public final class PaymentInfoExternalVoucher extends PaymentInfoTicket {

	public PaymentInfoExternalVoucher(double amount, String label) {
		super(amount, ExternalVoucher.PAYMENT, null, label);
	}

	@Override
	public PaymentInfo copyPayment() {
		return new PaymentInfoExternalVoucher(getTotal(), getLabel());
	}
}
