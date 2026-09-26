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
import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.ListCellRendererBasic;
import com.openbravo.data.loader.ComparatorCreator;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.panels.*;
import com.openbravo.data.loader.TableDefinition;
import com.openbravo.data.loader.Vectorer;
import com.openbravo.data.user.EditorRecord;
import com.openbravo.data.user.SaveProvider;
import com.openbravo.data.user.ListProvider;
import com.openbravo.data.user.ListProviderCreator;
import com.openbravo.data.user.BrowsableEditableData;
import com.openbravo.data.gui.MessageInf;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JPanel;

/**
 *
 * @author adrianromero
 */
public class PeoplePanel extends JPanelTable {

	private TableDefinition tpeople;
	private PeopleView jeditor;
	private DataLogicAdmin admin;
	private final Map<String, String> roleNames = new HashMap<>();

	/** Creates a new instance of JPanelPeople */
	public PeoplePanel() {
	}

	protected void init() {
		admin = app.getBean(DataLogicAdmin.class);
		tpeople = admin.getTablePeople();
		jeditor = new PeopleView(admin, dirty);
	}

	public ListProvider getListProvider() {
		return new ListProviderCreator(tpeople);
	}

	public SaveProvider getSaveProvider() {
		return new SaveProvider(tpeople);
	}

	public Vectorer getVectorer() {
		return tpeople.getVectorerBasic(new int[]{1});
	}

	public ComparatorCreator getComparatorCreator() {
		return tpeople.getComparatorCreator(new int[]{1, 3});
	}

	public ListCellRenderer getListCellRenderer() {
		return new ListCellRendererBasic(tpeople.getRenderStringBasic(new int[]{1}));
	}

	public EditorRecord getEditor() {
		return jeditor;
	}

	public void activate() throws BasicException {

		jeditor.activate(); // primero el editor
		roleNames.clear();
		for (Object value : admin.getAllRolesList().list()) {
			RoleInfo role = (RoleInfo) value;
			roleNames.put(role.getID(), role.getName());
		}
		super.activate(); // y luego cargamos los datos
		jeditor.setBrowsableData(bd);
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
		JButton add = new JButton(AppLocal.getIntString("Admin.NewUser"));
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
		return new AdminListNavigator(data, AppLocal.getIntString("Admin.SearchUsers"), row -> {
			Object[] person = (Object[]) row;
			return person[1] + " " + person[5];
		}, new AdminRowRenderer(row -> {
			Object[] person = (Object[]) row;
			return roleNames.getOrDefault(String.valueOf(person[3]), "") + " · "
					+ AppLocal.getIntString(Boolean.TRUE.equals(person[4]) ? "Admin.Visible" : "Admin.Hidden");
		}, row -> {
			java.awt.image.BufferedImage photo = (java.awt.image.BufferedImage) ((Object[]) row)[6];
			return photo == null
					? null
					: new javax.swing.ImageIcon(photo.getScaledInstance(32, 32, java.awt.Image.SCALE_SMOOTH));
		}));
	}
	public String getTitle() {
		return AppLocal.getIntString("Menu.Users");
	}
}
