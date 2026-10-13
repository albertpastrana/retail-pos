package com.openbravo.pos.ticket;

/**
 * Encodes receipt IDs without allowing them to be confused with product codes.
 */
public final class ReceiptBarcode {

	public static final int TYPE_SALE = TicketInfo.RECEIPT_NORMAL;
	public static final int TYPE_REFUND = TicketInfo.RECEIPT_REFUND;

	private ReceiptBarcode() {
	}

	public static String encode(TicketInfo ticket) {
		if (ticket.getTicketId() <= 0) {
			return "";
		}
		return (ticket.getTicketType() == TYPE_REFUND ? "R" : "T") + ticket.getTicketId();
	}

	public static boolean isReceiptBarcode(String value) {
		return value != null && value.length() > 1 && (value.charAt(0) == 'T' || value.charAt(0) == 'R')
				&& value.substring(1).matches("[0-9]+");
	}

	public static int type(String value) {
		return value.charAt(0) == 'R' ? TYPE_REFUND : TYPE_SALE;
	}

	public static int ticketId(String value) {
		return Integer.parseInt(value.substring(1));
	}
}
