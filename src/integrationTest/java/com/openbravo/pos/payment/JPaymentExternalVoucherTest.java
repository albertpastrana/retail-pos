package com.openbravo.pos.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.junit.jupiter.api.Test;

import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.util.PosIcons;
import com.openbravo.beans.JNumberKeys;
import com.openbravo.format.Formats;

public class JPaymentExternalVoucherTest {
	@Test
	public void partialAmountAllowsNextTenderWithoutAcceptingInvalidValuesInAllLanguages() throws Exception {
		Locale before = Locale.getDefault();
		try {
			Formats.setCurrencyPattern("0");
			try {
				assertTrue(Double.isNaN(JPaymentExternalVoucher.parseAmount("5.500")));
				assertEquals(5500.0, JPaymentExternalVoucher.parseAmount("5500"), 0.001);
			} finally {
				Formats.setCurrencyPattern(null);
			}
			Formats.setCurrencyPattern("#,##0");
			try {
				assertEquals(5500.0,
						JPaymentExternalVoucher.parseAmount("5" + Formats.getCurrencyGroupingSeparator() + "500"),
						0.001);
			} finally {
				Formats.setCurrencyPattern(null);
			}
			assertEquals("1234", JPaymentExternalVoucher.normalizedAmount("1.234", 2, ',', '.'));
			assertEquals("1234,56", JPaymentExternalVoucher.normalizedAmount("1.234,56", 2, ',', '.'));
			assertEquals("12,50", JPaymentExternalVoucher.normalizedAmount("12.50", 2, ',', '.'));
			assertNull(JPaymentExternalVoucher.normalizedAmount("5..00", 2, ',', '.'));
			assertEquals("1234", JPaymentExternalVoucher.normalizedAmount("1.234", 0, ',', '.'));
			assertNull(JPaymentExternalVoucher.normalizedAmount("5.50", 0, ',', '.'));
			assertTrue(PosIcons.ticketCheck().getIconWidth() > 0);
			assertEquals(PosIcons.ticketCheck().getIconWidth(), PosIcons.ticketCheck().getIconHeight());
			for (Locale locale : new Locale[]{Locale.ENGLISH, new Locale("es"), new Locale("ca")}) {
				AppLocal.setLocale(locale);
				SwingUtilities.invokeAndWait(() -> {
					Status status = new Status();
					JPaymentExternalVoucher panel = new JPaymentExternalVoucher(status, null);
					panel.activate(null, 40.0, null);
					JTextField amount = find(panel, JTextField.class);
					assertTrue(amount.getPreferredSize().width >= amount.getFontMetrics(UIManager.getFont("Label.font"))
							.stringWidth(Formats.CURRENCY.formatValue(99999.99)));
					JButton partial = find(panel, JButton.class);
					JTextArea feedback = findNamed(panel, "externalVoucherFeedback", JTextArea.class);
					JNumberKeys keys = find(panel, JNumberKeys.class);
					assertNotNull(feedback);
					assertNotNull(keys);
					panel.setSize(800, 600);
					panel.doLayout();
					keys.getParent().doLayout();
					assertEquals(keys.getPreferredSize().height, keys.getHeight());
					assertTrue(panel.getPreferredSize().width <= 700);
					assertFalse(partial.isEnabled());
					amount.setText("5");
					assertTrue(partial.isEnabled());
					assertFalse(status.complete);
					assertTrue(partial.getText().contains(Formats.CURRENCY.formatValue(5.0)));
					partial.doClick();
					assertEquals(1, status.applied);
					amount.setText("40");
					assertTrue(status.complete);
					assertFalse(partial.isEnabled());
					amount.setText("55");
					assertFalse(status.positive);
					assertFalse(partial.isEnabled());
					assertEquals(AppLocal.getIntString("message.externalvoucher.overremaining",
							Formats.CURRENCY.formatValue(40.0)), feedback.getText());
					assertTrue(feedback.getText().contains(Formats.CURRENCY.formatValue(40.0)));
					assertFalse(feedback.getText().contains("{0}"));
					assertEquals(AppLocal.getIntString("button.externalvoucher.addpartial"), partial.getText());
					amount.setText("5");
					assertEquals("", feedback.getText());
					amount.setText("0");
					assertFalse(status.positive);
					assertEquals(AppLocal.getIntString("message.externalvoucher.invalidamount"), feedback.getText());
					amount.setText("-5");
					assertEquals("-5", amount.getText());
					assertFalse(status.positive);
					assertNull(panel.executePayment());
					amount.setText("5..00");
					assertFalse(status.positive);
					assertFalse(partial.isEnabled());
					assertEquals(AppLocal.getIntString("message.externalvoucher.invalidamount"), feedback.getText());
					assertNull(panel.executePayment());
					String grouped = Formats.CURRENCY.formatValue(1234.56).replaceAll("[^\\p{Nd}.,]", "");
					assertEquals(1234.56, JPaymentExternalVoucher.parseAmount(grouped), 0.001);
				});
			}
			assertCompactAtLargeDisplayFont();
		} finally {
			AppLocal.setLocale(before);
		}
	}

	private static void assertCompactAtLargeDisplayFont() throws Exception {
		Font label = UIManager.getFont("Label.font");
		Font field = UIManager.getFont("TextField.font");
		Font button = UIManager.getFont("Button.font");
		try {
			UIManager.put("Label.font", label.deriveFont(label.getSize2D() * 2));
			UIManager.put("TextField.font", field.deriveFont(field.getSize2D() * 2));
			UIManager.put("Button.font", button.deriveFont(button.getSize2D() * 2));
			for (Locale locale : new Locale[]{Locale.ENGLISH, new Locale("es"), new Locale("ca")}) {
				AppLocal.setLocale(locale);
				SwingUtilities.invokeAndWait(() -> {
					Status status = new Status();
					JPaymentExternalVoucher voucher = new JPaymentExternalVoucher(status, null);
					JPaymentCashPos cash = new JPaymentCashPos(status, null);
					voucher.activate(null, 40, null);
					cash.activate(null, 40, null);
					JTextField amount = find(voucher, JTextField.class);
					assertTrue(amount.getPreferredSize().width >= amount.getFontMetrics(UIManager.getFont("Label.font"))
							.stringWidth(Formats.CURRENCY.formatValue(99999.99)));
					assertTrue(voucher.getPreferredSize().width <= cash.getPreferredSize().width + 250,
							() -> "Voucher width " + voucher.getPreferredSize().width + " vs cash "
									+ cash.getPreferredSize().width + " in " + locale + " (form "
									+ voucher.getComponent(0).getPreferredSize().width + ", amount "
									+ find(voucher, javax.swing.JLabel.class).getPreferredSize().width + ", input "
									+ find(voucher, JTextField.class).getPreferredSize().width + ", instructions "
									+ find(voucher, JTextArea.class).getPreferredSize().width + ", keypad "
									+ find(voucher, JNumberKeys.class).getPreferredSize().width + ", button "
									+ find(voucher, JButton.class).getPreferredSize().width + ")");
				});
			}
		} finally {
			UIManager.put("Label.font", label);
			UIManager.put("TextField.font", field);
			UIManager.put("Button.font", button);
		}
	}

	private static <T extends Component> T findNamed(Container parent, String name, Class<T> type) {
		for (Component child : parent.getComponents()) {
			if (name.equals(child.getName()) && type.isInstance(child)) {
				return type.cast(child);
			}
			if (child instanceof Container) {
				T nested = findNamed((Container) child, name, type);
				if (nested != null) {
					return nested;
				}
			}
		}
		return null;
	}

	private static <T extends Component> T find(Container parent, Class<T> type) {
		for (Component child : parent.getComponents()) {
			if (type.isInstance(child)) {
				return type.cast(child);
			}
			if (child instanceof Container) {
				T nested = find((Container) child, type);
				if (nested != null) {
					return nested;
				}
			}
		}
		return null;
	}

	private static final class Status implements JPaymentNotifier {
		boolean positive;
		boolean complete;
		int applied;

		public void setStatus(boolean positive, boolean complete) {
			this.positive = positive;
			this.complete = complete;
		}

		public boolean isVoucherSelected(String code) {
			return false;
		}

		public void addSelectedPayment() {
			applied++;
		}
	}
}
