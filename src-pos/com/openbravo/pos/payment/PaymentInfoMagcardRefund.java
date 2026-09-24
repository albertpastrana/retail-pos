//    Openbravo POS is a point of sales application designed for touch screens.
//    Copyright (C) 2008-2009 Openbravo, S.L.
//    http://www.openbravo.com/product/pos
//
//    This file is part of Openbravo POS.
//
//    Openbravo POS is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    Openbravo POS is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with Openbravo POS.  If not, see <http://www.gnu.org/licenses/>.

package com.openbravo.pos.payment;

public class PaymentInfoMagcardRefund extends PaymentInfoMagcard {

	public PaymentInfoMagcardRefund(String sTransactionID, double dTotal) {
		super(sTransactionID, dTotal);
	}

	@Override
	public PaymentInfo copyPayment() {
		PaymentInfoMagcard p = new PaymentInfoMagcardRefund(m_sTransactionID, m_dTotal);
		p.m_sAuthorization = m_sAuthorization;
		p.m_sErrorMessage = m_sErrorMessage;
		p.m_sReturnMessage = m_sReturnMessage;
		return p;
	}

	@Override
	public String getName() {
		return "magcardrefund";
	}
}
