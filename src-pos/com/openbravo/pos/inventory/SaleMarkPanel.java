package com.openbravo.pos.inventory;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;

import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.format.Formats;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.AppView;
import com.openbravo.pos.forms.BeanFactoryApp;
import com.openbravo.pos.forms.BeanFactoryException;
import com.openbravo.pos.forms.DataLogicSales;
import com.openbravo.pos.forms.JPanelView;
import com.openbravo.pos.ticket.LineDiscount;
import com.openbravo.pos.ticket.ProductInfoExt;

public class SaleMarkPanel extends JPanel implements JPanelView, BeanFactoryApp {

	private DataLogicSales dlSales;
	private SaleService sales;
	private final Map<String, ProductInfoExt> pending = new LinkedHashMap<String, ProductInfoExt>();
	private final PendingModel model = new PendingModel();
	private final JTable table = new JTable(model);
	private final JTextField scan = new JTextField();
	private final JTextField percent = new JTextField("20", 4);
	private final JLabel status = new JLabel(" ");

	public SaleMarkPanel() {
		setLayout(new BorderLayout(8, 8));

		JPanel north = new JPanel(new BorderLayout(8, 8));
		JPanel fields = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 6));
		fields.add(new JLabel(AppLocal.getIntString("label.sale.percent")));
		percent.setFont(percent.getFont().deriveFont(Font.BOLD, 18f));
		fields.add(percent);
		fields.add(new JLabel("%"));
		north.add(fields, BorderLayout.NORTH);

		JPanel scanRow = new JPanel(new BorderLayout(8, 6));
		scanRow.add(new JLabel(AppLocal.getIntString("label.sale.scan")), BorderLayout.WEST);
		scan.setFont(scan.getFont().deriveFont(Font.PLAIN, 22f));
		scan.addActionListener(this::scanEntered);
		scanRow.add(scan, BorderLayout.CENTER);
		north.add(scanRow, BorderLayout.CENTER);
		north.add(status, BorderLayout.SOUTH);
		add(north, BorderLayout.NORTH);

		table.setRowHeight(28);
		table.setFillsViewportHeight(true);
		add(new JScrollPane(table), BorderLayout.CENTER);

		JPanel south = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 6));
		JButton apply = new JButton(AppLocal.getIntString("button.sale.apply"));
		JButton clear = new JButton(AppLocal.getIntString("button.sale.clear"));
		JButton load = new JButton(AppLocal.getIntString("button.sale.load"));
		JButton remove = new JButton(AppLocal.getIntString("button.sale.remove"));
		apply.addActionListener(this::applySale);
		clear.addActionListener(this::clearSale);
		load.addActionListener(this::loadOnSale);
		remove.addActionListener(this::removeSelected);
		south.add(apply);
		south.add(clear);
		south.add(load);
		south.add(remove);
		add(south, BorderLayout.SOUTH);
	}

	@Override
	public void init(AppView app) throws BeanFactoryException {
		dlSales = (DataLogicSales) app.getBean("com.openbravo.pos.forms.DataLogicSales");
		sales = new SaleService(app.getSession());
	}

	@Override
	public Object getBean() {
		return this;
	}

	@Override
	public String getTitle() {
		return AppLocal.getIntString("Menu.SaleMark");
	}

	@Override
	public void activate() throws BasicException {
		pending.clear();
		model.fireTableDataChanged();
		status.setText(" ");
		scan.setText("");
		scan.requestFocusInWindow();
	}

	@Override
	public boolean deactivate() {
		return true;
	}

	@Override
	public JComponent getComponent() {
		return this;
	}

	private void scanEntered(ActionEvent event) {
		String code = scan.getText().trim();
		scan.setText("");
		scan.requestFocusInWindow();
		if (code.length() == 0) {
			return;
		}
		try {
			ProductInfoExt product = dlSales.getProductInfoByCode(code);
			if (product == null) {
				Toolkit.getDefaultToolkit().beep();
				status.setText(AppLocal.getIntString("message.noproduct", code));
				return;
			}
			if (pending.containsKey(product.getID())) {
				status.setText(AppLocal.getIntString("message.sale.duplicate", product.getName()));
				return;
			}
			pending.put(product.getID(), product);
			model.fireTableDataChanged();
			status.setText(product.getName());
		} catch (BasicException e) {
			new MessageInf(e).show(this);
		}
	}

	private void applySale(ActionEvent event) {
		Double value = readPercent();
		if (value == null || pending.isEmpty()) {
			return;
		}
		int ok = JOptionPane.showConfirmDialog(this,
				AppLocal.getIntString("message.sale.applyconfirm", new Object[]{new Integer(pending.size()), value}),
				AppLocal.getIntString("Menu.SaleMark"), JOptionPane.YES_NO_OPTION);
		if (ok != JOptionPane.YES_OPTION) {
			return;
		}
		try {
			sales.setSalePercent(pending.keySet(), value);
			status.setText(
					AppLocal.getIntString("message.sale.applied", new Object[]{new Integer(pending.size()), value}));
			pending.clear();
			model.fireTableDataChanged();
		} catch (SQLException e) {
			new MessageInf(e).show(this);
		}
	}

	private void clearSale(ActionEvent event) {
		if (pending.isEmpty()) {
			return;
		}
		int ok = JOptionPane.showConfirmDialog(this,
				AppLocal.getIntString("message.sale.clearconfirm", new Integer(pending.size())),
				AppLocal.getIntString("Menu.SaleMark"), JOptionPane.YES_NO_OPTION);
		if (ok != JOptionPane.YES_OPTION) {
			return;
		}
		try {
			sales.setSalePercent(pending.keySet(), null);
			status.setText(AppLocal.getIntString("message.sale.cleared", new Integer(pending.size())));
			pending.clear();
			model.fireTableDataChanged();
		} catch (SQLException e) {
			new MessageInf(e).show(this);
		}
	}

	private void loadOnSale(ActionEvent event) {
		try {
			List<ProductInfoExt> onSale = sales.findOnSale();
			pending.clear();
			for (ProductInfoExt product : onSale) {
				pending.put(product.getID(), product);
			}
			model.fireTableDataChanged();
			status.setText(AppLocal.getIntString("message.sale.loaded", new Integer(onSale.size())));
			scan.requestFocusInWindow();
		} catch (BasicException e) {
			new MessageInf(e).show(this);
		}
	}

	private void removeSelected(ActionEvent event) {
		int[] rows = table.getSelectedRows();
		List<ProductInfoExt> values = new ArrayList<ProductInfoExt>(pending.values());
		for (int i = rows.length - 1; i >= 0; i--) {
			pending.remove(values.get(rows[i]).getID());
		}
		model.fireTableDataChanged();
		scan.requestFocusInWindow();
	}

	private Double readPercent() {
		try {
			double value = Double.parseDouble(percent.getText().trim().replace(',', '.'));
			if (value > 0.0 && value <= 100.0) {
				return new Double(value);
			}
		} catch (NumberFormatException ignored) {
		}
		Toolkit.getDefaultToolkit().beep();
		new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.invaliddiscount")).show(this);
		return null;
	}

	private class PendingModel extends AbstractTableModel {

		@Override
		public int getRowCount() {
			return pending.size();
		}

		@Override
		public int getColumnCount() {
			return 4;
		}

		@Override
		public String getColumnName(int column) {
			switch (column) {
				case 0 :
					return AppLocal.getIntString("label.code");
				case 1 :
					return AppLocal.getIntString("label.prodname");
				case 2 :
					return AppLocal.getIntString("label.prodpricesell");
				default :
					return AppLocal.getIntString("label.sale.now");
			}
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			ProductInfoExt product = new ArrayList<ProductInfoExt>(pending.values()).get(rowIndex);
			double list = product.getPriceSell();
			double previewPercent = product.getSalePercent() > 0.0 ? product.getSalePercent() : previewPercent();
			switch (columnIndex) {
				case 0 :
					return product.getCode();
				case 1 :
					return product.getName();
				case 2 :
					return Formats.CURRENCY.formatValue(new Double(list));
				default :
					return previewPercent <= 0.0
							? Formats.CURRENCY.formatValue(new Double(list))
							: Formats.CURRENCY.formatValue(new Double(list * (1.0 - previewPercent / 100.0)));
			}
		}

		private double previewPercent() {
			try {
				return Double.parseDouble(percent.getText().trim().replace(',', '.'));
			} catch (NumberFormatException e) {
				return 20.0;
			}
		}
	}
}
