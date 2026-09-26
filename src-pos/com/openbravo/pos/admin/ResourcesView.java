package com.openbravo.pos.admin;

import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.ComboBoxValModel;
import com.openbravo.data.loader.ImageUtils;
import com.openbravo.data.user.BrowsableEditableData;
import com.openbravo.data.user.DirtyManager;
import com.openbravo.data.user.EditorRecord;
import com.openbravo.format.Formats;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.theme.RetailPOSColors;
import com.openbravo.pos.util.Base64Encoder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.Arrays;
import java.util.UUID;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class ResourcesView extends JPanel implements EditorRecord {
	private final DirtyManager dirty;
	private final JTextField name = new JTextField(22);
	private final JComboBox<ResourceType> type = new JComboBox<>();
	private final ComboBoxValModel types = new ComboBoxValModel();
	private final JTextArea text = new JTextArea();
	private final JTextArea lineNumbers = new JTextArea("1");
	private final com.openbravo.data.gui.JImageEditor image = new com.openbravo.data.gui.JImageEditor();
	private final JPanel content = new JPanel(new CardLayout());
	private final JButton restore = new JButton(AppLocal.getIntString("Admin.RestoreOriginal"));
	private final JButton save = new JButton(AppLocal.getIntString("Admin.SaveResource"));
	private final JButton discard = new JButton(AppLocal.getIntString("Admin.Discard"));
	private final JButton delete = new JButton(AppLocal.getIntString("Admin.Delete"));
	private final JLabel status = new JLabel();
	private byte[] original;
	private byte[] loadedImageBytes;
	private java.awt.image.BufferedImage loadedImage;
	private Object id;
	private BrowsableEditableData data;

	public ResourcesView(DirtyManager dirty) {
		super(new BorderLayout(8, 8));
		this.dirty = dirty;
		setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));
		types.add(ResourceType.TEXT);
		types.add(ResourceType.IMAGE);
		types.add(ResourceType.BINARY);
		type.setModel(types);
		JPanel top = new JPanel(new BorderLayout(8, 0));
		top.add(new JLabel(AppLocal.getIntString("label.resname")), BorderLayout.WEST);
		top.add(name, BorderLayout.CENTER);
		top.add(type, BorderLayout.EAST);
		add(top, BorderLayout.NORTH);
		text.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 13));
		JScrollPane editor = new JScrollPane(text);
		lineNumbers.setEditable(false);
		lineNumbers.setFocusable(false);
		lineNumbers.setFont(text.getFont());
		lineNumbers.setBackground(javax.swing.UIManager.getColor("Panel.background"));
		editor.setRowHeaderView(lineNumbers);
		content.add(editor, "text");
		content.add(image, "image");
		content.add(new JPanel(), "none");
		add(content, BorderLayout.CENTER);
		JPanel bottom = new JPanel(new BorderLayout());
		bottom.add(status, BorderLayout.WEST);
		JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		actions.add(delete);
		actions.add(restore);
		actions.add(discard);
		RetailPOSColors.primaryButton(save);
		actions.add(save);
		bottom.add(actions, BorderLayout.EAST);
		add(bottom, BorderLayout.SOUTH);
		name.getDocument().addDocumentListener(dirty);
		text.getDocument().addDocumentListener(dirty);
		text.getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) {
				updateLines();
				updateRestore();
			}
			public void removeUpdate(DocumentEvent e) {
				updateLines();
				updateRestore();
			}
			public void changedUpdate(DocumentEvent e) {
				updateLines();
				updateRestore();
			}
		});
		type.addActionListener(e -> {
			ResourceType selected = (ResourceType) types.getSelectedItem();
			((CardLayout) content.getLayout()).show(content,
					selected == ResourceType.IMAGE ? "image" : selected == null ? "none" : "text");
			dirty.setDirty(true);
			updateRestore();
		});
		image.addPropertyChangeListener("image", dirty);
		image.addPropertyChangeListener("image", event -> updateRestore());
		dirty.addDirtyListener(value -> status.setText(AppLocal.getIntString(value ? "Admin.Unsaved" : "Admin.Saved")));
		restore.addActionListener(e -> {
			if (original == null || JOptionPane.showConfirmDialog(this, AppLocal.getIntString("Admin.ConfirmRestore"),
					AppLocal.getIntString("Admin.RestoreOriginal"),
					JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
				return;
			if (types.getSelectedItem() == ResourceType.IMAGE) {
				image.setImage(ImageUtils.readImage(original));
				loadedImage = image.getImage();
				loadedImageBytes = original;
			} else if (types.getSelectedItem() == ResourceType.BINARY)
				text.setText(Base64Encoder.encodeChunked(original));
			else
				text.setText(new String(original, java.nio.charset.StandardCharsets.UTF_8));
			dirty.setDirty(true);
			updateRestore();
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

	void setBrowsableData(BrowsableEditableData data) {
		this.data = data;
	}
	private void updateLines() {
		int count = text.getLineCount();
		StringBuilder numbers = new StringBuilder();
		for (int i = 1; i <= count; i++)
			numbers.append(i).append('\n');
		lineNumbers.setText(numbers.toString());
	}
	private void updateRestore() {
		ResourceType selected = (ResourceType) types.getSelectedItem();
		original = selected == null ? null : ResourceOriginal.bytes(name.getText(), (Integer) selected.getKey());
		byte[] current = selected == ResourceType.TEXT
				? text.getText().getBytes(java.nio.charset.StandardCharsets.UTF_8)
				: selected == ResourceType.IMAGE
						? image.getImage() == loadedImage ? loadedImageBytes : ImageUtils.writeImage(image.getImage())
						: selected == ResourceType.BINARY ? Base64Encoder.decode(text.getText()) : null;
		boolean modified = original != null && !Arrays.equals(original, current);
		restore.setVisible(name.isEnabled() && modified);
		restore.setEnabled(name.isEnabled() && modified);
	}
	private void editable(boolean enabled) {
		name.setEnabled(enabled);
		type.setEnabled(enabled);
		text.setEnabled(enabled);
		image.setEnabled(enabled);
		save.setEnabled(enabled);
		discard.setEnabled(enabled);
		delete.setEnabled(enabled && id != null);
		updateRestore();
	}
	public void writeValueEOF() {
		id = null;
		loadedImageBytes = null;
		loadedImage = null;
		name.setText("");
		types.setSelectedItem(null);
		text.setText("");
		image.setImage(null);
		editable(false);
	}
	public void writeValueInsert() {
		writeValueEOF();
		types.setSelectedItem(ResourceType.TEXT);
		editable(true);
	}
	public void writeValueDelete(Object value) {
		writeValueEdit(value);
		editable(false);
	}
	public void writeValueEdit(Object value) {
		Object[] row = (Object[]) value;
		id = row[0];
		name.setText((String) row[1]);
		types.setSelectedKey(row[2]);
		ResourceType selected = (ResourceType) types.getSelectedItem();
		if (selected == ResourceType.IMAGE) {
			text.setText("");
			image.setImage(ImageUtils.readImage((byte[]) row[3]));
			loadedImage = image.getImage();
			loadedImageBytes = (byte[]) row[3];
		} else {
			loadedImage = null;
			loadedImageBytes = null;
			image.setImage(null);
			text.setText(selected == ResourceType.BINARY
					? row[3] == null ? "" : Base64Encoder.encodeChunked((byte[]) row[3])
					: Formats.BYTEA.formatValue(row[3]));
			text.setCaretPosition(0);
		}
		editable(true);
	}
	public Object createValue() throws BasicException {
		ResourceType selected = (ResourceType) types.getSelectedItem();
		if (selected == null)
			throw new BasicException(AppLocal.getIntString("label.type"));
		Object bytes = selected == ResourceType.IMAGE
				? image.getImage() == loadedImage ? loadedImageBytes : ImageUtils.writeImage(image.getImage())
				: selected == ResourceType.BINARY
						? Base64Encoder.decode(text.getText())
						: Formats.BYTEA.parseValue(text.getText());
		return new Object[]{id == null ? UUID.randomUUID().toString() : id, name.getText(), selected.getKey(), bytes};
	}
	public Component getComponent() {
		return this;
	}
	public void refresh() {
	}
}
