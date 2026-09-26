package com.openbravo.pos.admin;

import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.data.user.BrowsableEditableData;
import com.openbravo.data.user.BrowseListener;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 * A searchable view of the existing browsable model; indices always refer to
 * its unfiltered rows.
 */
final class AdminListNavigator extends JPanel implements BrowseListener {
	private final BrowsableEditableData data;
	private final DefaultListModel<Object> rows = new DefaultListModel<>();
	private final JList<Object> list = new JList<>(rows);
	private final JTextField search = new JTextField();
	private final List<Integer> indices = new ArrayList<>();
	private final Function<Object, String> searchText;
	private boolean adjusting;
	private Predicate<Object> filter = row -> true;

	AdminListNavigator(BrowsableEditableData data, String searchHint, Function<Object, String> searchText,
			ListCellRenderer<Object> renderer) {
		super(new BorderLayout(0, 8));
		this.data = data;
		this.searchText = searchText;
		search.putClientProperty("JTextField.placeholderText", searchHint);
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
		list.setCellRenderer(renderer);
		list.addListSelectionListener(e -> {
			if (e.getValueIsAdjusting() || adjusting || data.isAdjusting() || list.getSelectedIndex() < 0)
				return;
			int index = indices.get(list.getSelectedIndex());
			try {
				data.moveTo(index);
			} catch (BasicException ex) {
				new MessageInf(ex).show(this);
			}
			updateIndex(data.getIndex(), data.getListModel().getSize());
		});
		data.getListModel().addListDataListener(new ListDataListener() {
			public void intervalAdded(ListDataEvent e) {
				rebuild();
			}
			public void intervalRemoved(ListDataEvent e) {
				rebuild();
			}
			public void contentsChanged(ListDataEvent e) {
				rebuild();
			}
		});
		data.addBrowseListener(this);
		add(search, BorderLayout.NORTH);
		add(new JScrollPane(list), BorderLayout.CENTER);
		rebuild();
	}

	void setSearchText(String text) {
		search.setText(text);
	}

	void setFilter(Predicate<Object> filter) {
		this.filter = filter;
		rebuild();
	}

	private void rebuild() {
		adjusting = true;
		indices.clear();
		rows.clear();
		String term = search.getText().strip().toLowerCase(Locale.ROOT);
		for (int i = 0; i < data.getListModel().getSize(); i++) {
			Object row = data.getListModel().getElementAt(i);
			if (filter.test(row) && searchText.apply(row).toLowerCase(Locale.ROOT).contains(term)) {
				indices.add(i);
				rows.addElement(row);
			}
		}
		adjusting = false;
		updateIndex(data.getIndex(), data.getListModel().getSize());
	}

	@Override
	public void updateIndex(int index, int count) {
		adjusting = true;
		int visible = indices.indexOf(index);
		list.setSelectedIndex(visible);
		if (visible >= 0)
			list.ensureIndexIsVisible(visible);
		adjusting = false;
	}
}
