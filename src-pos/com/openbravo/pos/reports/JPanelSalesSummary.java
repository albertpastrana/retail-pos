package com.openbravo.pos.reports;

import com.openbravo.basic.BasicException;
import com.openbravo.beans.JCalendarDialog;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.format.Formats;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.AppView;
import com.openbravo.pos.forms.BeanFactoryApp;
import com.openbravo.pos.forms.BeanFactoryException;
import com.openbravo.pos.forms.JPanelView;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.Calendar;
import java.util.Date;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.ImageIcon;
import com.openbravo.pos.theme.RetailPOSColors;
import com.openbravo.pos.theme.RetailPOSTheme;

/** Simple, non-blocking view of sales totals for a selected period. */
public final class JPanelSalesSummary extends JPanel implements JPanelView, BeanFactoryApp {

	private AppView app;
	private final SalesSummaryRepository repository = new SalesSummaryRepository();
	private Date startDate;
	private Date endDate;
	private JTextField startField;
	private JTextField endField;
	private JButton load;
	private JLabel receipts;
	private JLabel gross;
	private JLabel refunds;
	private JLabel net;
	private JLabel tax;
	private JLabel average;

	public JPanelSalesSummary() {
		initComponents();
	}

	@Override
	public void init(AppView app) throws BeanFactoryException {
		this.app = app;
	}

	@Override
	public Object getBean() {
		return this;
	}

	@Override
	public javax.swing.JComponent getComponent() {
		return this;
	}

	@Override
	public String getTitle() {
		return AppLocal.getIntString("Menu.SalesSummary");
	}

	@Override
	public void activate() throws BasicException {
		loadSummary();
	}

	@Override
	public boolean deactivate() {
		return true;
	}

	private void initComponents() {
		Calendar today = Calendar.getInstance();
		endDate = startOfDay(today.getTime());
		Calendar yesterday = (Calendar) today.clone();
		yesterday.add(Calendar.DAY_OF_MONTH, -1);
		startDate = startOfDay(yesterday.getTime());

		load = new JButton(AppLocal.getIntString("Button.Load"));
		load.setFont(RetailPOSTheme.MANROPE_SEMIBOLD.deriveFont(15f));
		load.setPreferredSize(new Dimension(120, 48));
		RetailPOSColors.primaryButton(load);
		load.addActionListener(e -> loadSummary());

		JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT));
		filters.setBackground(RetailPOSColors.surface100());
		filters.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(RetailPOSColors.border()),
				BorderFactory.createEmptyBorder(16, 16, 16, 16)));
		JLabel heading = new JLabel(AppLocal.getIntString("Menu.SalesSummary"));
		heading.setFont(RetailPOSTheme.MANROPE_BOLD.deriveFont(20f));
		heading.setForeground(RetailPOSColors.ink());
		filters.add(heading);
		filters.add(new JLabel(AppLocal.getIntString("label.reportstart")));
		filters.add(createDatePicker(true));
		filters.add(new JLabel(AppLocal.getIntString("label.reportend")));
		filters.add(createDatePicker(false));
		filters.add(load);

		receipts = valueLabel();
		gross = valueLabel();
		refunds = valueLabel();
		net = valueLabel();
		tax = valueLabel();
		average = valueLabel();
		JPanel values = new JPanel(new GridLayout(3, 2, 16, 16));
		values.setOpaque(false);
		values.setBorder(BorderFactory.createEmptyBorder(24, 0, 24, 0));
		addValue(values, "label.reportreceipts", receipts, RetailPOSColors.ink());
		addValue(values, "label.reportgross", gross, RetailPOSColors.ink());
		addValue(values, "label.reportrefunds", refunds, RetailPOSColors.danger());
		addValue(values, "label.reportnet", net, RetailPOSColors.success());
		addValue(values, "label.reporttax", tax, RetailPOSColors.ink());
		addValue(values, "label.reportaverage", average, RetailPOSColors.ink());

		setLayout(new BorderLayout());
		setBackground(RetailPOSColors.surface0());
		add(filters, BorderLayout.NORTH);
		JPanel content = new JPanel(new BorderLayout());
		content.setOpaque(false);
		content.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 24));
		content.add(values, BorderLayout.NORTH);
		add(content, BorderLayout.CENTER);
	}

	private JLabel valueLabel() {
		JLabel label = new JLabel("-");
		label.setHorizontalAlignment(JLabel.RIGHT);
		return label;
	}

	private void addValue(JPanel panel, String key, JLabel value, Color valueColor) {
		JPanel card = new JPanel(new BorderLayout(8, 8));
		card.setBackground(RetailPOSColors.surface100());
		card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(RetailPOSColors.border()),
				BorderFactory.createEmptyBorder(16, 18, 16, 18)));
		JLabel caption = new JLabel(AppLocal.getIntString(key));
		caption.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f));
		caption.setForeground(RetailPOSColors.inkMuted());
		value.setFont(RetailPOSTheme.PLEX_MONO_SEMIBOLD.deriveFont(24f));
		value.setForeground(valueColor);
		card.add(caption, BorderLayout.NORTH);
		card.add(value, BorderLayout.CENTER);
		panel.add(card);
	}

	private void loadSummary() {
		final Date selectedStart = startOfDay(startDate);
		final Date selectedEnd = nextDay(startOfDay(endDate));
		if (!selectedStart.before(selectedEnd)) {
			new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.reportperiod")).show(this);
			return;
		}
		load.setEnabled(false);
		new SwingWorker<SalesSummary, Void>() {
			@Override
			protected SalesSummary doInBackground() throws Exception {
				return repository.load(app.getSession().getConnection(),
						new SalesSummaryParameters(selectedStart, selectedEnd));
			}

			@Override
			protected void done() {
				try {
					showSummary(get());
				} catch (Exception e) {
					new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotloadreport"), e)
							.show(JPanelSalesSummary.this);
				} finally {
					load.setEnabled(true);
				}
			}
		}.execute();
	}

	private JPanel createDatePicker(final boolean isStart) {
		final JTextField field = new JTextField(10);
		field.setEditable(false);
		field.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(15f));
		field.setPreferredSize(new Dimension(112, 48));
		field.setText(Formats.DATE.formatValue(isStart ? startDate : endDate));
		if (isStart) {
			startField = field;
		} else {
			endField = field;
		}

		JButton calendar = new JButton(new ImageIcon(getClass().getResource("/com/openbravo/images/date.png")));
		calendar.setToolTipText(AppLocal.getIntString("button.selectdate"));
		calendar.setPreferredSize(new Dimension(48, 48));
		calendar.setFocusPainted(false);
		calendar.addActionListener(e -> {
			Date selected = JCalendarDialog.showCalendar(JPanelSalesSummary.this, isStart ? startDate : endDate);
			if (selected != null) {
				selected = startOfDay(selected);
				if (isStart) {
					startDate = selected;
					startField.setText(Formats.DATE.formatValue(startDate));
				} else {
					endDate = selected;
					endField.setText(Formats.DATE.formatValue(endDate));
				}
			}
		});

		JPanel picker = new JPanel(new BorderLayout(4, 0));
		picker.setOpaque(false);
		picker.add(field, BorderLayout.CENTER);
		picker.add(calendar, BorderLayout.LINE_END);
		return picker;
	}

	private void showSummary(SalesSummary summary) {
		receipts.setText(Integer.toString(summary.getReceiptCount()));
		gross.setText(Formats.CURRENCY.formatValue(summary.getGrossSales()));
		refunds.setText(Formats.CURRENCY.formatValue(summary.getRefunds()));
		net.setText(Formats.CURRENCY.formatValue(summary.getNetSales()));
		tax.setText(Formats.CURRENCY.formatValue(summary.getTax()));
		average.setText(Formats.CURRENCY.formatValue(summary.getAverageReceipt()));
	}

	private static Date startOfDay(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTime();
	}

	private static Date nextDay(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.DAY_OF_MONTH, 1);
		return calendar.getTime();
	}
}
