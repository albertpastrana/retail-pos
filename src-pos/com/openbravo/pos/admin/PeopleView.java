package com.openbravo.pos.admin;

import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.ComboBoxValModel;
import com.openbravo.data.gui.JConfirmationDialog;
import com.openbravo.data.loader.SentenceList;
import com.openbravo.data.user.BrowsableEditableData;
import com.openbravo.data.user.DirtyManager;
import com.openbravo.data.user.EditorRecord;
import com.openbravo.format.Formats;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.theme.RetailPOSColors;
import com.openbravo.pos.util.Hashcypher;
import com.openbravo.pos.util.StringUtils;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.util.UUID;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class PeopleView extends JPanel implements EditorRecord {
	private final DataLogicAdmin admin;
	private final DirtyManager dirty;
	private final SentenceList roles;
	private ComboBoxValModel roleModel = new ComboBoxValModel();
	private final JTextField name = new JTextField(20);
	private final JTextField card = new JTextField(20);
	private final JComboBox role = new JComboBox();
	private final JCheckBox visible = new JCheckBox(AppLocal.getIntString("label.peoplevisible"));
	private final com.openbravo.data.gui.JImageEditor image = new com.openbravo.data.gui.JImageEditor();
	private final JButton passwordButton = new JButton(AppLocal.getIntString("button.peoplepassword"));
	private final JButton newCard = new JButton(AppLocal.getIntString("Admin.GenerateCard"));
	private final JButton removeCard = new JButton(AppLocal.getIntString("Admin.RemoveCard"));
	private final JButton save = new JButton(AppLocal.getIntString("Admin.SaveUser"));
	private final JButton discard = new JButton(AppLocal.getIntString("Admin.Discard"));
	private final JButton delete = new JButton(AppLocal.getIntString("Admin.Delete"));
	private final JLabel status = new JLabel();
	private Object id;
	private Object sortOrder;
	private String password;
	private BrowsableEditableData data;

	public PeopleView(DataLogicAdmin admin, DirtyManager dirty) {
		super(new BorderLayout(8, 8));
		this.admin = admin;
		this.dirty = dirty;
		roles = admin.getRolesList();
		setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));
		JPanel fields = new JPanel(new GridBagLayout());
		int y = 0;
		row(fields, y++, AppLocal.getIntString("label.peoplename"), name);
		row(fields, y++, AppLocal.getIntString("label.role"), role);
		row(fields, y++, "", new JLabel(AppLocal.getIntString("Admin.RoleHint")));
		row(fields, y++, AppLocal.getIntString("Admin.AccessCard"), card);
		row(fields, y++, "", new JLabel(AppLocal.getIntString("Admin.CardHint")));
		JPanel cardActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		cardActions.add(newCard);
		cardActions.add(removeCard);
		row(fields, y++, "", cardActions);
		row(fields, y++, AppLocal.getIntString("Label.Password"), passwordButton);
		row(fields, y++, AppLocal.getIntString("label.peopleimage"), image);
		row(fields, y++, "", visible);
		add(fields, BorderLayout.NORTH);
		JPanel footer = new JPanel(new BorderLayout(8, 0));
		footer.add(status, BorderLayout.WEST);
		JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		actions.add(delete);
		actions.add(discard);
		RetailPOSColors.primaryButton(save);
		actions.add(save);
		footer.add(actions, BorderLayout.EAST);
		add(footer, BorderLayout.SOUTH);
		card.setEditable(false);
		image.setMaxDimensions(new java.awt.Dimension(32, 32));
		image.setActionLabels(AppLocal.getIntString("Admin.ImageOpen"), AppLocal.getIntString("Admin.ImageRemove"),
				AppLocal.getIntString("Admin.ZoomIn"), AppLocal.getIntString("Admin.ZoomOut"));
		name.getDocument().addDocumentListener(dirty);
		role.addActionListener(dirty);
		visible.addActionListener(dirty);
		image.addPropertyChangeListener("image", dirty);
		dirty.addDirtyListener(value -> status.setText(AppLocal.getIntString(value ? "Admin.Unsaved" : "Admin.Saved")));
		passwordButton.addActionListener(e -> {
			String changed = Hashcypher.changePassword(this);
			if (changed != null) {
				password = changed;
				dirty.setDirty(true);
			}
		});
		newCard.addActionListener(e -> {
			if (JConfirmationDialog.show(this, AppLocal.getIntString("message.cardnew"),
					AppLocal.getIntString("title.editor"), AppLocal.getIntString("confirm.cancel"),
					AppLocal.getIntString("confirm.newcard"), false) == JOptionPane.YES_OPTION) {
				card.setText("c" + StringUtils.getCardNumber());
				dirty.setDirty(true);
			}
		});
		removeCard.addActionListener(e -> {
			if (JConfirmationDialog.show(this, AppLocal.getIntString("message.cardremove"),
					AppLocal.getIntString("title.editor"), AppLocal.getIntString("confirm.cancel"),
					AppLocal.getIntString("confirm.remove"), true) == JOptionPane.YES_OPTION) {
				card.setText("");
				dirty.setDirty(true);
			}
		});
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

	private static void row(JPanel panel, int y, String label, Component input) {
		GridBagConstraints c = new GridBagConstraints();
		c.gridy = y;
		c.gridx = 0;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.insets = new Insets(6, 2, 6, 12);
		panel.add(new JLabel(label), c);
		c.gridx = 1;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(6, 0, 6, 2);
		panel.add(input, c);
	}

	void setBrowsableData(BrowsableEditableData data) {
		this.data = data;
	}
	private void editable(boolean enabled) {
		name.setEnabled(enabled);
		role.setEnabled(enabled);
		visible.setEnabled(enabled);
		card.setEnabled(enabled);
		image.setEnabled(enabled);
		passwordButton.setEnabled(enabled);
		newCard.setEnabled(enabled);
		removeCard.setEnabled(enabled);
		save.setEnabled(enabled);
		discard.setEnabled(enabled);
		delete.setEnabled(enabled && id != null);
	}
	public void writeValueEOF() {
		id = null;
		sortOrder = null;
		password = null;
		name.setText("");
		roleModel.setSelectedKey(null);
		visible.setSelected(false);
		card.setText("");
		image.setImage(null);
		editable(false);
	}
	public void writeValueInsert() {
		writeValueEOF();
		visible.setSelected(true);
		editable(true);
	}
	public void writeValueDelete(Object value) {
		writeValueEdit(value);
		editable(false);
	}
	public void writeValueEdit(Object value) {
		Object[] person = (Object[]) value;
		id = person[0];
		sortOrder = person[7];
		password = Formats.STRING.formatValue(person[2]);
		name.setText(Formats.STRING.formatValue(person[1]));
		roleModel.setSelectedKey(person[3]);
		visible.setSelected(Boolean.TRUE.equals(person[4]));
		card.setText(Formats.STRING.formatValue(person[5]));
		image.setImage((BufferedImage) person[6]);
		editable(true);
	}
	public Object createValue() throws BasicException {
		return new Object[]{id == null ? UUID.randomUUID().toString() : id, Formats.STRING.parseValue(name.getText()),
				Formats.STRING.parseValue(password), roleModel.getSelectedKey(), visible.isSelected(),
				Formats.STRING.parseValue(card.getText()), image.getImage(),
				sortOrder == null ? admin.getNextPeopleSortOrder() : sortOrder};
	}
	public void activate() throws BasicException {
		roleModel = new ComboBoxValModel(roles.list());
		role.setModel(roleModel);
	}
	public Component getComponent() {
		return this;
	}
	public void refresh() {
	}
}
