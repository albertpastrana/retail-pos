package com.openbravo.pos.forms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.Test;

import com.openbravo.basic.BasicException;
import com.openbravo.data.loader.Session;
import com.openbravo.data.loader.Transaction;
import com.openbravo.pos.sales.DataLogicReceipts;
import com.openbravo.pos.sales.SharedTicketInfo;
import com.openbravo.pos.ticket.TicketInfo;
import com.openbravo.pos.ticket.UserInfo;

public class SharedTicketsIT {

	@Test
	public void claimingATicketRejectsChangesFromThePreviousTill() throws Exception {
		Session session = openDatabase();
		try {
			DataLogicReceipts receipts = receipts(session);
			TicketInfo scannerTicket = ticket("seller", "Anna");

			assertTrue(receipts.saveSharedTicket("ticket-1", scannerTicket, "scanner"));
			assertEquals("scanner", onlyTicket(receipts).getHost());

			TicketInfo centralTicket = receipts.claimSharedTicket("ticket-1", "central");
			assertEquals("Anna", centralTicket.getUser().getName());
			assertEquals("central", onlyTicket(receipts).getHost());
			assertFalse(receipts.isSharedTicketOwned("ticket-1", "scanner"));
			assertTrue(receipts.isSharedTicketOwned("ticket-1", "central"));

			scannerTicket.setUser(new UserInfo("seller", "Stale edit"));
			assertFalse(receipts.saveSharedTicket("ticket-1", scannerTicket, "scanner"));
			assertEquals("Anna", receipts.getSharedTicket("ticket-1").getUser().getName());
			assertFalse(receipts.deleteSharedTicket("ticket-1", "scanner"));
			assertTrue(receipts.deleteSharedTicket("ticket-1", "central"));
		} finally {
			session.close();
		}
	}

	@Test
	public void parkingReleasesTheTicketForAnotherTill() throws Exception {
		Session session = openDatabase();
		try {
			DataLogicReceipts receipts = receipts(session);
			TicketInfo ticket = ticket("seller", "Anna");

			assertTrue(receipts.saveSharedTicket("ticket-1", ticket, "scanner"));
			assertTrue(receipts.parkSharedTicket("ticket-1", ticket, "scanner"));
			assertNull(onlyTicket(receipts).getHost());

			assertEquals("Anna", receipts.claimSharedTicket("ticket-1", "central").getUser().getName());
			assertEquals("central", onlyTicket(receipts).getHost());
		} finally {
			session.close();
		}
	}

	@Test
	public void sellerClaimIsAtomicWithTheSellerLookup() throws Exception {
		Session session = openDatabase();
		try {
			DataLogicReceipts receipts = receipts(session);
			assertTrue(receipts.saveSharedTicket("ticket-1", ticket("seller", "Anna"), "scanner"));

			assertEquals("ticket-1", receipts.claimSharedTicketForSeller("seller", "central"));
			assertTrue(receipts.isSharedTicketOwned("ticket-1", "central"));
		} finally {
			session.close();
		}
	}

	@Test
	public void paymentPreventsAnotherTillFromClaimingTheTicket() throws Exception {
		Session session = openDatabase();
		try {
			DataLogicReceipts receipts = receipts(session);
			TicketInfo ticket = ticket("seller", "Anna");

			assertTrue(receipts.saveSharedTicket("ticket-1", ticket, "central"));
			assertTrue(receipts.beginSharedTicketPayment("ticket-1", "central"));
			assertNull(receipts.claimSharedTicket("ticket-1", "scanner"));

			receipts.cancelSharedTicketPayment("ticket-1", "central");
			assertEquals("Anna", receipts.claimSharedTicket("ticket-1", "scanner").getUser().getName());
		} finally {
			session.close();
		}
	}

	@Test
	public void failedSaleTransactionRestoresTheSharedTicket() throws Exception {
		Session session = openDatabase();
		try {
			final DataLogicReceipts receipts = receipts(session);
			assertTrue(receipts.saveSharedTicket("ticket-1", ticket("seller", "Anna"), "central"));
			assertTrue(receipts.beginSharedTicketPayment("ticket-1", "central"));

			try {
				new Transaction<Object>(session) {
					@Override
					protected Object transact() throws BasicException {
						assertTrue(receipts.deleteSharedTicket("ticket-1", "central"));
						throw new BasicException("sale failed");
					}
				}.execute();
			} catch (BasicException expected) {
				assertEquals("sale failed", expected.getMessage());
			}

			assertNull(receipts.claimSharedTicket("ticket-1", "scanner"));
			receipts.cancelSharedTicketPayment("ticket-1", "central");
			assertEquals("Anna", receipts.claimSharedTicket("ticket-1", "scanner").getUser().getName());
		} finally {
			session.close();
		}
	}

	private static Session openDatabase() throws Exception {
		String url = "jdbc:derby:memory:sharedTickets-" + UUID.randomUUID() + ";create=true";
		DatabaseMigrator.migrate(url, null, null);
		return new Session(url, null, null);
	}

	private static DataLogicReceipts receipts(Session session) {
		DataLogicReceipts receipts = new DataLogicReceipts();
		receipts.init(session);
		return receipts;
	}

	private static TicketInfo ticket(String sellerId, String sellerName) {
		TicketInfo ticket = new TicketInfo();
		ticket.setUser(new UserInfo(sellerId, sellerName));
		return ticket;
	}

	private static SharedTicketInfo onlyTicket(DataLogicReceipts receipts) throws Exception {
		List<SharedTicketInfo> tickets = receipts.getSharedTicketList();
		assertEquals(1, tickets.size());
		return tickets.get(0);
	}
}
