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
import com.openbravo.pos.forms.*;
import com.openbravo.pos.panels.*;
import com.openbravo.data.loader.TableDefinition;
import com.openbravo.data.loader.Vectorer;
import com.openbravo.data.user.*;
import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.MessageInf;
import java.awt.Component;
import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.ButtonGroup;

/**
 *
 * @author adrianromero
 */
public class ResourcesPanel extends JPanelTable {

	private TableDefinition tresources;
	private ResourcesView jeditor;
	private AdminListNavigator navigator;

	/** Creates a new instance of JPanelResources */
	public ResourcesPanel() {
	}

	protected void init() {
		DataLogicAdmin dlAdmin = app.getBean(DataLogicAdmin.class);
		tresources = dlAdmin.getTableResources();
		jeditor = new ResourcesView(dirty);
	}

	@Override
	public boolean deactivate() {
		if (super.deactivate()) {
			DataLogicSystem dlSystem = app.getBean(DataLogicSystem.class);
			dlSystem.resetResourcesCache();
			return true;
		} else {
			return false;
		}
	}

	public ListProvider getListProvider() {
		return new ListProviderCreator(tresources);
	}

	public SaveProvider getSaveProvider() {
		return new SaveProvider(tresources);
	}

	@Override
	public Vectorer getVectorer() {
		return tresources.getVectorerBasic(new int[]{1});
	}

	@Override
	public ComparatorCreator getComparatorCreator() {
		return tresources.getComparatorCreator(new int[]{1, 2});
	}

	@Override
	public ListCellRenderer getListCellRenderer() {
		return new ListCellRendererBasic(tresources.getRenderStringBasic(new int[]{1}));
	}

	public EditorRecord getEditor() {
		return jeditor;
	}

	public String getTitle() {
		return AppLocal.getIntString("Menu.Resources");
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
		JPanel bar = new JPanel(new java.awt.BorderLayout());
		JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT));
		ButtonGroup group = new ButtonGroup();
		for (int type = -1; type < 3; type++) {
			final int selectedType = type;
			JToggleButton filter = new JToggleButton(AppLocal.getIntString(type == -1
					? "Admin.All"
					: type == 0 ? "resource.text" : type == 1 ? "resource.image" : "resource.binary"));
			filter.setSelected(type == -1);
			filter.addActionListener(e -> {
				if (navigator != null)
					navigator.setFilter(
							row -> selectedType < 0 || ((Number) ((Object[]) row)[2]).intValue() == selectedType);
			});
			group.add(filter);
			filters.add(filter);
		}
		bar.add(filters, java.awt.BorderLayout.WEST);
		JButton add = new JButton(AppLocal.getIntString("Admin.NewResource"));
		add.addActionListener(e -> {
			try {
				bd.actionInsert();
			} catch (BasicException ex) {
				new MessageInf(ex).show(this);
			}
		});
		JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		actions.add(add);
		bar.add(actions, java.awt.BorderLayout.EAST);
		return bar;
	}

	@Override
	protected Component getListComponent(BrowsableEditableData data) {
		navigator = new AdminListNavigator(data, AppLocal.getIntString("Admin.SearchResources"),
				row -> String.valueOf(((Object[]) row)[1]), new AdminRowRenderer(row -> {
					Object[] resource = (Object[]) row;
					int type = ((Number) resource[2]).intValue();
					return AppLocal.getIntString(
							type == 0 ? "resource.text" : type == 1 ? "resource.image" : "resource.binary")
							+ " · "
							+ AppLocal.getIntString(((String) resource[1]).startsWith("Printer.")
									? "Admin.Resource.Printer"
									: ((String) resource[1]).startsWith("Role.")
											? "Admin.Resource.Role"
											: type == 1 ? "Admin.Resource.Image" : "Admin.Resource.Other")
							+ (ResourceOriginal.differs(resource)
									? " · " + AppLocal.getIntString("Admin.Modified")
									: "");
				}));
		return navigator;
	}

	@Override
	public void activate() throws BasicException {
		super.activate();
		jeditor.setBrowsableData(bd);
	}
}
