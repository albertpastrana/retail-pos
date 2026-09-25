package com.openbravo.pos.inventory;

import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.data.loader.TableDefinition;
import com.openbravo.data.user.BrowsableEditableData;
import com.openbravo.data.user.EditorRecord;
import com.openbravo.data.user.ListProvider;
import com.openbravo.data.user.ListProviderCreator;
import com.openbravo.data.user.SaveProvider;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.DataLogicSales;
import com.openbravo.pos.panels.JPanelTable;
import com.openbravo.pos.theme.RetailPOSColors;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class CategoriesPanel extends JPanelTable {
	private TableDefinition categories;
	private CategoriesEditor editor;
	private DataLogicSales sales;
	private CategoryNavigator navigator;

	protected void init() {
		sales = app.getBean(DataLogicSales.class);
		categories = sales.getTableCategories();
		editor = new CategoriesEditor(app, dirty);
		editor.setDeleteAction(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				confirmDelete();
			}
		});
	}

	public ListProvider getListProvider() {
		return new ListProviderCreator(categories);
	}
	public SaveProvider getSaveProvider() {
		return new SaveProvider(categories);
	}
	public EditorRecord getEditor() {
		return editor;
	}
	public String getTitle() {
		return AppLocal.getIntString("Menu.Categories");
	}

	protected boolean showToolbar() {
		return false;
	}

	protected boolean getSplitOneTouchExpandable() {
		return false;
	}

	protected double getSplitResizeWeight(boolean editorKeepsSize) {
		return 0.32;
	}

	protected double getSplitDividerLocation() {
		return 0.32;
	}

	public java.awt.Component getFilter() {
		JPanel header = new JPanel(new BorderLayout(0, 6));
		JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
		JButton newCategory = new JButton(AppLocal.getIntString("button.categorynew"));
		RetailPOSColors.primaryButton(newCategory);
		newCategory.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				try {
					bd.actionInsert();
				} catch (BasicException e) {
					new MessageInf(e).show(CategoriesPanel.this);
				}
			}
		});
		actions.add(newCategory);
		header.add(actions, BorderLayout.NORTH);
		return header;
	}

	protected java.awt.Component getListComponent(BrowsableEditableData data) {
		CategoryTreeNavigator categoryNavigator = new CategoryTreeNavigator(data, sales);
		navigator = categoryNavigator;
		return categoryNavigator;
	}

	public void activate() throws BasicException {
		super.activate();
		editor.setBrowsableData(bd);
	}

	private void confirmDelete() {
		String id = editor.getCategoryId();
		if (id == null)
			return;
		try {
			int children = sales.getCategorySubcategoryCount(id);
			int products = sales.getCategoryProductCount(id);
			if (children > 0 || products > 0) {
				String message = AppLocal.getIntString("category.deleteblocked");
				message = message.replace("{0}", Integer.toString(children)).replace("{1}", Integer.toString(products));
				JOptionPane.showMessageDialog(this, message, AppLocal.getIntString("category.delete"),
						JOptionPane.WARNING_MESSAGE);
				return;
			}
			int answer = JOptionPane.showConfirmDialog(this,
					AppLocal.getIntString("category.deleteconfirm").replace("{0}", editor.getCategoryName()),
					AppLocal.getIntString("category.delete"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if (answer == JOptionPane.YES_OPTION)
				bd.actionDelete();
		} catch (BasicException e) {
			new MessageInf(e).show(this);
		}
	}
}
