package com.openbravo.pos.sales;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import com.openbravo.pos.ticket.TaxInfo;
import com.openbravo.pos.ticket.TicketInfo;
import com.openbravo.pos.ticket.TicketLineInfo;

class ScannerSpacePaymentTest {

	@Test
	void spaceWithinScannedBarcodeDoesNotStartPaymentOrSplitTheCode() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			try {
				JPanelTicketSales till = tillWithTwoLines();
				Field codeField = JPanelTicket.class.getDeclaredField("m_jKeyFactory");
				codeField.setAccessible(true);
				JTextField code = (JTextField) codeField.get(till);
				code.setText("843322110201");
				code.setCaretPosition(code.getText().length());

				KeyEvent space = new KeyEvent(code, KeyEvent.KEY_TYPED, System.currentTimeMillis(), 0,
						KeyEvent.VK_UNDEFINED, ' ');
				for (KeyListener listener : code.getKeyListeners()) {
					listener.keyTyped(space);
				}
				assertTrue(space.isConsumed());
				assertEquals("843322110201", code.getText());

				code.replaceSelection("7");
				assertEquals("8433221102017", code.getText());
			} catch (ReflectiveOperationException e) {
				throw new AssertionError(e);
			}
		});
	}

	@Test
	void spaceOnEmptyCodeFieldDoesNotStartPayment() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			try {
				JPanelTicketSales till = tillWithTwoLines();
				Field codeField = JPanelTicket.class.getDeclaredField("m_jKeyFactory");
				codeField.setAccessible(true);
				JTextField code = (JTextField) codeField.get(till);
				KeyEvent space = new KeyEvent(code, KeyEvent.KEY_TYPED, System.currentTimeMillis(), 0,
						KeyEvent.VK_UNDEFINED, ' ');
				for (KeyListener listener : code.getKeyListeners()) {
					listener.keyTyped(space);
				}
				assertTrue(space.isConsumed());
				assertEquals("", code.getText());
			} catch (ReflectiveOperationException e) {
				throw new AssertionError(e);
			}
		});
	}

	@Test
	void equalsOnEmptyCodeFieldStillAttemptsToCloseTicket() throws Exception {
		AtomicBoolean closeAttempted = new AtomicBoolean();
		Logger logger = Logger.getLogger(JPanelTicket.class.getName());
		Handler handler = new Handler() {
			@Override
			public void publish(LogRecord record) {
				if (record.getMessage().contains("event=ticket_close_ignored reason=empty_ticket")) {
					closeAttempted.set(true);
				}
			}

			@Override
			public void flush() {
			}

			@Override
			public void close() {
			}
		};
		logger.addHandler(handler);
		try {
			SwingUtilities.invokeAndWait(() -> {
				try {
					JPanelTicketSales till = new JPanelTicketSales();
					till.m_oTicket = new TicketInfo();
					Method transition = JPanelTicket.class.getDeclaredMethod("stateTransition", char.class);
					transition.setAccessible(true);
					transition.invoke(till, '=');
				} catch (ReflectiveOperationException e) {
					throw new AssertionError(e);
				}
			});
			assertTrue(closeAttempted.get());
		} finally {
			logger.removeHandler(handler);
		}
	}

	private static JPanelTicketSales tillWithTwoLines() {
		JPanelTicketSales till = new JPanelTicketSales();
		TicketInfo ticket = new TicketInfo();
		TaxInfo tax = new TaxInfo("001", "IVA", "001", null, null, null, 0.21, false, null);
		ticket.addLine(new TicketLineInfo("shirt", "Shirt", "001", 1.0, 20.0, tax));
		ticket.addLine(new TicketLineInfo("shoes", "Shoes", "001", 1.0, 40.0, tax));
		till.m_oTicket = ticket;
		return till;
	}
}
