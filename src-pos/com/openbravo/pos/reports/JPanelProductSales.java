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
import java.awt.Font;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;

/** Non-blocking table of product sales for a selected period. */
public final class JPanelProductSales extends JPanel implements JPanelView, BeanFactoryApp {

	private AppView app;
	private final ProductSalesRepository repository = new ProductSalesRepository();
	private ReportPeriodSelector period;
	private JButton load;
	private JTable table;

	public JPanelProductSales() {
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
		return AppLocal.getIntString("Menu.ProductSalesSummary");
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
		period = new ReportPeriodSelector("Menu.ProductSalesSummary");
		load = new JButton(AppLocal.getIntString("Button.Load"));
		load.setFont(RetailPOSTheme.MANROPE_SEMIBOLD.deriveFont(15f));
		load.setPreferredSize(new Dimension(120, 48));
		RetailPOSColors.primaryButton(load);
		load.addActionListener(e -> loadRows());
		period.add(load);

		table = new JTable(new ProductSalesTableModel());
		table.setRowHeight(32);
		table.setFillsViewportHeight(true);
		table.setAutoCreateRowSorter(true);
		table.getTableHeader().setReorderingAllowed(false);
		table.getTableHeader().setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f));
		table.setFont(RetailPOSTheme.PLEX_MONO_REGULAR.deriveFont(15f));
		table.getColumnModel().getColumn(0).setPreferredWidth(130);
		table.getColumnModel().getColumn(1).setPreferredWidth(260);
		table.getColumnModel().getColumn(2).setPreferredWidth(180);
		table.getColumnModel().getColumn(3).setPreferredWidth(120);
		table.getColumnModel().getColumn(4).setPreferredWidth(150);

		setLayout(new BorderLayout(0, 16));
		setBackground(RetailPOSColors.surface0());
		setBorder(BorderFactory.createEmptyBorder(0, 24, 24, 24));
		add(period, BorderLayout.NORTH);
		add(new JScrollPane(table), BorderLayout.CENTER);
	}

	private void loadRows() {
		load.setEnabled(false);
		new SwingWorker<List<ProductSalesRow>, Void>() {
			@Override
			protected List<ProductSalesRow> doInBackground() throws Exception {
				return repository.load(app.getSession().getConnection(), period.getParameters());
			}

			@Override
			protected void done() {
				try {
					((ProductSalesTableModel) table.getModel()).setRows(get());
				} catch (Exception e) {
					new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotloadreport"), e)
							.show(JPanelProductSales.this);
				} finally {
					load.setEnabled(true);
				}
			}
		}.execute();
	}

	private static final class ProductSalesTableModel extends AbstractTableModel {

		private static final long serialVersionUID = 1L;
		private final String[] columns = {"label.reportreference", "label.reportproduct", "label.reportcategory",
				"label.reportunits", "label.reportamount"};
		private List<ProductSalesRow> rows = java.util.Collections.emptyList();

		void setRows(List<ProductSalesRow> rows) {
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
			ProductSalesRow value = rows.get(row);
			switch (column) {
				case 0 :
					return value.getReference();
				case 1 :
					return value.getProductName();
				case 2 :
					return value.getCategoryName();
				case 3 :
					return Formats.DOUBLE.formatValue(value.getUnits());
				case 4 :
					return Formats.CURRENCY.formatValue(value.getAmount());
				default :
					throw new IllegalArgumentException("Unknown product sales column " + column);
			}
		}
	}
}
