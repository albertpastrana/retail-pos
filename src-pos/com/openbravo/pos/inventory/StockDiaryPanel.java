//    Openbravo POS is a point of sales application designed for touch screens.
//    Copyright (C) 2007-2009 Openbravo, S.L.
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

package com.openbravo.pos.inventory;

import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.data.user.EditorRecord;
import com.openbravo.data.user.BrowsableEditableData;
import com.openbravo.data.user.ListProviderCreator;
import com.openbravo.data.user.SaveProvider;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.DataLogicSales;
import com.openbravo.pos.panels.JPanelTable2;
import com.openbravo.pos.theme.RetailPOSColors;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JPanel;

/**
 *
 * @author adrianromero
 */
public class StockDiaryPanel extends JPanelTable2 {

	private StockDiaryEditor jeditor;
	private StockDiaryFilter filter;
	private DataLogicSales m_dlSales;

	/** Creates a new instance of JPanelDiaryEditor */
	public StockDiaryPanel() {
	}

	protected void init() {
		m_dlSales = app.getBean(DataLogicSales.class);
		row = m_dlSales.getStockDiaryRow();
		filter = new StockDiaryFilter();
		filter.init(app);
		filter.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (bd == null) {
					return;
				}
				try {
					bd.actionLoad();
				} catch (BasicException ex) {
					new MessageInf(MessageInf.SGN_NOTICE, AppLocal.getIntString("message.cannotloadlists"), ex)
							.show(StockDiaryPanel.this);
				}
			}
		});
		lpr = new ListProviderCreator(m_dlSales.getStockDiaryList(), filter);
		spr = new SaveProvider(null, m_dlSales.getStockDiaryInsert(), m_dlSales.getStockDiaryDelete());
		jeditor = new StockDiaryEditor(app, dirty);
	}

	@Override
	public Component getFilter() {
		JPanel header = new JPanel(new BorderLayout(0, 6));
		JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		JButton newMovement = new JButton("+ " + AppLocal.getIntString("button.stocknewmovement"));
		RetailPOSColors.primaryButton(newMovement);
		newMovement.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					bd.actionInsert();
				} catch (BasicException ex) {
					new com.openbravo.data.gui.MessageInf(ex).show(StockDiaryPanel.this);
				}
			}
		});
		actionBar.add(newMovement);
		header.add(actionBar, BorderLayout.NORTH);
		header.add(filter, BorderLayout.CENTER);
		return header;
	}

	@Override
	protected boolean showToolbar() {
		return false;
	}

	@Override
	protected double getSplitResizeWeight(boolean editorKeepsSize) {
		return 1.0 / 3.0;
	}

	@Override
	protected double getSplitDividerLocation() {
		return 1.0 / 3.0;
	}

	@Override
	protected Component getListComponent(BrowsableEditableData data) {
		return new StockDiaryTableNavigator(data);
	}

	public EditorRecord getEditor() {
		return jeditor;
	}

	public String getTitle() {
		return AppLocal.getIntString("Menu.StockDiary");
	}

	public void activate() throws BasicException {
		filter.init(app);
		jeditor.activate(); // primero activo el editor
		super.activate(); // segundo activo el padre
	}
}
