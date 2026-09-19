package com.openbravo.pos.reports;

import com.openbravo.basic.BasicException;
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
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import com.openbravo.pos.theme.RetailPOSColors;
import com.openbravo.pos.theme.RetailPOSTheme;

/** Simple, non-blocking view of sales totals for a selected period. */
public final class JPanelSalesSummary extends JPanel implements JPanelView, BeanFactoryApp {

	private AppView app;
	private final SalesSummaryRepository repository = new SalesSummaryRepository();
	private ReportPeriodSelector period;
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
		period = new ReportPeriodSelector("Menu.SalesSummary");
		load = new JButton(AppLocal.getIntString("Button.Load"));
		load.setFont(RetailPOSTheme.MANROPE_SEMIBOLD.deriveFont(15f));
		load.setPreferredSize(new Dimension(120, 48));
		RetailPOSColors.primaryButton(load);
		load.addActionListener(e -> loadSummary());
		period.add(load);

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
		add(period, BorderLayout.NORTH);
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
		load.setEnabled(false);
		new SwingWorker<SalesSummary, Void>() {
			@Override
			protected SalesSummary doInBackground() throws Exception {
				return repository.load(app.getSession().getConnection(), period.getParameters());
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

	private void showSummary(SalesSummary summary) {
		receipts.setText(Integer.toString(summary.getReceiptCount()));
		gross.setText(Formats.CURRENCY.formatValue(summary.getGrossSales()));
		refunds.setText(Formats.CURRENCY.formatValue(summary.getRefunds()));
		net.setText(Formats.CURRENCY.formatValue(summary.getNetSales()));
		tax.setText(Formats.CURRENCY.formatValue(summary.getTax()));
		average.setText(Formats.CURRENCY.formatValue(summary.getAverageReceipt()));
	}

}
