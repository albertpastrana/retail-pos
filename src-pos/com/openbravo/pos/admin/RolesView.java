package com.openbravo.pos.admin;

import com.openbravo.basic.BasicException;
import com.openbravo.data.user.BrowsableEditableData;
import com.openbravo.data.user.DirtyManager;
import com.openbravo.data.user.EditorRecord;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.theme.RetailPOSColors;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class RolesView extends JPanel implements EditorRecord {
	private final DirtyManager dirty;
	private final JTextField name = new JTextField();
	private final JTextField search = new JTextField();
	private final JPanel groups = new JPanel();
	private final JLabel count = new JLabel();
	private final JLabel impact = new JLabel();
	private final Map<String, JCheckBox> boxes = new LinkedHashMap<>();
	private final JButton xmlButton = new JButton(AppLocal.getIntString("Admin.EditXml"));
	private final JButton save = new JButton(AppLocal.getIntString("Admin.SaveRole"));
	private final JButton discard = new JButton(AppLocal.getIntString("Admin.Discard"));
	private final JButton delete = new JButton(AppLocal.getIntString("Admin.Delete"));
	private Object id;
	private String originalXml = "<permissions/>";
	private Set<String> originalKeys = Set.of();
	private boolean changing;
	private boolean invalidXml;
	private BrowsableEditableData data;
	private int users;

	public RolesView(DirtyManager dirty) {
		super(new BorderLayout(8, 8));
		this.dirty = dirty;
		setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));
		JPanel top = new JPanel(new BorderLayout(0, 8));
		JPanel title = new JPanel(new BorderLayout(8, 0));
		title.add(new JLabel(AppLocal.getIntString("Label.Name")), BorderLayout.WEST);
		title.add(name, BorderLayout.CENTER);
		top.add(title, BorderLayout.NORTH);
		JPanel controls = new JPanel(new BorderLayout(8, 0));
		search.putClientProperty("JTextField.placeholderText", AppLocal.getIntString("Admin.SearchPermissions"));
		controls.add(search, BorderLayout.CENTER);
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
		JButton all = new JButton(AppLocal.getIntString("Admin.SelectAll"));
		JButton none = new JButton(AppLocal.getIntString("Admin.ClearAll"));
		all.addActionListener(e -> selectAll(true));
		none.addActionListener(e -> selectAll(false));
		buttons.add(all);
		buttons.add(none);
		controls.add(buttons, BorderLayout.EAST);
		top.add(controls, BorderLayout.SOUTH);
		add(top, BorderLayout.NORTH);
		groups.setLayout(new javax.swing.BoxLayout(groups, javax.swing.BoxLayout.Y_AXIS));
		add(new JScrollPane(groups), BorderLayout.CENTER);
		JPanel footer = new JPanel(new BorderLayout(0, 8));
		footer.add(count, BorderLayout.NORTH);
		footer.add(impact, BorderLayout.CENTER);
		JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		actions.add(delete);
		actions.add(xmlButton);
		actions.add(discard);
		RetailPOSColors.primaryButton(save);
		actions.add(save);
		footer.add(actions, BorderLayout.SOUTH);
		add(footer, BorderLayout.SOUTH);
		name.getDocument().addDocumentListener(dirty);
		search.getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) {
				rebuild();
			}
			public void removeUpdate(DocumentEvent e) {
				rebuild();
			}
			public void changedUpdate(DocumentEvent e) {
				rebuild();
			}
		});
		xmlButton.addActionListener(e -> editXml());
		discard.addActionListener(e -> {
			if (data != null)
				data.actionReloadCurrent(this);
		});
		delete.addActionListener(e -> AdminDelete.confirm(this, data, dirty, name.getText()));
		save.addActionListener(e -> {
			if (data != null)
				try {
					data.saveData();
				} catch (BasicException ex) {
					new com.openbravo.data.gui.MessageInf(ex).show(this);
				}
		});
		writeValueEOF();
	}

	void setBrowsableData(BrowsableEditableData data) {
		this.data = data;
	}
	void setUserCount(int users) {
		this.users = users;
		updateImpact();
	}

	private void selectAll(boolean selected) {
		for (String key : RolePermissions.catalogue())
			boxes.get(key).setSelected(selected);
		if (!changing)
			dirty.setDirty(true);
		updateImpact();
	}

	private Set<String> selectedKeys() {
		Set<String> keys = new LinkedHashSet<>();
		boxes.forEach((key, box) -> {
			if (box.isSelected())
				keys.add(key);
		});
		return keys;
	}

	private void updateImpact() {
		Set<String> selected = selectedKeys();
		count.setText(String.format(AppLocal.getIntString("Admin.PermissionCount"), selected.size(), boxes.size()));
		int removed = 0;
		for (String key : originalKeys)
			if (boxes.containsKey(key) && !selected.contains(key))
				removed++;
		impact.setText(String.format(AppLocal.getIntString("Admin.RoleImpact"), removed, users));
	}

	private void rebuild() {
		if (groups == null)
			return;
		groups.removeAll();
		String term = search.getText().strip().toLowerCase(java.util.Locale.ROOT);
		for (String area : List.of("sales", "maintenance", "reports", "system", "other")) {
			List<Map.Entry<String, JCheckBox>> matches = new ArrayList<>();
			int active = 0;
			int total = 0;
			for (var entry : boxes.entrySet()) {
				String group = RolePermissions.catalogue().contains(entry.getKey())
						? RolePermissions.area(entry.getKey())
						: "other";
				if (!area.equals(group))
					continue;
				total++;
				if (entry.getValue().isSelected())
					active++;
				if ((entry.getValue().getText() + " " + entry.getKey()).toLowerCase(java.util.Locale.ROOT)
						.contains(term))
					matches.add(entry);
			}
			if (matches.isEmpty())
				continue;
			JPanel section = new JPanel(new GridLayout(0, 1, 0, 4));
			section.setBorder(javax.swing.BorderFactory.createTitledBorder(
					AppLocal.getIntString("Admin.Area." + area) + " (" + active + "/" + total + ")"));
			for (var entry : matches)
				section.add(entry.getValue());
			section.setAlignmentX(LEFT_ALIGNMENT);
			groups.add(section);
		}
		groups.revalidate();
		groups.repaint();
		updateImpact();
	}

	private void load(String xml) {
		changing = true;
		originalXml = xml == null || xml.isBlank() ? "<permissions/>" : xml;
		boxes.clear();
		invalidXml = false;
		try {
			originalKeys = RolePermissions.keys(originalXml);
		} catch (Exception ex) {
			originalKeys = Set.of();
			invalidXml = true;
		}
		Set<String> available = new LinkedHashSet<>(RolePermissions.catalogue());
		available.addAll(originalKeys);
		for (String key : available) {
			JCheckBox box = new JCheckBox(RolePermissions.label(key), originalKeys.contains(key));
			box.setToolTipText(key);
			box.addActionListener(e -> {
				dirty.setDirty(true);
				rebuild();
			});
			boxes.put(key, box);
		}
		rebuild();
		changing = false;
		xmlButton.setEnabled(id != null || name.isEnabled());
	}

	private void editXml() {
		String current = originalXml;
		if (!invalidXml && !selectedKeys().equals(originalKeys)) {
			try {
				current = RolePermissions.update(originalXml, selectedKeys());
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(this, ex.getMessage(), AppLocal.getIntString("Admin.InvalidXml"),
						JOptionPane.ERROR_MESSAGE);
				return;
			}
		}
		JTextArea editor = new JTextArea(current, 20, 55);
		editor.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 13));
		int answer = JOptionPane.showConfirmDialog(this, new JScrollPane(editor),
				AppLocal.getIntString("Admin.EditXml"), JOptionPane.OK_CANCEL_OPTION);
		if (answer != JOptionPane.OK_OPTION)
			return;
		try {
			RolePermissions.keys(editor.getText());
			load(editor.getText());
			dirty.setDirty(true);
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, ex.getMessage(), AppLocal.getIntString("Admin.InvalidXml"),
					JOptionPane.ERROR_MESSAGE);
		}
	}

	public void writeValueEOF() {
		id = null;
		name.setText("");
		name.setEnabled(false);
		load("<permissions/>");
		setEnabled(false);
	}
	public void writeValueInsert() {
		id = null;
		name.setText("");
		name.setEnabled(true);
		load("<permissions/>");
		setEnabled(true);
	}
	public void writeValueDelete(Object value) {
		writeValueEdit(value);
		name.setEnabled(false);
		setEnabled(false);
	}
	public void writeValueEdit(Object value) {
		Object[] role = (Object[]) value;
		id = role[0];
		name.setText((String) role[1]);
		name.setEnabled(true);
		load(RolePermissions.text(role[2]));
		setEnabled(true);
	}
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		if (groups == null)
			return;
		boxes.forEach(
				(key, box) -> box.setEnabled(enabled && !invalidXml && RolePermissions.catalogue().contains(key)));
		search.setEnabled(enabled && !invalidXml);
		save.setEnabled(enabled);
		discard.setEnabled(enabled);
		delete.setEnabled(enabled && id != null);
		xmlButton.setEnabled(enabled);
	}
	public Object createValue() throws BasicException {
		if (invalidXml)
			throw new BasicException(AppLocal.getIntString("Admin.InvalidXml"));
		try {
			Set<String> selected = selectedKeys();
			String xml = selected.equals(originalKeys) ? originalXml : RolePermissions.update(originalXml, selected);
			return new Object[]{id == null ? UUID.randomUUID().toString() : id, name.getText(),
					com.openbravo.format.Formats.BYTEA.parseValue(xml)};
		} catch (Exception ex) {
			throw new BasicException(ex);
		}
	}
	public Component getComponent() {
		return this;
	}
	public void refresh() {
	}
}
