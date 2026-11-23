package com.openbravo.pos.payment;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import com.openbravo.beans.JNumberKeys;
import com.openbravo.format.Formats;
import com.openbravo.pos.customers.CustomerInfoExt;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.DataLogicSystem;
import com.openbravo.pos.theme.RetailPOSColors;
import com.openbravo.pos.util.RoundUtils;

/** Manual acceptance of an amount already redeemed with an external issuer. */
public final class JPaymentExternalVoucher extends JPanel implements JPaymentInterface {
	private final JPaymentNotifier notifier;
	private final DataLogicSystem system;
	private final JTextField amount = new JTextField();
	private final JButton apply = new JButton();
	private final JTextArea feedback = new JTextArea(3, 0);
	private double remaining;

	public JPaymentExternalVoucher(JPaymentNotifier notifier, DataLogicSystem system) {
		this.notifier = notifier;
		this.system = system;
		setLayout(new BorderLayout());
		JPanel fields = new JPanel(new GridBagLayout());
		fields.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 16, 16, 16));
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.insets = new Insets(6, 6, 6, 6);
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.anchor = GridBagConstraints.LINE_END;
		fields.add(new JLabel(AppLocal.getIntString("label.externalvoucher.amount")), constraints);
		constraints.gridx = 1;
		constraints.anchor = GridBagConstraints.LINE_START;
		FontMetrics amountMetrics = amount.getFontMetrics(javax.swing.UIManager.getFont("Label.font"));
		// Fit a representative formatted amount at the active display/font scale.
		amount.setPreferredSize(new Dimension(amountMetrics.stringWidth(Formats.CURRENCY.formatValue(99999.99)) + 24,
				Math.max(amount.getPreferredSize().height, amountMetrics.getHeight() + 12)));
		amount.setHorizontalAlignment(JTextField.RIGHT);
		fields.add(amount, constraints);
		JTextArea instructions = new JTextArea(AppLocal.getIntString("label.externalvoucher.redeemfirst"), 3, 0);
		configureHint(instructions);
		configureHint(feedback);
		constraints.gridx = 0;
		constraints.gridy = 1;
		constraints.gridwidth = 2;
		constraints.anchor = GridBagConstraints.LINE_START;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		fields.add(instructions, constraints);
		constraints.gridy = 2;
		feedback.setName("externalVoucherFeedback");
		fields.add(feedback, constraints);
		constraints.gridy = 3;
		constraints.fill = GridBagConstraints.NONE;
		apply.setFocusable(false);
		apply.setMargin(new Insets(8, 12, 8, 12));
		apply.addActionListener(event -> notifier.addSelectedPayment());
		fields.add(apply, constraints);
		JPanel formColumn = new JPanel(new BorderLayout());
		formColumn.add(fields, BorderLayout.NORTH);
		add(formColumn, BorderLayout.CENTER);

		JNumberKeys keys = new JNumberKeys();
		keys.setNumbersOnly(true);
		keys.addJNumberEventListener(event -> {
			if (event.getKey() == '\u007f') {
				amount.setText("");
			} else {
				amount.replaceSelection(Character.toString(event.getKey()));
			}
		});
		JPanel keypad = new JPanel(new BorderLayout());
		keypad.add(keys, BorderLayout.NORTH);
		add(keypad, BorderLayout.LINE_END);
		((AbstractDocument) amount.getDocument()).setDocumentFilter(new NumericFilter());
		amount.getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) {
				updateState();
			}

			public void removeUpdate(DocumentEvent e) {
				updateState();
			}

			public void changedUpdate(DocumentEvent e) {
				updateState();
			}
		});
	}

	private static void configureHint(JTextArea hint) {
		hint.setFont(javax.swing.UIManager.getFont("Label.font"));
		FontMetrics metrics = hint.getFontMetrics(hint.getFont());
		// JTextArea's unwrapped preferred width can otherwise make the packed payment
		// dialog enormous. Reserve eight digit-widths and the requested lines.
		hint.setPreferredSize(new Dimension(metrics.charWidth('0') * 8 + hint.getInsets().left + hint.getInsets().right,
				metrics.getHeight() * hint.getRows() + hint.getInsets().top + hint.getInsets().bottom));
		hint.setForeground(RetailPOSColors.inkMuted());
		hint.setEditable(false);
		hint.setFocusable(false);
		hint.setOpaque(false);
		hint.setLineWrap(true);
		hint.setWrapStyleWord(true);
	}

	@Override
	public void activate(CustomerInfoExt customer, double total, String transactionId) {
		remaining = total;
		apply.setText(AppLocal.getIntString("button.externalvoucher.addpartial"));
		amount.setText("");
		java.awt.EventQueue.invokeLater(() -> amount.requestFocusInWindow());
		updateState();
	}

	@Override
	public PaymentInfo executePayment() {
		double value = enteredAmount();
		return ExternalVoucher.valid(value, remaining)
				? new PaymentInfoExternalVoucher(RoundUtils.round(value),
						ExternalVoucher.name(system.getExternalVoucherName()))
				: null;
	}

	@Override
	public Component getComponent() {
		return this;
	}

	private double enteredAmount() {
		return parseAmount(amount.getText());
	}

	static double parseAmount(String text) {
		String normalized = normalizedAmount(text);
		if (normalized == null) {
			return Double.NaN;
		}
		try {
			Double value = (Double) Formats.CURRENCY.parseValue(normalized);
			return value == null ? Double.NaN : value.doubleValue();
		} catch (Exception e) {
			return Double.NaN;
		}
	}

	private static String normalizedAmount(String text) {
		// Formats.CURRENCY accepts numeric prefixes; check the entire input against
		// its configured separators before using its parsed value.
		String sample = Formats.CURRENCY.formatValue(12345.67);
		int decimals = Formats.getCurrencyDecimals();
		char lastSeparator = sample.lastIndexOf(',') > sample.lastIndexOf('.') ? ',' : '.';
		char decimal = lastSeparator;
		char grouping = decimals == 0 ? lastSeparator : decimal == ',' ? '.' : ',';
		return normalizedAmount(text, decimals, decimal, grouping);
	}

	static String normalizedAmount(String text, int decimals, char decimal, char grouping) {
		if (text == null || text.isEmpty()) {
			return null;
		}
		String fraction = decimals > 0
				? "(?:" + Pattern.quote(Character.toString(decimal)) + "\\p{Nd}{1," + decimals + "})?"
				: "";
		String grouped = "\\p{Nd}{1,3}(?:" + Pattern.quote(Character.toString(grouping)) + "\\p{Nd}{3})+";
		if (text.matches(grouped + fraction)) {
			return text.replace(Character.toString(grouping), "");
		}
		String normalized = decimals > 0 && decimal == ',' && !text.contains(",") ? text.replace('.', ',') : text;
		return normalized.matches("\\p{Nd}+" + fraction) ? normalized : null;
	}

	private void updateState() {
		double value = enteredAmount();
		if (Double.isFinite(value) && RoundUtils.compare(value, remaining) > 0) {
			feedback.setForeground(RetailPOSColors.dangerText());
			feedback.setText(AppLocal.getIntString("message.externalvoucher.overremaining",
					Formats.CURRENCY.formatValue(remaining)));
		} else if (!amount.getText().isEmpty() && !ExternalVoucher.valid(value, remaining)) {
			feedback.setForeground(RetailPOSColors.dangerText());
			feedback.setText(AppLocal.getIntString("message.externalvoucher.invalidamount"));
		} else {
			feedback.setText("");
		}
		boolean valid = ExternalVoucher.valid(value, remaining);
		boolean partial = valid && RoundUtils.compare(value, remaining) < 0;
		apply.setEnabled(partial);
		if (partial) {
			apply.setText(
					AppLocal.getIntString("button.externalvoucher.applypartial", Formats.CURRENCY.formatValue(value)));
		} else {
			apply.setText(AppLocal.getIntString("button.externalvoucher.addpartial"));
		}
		notifier.setStatus(valid, valid && !partial);
	}

	private static final class NumericFilter extends DocumentFilter {
		@Override
		public void insertString(FilterBypass bypass, int offset, String text, AttributeSet attributes)
				throws BadLocationException {
			if (text != null) {
				bypass.insertString(offset, numericCharacters(text), attributes);
			}
		}

		@Override
		public void replace(FilterBypass bypass, int offset, int length, String text, AttributeSet attributes)
				throws BadLocationException {
			bypass.replace(offset, length, text == null ? null : numericCharacters(text), attributes);
		}

		private String numericCharacters(String text) {
			StringBuilder result = new StringBuilder();
			for (int i = 0; i < text.length(); i++) {
				char ch = text.charAt(i);
				if (Character.isDigit(ch) || ch == '.' || ch == ',' || ch == '-') {
					result.append(ch);
				}
			}
			return result.toString();
		}
	}
}
