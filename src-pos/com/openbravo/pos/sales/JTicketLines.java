//    Openbravo POS is a point of sales application designed for touch screens.
//    Copyright (C) 2008-2009 Openbravo, S.L.
//    http://www.openbravo.com/product/pos
//
//    This file is part of Openbravo POS.
//
//    Openbravo POS is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    Openbravo POS is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with Openbravo POS.  If not, see <http://www.gnu.org/licenses/>.

package com.openbravo.pos.sales;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.util.ArrayList;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.ticket.TicketLineInfo;
import com.openbravo.pos.theme.RetailPOSColors;
import com.openbravo.pos.theme.RetailPOSTheme;
import com.openbravo.pos.util.StringUtils;

public class JTicketLines extends javax.swing.JPanel {

	private static final ColumnTicket[] COLUMNS = {new ColumnTicket("label.item", 470, javax.swing.SwingConstants.LEFT),
			new ColumnTicket("label.ticketline.price", 45, javax.swing.SwingConstants.RIGHT),
			new ColumnTicket("label.ticketline.units", 10, javax.swing.SwingConstants.RIGHT),
			new ColumnTicket("label.ticketline.tax", 10, javax.swing.SwingConstants.RIGHT),
			new ColumnTicket("label.ticketline.value", 45, javax.swing.SwingConstants.RIGHT)};
	private TicketTableModel m_jTableModel;

	/** Creates new form JLinesTicket */
	public JTicketLines() {

		initComponents();

		m_jTableModel = new TicketTableModel(COLUMNS);
		m_jTicketTable.setModel(m_jTableModel);

		// m_jTicketTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		TableColumnModel jColumns = m_jTicketTable.getColumnModel();
		for (int i = 0; i < COLUMNS.length; i++) {
			jColumns.getColumn(i).setPreferredWidth(COLUMNS[i].width);
			jColumns.getColumn(i).setResizable(false);
			jColumns.getColumn(i).setHeaderRenderer(headerRenderer(COLUMNS[i].align));
			jColumns.getColumn(i)
					.setCellRenderer(i == 0 ? new TicketItemRenderer() : alignedRenderer(COLUMNS[i].align));
		}

		m_jScrollTableTicket.getVerticalScrollBar().setPreferredSize(new Dimension(35, 35));

		m_jTicketTable.getTableHeader().setReorderingAllowed(false);
		m_jTicketTable.getTableHeader().setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f));
		m_jTicketTable.setFont(RetailPOSTheme.PLEX_MONO_REGULAR.deriveFont(16f));
		m_jTicketTable.setSelectionBackground(RetailPOSColors.brandSubtle());
		m_jTicketTable.setSelectionForeground(RetailPOSColors.ink());
		m_jTicketTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		m_jTicketTable.setRowHeight(35);
		m_jTicketTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// reseteo la tabla...
		m_jTableModel.clear();
	}

	public void addListSelectionListener(ListSelectionListener l) {
		m_jTicketTable.getSelectionModel().addListSelectionListener(l);
	}
	public void removeListSelectionListener(ListSelectionListener l) {
		m_jTicketTable.getSelectionModel().removeListSelectionListener(l);
	}

	public void clearTicketLines() {
		m_jTableModel.clear();
	}

	public void setTicketLine(int index, TicketLineInfo oLine) {

		m_jTableModel.setRow(index, oLine);
	}

	public void addTicketLine(TicketLineInfo oLine) {

		m_jTableModel.addRow(oLine);

		// Selecciono la que acabamos de anadir.
		setSelectedIndex(m_jTableModel.getRowCount() - 1);
	}

	public void insertTicketLine(int index, TicketLineInfo oLine) {

		m_jTableModel.insertRow(index, oLine);

		// Selecciono la que acabamos de anadir.
		setSelectedIndex(index);
	}
	public void removeTicketLine(int i) {

		m_jTableModel.removeRow(i);

		// Escojo una a seleccionar
		if (i >= m_jTableModel.getRowCount()) {
			i = m_jTableModel.getRowCount() - 1;
		}

		if ((i >= 0) && (i < m_jTableModel.getRowCount())) {
			// Solo seleccionamos si podemos.
			setSelectedIndex(i);
		}
	}

	public void setSelectedIndex(int i) {

		// Seleccionamos
		m_jTicketTable.getSelectionModel().setSelectionInterval(i, i);

		// Hacemos visible la seleccion.
		Rectangle oRect = m_jTicketTable.getCellRect(i, 0, true);
		m_jTicketTable.scrollRectToVisible(oRect);
	}

	public int getSelectedIndex() {
		return m_jTicketTable.getSelectionModel().getMinSelectionIndex(); // solo sera uno, luego no importa...
	}

	public void selectionDown() {

		int i = m_jTicketTable.getSelectionModel().getMaxSelectionIndex();
		if (i < 0) {
			i = 0; // No hay ninguna seleccionada
		} else {
			i++;
			if (i >= m_jTableModel.getRowCount()) {
				i = m_jTableModel.getRowCount() - 1;
			}
		}

		if ((i >= 0) && (i < m_jTableModel.getRowCount())) {
			// Solo seleccionamos si podemos.

			setSelectedIndex(i);
		}
	}

	public void selectionUp() {

		int i = m_jTicketTable.getSelectionModel().getMinSelectionIndex();
		if (i < 0) {
			i = m_jTableModel.getRowCount() - 1; // No hay ninguna seleccionada
		} else {
			i--;
			if (i < 0) {
				i = 0;
			}
		}

		if ((i >= 0) && (i < m_jTableModel.getRowCount())) {
			// Solo seleccionamos si podemos.
			setSelectedIndex(i);
		}
	}

	private static DefaultTableCellRenderer alignedRenderer(int alignment) {
		DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
		renderer.setHorizontalAlignment(alignment);
		return renderer;
	}

	private static DefaultTableCellRenderer headerRenderer(int alignment) {
		return new TicketHeaderRenderer(alignment);
	}

	private static class TicketItemRenderer extends DefaultTableCellRenderer {

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {

			JLabel aux = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			aux.setVerticalAlignment(javax.swing.SwingConstants.CENTER);
			aux.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
			if (column == 0 && table.getModel() instanceof TicketTableModel) {
				TicketLineInfo line = ((TicketTableModel) table.getModel()).getLine(row);
				aux.setText(itemText(line, aux.getFontMetrics(aux.getFont()),
						table.getColumnModel().getColumn(column).getWidth()));
				aux.setToolTipText(line.getProductName());
			} else {
				aux.setToolTipText(null);
			}
			return aux;
		}

		private String itemText(TicketLineInfo line, FontMetrics metrics, int columnWidth) {
			String name = line.getProductName() == null ? "" : line.getProductName();
			String prefix = line.isProductCom() ? "*  " : "";
			int availableWidth = Math.max(0, columnWidth - 8);
			if (metrics.stringWidth(prefix + name) <= availableWidth) {
				return itemHtml(name, line.isProductCom());
			}

			String suffix = "...";
			int end = name.length();
			while (end > 0 && metrics.stringWidth(prefix + name.substring(0, end) + suffix) > availableWidth) {
				end--;
			}
			return itemHtml(name.substring(0, end) + suffix, line.isProductCom());
		}

		private String itemHtml(String name, boolean composite) {
			String text = StringUtils.encodeXML(name);
			return composite ? "<html><i>*&nbsp;&nbsp;" + text + "</i>" : "<html>" + text;
		}
	}

	private static class TicketHeaderRenderer extends DefaultTableCellRenderer {

		private int alignment;

		private TicketHeaderRenderer(int alignment) {
			this.alignment = alignment;
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {

			JLabel header = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
					column);
			header.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(16f));
			header.setHorizontalAlignment(alignment);
			return header;
		}
	}

	private static class TicketTableModel extends AbstractTableModel {

		// private AppView m_App;
		private ColumnTicket[] m_acolumns;
		private ArrayList<TicketLineInfo> m_rows = new ArrayList<TicketLineInfo>();

		public TicketTableModel(ColumnTicket[] acolumns) {
			m_acolumns = acolumns;
		}
		public int getRowCount() {
			return m_rows.size();
		}
		public int getColumnCount() {
			return m_acolumns.length;
		}
		@Override
		public String getColumnName(int column) {
			return AppLocal.getIntString(m_acolumns[column].name);
			// return m_acolumns[column].name;
		}
		public Object getValueAt(int row, int column) {
			return valueForLine(m_rows.get(row), column);
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			return false;
		}

		public void clear() {
			int old = getRowCount();
			if (old > 0) {
				m_rows.clear();
				fireTableRowsDeleted(0, old - 1);
			}
		}

		public void setRow(int index, TicketLineInfo oLine) {

			m_rows.set(index, oLine);
			for (int i = 0; i < m_acolumns.length; i++) {
				fireTableCellUpdated(index, i);
			}
		}

		public void addRow(TicketLineInfo oLine) {

			insertRow(m_rows.size(), oLine);
		}

		public void insertRow(int index, TicketLineInfo oLine) {

			m_rows.add(index, oLine);
			fireTableRowsInserted(index, index);
		}

		public void removeRow(int row) {
			m_rows.remove(row);
			fireTableRowsDeleted(row, row);
		}

		private TicketLineInfo getLine(int row) {
			return m_rows.get(row);
		}

		private String valueForLine(TicketLineInfo line, int column) {
			switch (column) {
				case 0 :
					return line.getProductName();
				case 1 :
					return line.printPriceTax();
				case 2 :
					return "x" + line.printMultiply();
				case 3 :
					return line.printTaxRate();
				case 4 :
					return line.printValue();
				default :
					return "";
			}
		}
	}

	private static class ColumnTicket {
		private String name;
		private int width;
		private int align;

		private ColumnTicket(String name, int width, int align) {
			this.name = name;
			this.width = width;
			this.align = align;
		}
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	// <editor-fold defaultstate="collapsed" desc=" Generated Code
	// ">//GEN-BEGIN:initComponents
	private void initComponents() {
		m_jScrollTableTicket = new javax.swing.JScrollPane();
		m_jTicketTable = new javax.swing.JTable();

		setLayout(new java.awt.BorderLayout());

		m_jScrollTableTicket.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		m_jScrollTableTicket.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		m_jTicketTable.setFocusable(false);
		m_jTicketTable.setIntercellSpacing(new java.awt.Dimension(0, 1));
		m_jTicketTable.setRequestFocusEnabled(false);
		m_jTicketTable.setShowVerticalLines(false);
		m_jScrollTableTicket.setViewportView(m_jTicketTable);

		add(m_jScrollTableTicket, java.awt.BorderLayout.CENTER);

	}// </editor-fold>//GEN-END:initComponents

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JScrollPane m_jScrollTableTicket;
	private javax.swing.JTable m_jTicketTable;
	// End of variables declaration//GEN-END:variables

}
