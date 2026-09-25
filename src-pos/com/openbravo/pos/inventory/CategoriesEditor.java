package com.openbravo.pos.inventory;

import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.ComboBoxValModel;
import com.openbravo.data.gui.JImageEditor;
import com.openbravo.data.gui.JMessageDialog;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.data.loader.SentenceExec;
import com.openbravo.data.loader.SentenceList;
import com.openbravo.data.user.BrowsableEditableData;
import com.openbravo.data.user.DirtyManager;
import com.openbravo.data.user.EditorRecord;
import com.openbravo.format.Formats;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.AppView;
import com.openbravo.pos.forms.DataLogicSales;
import com.openbravo.pos.ticket.CategoryInfo;
import com.openbravo.pos.theme.RetailPOSColors;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class CategoriesEditor extends JPanel implements EditorRecord {
	private final DataLogicSales sales;
	private final DirtyManager dirty;
	private SentenceList categorySentence;
	private ComboBoxValModel categoryModel;
	private SentenceExec catalogAdd;
	private SentenceExec catalogDelete;
	private BrowsableEditableData data;
	private ActionListener deleteAction;
	private String id;
	private final JLabel path = new JLabel();
	private final JLabel categoryTitle = new JLabel();
	private final JTextField name = new JTextField();
	private final JComboBox category = new JComboBox();
	private final JImageEditor image = new JImageEditor();
	private final JLabel catalogState = new JLabel();
	private final JButton catalogAddButton = new JButton();
	private final JButton catalogDeleteButton = new JButton();
	private final JButton deleteButton = new JButton();
	private final JButton saveButton = new JButton();

	public CategoriesEditor(AppView app, DirtyManager dirty) {
		this.dirty = dirty;
		sales = app.getBean(DataLogicSales.class);
		categorySentence = sales.getCategoriesList();
		categoryModel = new ComboBoxValModel();
		catalogAdd = sales.getCatalogCategoryAdd();
		catalogDelete = sales.getCatalogCategoryDel();
		buildUi();
		name.getDocument().addDocumentListener(dirty);
		category.addActionListener(dirty);
		image.addPropertyChangeListener("image", dirty);
		writeValueEOF();
	}

	private void buildUi() {
		setLayout(new BorderLayout(0, 8));
		JPanel heading = new JPanel(new BorderLayout(0, 2));
		path.setForeground(RetailPOSColors.inkMuted());
		categoryTitle.setFont(categoryTitle.getFont().deriveFont(java.awt.Font.BOLD, 20f));
		heading.add(path, BorderLayout.NORTH);
		heading.add(categoryTitle, BorderLayout.CENTER);
		add(heading, BorderLayout.NORTH);
		JPanel fields = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(4, 4, 4, 4);
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0;
		fields.add(new JLabel(AppLocal.getIntString("Label.Name")), c);
		c.gridx = 1;
		c.weightx = 1;
		fields.add(name, c);
		c.gridx = 0;
		c.gridy++;
		c.weightx = 0;
		fields.add(new JLabel(AppLocal.getIntString("label.prodcategory")), c);
		c.gridx = 1;
		c.weightx = 1;
		fields.add(category, c);
		c.gridx = 0;
		c.gridy++;
		c.weightx = 0;
		fields.add(new JLabel(AppLocal.getIntString("label.image")), c);
		c.gridx = 1;
		c.weightx = 1;
		c.fill = GridBagConstraints.BOTH;
		c.weighty = 1;
		image.setPreferredSize(new java.awt.Dimension(220, 150));
		fields.add(image, c);
		add(fields, BorderLayout.CENTER);

		JPanel catalog = new JPanel(new GridBagLayout());
		catalog.setBorder(BorderFactory.createTitledBorder(AppLocal.getIntString("category.catalog")));
		GridBagConstraints cc = new GridBagConstraints();
		cc.gridx = 0;
		cc.gridy = 0;
		cc.gridwidth = 2;
		cc.anchor = GridBagConstraints.WEST;
		cc.fill = GridBagConstraints.HORIZONTAL;
		cc.weightx = 1;
		JTextArea explanation = new JTextArea(AppLocal.getIntString("category.catalog.explanation"));
		explanation.setLineWrap(true);
		explanation.setWrapStyleWord(true);
		explanation.setEditable(false);
		explanation.setOpaque(false);
		catalog.add(explanation, cc);
		cc.gridy++;
		catalog.add(catalogState, cc);
		catalogAddButton.setText(AppLocal.getIntString("button.catalogadd"));
		catalogDeleteButton.setText(AppLocal.getIntString("button.catalogdel"));
		cc.gridy++;
		cc.gridwidth = 1;
		cc.weightx = 0.5;
		catalog.add(catalogAddButton, cc);
		cc.gridx = 1;
		catalog.add(catalogDeleteButton, cc);
		catalogAddButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				changeCatalog(true);
			}
		});
		catalogDeleteButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				changeCatalog(false);
			}
		});
		JPanel bottom = new JPanel(new BorderLayout(8, 0));
		bottom.add(catalog, BorderLayout.CENTER);
		JPanel footer = new JPanel(new BorderLayout(8, 0));
		deleteButton.setText(AppLocal.getIntString("button.categorydelete"));
		deleteButton.setForeground(RetailPOSColors.danger());
		saveButton.setText(AppLocal.getIntString("button.categorysave"));
		RetailPOSColors.primaryButton(saveButton);
		footer.add(deleteButton, BorderLayout.WEST);
		footer.add(saveButton, BorderLayout.EAST);
		bottom.add(footer, BorderLayout.SOUTH);
		add(bottom, BorderLayout.SOUTH);
		deleteButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				if (deleteAction != null)
					deleteAction.actionPerformed(event);
			}
		});
		saveButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				try {
					if (data != null)
						data.saveData();
				} catch (BasicException e) {
					new MessageInf(e).show(CategoriesEditor.this);
				}
			}
		});
	}

	public void setBrowsableData(BrowsableEditableData data) {
		this.data = data;
	}
	public void setDeleteAction(ActionListener action) {
		deleteAction = action;
	}
	public String getCategoryId() {
		return id;
	}
	public String getCategoryName() {
		return name.getText();
	}

	public void refresh() {
		try {
			List list = categorySentence.list();
			categoryModel = new ComboBoxValModel(filteredCategories(list, id));
			category.setModel(categoryModel);
		} catch (BasicException e) {
			new MessageInf(MessageInf.SGN_NOTICE, AppLocal.getIntString("message.cannotloadlists"), e).show(this);
		}
	}

	private List filteredCategories(List all, String current) {
		Set<String> blocked = new HashSet<String>();
		blocked.add(current);
		boolean changed = true;
		while (changed) {
			changed = false;
			for (Object value : all) {
				CategoryInfo item = (CategoryInfo) value;
				if (blocked.contains(item.getParentID()) && blocked.add(item.getID()))
					changed = true;
			}
		}
		List allowed = new ArrayList();
		allowed.add(null);
		for (Object value : all)
			if (!blocked.contains(((CategoryInfo) value).getID()))
				allowed.add(value);
		return allowed;
	}

	public void writeValueEOF() {
		setValue(null, null, null, null, false);
	}
	public void writeValueInsert() {
		setValue(UUID.randomUUID().toString(), null, null, null, true);
	}
	public void writeValueDelete(Object value) {
		setValue((Object[]) value, false);
	}
	public void writeValueEdit(Object value) {
		setValue((Object[]) value, true);
	}

	private void setValue(Object[] value, boolean enabled) {
		setValue((String) value[0], Formats.STRING.formatValue(value[1]), (String) value[2], (BufferedImage) value[3],
				enabled);
	}

	private void setValue(String valueId, String valueName, String parent, BufferedImage valueImage, boolean enabled) {
		id = valueId;
		name.setText(valueName);
		categoryTitle.setText(valueName == null ? "" : valueName);
		image.setImage(valueImage);
		refresh();
		categoryModel.setSelectedKey(parent);
		name.setEnabled(enabled);
		category.setEnabled(enabled);
		image.setEnabled(enabled);
		deleteButton.setEnabled(valueId != null && enabled);
		catalogAddButton.setEnabled(valueId != null && enabled);
		catalogDeleteButton.setEnabled(valueId != null && enabled);
		updatePath();
		updateCatalog();
	}

	private void updatePath() {
		try {
			List list = categorySentence.list();
			Map<String, CategoryInfo> byId = new HashMap<String, CategoryInfo>();
			for (Object value : list)
				byId.put(((CategoryInfo) value).getID(), (CategoryInfo) value);
			StringBuilder result = new StringBuilder();
			Set<String> visited = new HashSet<String>();
			CategoryInfo current = byId.get(id);
			while (current != null && visited.add(current.getID())) {
				if (result.length() > 0)
					result.insert(0, " / ");
				result.insert(0, current.getName());
				current = byId.get(current.getParentID());
			}
			path.setText(result.toString());
		} catch (BasicException e) {
			path.setText("");
		}
	}

	private void updateCatalog() {
		if (id == null) {
			catalogState.setText("");
			return;
		}
		try {
			catalogState.setText(AppLocal.getIntString("category.catalog.state")
					.replace("{0}", Integer.toString(sales.getCategoryCatalogCount(id)))
					.replace("{1}", Integer.toString(sales.getCategoryProductCount(id))));
		} catch (BasicException e) {
			catalogState.setText("");
		}
	}

	private void changeCatalog(boolean add) {
		if (id == null)
			return;
		try {
			if (!add && JOptionPane.showConfirmDialog(this, AppLocal.getIntString("category.catalog.remove.confirm"),
					AppLocal.getIntString("category.catalog"), JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
				return;
			catalogDelete.exec(id);
			if (add)
				catalogAdd.exec(id);
			updateCatalog();
			new MessageInf(MessageInf.SGN_NOTICE, AppLocal.getIntString("category.catalog.updated")).show(this);
		} catch (BasicException e) {
			JMessageDialog.showMessage(this,
					new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotexecute"), e));
		}
	}

	public Object createValue() throws BasicException {
		Object[] value = new Object[]{id, name.getText(), categoryModel.getSelectedKey(), image.getImage()};
		checkNameFreeAmongSiblings((String) value[1], (String) value[2]);
		return value;
	}

	private void checkNameFreeAmongSiblings(String valueName, String parent) throws BasicException {
		for (CategoryInfo sibling : sales.getCategorySiblings(parent)) {
			if (!sibling.getID().equals(id) && sibling.getName().equalsIgnoreCase(valueName)) {
				throw new BasicException(AppLocal.getIntString("message.categorynamerepeated"));
			}
		}
	}

	public java.awt.Component getComponent() {
		return this;
	}
}
