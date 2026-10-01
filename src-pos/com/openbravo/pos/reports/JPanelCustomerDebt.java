package com.openbravo.pos.reports;

import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.format.Formats;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.AppView;
import com.openbravo.pos.forms.BeanFactoryApp;
import com.openbravo.pos.forms.BeanFactoryException;
import com.openbravo.pos.forms.JPanelView;
import com.openbravo.pos.theme.RetailPOSColors;
import com.openbravo.pos.theme.RetailPOSTheme;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;

/** Non-blocking table of customer debt and available credit. */
public final class JPanelCustomerDebt extends JPanel implements JPanelView, BeanFactoryApp {

	private AppView app;
	private final CustomerDebtRepository repository = new CustomerDebtRepository();
	private JButton refresh;
	private JTable table;

	public JPanelCustomerDebt() {
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
		return AppLocal.getIntString("Menu.CustomerDebtSummary");
	}

	@Override
	public void activate() throws BasicException {
		loadRows();
	}

	@Override
	public boolean deactivate() {
		return true;
	}

	private void initComponents() {
		JPanel header = new JPanel(new BorderLayout(16, 0));
		header.setBackground(RetailPOSColors.surface100());
		header.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(RetailPOSColors.border()),
				BorderFactory.createEmptyBorder(16, 16, 16, 16)));
		refresh = new JButton(AppLocal.getIntString("Button.Load"));
		refresh.setFont(RetailPOSTheme.MANROPE_SEMIBOLD.deriveFont(15f));
		refresh.setPreferredSize(new Dimension(130, 48));
		RetailPOSColors.primaryButton(refresh);
		refresh.addActionListener(e -> loadRows());
		header.add(refresh, BorderLayout.LINE_END);

		table = new JTable(new CustomerDebtTableModel());
		table.setRowHeight(32);
		table.setFillsViewportHeight(true);
		table.setAutoCreateRowSorter(true);
		table.getTableHeader().setReorderingAllowed(false);
		table.getTableHeader().setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f));
		table.setFont(RetailPOSTheme.PLEX_MONO_REGULAR.deriveFont(15f));

		setLayout(new BorderLayout(0, 16));
		setBackground(RetailPOSColors.surface0());
		setBorder(BorderFactory.createEmptyBorder(0, 24, 24, 24));
		add(header, BorderLayout.NORTH);
		add(new JScrollPane(table), BorderLayout.CENTER);
	}

	private void loadRows() {
		refresh.setEnabled(false);
		final long started = ReportLog.start("customer_debt");
		new SwingWorker<List<CustomerDebtRow>, Void>() {
			@Override
			protected List<CustomerDebtRow> doInBackground() throws Exception {
				return ReportLog.inOperation(() -> repository.load(app.getSession().getConnection()));
			}

			@Override
			protected void done() {
				try {
					List<CustomerDebtRow> rows = get();
					((CustomerDebtTableModel) table.getModel()).setRows(rows);
					ReportLog.success("customer_debt", rows.size(), started);
				} catch (Exception e) {
					ReportLog.failure("customer_debt", started, e);
					new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotloadreport"), e)
							.show(JPanelCustomerDebt.this);
				} finally {
					refresh.setEnabled(true);
				}
			}
		}.execute();
	}

	private static final class CustomerDebtTableModel extends AbstractTableModel {

		private static final long serialVersionUID = 1L;
		private final String[] columns = {"label.reportsearchkey", "label.reportcustomer", "label.reportcurrentdebt",
				"label.reportdebtlimit", "label.reportcreditavailable"};
		private List<CustomerDebtRow> rows = java.util.Collections.emptyList();

		void setRows(List<CustomerDebtRow> rows) {
			this.rows = rows;
			fireTableDataChanged();
		}

		@Override
		public String getColumnName(int column) {
			return AppLocal.getIntString(columns[column]);
		}

		@Override
		public int getRowCount() {
			return rows.size();
		}

		@Override
		public int getColumnCount() {
			return columns.length;
		}

		@Override
		public Object getValueAt(int row, int column) {
			CustomerDebtRow value = rows.get(row);
			switch (column) {
				case 0 :
					return value.getSearchKey();
				case 1 :
					return value.getCustomerName();
				case 2 :
					return Formats.CURRENCY.formatValue(value.getCurrentDebt());
				case 3 :
					return Formats.CURRENCY.formatValue(value.getMaximumDebt());
				case 4 :
					return Formats.CURRENCY.formatValue(value.getRemainingCredit());
				default :
					throw new IllegalArgumentException("Unknown customer debt column " + column);
			}
		}
	}
}
