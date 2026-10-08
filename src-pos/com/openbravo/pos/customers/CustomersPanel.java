package com.openbravo.pos.customers;

import com.openbravo.basic.BasicException;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.AppView;
import com.openbravo.pos.forms.BeanFactoryApp;
import com.openbravo.pos.forms.BeanFactoryException;
import com.openbravo.pos.forms.JPanelView;
import com.openbravo.pos.theme.RetailPOSColors;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.text.Collator;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import javax.swing.RowSorter;

public class CustomersPanel extends JPanel implements JPanelView, BeanFactoryApp {
	private static final Color DARK_RED = new Color(145, 42, 35);
	private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00",
			DecimalFormatSymbols.getInstance(Locale.forLanguageTag("ca")));
	private final CustomersTableModel model = new CustomersTableModel();
	private final JTable table = new JTable(model);
	private final TableRowSorter<CustomersTableModel> sorter = new TableRowSorter<CustomersTableModel>(model);
	private final HintTextField search = new HintTextField("Cerca per nom o telèfon…");
	private final JButton clearSearch = new JButton("×");
	private final JCheckBox debtOnly = new JCheckBox("Només amb deute");
	private final JCheckBox inactive = new JCheckBox("Mostra inactives");
	private final JTextField name = new JTextField();
	private final JTextField phone = new JTextField();
	private final JTextField maxDebt = new JTextField();
	private final JTextArea notes = new JTextArea(6, 20);
	private final JLabel detailTitle = new JLabel();
	private final JLabel detailStatus = new JLabel();
	private final JLabel debtValue = new JLabel();
	private final JLabel state = new JLabel(" ");
	private final JLabel count = new JLabel();
	private final JLabel listCount = new JLabel();
	private final JLabel totalDebt = new JLabel();
	private final JButton save = new JButton("Desa");
	private final JButton cancel = new JButton("Cancel·la");
	private final JButton archive = new JButton();
	private final JButton newCustomer = new JButton("+ Nova clienta");
	private DataLogicCustomers customers;
	private CustomerInfoExt selected;
	private boolean creating;
	private boolean loading;
	private boolean resettingFilters;
	private String originalName = "", originalPhone = "", originalNotes = "";
	private boolean originalVisible;
	private double originalMaxDebt;
	private final Timer searchTimer;

	public CustomersPanel() {
		setLayout(new BorderLayout(0, 8));
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		add(buildHeader(), BorderLayout.NORTH);
		add(buildBody(), BorderLayout.CENTER);
		searchTimer = new Timer(200, e -> loadCustomers());
		searchTimer.setRepeats(false);
		search.getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) {
				searchTimer.restart();
				updateActions();
			}
			public void removeUpdate(DocumentEvent e) {
				searchTimer.restart();
				updateActions();
			}
			public void changedUpdate(DocumentEvent e) {
				searchTimer.restart();
				updateActions();
			}
		});
		DocumentListener dirty = new DocumentListener() {
			public void insertUpdate(DocumentEvent e) {
				updateActions();
			}
			public void removeUpdate(DocumentEvent e) {
				updateActions();
			}
			public void changedUpdate(DocumentEvent e) {
				updateActions();
			}
		};
		name.getDocument().addDocumentListener(dirty);
		phone.getDocument().addDocumentListener(dirty);
		maxDebt.getDocument().addDocumentListener(dirty);
		notes.getDocument().addDocumentListener(dirty);
		newCustomer.addActionListener(e -> startNewCustomer());
		debtOnly.addActionListener(e -> {
			if (!resettingFilters)
				loadCustomers();
		});
		inactive.addActionListener(e -> {
			if (!resettingFilters)
				loadCustomers();
		});
		clearSearch.addActionListener(e -> search.setText(""));
		debtOnly.setFont(debtOnly.getFont().deriveFont(Font.PLAIN, debtOnly.getFont().getSize2D() + 1f));
		inactive.setFont(inactive.getFont().deriveFont(Font.PLAIN, inactive.getFont().getSize2D() + 1f));
		save.addActionListener(e -> saveCustomer());
		cancel.addActionListener(e -> cancelEdit());
		archive.addActionListener(e -> toggleArchive());
		installActions();
		setDetailEnabled(false);
	}

	private JPanel buildHeader() {
		JPanel header = new JPanel(new BorderLayout(18, 0));
		header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(205, 205, 205)));
		count.setForeground(Color.GRAY);
		header.add(count, BorderLayout.WEST);
		JPanel debt = new JPanel(new FlowLayout(FlowLayout.TRAILING, 8, 0));
		debt.add(new JLabel("Deute total pendent"));
		totalDebt.setFont(totalDebt.getFont().deriveFont(Font.BOLD, 20f));
		totalDebt.setForeground(RetailPOSColors.ink());
		debt.add(totalDebt);
		header.add(debt, BorderLayout.EAST);
		return header;
	}

	private JPanel buildBody() {
		JPanel left = new JPanel(new BorderLayout(0, 6));
		JPanel tools = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridy = 0;
		c.insets = new Insets(0, 0, 0, 8);
		c.fill = GridBagConstraints.HORIZONTAL;
		search.setMinimumSize(new Dimension(200, search.getPreferredSize().height));
		JPanel searchBox = new JPanel(new BorderLayout());
		searchBox.add(search, BorderLayout.CENTER);
		clearSearch.setMargin(new Insets(0, 7, 0, 7));
		clearSearch.setToolTipText("Neteja la cerca");
		searchBox.add(clearSearch, BorderLayout.EAST);
		c.weightx = 1;
		tools.add(searchBox, c);
		c.weightx = 0;
		tools.add(debtOnly, c);
		tools.add(inactive, c);
		c.weightx = 1;
		tools.add(BoxPanel.glue(), c);
		c.weightx = 0;
		newCustomer.setMargin(new Insets(9, 14, 9, 14));
		RetailPOSColors.primaryButton(newCustomer);
		newCustomer.setFont(newCustomer.getFont().deriveFont(Font.BOLD));
		tools.add(newCustomer, c);
		left.add(tools, BorderLayout.NORTH);
		table.setRowSorter(sorter);
		sorter.setComparator(0, Collator.getInstance(new Locale("ca")));
		sorter.setSortKeys(Collections.singletonList(new RowSorter.SortKey(0, javax.swing.SortOrder.ASCENDING)));
		table.setAutoCreateRowSorter(false);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setRowHeight(34);
		table.setShowVerticalLines(false);
		table.setShowHorizontalLines(true);
		table.setGridColor(new Color(235, 235, 235));
		table.setFillsViewportHeight(true);
		table.getSelectionModel().addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting() && !loading)
				selectionChanged();
		});
		table.getColumnModel().getColumn(2).setCellRenderer(new DebtRenderer());
		table.getColumnModel().getColumn(0).setCellRenderer(new NameRenderer());
		left.add(new JScrollPane(table), BorderLayout.CENTER);
		JPanel footer = new JPanel(new BorderLayout());
		footer.setBorder(BorderFactory.createEmptyBorder(5, 8, 0, 8));
		footer.add(listCount, BorderLayout.WEST);
		footer.add(new JLabel("↑↓ moure · Enter editar · Ctrl+N nova"), BorderLayout.EAST);
		left.add(footer, BorderLayout.SOUTH);
		search.requestFocusInWindow();

		JPanel detail = new JPanel(new BorderLayout());
		detail.setBorder(BorderFactory.createLineBorder(new Color(180, 180, 180)));
		JPanel detailHead = new JPanel(new GridLayout(2, 1));
		detailHead.setBorder(BorderFactory.createEmptyBorder(14, 14, 12, 14));
		detailTitle.setFont(detailTitle.getFont().deriveFont(Font.BOLD, 20f));
		detailStatus.setForeground(Color.GRAY);
		detailHead.add(detailTitle);
		detailHead.add(detailStatus);
		detail.add(detailHead, BorderLayout.NORTH);
		JPanel content = new JPanel(new GridBagLayout());
		content.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
		GridBagConstraints f = new GridBagConstraints();
		f.gridx = 0;
		f.weightx = 1;
		f.fill = GridBagConstraints.HORIZONTAL;
		f.anchor = GridBagConstraints.NORTHWEST;
		f.insets = new Insets(0, 0, 5, 0);
		addField(content, f, 0, "Nom", name);
		addField(content, f, 2, "Telèfon", phone);
		f.gridy = 4;
		content.add(new JLabel("Notes"), f);
		f.gridy = 5;
		JScrollPane notesScroll = new JScrollPane(notes);
		notesScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		notesScroll.setMinimumSize(new Dimension(0, notesScroll.getPreferredSize().height));
		content.add(notesScroll, f);
		addField(content, f, 6, "Deute permès (€)", maxDebt);
		f.gridy = 8;
		content.add(debtBlock(), f);
		f.gridy = 9;
		f.weighty = 1;
		f.fill = GridBagConstraints.BOTH;
		content.add(BoxPanel.glue(), f);
		detail.add(content, BorderLayout.CENTER);
		JPanel bottom = new JPanel(new BorderLayout());
		bottom.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(210, 210, 210)));
		JPanel actions = new JPanel(new GridLayout(1, 2, 8, 0));
		actions.setBorder(BorderFactory.createEmptyBorder(12, 14, 6, 14));
		save.setMargin(new Insets(10, 12, 10, 12));
		cancel.setMargin(new Insets(10, 12, 10, 12));
		RetailPOSColors.primaryButton(save);
		save.setFont(save.getFont().deriveFont(Font.BOLD));
		cancel.setContentAreaFilled(false);
		actions.add(save);
		actions.add(cancel);
		bottom.add(actions, BorderLayout.NORTH);
		JPanel statePanel = new JPanel(new BorderLayout());
		statePanel.setBorder(BorderFactory.createEmptyBorder(0, 14, 8, 14));
		statePanel.add(state, BorderLayout.WEST);
		bottom.add(statePanel, BorderLayout.CENTER);
		archive.setBorderPainted(false);
		archive.setContentAreaFilled(false);
		archive.setForeground(DARK_RED);
		JPanel archivePanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
		archivePanel.add(archive);
		bottom.add(archivePanel, BorderLayout.SOUTH);
		detail.add(bottom, BorderLayout.SOUTH);
		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, detail);
		split.setResizeWeight(1.0);
		split.setDividerLocation(-390);
		detail.setMinimumSize(new Dimension(340, 0));
		detail.setPreferredSize(new Dimension(390, 0));
		return panelWith(split);
	}

	private JPanel panelWith(Component component) {
		JPanel p = new JPanel(new BorderLayout());
		p.add(component);
		return p;
	}
	private void addField(JPanel p, GridBagConstraints c, int row, String label, JComponent field) {
		c.gridy = row;
		p.add(new JLabel(label), c);
		c.gridy = row + 1;
		p.add(field, c);
	}
	private JPanel debtBlock() {
		JPanel p = new JPanel(new BorderLayout(4, 4));
		p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(215, 215, 215)),
				BorderFactory.createEmptyBorder(10, 12, 10, 12)));
		p.add(new JLabel("Deute acumulat"), BorderLayout.WEST);
		debtValue.setFont(debtValue.getFont().deriveFont(Font.BOLD, 20f));
		debtValue.setForeground(RetailPOSColors.ink());
		p.add(debtValue, BorderLayout.EAST);
		JLabel note = new JLabel("<html>El deute acumulat es calcula a partir dels albarans.</html>");
		note.setForeground(Color.GRAY);
		p.add(note, BorderLayout.SOUTH);
		return p;
	}

	private void installActions() {
		getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke("control N"), "new");
		getActionMap().put("new", new AbstractAction() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				startNewCustomer();
			}
		});
		getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke("control S"), "save");
		getActionMap().put("save", new AbstractAction() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				saveCustomer();
			}
		});
		getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke("ESCAPE"), "cancel");
		getActionMap().put("cancel", new AbstractAction() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				cancelEdit();
			}
		});
		table.getInputMap(WHEN_FOCUSED).put(javax.swing.KeyStroke.getKeyStroke("ENTER"), "edit");
		table.getActionMap().put("edit", new AbstractAction() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				name.requestFocusInWindow();
			}
		});
	}

	public void init(AppView app) throws BeanFactoryException {
		customers = app.getBean(DataLogicCustomers.class);
	}
	public Object getBean() {
		return this;
	}
	public String getTitle() {
		return AppLocal.getIntString("Menu.CustomersManagement");
	}
	public JComponent getComponent() {
		return this;
	}
	public void activate() throws BasicException {
		resetFilters();
		loadCustomers();
		SwingUtilities.invokeLater(() -> search.requestFocusInWindow());
	}

	private void resetFilters() {
		resettingFilters = true;
		search.setText("");
		debtOnly.setSelected(false);
		inactive.setSelected(false);
		resettingFilters = false;
		searchTimer.stop();
	}
	public boolean deactivate() {
		return confirmDiscard();
	}

	private void loadCustomers() {
		if (customers == null)
			return;
		loading = true;
		final String query = search.getText();
		final boolean debt = debtOnly.isSelected(), showInactive = inactive.isSelected();
		new SwingWorker<List<CustomerInfoExt>, Void>() {
			private List<CustomerInfoExt> all;
			protected List<CustomerInfoExt> doInBackground() throws Exception {
				all = customers.searchCustomerSummaries("", false, true);
				return customers.searchCustomerSummaries(query, debt, showInactive);
			}
			protected void done() {
				try {
					List<CustomerInfoExt> result = get();
					model.setCustomers(result);
					int active = 0;
					double total = 0;
					for (CustomerInfoExt c : all) {
						if (c.isVisible())
							active++;
						if (c.getCurdebt() != null)
							total += c.getCurdebt();
					}
					count.setText(active + " clientes actives");
					totalDebt.setText(MONEY.format(total) + " €");
					totalDebt.setForeground(total > 0 ? RetailPOSColors.dangerText() : RetailPOSColors.ink());
					listCount.setText(result.size() + " de " + all.size() + " fitxes");
					if (result.isEmpty()) {
						if (creating || isDirty())
							table.clearSelection();
						else
							clearDetail();
					} else if (creating || isDirty()) {
						if (selected == null || !selectId(selected.getId()))
							table.clearSelection();
					} else {
						if (selected == null || !selectId(selected.getId()))
							table.setRowSelectionInterval(0, 0);
						showCustomer(model.customers.get(table.convertRowIndexToModel(table.getSelectedRow())));
					}
				} catch (Exception e) {
					state.setForeground(DARK_RED);
					state.setText("No s'han pogut carregar les fitxes");
				} finally {
					loading = false;
				}
			}
		}.execute();
	}

	private void selectionChanged() {
		int view = table.getSelectedRow();
		if (view < 0)
			return;
		if (!confirmDiscard()) {
			loading = true;
			if (selected == null || !selectId(selected.getId()))
				table.clearSelection();
			loading = false;
			return;
		}
		showCustomer(model.customers.get(table.convertRowIndexToModel(view)));
	}
	private boolean selectId(String id) {
		for (int i = 0; i < model.customers.size(); i++)
			if (model.customers.get(i).getId().equals(id)) {
				int v = table.convertRowIndexToView(i);
				table.setRowSelectionInterval(v, v);
				return true;
			}
		return false;
	}
	private void showCustomer(CustomerInfoExt c) {
		selected = c;
		creating = false;
		name.setText(value(c.getName()));
		phone.setText(value(c.getPhone()));
		notes.setText(value(c.getNotes()));
		originalName = name.getText();
		originalPhone = phone.getText();
		originalMaxDebt = value(c.getMaxdebt());
		maxDebt.setText(MONEY.format(originalMaxDebt));
		originalNotes = notes.getText();
		originalVisible = c.isVisible();
		detailTitle.setText(originalName);
		detailStatus.setText(c.isVisible() ? "Fitxa activa" : "Fitxa arxivada");
		debtValue.setText(MONEY.format(c.getCurdebt() == null ? 0 : c.getCurdebt()) + " €");
		debtValue.setForeground(
				c.getCurdebt() != null && c.getCurdebt() > 0 ? RetailPOSColors.dangerText() : RetailPOSColors.ink());
		archive.setText(c.isVisible() ? "Arxiva la fitxa…" : "Reactiva la fitxa");
		state.setForeground(Color.GRAY);
		state.setText(" ");
		setDetailEnabled(true);
	}
	private String value(String s) {
		return s == null ? "" : s;
	}
	private void startNewCustomer() {
		if (!confirmDiscard())
			return;
		selected = null;
		creating = true;
		name.setText("");
		phone.setText("");
		maxDebt.setText(MONEY.format(100.0));
		notes.setText("");
		originalName = originalPhone = originalNotes = "";
		originalMaxDebt = 100.0;
		originalVisible = true;
		detailTitle.setText("Nova clienta");
		detailStatus.setText("Nova clienta");
		debtValue.setText("0,00 €");
		debtValue.setForeground(RetailPOSColors.ink());
		archive.setText("");
		archive.setVisible(false);
		setDetailEnabled(true);
		name.requestFocusInWindow();
	}
	private boolean isDirty() {
		return !name.getText().equals(originalName)
				|| !CustomerInfo.normalizePhone(phone.getText()).equals(CustomerInfo.normalizePhone(originalPhone))
				|| !isMaxDebtUnchanged() || !notes.getText().equals(originalNotes)
				|| (selected != null && !creating && originalVisible != selected.isVisible());
	}
	private boolean isMaxDebtUnchanged() {
		try {
			return Double.compare(parseMaxDebt(), originalMaxDebt) == 0;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	private double parseMaxDebt() {
		String text = maxDebt.getText().trim().replace(" ", "");
		if (text.matches("\\d{1,3}(\\.\\d{3})+(,\\d+)?"))
			text = text.replace(".", "");
		text = text.replace(',', '.');
		if (text.isEmpty())
			return 0.0;
		double value = Double.parseDouble(text);
		if (!Double.isFinite(value) || value < 0.0)
			throw new NumberFormatException();
		return value;
	}
	private double value(Double value) {
		return value == null ? 0.0 : value.doubleValue();
	}
	private boolean confirmDiscard() {
		if (!isDirty())
			return true;
		String who = creating ? "Nova clienta" : originalName;
		int answer = JOptionPane.showConfirmDialog(this, "Tens canvis sense desar a «" + who + "». Els vols descartar?",
				"Canvis sense desar", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
		return answer == JOptionPane.YES_OPTION;
	}
	private void cancelEdit() {
		if (creating) {
			clearDetail();
			return;
		}
		if (selected != null)
			showCustomer(selected);
	}
	private void saveCustomer() {
		if (!isDirty())
			return;
		final boolean wasCreating = creating;
		final String n = name.getText().trim(), p = CustomerInfo.normalizePhone(phone.getText()), nt = notes.getText();
		final double md;
		try {
			md = parseMaxDebt();
		} catch (NumberFormatException e) {
			state.setForeground(DARK_RED);
			state.setText("El deute permès ha de ser un import positiu o zero");
			return;
		}
		if (n.isEmpty()) {
			state.setForeground(DARK_RED);
			state.setText("El nom és obligatori");
			return;
		}
		setDetailEnabled(false);
		new SwingWorker<CustomerInfoExt, Void>() {
			protected CustomerInfoExt doInBackground() throws Exception {
				if (creating)
					return customers.createCustomer(n, p, nt, md);
				selected.setName(n);
				selected.setPhone(p);
				selected.setMaxdebt(md);
				selected.setNotes(nt);
				customers.updateCustomer(selected);
				return selected;
			}
			protected void done() {
				try {
					selected = get();
					creating = false;
					showCustomer(selected);
					state.setForeground(new Color(35, 120, 65));
					state.setText((wasCreating ? "Clienta creada · " : "Desat ✓ ")
							+ new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date()));
					loadCustomers();
				} catch (Exception e) {
					setDetailEnabled(true);
					state.setForeground(DARK_RED);
					state.setText("No s'ha pogut desar la fitxa");
				}
			}
		}.execute();
	}
	private void toggleArchive() {
		if (selected == null || creating)
			return;
		String action = selected.isVisible() ? "Arxivar" : "Reactivar";
		int answer = JOptionPane.showConfirmDialog(this, action + " aquesta fitxa? Conserva l'historial i el deute.",
				action + " fitxa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		if (answer != JOptionPane.YES_OPTION)
			return;
		final boolean visible = !selected.isVisible();
		new SwingWorker<Void, Void>() {
			protected Void doInBackground() throws Exception {
				selected.setVisible(visible);
				customers.updateCustomer(selected);
				return null;
			}
			protected void done() {
				try {
					state.setForeground(new Color(35, 120, 65));
					state.setText((visible ? "Fitxa reactivada" : "Fitxa arxivada") + " · "
							+ new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date()));
					loadCustomers();
				} catch (Exception e) {
					state.setForeground(DARK_RED);
					state.setText("No s'ha pogut actualitzar l'estat");
				}
			}
		}.execute();
	}
	private void clearDetail() {
		selected = null;
		creating = false;
		name.setText("");
		phone.setText("");
		maxDebt.setText("");
		notes.setText("");
		originalName = originalPhone = originalNotes = "";
		originalMaxDebt = 0.0;
		detailTitle.setText("");
		detailStatus.setText("Selecciona una fila de la llista");
		debtValue.setText("");
		archive.setVisible(false);
		setDetailEnabled(false);
	}
	private void updateActions() {
		save.setEnabled((creating || selected != null) && isDirty());
		cancel.setEnabled((creating || selected != null) && isDirty());
	}
	private void setDetailEnabled(boolean enabled) {
		name.setEnabled(enabled);
		phone.setEnabled(enabled);
		maxDebt.setEnabled(enabled);
		notes.setEnabled(enabled);
		updateActions();
		archive.setEnabled(enabled && !creating);
		archive.setVisible(enabled && !creating);
	}

	private static class CustomersTableModel extends AbstractTableModel {
		private List<CustomerInfoExt> customers = Collections.emptyList();
		public void setCustomers(List<CustomerInfoExt> c) {
			customers = c;
			fireTableDataChanged();
		}
		public int getRowCount() {
			return customers.size();
		}
		public int getColumnCount() {
			return 3;
		}
		public String getColumnName(int c) {
			return c == 0 ? "Nom" : c == 1 ? "Telèfon" : "Deute";
		}
		public Object getValueAt(int r, int c) {
			CustomerInfoExt x = customers.get(r);
			return c == 0
					? x.getName() + (x.isVisible() ? "" : "  (arxivada)")
					: c == 1 ? x.getPhone() : (x.getCurdebt() == null ? 0 : x.getCurdebt());
		}
		public Class<?> getColumnClass(int c) {
			return c == 2 ? Double.class : String.class;
		}
	}
	private static class DebtRenderer extends DefaultTableCellRenderer {
		DebtRenderer() {
			setHorizontalAlignment(SwingConstants.RIGHT);
			setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
		}
		public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int r, int c) {
			super.getTableCellRendererComponent(t, v, sel, focus, r, c);
			double d = v == null ? 0 : ((Number) v).doubleValue();
			setText(MONEY.format(d));
			if (!sel)
				setForeground(d > 0 ? RetailPOSColors.dangerText() : RetailPOSColors.inkMuted());
			return this;
		}
	}
	private static class NameRenderer extends DefaultTableCellRenderer {
		public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int r, int c) {
			super.getTableCellRendererComponent(t, v, sel, focus, r, c);
			if (!sel && v != null && v.toString().endsWith("(arxivada)"))
				setForeground(Color.GRAY);
			return this;
		}
	}
	private static class HintTextField extends JTextField {
		private final String hint;
		HintTextField(String hint) {
			this.hint = hint;
		}
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			if (getText().length() == 0 && !isFocusOwner()) {
				g.setColor(RetailPOSColors.inkMuted());
				g.drawString(hint, getInsets().left + 2, (getHeight() + g.getFontMetrics().getAscent()) / 2 - 2);
			}
		}
	}
	private static class BoxPanel {
		static Component glue() {
			return new JPanel();
		}
	}
}
