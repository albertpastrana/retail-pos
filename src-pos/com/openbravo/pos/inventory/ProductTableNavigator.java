package com.openbravo.pos.inventory;

import java.awt.BorderLayout;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.JScrollPane;
import javax.swing.table.AbstractTableModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.data.loader.LocalRes;
import com.openbravo.data.user.BrowsableEditableData;
import com.openbravo.data.user.BrowseListener;

final class ProductTableNavigator extends javax.swing.JPanel implements BrowseListener, ListSelectionListener {
	private final BrowsableEditableData data;
	private final JTable table;
	private final ProductTableModel model;
	private boolean updating;

	ProductTableNavigator(BrowsableEditableData data) {
		this.data = data;
		this.model = new ProductTableModel(data);
		this.table = new JTable(model);
		this.table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		this.table.setAutoCreateRowSorter(true);
		this.table.setFillsViewportHeight(true);
		this.table.getColumnModel().getColumn(0).setPreferredWidth(90);
		this.table.getColumnModel().getColumn(1).setPreferredWidth(220);
		this.table.getSelectionModel().addListSelectionListener(this);
		data.addBrowseListener(this);
		data.getListModel().addListDataListener(new ListDataListener() {
			public void intervalAdded(ListDataEvent e) { model.fireTableDataChanged(); }
			public void intervalRemoved(ListDataEvent e) { model.fireTableDataChanged(); }
			public void contentsChanged(ListDataEvent e) { model.fireTableDataChanged(); }
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
		if (event.getValueIsAdjusting() || updating) return;
		int viewRow = table.getSelectedRow();
		if (viewRow < 0 || data.isAdjusting()) return;
		int modelRow = table.convertRowIndexToModel(viewRow);
		try {
			data.moveTo(modelRow);
			if (data.getIndex() != modelRow) updateIndex(data.getIndex(), data.getListModel().getSize());
		} catch (BasicException e) {
			new MessageInf(MessageInf.SGN_NOTICE, LocalRes.getIntString("message.nomove"), e).show(this);
		}
	}

	private static final class ProductTableModel extends AbstractTableModel {
		private final BrowsableEditableData data;

		ProductTableModel(BrowsableEditableData data) { this.data = data; }
		public int getRowCount() { return data.getListModel().getSize(); }
		public int getColumnCount() { return 2; }
		public String getColumnName(int column) { return column == 0 ? "Referència" : "Nom"; }
		public Object getValueAt(int row, int column) {
			Object[] product = (Object[]) data.getListModel().getElementAt(row);
			return product[column == 0 ? 1 : 3];
		}
	}
}
