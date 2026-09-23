package com.openbravo.pos.inventory;

import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.format.Formats;
import com.openbravo.data.loader.LocalRes;
import com.openbravo.data.user.BrowseListener;
import com.openbravo.data.user.BrowsableEditableData;
import com.openbravo.pos.forms.AppLocal;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

final class StockDiaryTableNavigator extends JPanel implements BrowseListener, ListSelectionListener {
	private final BrowsableEditableData data;
	private final JTable table;
	private final StockDiaryTableModel model;
	private boolean updating;

	StockDiaryTableNavigator(BrowsableEditableData data) {
		this.data = data;
		model = new StockDiaryTableModel(data);
		table = new JTable(model);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setAutoCreateRowSorter(true);
		table.setFillsViewportHeight(true);
		table.getSelectionModel().addListSelectionListener(this);
		data.addBrowseListener(this);
		data.getListModel().addListDataListener(new ListDataListener() {
			public void intervalAdded(ListDataEvent e) {
				model.fireTableDataChanged();
			}
			public void intervalRemoved(ListDataEvent e) {
				model.fireTableDataChanged();
			}
			public void contentsChanged(ListDataEvent e) {
				model.fireTableDataChanged();
			}
		});
		setLayout(new BorderLayout());
		add(new JScrollPane(table), BorderLayout.CENTER);
	}

	public void updateIndex(int index, int count) {
		updating = true;
		try {
			table.clearSelection();
			if (index >= 0 && index < count) {
				int viewIndex = table.convertRowIndexToView(index);
				table.setRowSelectionInterval(viewIndex, viewIndex);
			}
		} finally {
			updating = false;
		}
	}

	public void valueChanged(ListSelectionEvent event) {
		if (event.getValueIsAdjusting() || updating)
			return;
		int viewRow = table.getSelectedRow();
		if (viewRow < 0 || data.isAdjusting())
			return;
		try {
			data.moveTo(table.convertRowIndexToModel(viewRow));
		} catch (BasicException e) {
			new MessageInf(MessageInf.SGN_NOTICE, LocalRes.getIntString("message.nomove"), e).show(this);
		}
	}

	private static final class StockDiaryTableModel extends AbstractTableModel {
		private final BrowsableEditableData data;
		StockDiaryTableModel(BrowsableEditableData data) {
			this.data = data;
		}
		public int getRowCount() {
			return data.getListModel().getSize();
		}
		public int getColumnCount() {
			return 6;
		}
		public String getColumnName(int column) {
			return new String[]{AppLocal.getIntString("label.stockdate"),
					AppLocal.getIntString("label.stockproduct"), AppLocal.getIntString("label.stockreason"),
					AppLocal.getIntString("label.units"), AppLocal.getIntString("label.warehouse"),
					AppLocal.getIntString("label.price")}[column];
		}
		public Object getValueAt(int row, int column) {
			Object[] movement = (Object[]) data.getListModel().getElementAt(row);
			switch (column) {
				case 0 :
					return Formats.TIMESTAMP.formatValue(movement[1]);
				case 1 :
					return movement[10];
				case 2 :
					return reason((Integer) movement[2]);
				case 3 :
					return Formats.DOUBLE.formatValue(movement[6]);
				case 4 :
					return movement[13];
				default :
					return Formats.CURRENCY.formatValue(movement[7]);
			}
		}
		private String reason(Integer key) {
			if (key == null)
				return "";
			switch (key.intValue()) {
				case 1 :
					return AppLocal.getIntString("stock.in.purchase");
				case 2 :
					return AppLocal.getIntString("stock.in.refund");
				case 4 :
					return AppLocal.getIntString("stock.in.movement");
				case -1 :
					return AppLocal.getIntString("stock.out.sale");
				case -2 :
					return AppLocal.getIntString("stock.out.refund");
				case -3 :
					return AppLocal.getIntString("stock.out.break");
				case -4 :
					return AppLocal.getIntString("stock.out.movement");
				default :
					return String.valueOf(key);
			}
		}
	}
}
