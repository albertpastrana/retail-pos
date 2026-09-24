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

/** A card tender recorded after payment on an external terminal. */
public class PaymentInfoMagcard extends PaymentInfo {

	protected double m_dTotal;
	protected String m_sTransactionID;
	protected String m_sAuthorization;
	protected String m_sErrorMessage;
	protected String m_sReturnMessage;

	public PaymentInfoMagcard(String sTransactionID, double dTotal) {
		m_sTransactionID = sTransactionID;
		m_dTotal = dTotal;
	}

	public PaymentInfo copyPayment() {
		PaymentInfoMagcard p = new PaymentInfoMagcard(m_sTransactionID, m_dTotal);
		p.m_sAuthorization = m_sAuthorization;
		p.m_sErrorMessage = m_sErrorMessage;
		p.m_sReturnMessage = m_sReturnMessage;
		return p;
	}

	public String getName() {
		return "magcard";
	}

	public double getTotal() {
		return m_dTotal;
	}

	public boolean isPaymentOK() {
		return m_sAuthorization != null;
	}

	public String getTransactionID() {
		return m_sTransactionID;
	}

	public String getAuthorization() {
		return m_sAuthorization;
	}

	public String getMessage() {
		return m_sErrorMessage;
	}

	public void paymentError(String sMessage, String moreInfo) {
		m_sAuthorization = null;
		m_sErrorMessage = sMessage + "\n" + moreInfo;
	}

	public void setReturnMessage(String returnMessage) {
		m_sReturnMessage = returnMessage;
	}

	public String getReturnMessage() {
		return m_sReturnMessage;
	}

	public void paymentOK(String sAuthorization, String sTransactionId, String sReturnMessage) {
		m_sAuthorization = sAuthorization;
		m_sTransactionID = sTransactionId;
		m_sReturnMessage = sReturnMessage;
		m_sErrorMessage = null;
	}

	public String printAuthorization() {
		return m_sAuthorization;
	}

	public String printTransactionID() {
		return m_sTransactionID;
	}
}
