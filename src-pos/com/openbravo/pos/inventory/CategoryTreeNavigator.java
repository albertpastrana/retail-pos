package com.openbravo.pos.inventory;

import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.data.user.BrowseListener;
import com.openbravo.data.user.BrowsableEditableData;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.DataLogicSales;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListModel;
import javax.swing.UIManager;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

final class CategoryTreeNavigator extends JPanel implements CategoryNavigator, BrowseListener, TreeSelectionListener {
	private final BrowsableEditableData data;
	private final DataLogicSales sales;
	private final JTree tree;
	private String filter = "";
	private boolean updating;
	private int matchCount;
	private int totalCount;
	private final JLabel results = new JLabel();

	CategoryTreeNavigator(BrowsableEditableData data, DataLogicSales sales) {
		this.data = data;
		this.sales = sales;
		tree = new JTree(new DefaultTreeModel(new DefaultMutableTreeNode()));
		tree.setRootVisible(false);
		tree.setShowsRootHandles(true);
		tree.setRowHeight(24);
		tree.setCellRenderer(new CategoryTreeCellRenderer());
		tree.getSelectionModel().addTreeSelectionListener(this);
		data.addBrowseListener(this);
		data.getListModel().addListDataListener(new ListDataListener() {
			public void intervalAdded(ListDataEvent event) {
				rebuild();
			}

			public void intervalRemoved(ListDataEvent event) {
				rebuild();
			}

			public void contentsChanged(ListDataEvent event) {
				rebuild();
			}
		});
		JTextField search = new JTextField();
		search.setToolTipText(AppLocal.getIntString("category.search"));
		search.putClientProperty("JTextField.placeholderText", AppLocal.getIntString("category.search"));
		javax.swing.Timer timer = new javax.swing.Timer(300, new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				setFilter(search.getText());
				updateResults();
			}
		});
		timer.setRepeats(false);
		search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
			public void insertUpdate(javax.swing.event.DocumentEvent event) {
				timer.restart();
			}
			public void removeUpdate(javax.swing.event.DocumentEvent event) {
				timer.restart();
			}
			public void changedUpdate(javax.swing.event.DocumentEvent event) {
				timer.restart();
			}
		});
		JButton collapse = new JButton("<html><u>" + AppLocal.getIntString("category.collapse") + "</u></html>");
		collapse.setFont(collapse.getFont().deriveFont(12f));
		collapse.setBorderPainted(false);
		collapse.setContentAreaFilled(false);
		collapse.setFocusPainted(false);
		collapse.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		collapse.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				collapseAll();
			}
		});
		JPanel searchLine = new JPanel(new BorderLayout());
		searchLine.add(search, BorderLayout.CENTER);
		JPanel resultLine = new JPanel(new BorderLayout());
		results.setFont(results.getFont().deriveFont(12f));
		resultLine.add(results, BorderLayout.WEST);
		resultLine.add(collapse, BorderLayout.EAST);
		JPanel controls = new JPanel(new BorderLayout(0, 6));
		controls.add(searchLine, BorderLayout.NORTH);
		controls.add(resultLine, BorderLayout.CENTER);
		setLayout(new BorderLayout(0, 6));
		add(controls, BorderLayout.NORTH);
		add(new JScrollPane(tree), BorderLayout.CENTER);
		rebuild();
	}

	public void setFilter(String value) {
		filter = value == null ? "" : value.trim().toLowerCase();
		rebuild();
	}

	public void collapseAll() {
		for (int row = tree.getRowCount() - 1; row >= 0; row--)
			tree.collapseRow(row);
	}

	public int getMatchCount() {
		return matchCount;
	}

	public int getTotalCount() {
		return totalCount;
	}

	private void updateResults() {
		results.setText(AppLocal.getIntString("category.results").replace("{0}", Integer.toString(matchCount))
				.replace("{1}", Integer.toString(totalCount)));
	}

	private void rebuild() {
		List<CategoryNode> categories = new ArrayList<CategoryNode>();
		Map<String, CategoryNode> byId = new HashMap<String, CategoryNode>();
		ListModel<?> model = data.getListModel();
		for (int i = 0; i < model.getSize(); i++) {
			CategoryNode node = new CategoryNode((Object[]) model.getElementAt(i), i, sales);
			categories.add(node);
			byId.put(node.id(), node);
		}
		totalCount = categories.size();
		matchCount = 0;
		for (CategoryNode node : categories) {
			if (filter.isEmpty() || node.fullPath().toLowerCase().contains(filter))
				matchCount++;
			CategoryNode parent = byId.get(node.parentId());
			if (parent != null) {
				node.parent = parent;
				parent.children.add(node);
			}
		}

		DefaultMutableTreeNode root = new DefaultMutableTreeNode();
		for (CategoryNode node : categories)
			if (node.parentId() == null || !byId.containsKey(node.parentId()))
				addNode(root, node);
		tree.setModel(new DefaultTreeModel(root));
		if (!filter.isEmpty())
			expandAll();
		updateIndex(data.getIndex(), model.getSize());
		updateResults();
	}

	private void addNode(DefaultMutableTreeNode parent, CategoryNode node) {
		if (filter.isEmpty() || node.fullPath().toLowerCase().contains(filter) || hasMatchingChild(node)) {
			DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(node);
			parent.add(treeNode);
			Collections.sort(node.children, new Comparator<CategoryNode>() {
				public int compare(CategoryNode left, CategoryNode right) {
					return left.name().compareToIgnoreCase(right.name());
				}
			});
			for (CategoryNode child : node.children)
				addNode(treeNode, child);
		}
	}

	private boolean hasMatchingChild(CategoryNode node) {
		if (node.fullPath().toLowerCase().contains(filter))
			return true;
		for (CategoryNode child : node.children)
			if (hasMatchingChild(child))
				return true;
		return false;
	}

	private void expandAll() {
		for (int row = 0; row < tree.getRowCount(); row++)
			tree.expandRow(row);
	}

	public void updateIndex(int index, int count) {
		updating = true;
		try {
			tree.clearSelection();
			if (index < 0 || index >= count)
				return;
			String id = ((Object[]) data.getListModel().getElementAt(index))[0].toString();
			TreePath path = findPath((DefaultMutableTreeNode) tree.getModel().getRoot(), id, new ArrayList<Object>());
			if (path != null) {
				tree.setSelectionPath(path);
				tree.scrollPathToVisible(path);
			}
		} finally {
			updating = false;
		}
	}

	private TreePath findPath(DefaultMutableTreeNode node, String id, List<Object> path) {
		path.add(node);
		if (node.getUserObject() instanceof CategoryNode && id.equals(((CategoryNode) node.getUserObject()).id()))
			return new TreePath(path.toArray());
		for (int i = 0; i < node.getChildCount(); i++) {
			TreePath result = findPath((DefaultMutableTreeNode) node.getChildAt(i), id, path);
			if (result != null)
				return result;
		}
		path.remove(path.size() - 1);
		return null;
	}

	public void valueChanged(TreeSelectionEvent event) {
		if (event.getNewLeadSelectionPath() == null || updating || data.isAdjusting())
			return;
		Object value = ((DefaultMutableTreeNode) event.getNewLeadSelectionPath().getLastPathComponent())
				.getUserObject();
		if (!(value instanceof CategoryNode))
			return;
		CategoryNode node = (CategoryNode) value;
		try {
			data.moveTo(node.index);
			if (data.getIndex() != node.index)
				updateIndex(data.getIndex(), data.getListModel().getSize());
		} catch (BasicException e) {
			new MessageInf(MessageInf.SGN_NOTICE, AppLocal.getIntString("message.nomove"), e).show(this);
		}
	}

	private static final class CategoryTreeCellRenderer extends JPanel implements TreeCellRenderer {
		private final JLabel name = new JLabel();

		CategoryTreeCellRenderer() {
			setLayout(new BorderLayout());
			setOpaque(true);
			add(name, BorderLayout.CENTER);
		}

		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
				boolean leaf, int row, boolean hasFocus) {
			Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
			Color background = selected ? UIManager.getColor("Tree.selectionBackground") : tree.getBackground();
			if (background == null)
				background = tree.getBackground();
			Color foreground = selected ? UIManager.getColor("Tree.selectionForeground") : tree.getForeground();
			if (foreground == null)
				foreground = tree.getForeground();
			setBackground(background);
			name.setForeground(foreground);
			if (!(userObject instanceof CategoryNode)) {
				name.setText("");
				return this;
			}
			CategoryNode node = (CategoryNode) userObject;
			name.setText(node.name());
			return this;
		}
	}

	private static final class CategoryNode {
		private final Object[] value;
		private final int index;
		private final DataLogicSales sales;
		private final List<CategoryNode> children = new ArrayList<CategoryNode>();
		private CategoryNode parent;

		CategoryNode(Object[] value, int index, DataLogicSales sales) {
			this.value = value;
			this.index = index;
			this.sales = sales;
		}

		String id() {
			return (String) value[0];
		}

		String name() {
			return value[1] == null ? "" : value[1].toString();
		}

		String parentId() {
			return (String) value[2];
		}

		String fullPath() {
			return fullPath(new HashSet<String>());
		}

		private String fullPath(Set<String> visited) {
			if (!visited.add(id()))
				return name();
			return parent == null ? name() : parent.fullPath(visited) + " / " + name();
		}

		int products() {
			try {
				return sales.getCategoryProductCount(id());
			} catch (BasicException e) {
				return 0;
			}
		}
	}
}
