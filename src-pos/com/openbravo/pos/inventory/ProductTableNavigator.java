package com.openbravo.pos.inventory;

import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.data.loader.LocalRes;
import com.openbravo.data.user.BrowsableEditableData;
import com.openbravo.data.user.BrowseListener;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.DataLogicSales;
import com.openbravo.pos.sales.TaxesLogic;
import com.openbravo.pos.theme.RetailPOSColors;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.EventQueue;
import java.awt.AWTEvent;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

final class ProductTableNavigator extends JPanel implements BrowseListener, ListSelectionListener {
	private static final int CHECK = 0, REF = 1, NAME = 2, CATEGORY = 3, PRICE = 4;
	private final BrowsableEditableData data;
	private final DataLogicSales sales;
	private final TaxesLogic taxes;
	private final ProductsPanel owner;
	private final ProductBatchUpdate updates;
	private final PriceRuleService rules;
	private final JTable table;
	private final Model model = new Model();
	private final Set<String> selected = new HashSet<>();
	private final Map<String, Boolean> locked = new HashMap<>();
	private final Map<String, Change> pending = new LinkedHashMap<>();
	private final ArrayDeque<Map<String, Change>> history = new ArrayDeque<>();
	private Map<String, Change> lastSaved = new LinkedHashMap<>();
	private final Map<String, Change> preview = new HashMap<>();
	private boolean previewActive;
	private boolean categoryActive;
	private boolean priceActive;
	private final JPanel batch = new JPanel(new BorderLayout());
	private final JPanel footer = new JPanel(new BorderLayout());
	private final JPanel secondaryActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
	private final JLabel count = new JLabel();
	private final JPanel references = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
	private final JTextArea summary = new JTextArea(1, 20);
	private final JTextArea effect = new JTextArea(2, 20);
	private final JLabel currentPrice = new JLabel();
	private final JLabel status = new JLabel();
	private final JToggleButton[] modes = {new JToggleButton(text("batch.fixedShort")),
			new JToggleButton(text("batch.percentShort")), new JToggleButton(text("batch.eurosShort"))};
	private final JTextField amount = new JTextField(8);
	private final JComboBox<CategoryChoice> category = new JComboBox<>();
	private final JButton apply = new TouchButton("");
	private final JButton clear = new TouchButton(text("batch.clearSelection"));
	private final JButton discard = new TouchButton(text("batch.discard"));
	private final JButton undoSaved = new TouchButton(text("batch.undoSaved"));
	private boolean navigating;
	private String deniedCheck;
	private boolean updatingControls;

	ProductTableNavigator(BrowsableEditableData data, TaxesLogic taxes, DataLogicSales sales, ProductsPanel owner)
			throws BasicException {
		super(new BorderLayout());
		this.data = data;
		this.sales = sales;
		this.taxes = taxes;
		this.owner = owner;
		this.updates = new ProductBatchUpdate(owner.getSession());
		this.rules = new PriceRuleService(owner.getSession());
		reloadChoices();
		table = new JTable(model);
		table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		table.setAutoCreateRowSorter(true);
		table.getTableHeader().setReorderingAllowed(false);
		table.setFillsViewportHeight(true);
		table.setRowHeight(Math.max(48, table.getRowHeight()));
		int[] widths = {36, 110, 260, 135, 80};
		for (int i = 0; i < widths.length; i++) {
			table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
		}
		table.getColumnModel().getColumn(CHECK).setMaxWidth(44);
		table.getColumnModel().getColumn(PRICE).setMaxWidth(90);
		table.getColumnModel().getColumn(CHECK).setHeaderRenderer(new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean selectedRow,
					boolean focus, int row, int column) {
				super.getTableCellRendererComponent(table, value, selectedRow, focus, row, column);
				setHorizontalAlignment(JLabel.CENTER);
				setText(model.getRowCount() > 0 && ProductTableNavigator.this.selected.size() == model.getRowCount()
						? "☑"
						: "☐");
				setToolTipText(text("batch.selectAll"));
				return this;
			}
		});
		table.getColumnModel().getColumn(PRICE).setCellRenderer(new ChangedRenderer());
		table.getColumnModel().getColumn(CATEGORY).setCellRenderer(new ChangedRenderer());
		table.getSelectionModel().addListSelectionListener(this);
		table.getTableHeader().addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseClicked(java.awt.event.MouseEvent e) {
				if (table.columnAtPoint(e.getPoint()) == CHECK) {
					selectAll();
				}
			}
		});
		data.addBrowseListener(this);
		data.getListModel().addListDataListener(new ListDataListener() {
			public void intervalAdded(ListDataEvent e) {
				listChanged();
			}
			public void intervalRemoved(ListDataEvent e) {
				listChanged();
			}
			public void contentsChanged(ListDataEvent e) {
				listChanged();
			}
		});
		ButtonGroup priceModes = new ButtonGroup();
		JPanel modeChoices = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
		String[] modeHints = {"batch.fixed", "batch.percent", "batch.euros"};
		for (int i = 0; i < modes.length; i++) {
			JToggleButton button = modes[i];
			priceModes.add(button);
			button.setToolTipText(text(modeHints[i]));
			button.setPreferredSize(new Dimension(button.getPreferredSize().width + 16, 48));
			button.addActionListener(e -> {
				if (!updatingControls && !amount.getText().trim().isEmpty()) {
					priceActive = true;
					previewActive = true;
				}
				updatePreview();
			});
			modeChoices.add(button);
		}
		modes[0].setSelected(true);
		amount.getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) {
				changed();
			}
			public void removeUpdate(DocumentEvent e) {
				changed();
			}
			public void changedUpdate(DocumentEvent e) {
				changed();
			}
			private void changed() {
				if (!updatingControls) {
					priceActive = !amount.getText().trim().isEmpty();
					previewActive = priceActive || categoryActive;
					updatePreview();
				}
			}
		});
		category.addActionListener(e -> {
			if (!updatingControls) {
				CategoryChoice choice = (CategoryChoice) category.getSelectedItem();
				categoryActive = choice != null && choice.id != null;
				previewActive = priceActive || categoryActive;
				updatePreview();
			}
		});
		category.setPrototypeDisplayValue(new CategoryChoice(null, "MMMMMMMMMMMMMMMMMMMM"));
		Font priceFont = UIManager.getFont("Table.font");
		if (priceFont != null)
			amount.setFont(priceFont);
		for (JComponent control : new JComponent[]{amount, category}) {
			Dimension size = control.getPreferredSize();
			control.setPreferredSize(new Dimension(size.width, Math.max(48, size.height)));
		}
		for (JTextArea text : new JTextArea[]{summary, effect}) {
			text.setEditable(false);
			text.setOpaque(false);
			text.setLineWrap(true);
			text.setWrapStyleWord(true);
			text.setFont(UIManager.getFont("Label.font"));
		}
		JPanel heading = new JPanel(new BorderLayout(0, 4));
		heading.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(16, 16, 0, 16),
				BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Separator.foreground"))));
		JLabel eyebrow = new JLabel(text("batch.heading"));
		Font headingFont = count.getFont();
		count.setFont(headingFont.deriveFont(Font.BOLD, headingFont.getSize2D() + 5f));
		JPanel headingText = new JPanel(new BorderLayout(0, 4));
		headingText.add(eyebrow, BorderLayout.NORTH);
		headingText.add(count, BorderLayout.CENTER);
		heading.add(headingText, BorderLayout.NORTH);
		JScrollPane selectedRefs = new JScrollPane(references, JScrollPane.VERTICAL_SCROLLBAR_NEVER,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		selectedRefs.setBorder(null);
		// One line of touch-sized reference chips; overflow scrolls horizontally.
		selectedRefs.setPreferredSize(new Dimension(0, new JLabel("X").getPreferredSize().height + 28));
		heading.add(selectedRefs, BorderLayout.CENTER);
		batch.add(heading, BorderLayout.NORTH);
		amount.putClientProperty("JTextField.placeholderText", text("batch.pricePlaceholder"));
		JPanel form = new JPanel(new GridBagLayout());
		addBatchRow(form, 0, new JLabel(text("batch.newCategory")));
		addBatchRow(form, 1, category);
		addBatchRow(form, 2, summary);
		addBatchRow(form, 3, new JLabel(text("batch.newPrice")));
		addBatchRow(form, 4, modeChoices);
		addBatchRow(form, 5, amount);
		addBatchRow(form, 6, currentPrice);
		addBatchRow(form, 7, effect);
		GridBagConstraints filler = new GridBagConstraints();
		filler.gridy = 8;
		filler.weighty = 1;
		form.add(new JLabel(), filler);
		JScrollPane formScroll = new JScrollPane(form);
		formScroll.setBorder(null);
		formScroll.getVerticalScrollBar().setUnitIncrement(16);
		batch.add(formScroll, BorderLayout.CENTER);
		apply.addActionListener(e -> applyOrRetry());
		JPanel batchActions = new JPanel(new BorderLayout(8, 0));
		JPanel batchEdits = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
		batchEdits.add(discard);
		batchEdits.add(apply);
		batchActions.add(batchEdits, BorderLayout.EAST);
		clear.addActionListener(e -> {
			selected.clear();
			resetProposal();
			table.clearSelection();
			refresh();
		});
		batchActions.setBorder(BorderFactory.createEmptyBorder(12, 16, 16, 16));
		batch.add(batchActions, BorderLayout.SOUTH);
		footer.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
		footer.add(status, BorderLayout.NORTH);
		footer.add(clear, BorderLayout.WEST);
		discard.addActionListener(e -> {
			pending.clear();
			resetProposal();
			history.clear();
			refresh();
			setStatus("");
		});
		undoSaved.addActionListener(e -> undoSaved());
		secondaryActions.add(undoSaved);
		footer.add(secondaryActions, BorderLayout.EAST);
		add(new JScrollPane(table), BorderLayout.CENTER);
		add(footer, BorderLayout.SOUTH);
		bind("control Z", "undo", () -> undo());
		bind("control A", "selectAll", () -> selectAll());
		refresh();
	}
	JPanel getBatchEditor() {
		return batch;
	}
	private static void addBatchRow(JPanel form, int row, Component component) {
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = row;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.insets = new Insets(row == 0 || row == 3 ? 16 : 4, 16, row == 2 || row == 6 ? 8 : 4, 16);
		form.add(component, c);
	}
	private void resetProposal() {
		updatingControls = true;
		try {
			category.setSelectedIndex(0);
			amount.setText("");
			modes[0].setSelected(true);
		} finally {
			updatingControls = false;
		}
		categoryActive = false;
		priceActive = false;
		previewActive = false;
	}

	private static String text(String key) {
		return AppLocal.getIntString(key);
	}
	private int modeIndex() {
		for (int i = 0; i < modes.length; i++)
			if (modes[i].isSelected())
				return i;
		return 0;
	}
	void reloadChoices() throws BasicException {
		updatingControls = true;
		try {
			category.removeAllItems();
			category.addItem(new CategoryChoice(null, text("batch.noChange")));
			for (Object item : sales.getCategoriesList().list()) {
				com.openbravo.pos.ticket.CategoryInfo info = (com.openbravo.pos.ticket.CategoryInfo) item;
				CategoryChoice choice = new CategoryChoice(info.getID(), info.getName());
				category.addItem(choice);
			}
			category.setSelectedIndex(0);
		} finally {
			updatingControls = false;
		}
	}
	private Object[] row(int i) {
		return (Object[]) data.getListModel().getElementAt(i);
	}
	private String id(int i) {
		return (String) row(i)[0];
	}
	private int index(String id) {
		for (int i = 0; i < model.getRowCount(); i++)
			if (id.equals(id(i)))
				return i;
		return -1;
	}
	private double rate(Object[] row) {
		return taxes.getTaxRate((String) row[8], new Date(), null);
	}
	private double net(int i) {
		return ((Number) row(i)[6]).doubleValue();
	}
	private double gross(int i, double net) {
		return ProductPriceMath.grossFromNet(net, rate(row(i)));
	}
	private void bind(String key, String name, Runnable action) {
		table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(key), name);
		table.getActionMap().put(name, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent event) {
				action.run();
			}
		});
	}
	private void listChanged() {
		// A filter/reload replaces the list: never carry edits onto a different result
		// set.
		selected.removeIf(id -> index(id) < 0);
		locked.clear();
		for (String id : selected)
			loadLock(id);
		model.fireTableDataChanged();
		refresh();
	}
	private void selectAll() {
		if (selected.size() == model.getRowCount())
			selected.clear();
		else {
			if (model.getRowCount() >= 2 && !ready())
				return;
			for (int i = 0; i < model.getRowCount(); i++) {
				selected.add(id(i));
				loadLock(id(i));
			}
		}
		refresh();
	}
	@Override
	public void updateIndex(int index, int total) {
		if (navigating || index < 0 || index >= total)
			return;
		navigating = true;
		try {
			table.getSelectionModel().setSelectionInterval(table.convertRowIndexToView(index),
					table.convertRowIndexToView(index));
		} finally {
			navigating = false;
		}
	}
	@Override
	public void valueChanged(ListSelectionEvent event) {
		if (event.getValueIsAdjusting() || navigating || data.isAdjusting())
			return;
		int view = table.getSelectionModel().getLeadSelectionIndex();
		if (view < 0 || view >= table.getRowCount())
			return;
		int row = table.convertRowIndexToModel(view);
		AWTEvent current = EventQueue.getCurrentEvent();
		if (current instanceof MouseEvent && current.getSource() == table && !selected.contains(id(row))
				&& table.columnAtPoint(((MouseEvent) current).getPoint()) == CHECK && owner.hasDirtyIndividualEditor()
				&& !ready()) {
			deniedCheck = id(row);
			javax.swing.SwingUtilities.invokeLater(() -> deniedCheck = null);
			updateIndex(data.getIndex(), model.getRowCount());
			return;
		}
		try {
			data.moveTo(row);
			if (data.getIndex() != row)
				updateIndex(data.getIndex(), model.getRowCount());
		} catch (BasicException ex) {
			new MessageInf(MessageInf.SGN_NOTICE, LocalRes.getIntString("message.nomove"), ex).show(this);
		}
	}
	private boolean ready() {
		try {
			return owner.prepareBatchEdit();
		} catch (BasicException ex) {
			new MessageInf(ex).show(this);
			return false;
		}
	}
	private void loadLock(String id) {
		try {
			locked.put(id, rules.isRulePricedProduct(id));
		} catch (SQLException ex) {
			error(ex);
		}
	}
	private void recordHistory() {
		Map<String, Change> snapshot = new LinkedHashMap<>();
		pending.forEach((id, change) -> snapshot.put(id, change.copy()));
		history.push(snapshot);
	}
	void stageEditorDraft(ProductBatchDraft draft) {
		int i = index(draft.id);
		if (i < 0)
			return;
		recordHistory();
		selected.add(draft.id);
		loadLock(draft.id);
		Change change = pending.containsKey(draft.id)
				? pending.get(draft.id).copy()
				: new Change(net(i), (String) row(i)[7]);
		if (draft.priceChanged)
			change.net = ProductPriceMath.netFromGross(draft.gross, rate(row(i)));
		if (draft.categoryChanged)
			change.category = draft.category;
		pending.put(draft.id, change);
		updatingControls = true;
		try {
			if (draft.categoryChanged)
				category.setSelectedItem(findChoiceById(draft.category));
			if (draft.priceChanged)
				amount.setText(ProductPriceMath.formatCurrency(draft.gross));
			modes[0].setSelected(true);
		} finally {
			updatingControls = false;
		}
		categoryActive = draft.categoryChanged;
		priceActive = draft.priceChanged;
		previewActive = categoryActive || priceActive;
		lastSaved.clear();
		refresh();
	}
	private void applyOrRetry() {
		if (selected.size() > 1 && previewActive && !preview.isEmpty()) {
			applyBatch();
		} else if (!pending.isEmpty() && ready()) {
			if (pending.values().stream().anyMatch(c -> text("batch.conflict").equals(c.error)))
				reloadAndRetry();
			else
				savePending();
		}
	}
	private void applyBatch() {
		if (selected.size() < 2 || !ready())
			return;
		Double input = priceActive
				? (modeIndex() == 1
						? ProductPriceMath.parseCurrency(amount.getText())
						: ProductPriceMath.parsePositiveCurrency(amount.getText(), false))
				: null;
		if (priceActive && (input == null || !Double.isFinite(input))) {
			setStatus(text("batch.invalidPrice"));
			return;
		}
		CategoryChoice choice = (CategoryChoice) category.getSelectedItem();
		if (!priceActive && !categoryActive || categoryActive && choice == null)
			return;
		Map<String, Change> staged = new LinkedHashMap<>();
		int locked = 0;
		for (String id : selected) {
			int i = index(id);
			if (i < 0)
				continue;
			Change change = pending.containsKey(id) ? pending.get(id).copy() : new Change(net(i), (String) row(i)[7]);
			if (categoryActive)
				change.category = choice.id;
			if (priceActive) {
				try {
					if (rules.isRulePricedProduct(id)) {
						this.locked.put(id, true);
						locked++;
					} else {
						Double result = proposedGross(id, i, change.net, input);
						if (result == null) {
							setStatus(text("batch.invalidPrice"));
							return;
						}
						double proposedNet = ProductPriceMath.netFromGross(result, rate(row(i)));
						change.net = sameDisplayedPrice(i, proposedNet, change.oldNet) ? change.oldNet : proposedNet;
					}
				} catch (SQLException ex) {
					error(ex);
					return;
				}
			}
			staged.put(id, change);
		}
		recordHistory();
		staged.forEach((id, change) -> {
			if (Double.compare(change.net, change.oldNet) == 0 && Objects.equals(change.category, change.oldCategory))
				pending.remove(id);
			else
				pending.put(id, change);
		});
		resetProposal();
		preview.clear();
		lastSaved.clear();
		if (!pending.isEmpty())
			savePending();
		else
			refresh();
		if (locked > 0)
			setStatus((status.getText().isEmpty() ? "" : status.getText() + " · ") + text("batch.skipped") + " "
					+ locked + " · " + text("batch.locked"));
	}
	private Double proposedGross(String id, int i, double currentNet, double input) throws SQLException {
		double oldGross = gross(i, currentNet);
		double result = modeIndex() == 0 ? input : modeIndex() == 1 ? oldGross * (1 + input / 100) : oldGross + input;
		if (!Double.isFinite(result) || result < 0)
			return null;
		return modeIndex() == 1 ? rules.roundGrossForProduct(id, result) : Math.round(result * 100.0) / 100.0;
	}
	private boolean sameDisplayedPrice(int i, double firstNet, double secondNet) {
		return ProductPriceMath.formatCurrency(gross(i, firstNet))
				.equals(ProductPriceMath.formatCurrency(gross(i, secondNet)));
	}
	private void undo() {
		if (!history.isEmpty()) {
			previewActive = false;
			pending.clear();
			pending.putAll(history.pop());
			refresh();
		}
	}
	private void savePending() {
		if (pending.isEmpty())
			return;
		previewActive = false;
		int saved = 0;
		Map<String, Change> successes = new LinkedHashMap<>();
		for (Map.Entry<String, Change> entry : new ArrayList<>(pending.entrySet())) {
			String id = entry.getKey();
			Change change = entry.getValue();
			try {
				if (Double.compare(change.net, change.oldNet) != 0 && rules.isRulePricedProduct(id)) {
					change.error = text("batch.locked");
					continue;
				}
				if (!updates.save(id, change.oldNet, change.oldCategory, change.net, change.category)) {
					change.error = text("batch.conflict");
					continue;
				}
				int i = index(id);
				if (i >= 0) {
					row(i)[6] = change.net;
					row(i)[7] = change.category;
				}
				successes.put(id, change);
				pending.remove(id);
				saved++;
			} catch (SQLException ex) {
				change.error = ex.getMessage();
			}
		}
		lastSaved.putAll(successes);
		history.clear();
		owner.refreshBatchEditor();
		refresh();
		setStatus(text("batch.saved") + " " + saved + " · " + text("batch.failed") + " " + pending.size());
	}

	private void reloadAndRetry() {
		for (Map.Entry<String, Change> entry : new ArrayList<>(pending.entrySet())) {
			if (!text("batch.conflict").equals(entry.getValue().error))
				continue;
			try {
				Object[] current = updates.current(entry.getKey());
				if (current == null) {
					entry.getValue().error = text("batch.missing");
					continue;
				}
				Change previous = entry.getValue();
				Change rebased = new Change((Double) current[0], (String) current[1]);
				rebased.net = Double.compare(previous.net, previous.oldNet) == 0 ? rebased.oldNet : previous.net;
				rebased.category = Objects.equals(previous.category, previous.oldCategory)
						? rebased.oldCategory
						: previous.category;
				int i = index(entry.getKey());
				if (i >= 0) {
					row(i)[6] = current[0];
					row(i)[7] = current[1];
				}
				pending.put(entry.getKey(), rebased);
			} catch (SQLException ex) {
				entry.getValue().error = ex.getMessage();
			}
		}
		savePending();
	}
	private void undoSaved() {
		previewActive = false;
		for (Map.Entry<String, Change> entry : new ArrayList<>(lastSaved.entrySet())) {
			Change change = entry.getValue();
			try {
				if (updates.save(entry.getKey(), change.net, change.category, change.oldNet, change.oldCategory)) {
					int i = index(entry.getKey());
					if (i >= 0) {
						row(i)[6] = change.oldNet;
						row(i)[7] = change.oldCategory;
					}
					lastSaved.remove(entry.getKey());
				} else
					setStatus(text("batch.conflict"));
			} catch (SQLException ex) {
				error(ex);
			}
		}
		owner.refreshBatchEditor();
		refresh();
	}
	private void error(Exception ex) {
		setStatus(ex.getMessage());
	}
	private void setStatus(String message) {
		status.setText(message);
		status.setVisible(message != null && !message.isEmpty());
	}
	private void updatePreview() {
		if (table != null)
			refresh();
	}
	private void computePreview() {
		preview.clear();
		if (!previewActive || selected.size() < 2)
			return;
		Double input = priceActive
				? (modeIndex() == 1
						? ProductPriceMath.parseCurrency(amount.getText())
						: ProductPriceMath.parsePositiveCurrency(amount.getText(), false))
				: null;
		if (priceActive && (input == null || !Double.isFinite(input)))
			return;
		CategoryChoice choice = (CategoryChoice) category.getSelectedItem();
		if (categoryActive && choice == null)
			return;
		for (String id : selected) {
			int i = index(id);
			if (i < 0)
				continue;
			Change current = pending.get(id);
			Change change = current == null ? new Change(net(i), (String) row(i)[7]) : current.copy();
			if (categoryActive)
				change.category = choice.id;
			if (priceActive && !Boolean.TRUE.equals(locked.get(id))) {
				try {
					Double proposed = proposedGross(id, i, change.net, input);
					if (proposed == null) {
						preview.clear();
						return;
					}
					double proposedNet = ProductPriceMath.netFromGross(proposed, rate(row(i)));
					change.net = sameDisplayedPrice(i, proposedNet, change.oldNet) ? change.oldNet : proposedNet;
				} catch (SQLException ex) {
					preview.clear();
					error(ex);
					return;
				}
			}
			if (current == null
					? Double.compare(change.net, change.oldNet) != 0
							|| !Objects.equals(change.category, change.oldCategory)
					: Double.compare(change.net, current.net) != 0
							|| !Objects.equals(change.category, current.category))
				preview.put(id, change);
		}
	}
	private void refresh() {
		computePreview();
		amount.putClientProperty("JTextField.placeholderText",
				text(modeIndex() == 1 ? "batch.percentPlaceholder" : "batch.pricePlaceholder"));
		count.setText(selected.isEmpty() && !pending.isEmpty()
				? text("batch.pendingProducts") + " " + pending.size()
				: text("batch.editing") + " " + selected.size() + " " + text("batch.products"));
		references.removeAll();
		for (int i = 0; i < model.getRowCount(); i++) {
			if (!selected.contains(id(i)))
				continue;
			String reference = String.valueOf(row(i)[1]);
			JLabel chip = new ReferenceChip(reference);
			chip.setToolTipText(reference);
			chip.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
			references.add(chip);
		}
		references.revalidate();
		references.repaint();
		boolean proposing = selected.size() > 1 && previewActive && !preview.isEmpty();
		boolean failed = pending.values().stream().anyMatch(c -> c.error != null);
		apply.setText(text(!proposing && failed ? "batch.retry" : "batch.apply") + " "
				+ (proposing || pending.isEmpty() ? selected.size() : pending.size()));
		apply.setToolTipText(null);
		CategoryChoice chosen = (CategoryChoice) category.getSelectedItem();
		if (previewActive && !preview.isEmpty()) {
			StringJoiner proposal = new StringJoiner(" · ");
			if (categoryActive && chosen != null)
				proposal.add(text("batch.previewCategory") + " " + chosen.name);
			if (priceActive) {
				String value = modeIndex() == 0
						? ProductPriceMath
								.formatCurrency(ProductPriceMath.parsePositiveCurrency(amount.getText(), false))
						: amount.getText().trim() + (modeIndex() == 1 ? " %" : " €");
				proposal.add(text(modeIndex() == 0 ? "batch.previewPrice" : "batch.previewChange") + " " + value);
			}
			effect.setText(proposal.toString());
		} else
			effect.setText("");
		Double priceInput = priceActive
				? (modeIndex() == 1
						? ProductPriceMath.parseCurrency(amount.getText())
						: ProductPriceMath.parsePositiveCurrency(amount.getText(), false))
				: null;
		apply.setEnabled(
				(proposing || !pending.isEmpty()) && (!priceActive || selected.size() < 2 || priceInput != null));
		String sharedCategory = null;
		Double sharedPrice = null;
		boolean mixedCategory = false, mixedPrice = false;
		Set<String> categoryValues = new HashSet<>();
		for (String id : selected) {
			int i = index(id);
			if (i < 0)
				continue;
			Change change = pending.get(id);
			String value = label(change == null ? (String) row(i)[7] : change.category);
			categoryValues.add(value);
			double priceValue = Math.round(gross(i, change == null ? net(i) : change.net) * 100.0) / 100.0;
			if (sharedCategory != null && !sharedCategory.equals(value))
				mixedCategory = true;
			if (sharedPrice != null && Double.compare(sharedPrice, priceValue) != 0)
				mixedPrice = true;
			sharedCategory = value;
			sharedPrice = priceValue;
		}
		summary.setText(sharedPrice == null
				? ""
				: mixedCategory
						? java.text.MessageFormat.format(text("batch.categoryDifferent"), categoryValues.size(),
								selected.size())
						: text("batch.commonCategory") + " " + sharedCategory);
		currentPrice.setText(sharedPrice == null
				? ""
				: text("batch.commonPrice") + " "
						+ (mixedPrice ? text("batch.mixed") : ProductPriceMath.formatCurrency(sharedPrice)));
		int cells = 0;
		for (Change change : pending.values()) {
			if (Double.compare(change.net, change.oldNet) != 0)
				cells++;
			if (!Objects.equals(change.category, change.oldCategory))
				cells++;
		}
		if (pending.isEmpty() && lastSaved.isEmpty())
			setStatus("");
		else if (!pending.isEmpty())
			setStatus(text("batch.pending") + " " + cells + " / " + pending.size());
		discard.setEnabled(previewActive || !pending.isEmpty());
		clear.setVisible(!selected.isEmpty());
		undoSaved.setVisible(!lastSaved.isEmpty());
		secondaryActions.setVisible(undoSaved.isVisible());
		RetailPOSColors.primaryButton(apply);
		owner.batchStateChanged(selected.size() >= 2 || !pending.isEmpty());
		model.fireTableDataChanged();
		table.getTableHeader().repaint();
	}
	boolean hasPending() {
		return !pending.isEmpty();
	}

	void clearSelection() {
		selected.clear();
		previewActive = false;
		table.clearSelection();
		refresh();
	}
	void discardPending() {
		pending.clear();
		previewActive = false;
		history.clear();
		lastSaved.clear();
		selected.clear();
		refresh();
	}

	private final class Model extends AbstractTableModel {
		@Override
		public int getRowCount() {
			return data.getListModel().getSize();
		}
		@Override
		public int getColumnCount() {
			return 5;
		}
		@Override
		public Class<?> getColumnClass(int col) {
			return col == CHECK ? Boolean.class : String.class;
		}
		@Override
		public String getColumnName(int col) {
			return col == CHECK
					? "☐"
					: text(new String[]{"", "label.prodref", "label.prodname", "label.prodcategory",
							"batch.price"}[col]);
		}
		@Override
		public boolean isCellEditable(int i, int col) {
			return col == CHECK;
		}
		@Override
		public Object getValueAt(int i, int col) {
			Object[] row = row(i);
			Change change = preview.containsKey(id(i)) ? preview.get(id(i)) : pending.get(id(i));
			switch (col) {
				case CHECK :
					return selected.contains(id(i));
				case REF :
					return row[1];
				case NAME :
					return row[3];
				case CATEGORY :
					return change == null ? label((String) row[7]) : label(change.category);
				default :
					return ProductPriceMath.formatCurrency(gross(i, change == null ? net(i) : change.net))
							+ (Boolean.TRUE.equals(locked.get(id(i))) ? " · " + text("batch.lockedShort") : "");
			}
		}
		@Override
		public void setValueAt(Object value, int i, int col) {
			if (col == CHECK) {
				if (Objects.equals(deniedCheck, id(i))) {
					deniedCheck = null;
					return;
				}
				if (Boolean.TRUE.equals(value)) {
					if (selected.size() == 1 && !selected.contains(id(i)) && !ready())
						return;
					selected.add(id(i));
					loadLock(id(i));
				} else
					selected.remove(id(i));
				refresh();
				return;
			}
		}
	}
	private CategoryChoice findChoiceById(String id) {
		for (int i = 0; i < category.getItemCount(); i++)
			if (Objects.equals(category.getItemAt(i).id, id))
				return category.getItemAt(i);
		return null;
	}
	private String label(String id) {
		for (int i = 0; i < category.getItemCount(); i++)
			if (Objects.equals(category.getItemAt(i).id, id))
				return category.getItemAt(i).name;
		return id == null ? "" : id;
	}
	private final class ChangedRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean selectedRow, boolean focus,
				int viewRow, int column) {
			super.getTableCellRendererComponent(table, value, selectedRow, focus, viewRow, column);
			int i = table.convertRowIndexToModel(viewRow);
			Change change = preview.containsKey(id(i)) ? preview.get(id(i)) : pending.get(id(i));
			boolean changed = change != null && (column == PRICE
					? !sameDisplayedPrice(i, change.net, change.oldNet)
					: !Objects.equals(change.category, change.oldCategory));
			if (changed) {
				String old = column == PRICE
						? ProductPriceMath.formatCurrency(gross(i, change.oldNet))
						: label(change.oldCategory);
				setText("<html>" + escape(String.valueOf(value)) + "<br><strike>" + escape(old) + "</strike>"
						+ (change.error == null ? "" : " · " + text("batch.error")) + "</html>");
				setToolTipText(change.error);
			} else
				setToolTipText(change != null
						? change.error
						: Boolean.TRUE.equals(locked.get(id(i))) && column == PRICE ? text("batch.locked") : null);
			if (!changed && change != null && change.error != null)
				setText(getText() + " · " + text("batch.error"));
			return this;
		}
	}
	private static String escape(String text) {
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
	private static final class ReferenceChip extends JLabel {
		ReferenceChip(String reference) {
			super(reference);
			setOpaque(false);
		}
		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D background = (Graphics2D) graphics.create();
			try {
				background.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				background.setColor(UIManager.getColor("TextField.background"));
				background.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
				java.awt.Color border = UIManager.getColor("Component.borderColor");
				background.setColor(border != null ? border : UIManager.getColor("Separator.foreground"));
				background.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
			} finally {
				background.dispose();
			}
			super.paintComponent(graphics);
		}
	}
	private static final class TouchButton extends JButton {
		TouchButton(String label) {
			super(label);
		}
		@Override
		public Dimension getPreferredSize() {
			Dimension size = super.getPreferredSize();
			return new Dimension(size.width, Math.max(48, size.height));
		}
	}
	private static final class Change {
		final double oldNet;
		final String oldCategory;
		double net;
		String category, error;
		Change(double net, String category) {
			oldNet = this.net = net;
			oldCategory = this.category = category;
		}
		Change copy() {
			Change copy = new Change(oldNet, oldCategory);
			copy.net = net;
			copy.category = category;
			copy.error = error;
			return copy;
		}
	}
	private static final class CategoryChoice {
		final String id, name;
		CategoryChoice(String id, String name) {
			this.id = id;
			this.name = name;
		}
		@Override
		public String toString() {
			return name;
		}
	}
}
