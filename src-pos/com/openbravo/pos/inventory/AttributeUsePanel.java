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

package com.openbravo.pos.inventory;

import com.openbravo.basic.BasicException;
import com.openbravo.data.loader.Datas;
import com.openbravo.data.model.Column;
import com.openbravo.data.model.Field;
import com.openbravo.data.model.PrimaryKey;
import com.openbravo.data.model.Row;
import com.openbravo.data.model.Table;
import com.openbravo.data.loader.PreparedSentence;
import com.openbravo.data.loader.SerializerWriteBasicExt;
import com.openbravo.data.user.EditorRecord;
import com.openbravo.format.Formats;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.panels.JPanelTable2;
import java.awt.FlowLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JPanel;

/**
 *
 * @author adrianromero
 */
public class AttributeUsePanel extends JPanelTable2 {

	private AttributeUseEditor editor;
	private AttributeSetFilter filter;

	public void init() {

		filter = new AttributeSetFilter();
		filter.init(app);
		filter.addActionListener(new ReloadActionListener());

		row = new Row(new Field("ID", Datas.STRING, Formats.STRING),
				new Field("ATRIBUTESET_ID", Datas.STRING, Formats.STRING),
				new Field("ATTRIBUTE_ID", Datas.STRING, Formats.STRING),
				new Field(AppLocal.getIntString("label.order"), Datas.INT, Formats.INT, false, true, true),
				new Field(AppLocal.getIntString("label.name"), Datas.STRING, Formats.STRING, true, true, true));

		Table table = new Table("ATTRIBUTEUSE", new PrimaryKey("ID"), new Column("ATTRIBUTESET_ID"),
				new Column("ATTRIBUTE_ID"), new Column("LINENO"));

		lpr = row.getListProvider(app.getSession(),
				"SELECT ATTUSE.ID, ATTUSE.ATTRIBUTESET_ID, ATTUSE.ATTRIBUTE_ID, ATTUSE.LINENO, ATT.NAME "
						+ "FROM ATTRIBUTEUSE ATTUSE, ATTRIBUTE ATT "
						+ "WHERE ATTUSE.ATTRIBUTE_ID = ATT.ID AND ATTUSE.ATTRIBUTESET_ID = ? ORDER BY LINENO",
				filter);
		spr = row.getSaveProvider(app.getSession(), table);

		editor = new AttributeUseEditor(app, dirty);
	}

	@Override
	public void activate() throws BasicException {
		filter.activate();
		editor.activate();

		// super.activate();
		startNavigation();
		reload();
	}

	@Override
	public Component getFilter() {
		return filter.getComponent();
	}

	@Override
	public Component getToolbarExtras() {
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 0));
		JButton up = new JButton(AppLocal.getIntString("button.moveup"));
		JButton down = new JButton(AppLocal.getIntString("button.movedown"));
		up.addActionListener(new MoveAction(-1));
		down.addActionListener(new MoveAction(1));
		buttons.add(up);
		buttons.add(down);
		return buttons;
	}

	public EditorRecord getEditor() {
		return editor;
	}

	private void reload() throws BasicException {

		String attsetid = (String) filter.createValue();
		editor.setInsertId(attsetid); // must be set before load
		bd.setEditable(attsetid != null);
		bd.actionLoad();
	}

	public String getTitle() {
		return AppLocal.getIntString("Menu.AttributeUse");
	}

	private class ReloadActionListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				reload();
			} catch (BasicException w) {
			}
		}
	}

	private class MoveAction implements ActionListener {
		private final int direction;

		MoveAction(int direction) {
			this.direction = direction;
		}

		@Override
		public void actionPerformed(ActionEvent event) {
			int index = bd.getIndex();
			int target = index + direction;
			if (index < 0 || target < 0 || target >= bd.getListModel().getSize()) {
				return;
			}
			try {
				bd.saveData();
				reorder(index, target);
				bd.actionLoad();
			} catch (BasicException exception) {
				new com.openbravo.data.gui.MessageInf(com.openbravo.data.gui.MessageInf.SGN_WARNING,
						AppLocal.getIntString("message.cannotmoveattribute"), exception).show(AttributeUsePanel.this);
			}
		}
	}

	private void reorder(int index, int target) throws BasicException {
		Object[] selected = (Object[]) bd.getListModel().getElementAt(index);
		Object[] adjacent = (Object[]) bd.getListModel().getElementAt(target);
		int selectedLine = ((Number) selected[3]).intValue();
		int adjacentLine = ((Number) adjacent[3]).intValue();
		PreparedSentence update = new PreparedSentence(app.getSession(),
				"UPDATE ATTRIBUTEUSE SET LINENO = ? WHERE ID = ?",
				new SerializerWriteBasicExt(new Datas[]{Datas.INT, Datas.STRING}, new int[]{1, 0}));

		try {
			app.getSession().begin();
			update.exec(new Object[]{selected[0], -1});
			update.exec(new Object[]{adjacent[0], selectedLine});
			update.exec(new Object[]{selected[0], adjacentLine});
			app.getSession().commit();
		} catch (BasicException exception) {
			rollback();
			throw exception;
		} catch (java.sql.SQLException exception) {
			rollback();
			throw new BasicException(exception);
		}
	}

	private void rollback() {
		try {
			app.getSession().rollback();
		} catch (java.sql.SQLException ignored) {
		}
	}
}
