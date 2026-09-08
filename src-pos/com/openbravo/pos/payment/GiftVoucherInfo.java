package com.openbravo.pos.payment;

public class GiftVoucherInfo {

	private final String code;
	private final double initialValue;
	private final double balance;

	public GiftVoucherInfo(String code, double initialValue, double balance) {
		this.code = code;
		this.initialValue = initialValue;
		this.balance = balance;
	}

	public String getCode() {
		return code;
	}

	public double getInitialValue() {
		return initialValue;
	}

	public double getBalance() {
		return balance;
	}
}
