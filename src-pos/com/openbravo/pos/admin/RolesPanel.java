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

package com.openbravo.pos.admin;

import javax.swing.ListCellRenderer;
import com.openbravo.data.gui.ListCellRendererBasic;
import com.openbravo.data.loader.ComparatorCreator;
import com.openbravo.data.loader.TableDefinition;
import com.openbravo.data.loader.Vectorer;
import com.openbravo.data.user.EditorRecord;
import com.openbravo.data.user.ListProvider;
import com.openbravo.data.user.ListProviderCreator;
import com.openbravo.data.user.SaveProvider;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.panels.JPanelTable;
import com.openbravo.data.user.BrowsableEditableData;
import com.openbravo.data.user.EditorListener;
import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.MessageInf;
import java.util.HashMap;
import java.util.Map;
import java.awt.Component;
import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

/**
 *
 * @author adrianromero
 */
public class RolesPanel extends JPanelTable implements EditorListener {

	private TableDefinition troles;
	private RolesView jeditor;
	private DataLogicAdmin admin;
	private boolean listening;
	private final Map<String, Integer> userCounts = new HashMap<>();

	/** Creates a new instance of RolesPanel */
	public RolesPanel() {
	}

	protected void init() {
		admin = app.getBean(DataLogicAdmin.class);
		troles = admin.getTableRoles();
		jeditor = new RolesView(dirty);
	}

	public ListProvider getListProvider() {
		return new ListProviderCreator(troles);
	}

	public SaveProvider getSaveProvider() {
		return new SaveProvider(troles);
	}

	public Vectorer getVectorer() {
		return troles.getVectorerBasic(new int[]{1});
	}

	public ComparatorCreator getComparatorCreator() {
		return troles.getComparatorCreator(new int[]{1});
	}

	public ListCellRenderer getListCellRenderer() {
		return new ListCellRendererBasic(troles.getRenderStringBasic(new int[]{1}));
	}

	public EditorRecord getEditor() {
		return jeditor;
	}

	public String getTitle() {
		return AppLocal.getIntString("Menu.Roles");
	}

	@Override
	protected boolean showToolbar() {
		return false;
	}
	@Override
	protected double getSplitDividerLocation() {
		return 0.33;
	}
	@Override
	protected double getSplitResizeWeight(boolean keepsSize) {
		return 0.33;
	}

	@Override
	public Component getFilter() {
		JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton add = new JButton(AppLocal.getIntString("Admin.NewRole"));
		add.addActionListener(e -> {
			try {
				bd.actionInsert();
			} catch (BasicException ex) {
				new MessageInf(ex).show(this);
			}
		});
		bar.add(add);
		return bar;
	}

	@Override
	protected Component getListComponent(BrowsableEditableData data) {
		return new AdminListNavigator(data, AppLocal.getIntString("Admin.SearchRoles"),
				row -> String.valueOf(((Object[]) row)[1]), new AdminRowRenderer(row -> {
					Object[] role = (Object[]) row;
					int users = userCounts.getOrDefault((String) role[0], 0);
					int permissions = 0;
					try {
						permissions = RolePermissions.keys(RolePermissions.text(role[2])).size();
					} catch (Exception ex) {
						/* XML can be repaired from the editor */ }
					return String.format(AppLocal.getIntString("Admin.RoleRow"), users, permissions,
							RolePermissions.catalogue().size());
				}));
	}

	@Override
	public void activate() throws BasicException {
		startNavigation();
		jeditor.setBrowsableData(bd);
		if (!listening) {
			bd.addEditorListener(this);
			listening = true;
		}
		bd.actionLoad();
		userCounts.clear();
		for (int i = 0; i < bd.getListModel().getSize(); i++) {
			Object[] role = (Object[]) bd.getListModel().getElementAt(i);
			userCounts.put((String) role[0], admin.getRoleUserCount((String) role[0]));
		}
		updateValue(bd.getIndex() < 0 ? null : bd.getListModel().getElementAt(bd.getIndex()));
		repaint();
	}

	@Override
	public void updateValue(Object value) {
		int users = value == null ? 0 : userCounts.getOrDefault((String) ((Object[]) value)[0], 0);
		jeditor.setUserCount(users);
	}
}
