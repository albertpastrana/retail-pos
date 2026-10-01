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

/** Non-blocking table of products below their configured safety stock. */
public final class JPanelLowStock extends javax.swing.JPanel implements JPanelView, BeanFactoryApp {

	private AppView app;
	private final LowStockRepository repository = new LowStockRepository();
	private JButton refresh;
	private JTable table;

	public JPanelLowStock() {
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
		return AppLocal.getIntString("Menu.LowStockSummary");
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

		table = new JTable(new LowStockTableModel());
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
		final long started = ReportLog.start("low_stock");
		new SwingWorker<List<LowStockRow>, Void>() {
			@Override
			protected List<LowStockRow> doInBackground() throws Exception {
				return ReportLog.inOperation(() -> repository.load(app.getSession().getConnection()));
			}

			@Override
			protected void done() {
				try {
					List<LowStockRow> rows = get();
					((LowStockTableModel) table.getModel()).setRows(rows);
					ReportLog.success("low_stock", rows.size(), started);
				} catch (Exception e) {
					ReportLog.failure("low_stock", started, e);
					new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotloadreport"), e)
							.show(JPanelLowStock.this);
				} finally {
					refresh.setEnabled(true);
				}
			}
		}.execute();
	}

	private static final class LowStockTableModel extends AbstractTableModel {

		private static final long serialVersionUID = 1L;
		private final String[] columns = {"label.reportreference", "label.reportproduct", "label.reportcategory",
				"label.reportlocation", "label.reportcurrentstock", "label.reportminimumstock",
				"label.reportmaximumstock", "label.reporttoorder"};
		private List<LowStockRow> rows = java.util.Collections.emptyList();

		void setRows(List<LowStockRow> rows) {
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
			LowStockRow value = rows.get(row);
			switch (column) {
				case 0 :
					return value.getReference();
				case 1 :
					return value.getProductName();
				case 2 :
					return value.getCategoryName();
				case 3 :
					return value.getLocationName();
				case 4 :
					return Formats.DOUBLE.formatValue(value.getCurrentUnits());
				case 5 :
					return Formats.DOUBLE.formatValue(value.getMinimumUnits());
				case 6 :
					return Formats.DOUBLE.formatValue(value.getMaximumUnits());
				case 7 :
					return Formats.DOUBLE.formatValue(value.getUnitsToOrder());
				default :
					throw new IllegalArgumentException("Unknown low stock column " + column);
			}
		}
	}
}
