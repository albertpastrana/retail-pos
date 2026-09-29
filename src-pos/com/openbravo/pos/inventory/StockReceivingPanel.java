package com.openbravo.pos.inventory;

import com.openbravo.basic.BasicException;
import com.openbravo.beans.JNumberKeys;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.AppView;
import com.openbravo.pos.forms.BeanFactoryApp;
import com.openbravo.pos.forms.BeanFactoryException;
import com.openbravo.pos.forms.DataLogicSales;
import com.openbravo.pos.forms.JPanelView;
import com.openbravo.pos.panels.JProductFinder;
import com.openbravo.pos.theme.RetailPOSColors;
import com.openbravo.pos.theme.RetailPOSTheme;
import com.openbravo.pos.ticket.ProductInfoExt;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.RenderingHints;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.DefaultCellEditor;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Icon;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableCellRenderer;

/**
 * Scanner-first goods receiving. The review is a separate, non-editable step.
 */
public final class StockReceivingPanel extends JPanel implements JPanelView, BeanFactoryApp {
	private static final Logger logger = Logger.getLogger(StockReceivingPanel.class.getName());
	private AppView app;
	private StockSessionRepository repository;
	private DataLogicSales sales;
	private StockSessionRepository.Session session;
	private List<StockSessionRepository.Line> lines;
	private final JTextField code = new JTextField(10);
	private final JTextField selectedQuantity = new JTextField(5);
	private final JLabel selectedName = new JLabel();
	private final JNumberKeys numberKeys = new JNumberKeys();
	private final JLabel heading = new JLabel();
	private final JPanel top = new JPanel(new BorderLayout(12, 6));
	private final JPanel topActions = new JPanel(new FlowLayout(FlowLayout.TRAILING, 8, 0));
	private final JPanel listHeader = new JPanel(new GridBagLayout());
	private final JLabel totals = new JLabel();
	private final JLabel checked = new JLabel();
	private final JComboBox<String> sort = new JComboBox<>(
			new String[]{tr("receiving.last"), tr("receiving.nameSort")});
	private final DefaultTableModel model = new DefaultTableModel(
			new Object[]{tr("receiving.tick"), tr("receiving.product"), tr("receiving.code"), tr("receiving.units"),
					tr("receiving.current"), tr("receiving.after")},
			0) {
		@Override
		public boolean isCellEditable(int row, int col) {
			return !reviewing && session != null && (col == 0 || col == 3);
		}
		@Override
		public Class<?> getColumnClass(int col) {
			return col == 0 ? Boolean.class : String.class;
		}
		@Override
		public void setValueAt(Object value, int row, int col) {
			if (lines == null || row >= lines.size() || reviewing || session == null)
				return;
			StockSessionRepository.Line line = lines.get(row);
			try {
				if (col == 0)
					repository.tick(session.id, line.id, Boolean.TRUE.equals(value));
				else if (col == 3)
					repository.quantity(session.id, line.id, receivedQuantity(String.valueOf(value)));
				else
					return;
				refresh();
			} catch (IllegalArgumentException e) {
				warn(tr("receiving.invalidQuantity"));
				refresh();
			} catch (SQLException e) {
				error(e);
				refresh();
			}
		}
	};
	private final JTable table = new JTable(model);
	private final TableColumn currentColumn = table.getColumnModel().getColumn(4);
	private final TableColumn afterColumn = table.getColumnModel().getColumn(5);
	private final JPanel list = new JPanel(new BorderLayout());
	private final JPanel actions = new JPanel(new BorderLayout(12, 12));
	private final JScrollPane tools = new JScrollPane(actions);
	private final JPanel workspace = new JPanel(new BorderLayout(14, 14));
	private int scanningToolsWidth;
	private int twoColumnWidth;
	private boolean compact;
	private boolean stackedTop;
	private int topButtonsWidth;
	private boolean reviewing;
	private final JButton reviewAction = button("receiving.review", () -> {
		reviewing = !reviewing;
		refresh();
	});
	private final JButton laterAction = button("receiving.later", () -> app.getAppUserView()
			.getTaskAction("com.openbravo.pos.forms.MenuStockManagement").actionPerformed(null));
	private final JButton removeAction = button("receiving.remove", this::remove);
	private final JButton searchAction = button("receiving.search", this::searchProduct);

	public StockReceivingPanel() {
		setLayout(new BorderLayout(14, 14));
		setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));
		heading.setFont(heading.getFont().deriveFont(java.awt.Font.BOLD, 22f));
		top.add(heading, BorderLayout.CENTER);
		topActions.add(button("receiving.new", this::startNew));
		topActions.add(laterAction);
		RetailPOSColors.primaryButton(reviewAction);
		topActions.add(reviewAction);
		top.add(topActions, BorderLayout.EAST);
		add(top, BorderLayout.NORTH);
		topButtonsWidth = topActions.getPreferredSize().width + getInsets().left + getInsets().right;
		table.setRowHeight(Math.max(58, table.getFontMetrics(table.getFont()).getHeight() * 2 + 18));
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setFont(RetailPOSTheme.PLEX_MONO_REGULAR);
		table.setSelectionBackground(RetailPOSColors.brandSubtle());
		table.setSelectionForeground(RetailPOSColors.ink());
		table.setFillsViewportHeight(true);
		table.getTableHeader().setReorderingAllowed(false);
		table.getColumnModel().getColumn(0).setHeaderRenderer((source, value, selected, focused, row, column) -> {
			JCheckBox checkbox = new JCheckBox();
			checkbox.setHorizontalAlignment(SwingConstants.CENTER);
			checkbox.setOpaque(false);
			checkbox.setSelected(lines != null && !lines.isEmpty() && lines.stream().allMatch(line -> line.ticked));
			checkbox.setEnabled(!reviewing && lines != null && !lines.isEmpty());
			checkbox.setToolTipText(tr("receiving.tickAll"));
			return checkbox;
		});
		table.getTableHeader().addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseClicked(java.awt.event.MouseEvent event) {
				if (!reviewing && table.getColumnModel().getColumnIndexAtX(event.getX()) == 0)
					toggleAllTicks();
			}
		});
		table.getSelectionModel().addListSelectionListener(event -> {
			if (!event.getValueIsAdjusting())
				updateSelection();
		});
		table.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
			@Override
			public java.awt.Component getTableCellRendererComponent(JTable source, Object value, boolean selected,
					boolean focused, int row, int column) {
				super.getTableCellRendererComponent(source, value, selected, focused, row, column);
				boolean unknown = lines != null && row < lines.size() && lines.get(row).product == null;
				setText(unknown
						? "<html><b>" + html(tr("receiving.unknownTitle")) + "</b><br>"
								+ html(tr("receiving.unknownHint")) + "</html>"
						: String.valueOf(value));
				setToolTipText(unknown ? tr("receiving.unknown") : String.valueOf(value));
				setForeground(selected ? source.getSelectionForeground() : RetailPOSColors.ink());
				if (!selected && unknown)
					setForeground(RetailPOSColors.danger());
				return this;
			}
		});
		table.getColumnModel().getColumn(0).setMaxWidth(48);
		table.getColumnModel().getColumn(0).setPreferredWidth(44);
		table.getColumnModel().getColumn(1).setPreferredWidth(360);
		table.getColumnModel().getColumn(2).setPreferredWidth(155);
		table.getColumnModel().getColumn(3).setPreferredWidth(75);
		table.getColumnModel().getColumn(3).setMaxWidth(90);
		table.getColumnModel().getColumn(4).setMaxWidth(100);
		table.getColumnModel().getColumn(5).setMaxWidth(100);
		TableCellRenderer defaultHeader = table.getTableHeader().getDefaultRenderer();
		for (int column = 1; column < 6; column++) {
			int alignment = column >= 3 ? SwingConstants.RIGHT : SwingConstants.LEFT;
			table.getColumnModel().getColumn(column)
					.setHeaderRenderer((source, value, selected, focused, row, viewColumn) -> {
						java.awt.Component component = defaultHeader.getTableCellRendererComponent(source, value,
								selected, focused, row, viewColumn);
						if (component instanceof JLabel label)
							label.setHorizontalAlignment(alignment);
						return component;
					});
		}
		DefaultTableCellRenderer quantityRenderer = new DefaultTableCellRenderer();
		quantityRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
		for (int column = 3; column < 6; column++)
			table.getColumnModel().getColumn(column).setCellRenderer(quantityRenderer);
		JTextField quantityEditor = new JTextField();
		quantityEditor.setHorizontalAlignment(SwingConstants.RIGHT);
		quantityEditor.setFont(table.getFont());
		table.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor(quantityEditor));
		listHeader.setBorder(BorderFactory.createEmptyBorder(4, 4, 8, 4));
		layoutListHeader(false);
		list.add(listHeader, BorderLayout.NORTH);
		JScrollPane tableScroll = new JScrollPane(table);
		tableScroll.setColumnHeaderView(table.getTableHeader());
		list.add(tableScroll, BorderLayout.CENTER);
		JPanel footer = new JPanel(new BorderLayout());
		footer.setBorder(BorderFactory.createEmptyBorder(8, 4, 4, 4));
		footer.setBackground(RetailPOSColors.surface200());
		footer.add(totals, BorderLayout.WEST);
		footer.add(checked, BorderLayout.EAST);
		list.add(footer, BorderLayout.SOUTH);
		numberKeys.setNumbersOnly(true);
		numberKeys.setDotVisible(false);
		numberKeys.addJNumberEventListener(event -> numberKey(event.getKey()));
		selectedName.setPreferredSize(
				new Dimension(numberKeys.getPreferredSize().width, selectedName.getPreferredSize().height));
		searchAction.setIcon(magnifierIcon());
		searchAction.setAlignmentX(LEFT_ALIGNMENT);
		searchAction.setMaximumSize(new Dimension(Integer.MAX_VALUE, searchAction.getPreferredSize().height));
		actions.setBorder(BorderFactory.createEmptyBorder(4, 14, 4, 4));
		// The shared bean's 56px touch keys define the minimum tools width;
		// extra window width belongs to the receiving table.
		tools.setBorder(null);
		tools.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scanningToolsWidth = numberKeys.getPreferredSize().width + actions.getBorder().getBorderInsets(actions).left
				+ tools.getVerticalScrollBar().getPreferredSize().width + 24;
		tools.setPreferredSize(new Dimension(scanningToolsWidth, tools.getPreferredSize().height));
		tools.setMinimumSize(new Dimension(scanningToolsWidth, 0));
		workspace.add(list, BorderLayout.CENTER);
		workspace.add(tools, BorderLayout.EAST);
		add(workspace, BorderLayout.CENTER);
		twoColumnWidth = listHeader.getPreferredSize().width + scanningToolsWidth + 14 + getInsets().left
				+ getInsets().right + 14;
		code.addActionListener(e -> scan());
		selectedQuantity.addActionListener(e -> setQuantity());
		sort.addActionListener(e -> refresh());
	}

	@Override
	public void doLayout() {
		boolean shouldCompact = getWidth() > 0 && getWidth() < twoColumnWidth;
		if (compact != shouldCompact) {
			boolean restoreScanFocus = code.isFocusOwner();
			compact = shouldCompact;
			table.setAutoResizeMode(compact || reviewing ? JTable.AUTO_RESIZE_OFF : JTable.AUTO_RESIZE_ALL_COLUMNS);
			table.setRowHeight(compact
					? Math.max(84, table.getRowHeight())
					: Math.max(58, table.getFontMetrics(table.getFont()).getHeight() * 2 + 18));
			layoutListHeader(compact);
			workspace.removeAll();
			workspace.add(list, compact ? BorderLayout.NORTH : BorderLayout.CENTER);
			workspace.add(tools, compact ? BorderLayout.CENTER : BorderLayout.EAST);
			tools.setHorizontalScrollBarPolicy(
					compact ? JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED : JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			tools.setMinimumSize(new Dimension(compact ? 0 : tools.getPreferredSize().width, 0));
			workspace.revalidate();
			if (restoreScanFocus)
				SwingUtilities.invokeLater(() -> code.requestFocusInWindow());
		}
		boolean shouldStackTop = getWidth() > 0 && getWidth() < topButtonsWidth;
		if (stackedTop != shouldStackTop || topActions.getParent() != top
				|| !((compact || shouldStackTop) ? BorderLayout.SOUTH : BorderLayout.EAST)
						.equals(((BorderLayout) top.getLayout()).getConstraints(topActions))) {
			stackedTop = shouldStackTop;
			top.remove(topActions);
			topActions.setLayout(stackedTop ? new GridLayout(0, 1, 4, 4) : new FlowLayout(FlowLayout.TRAILING, 8, 0));
			top.add(topActions, compact ? BorderLayout.SOUTH : BorderLayout.EAST);
			revalidate();
		}
		if (compact) {
			int available = getHeight() - getInsets().top - getInsets().bottom - top.getPreferredSize().height
					- ((BorderLayout) getLayout()).getVgap();
			int listHeight = Math.min(Math.max(140, (int) (available * .60)), Math.max(0, available - 160));
			list.setPreferredSize(new Dimension(0, listHeight));
		} else {
			list.setPreferredSize(null);
		}
		updateHeading();
		super.doLayout();
	}

	private void layoutListHeader(boolean stacked) {
		listHeader.removeAll();
		JLabel title = new JLabel(tr("receiving.listHeading"));
		title.setToolTipText(title.getText());
		if (stacked) {
			JPanel first = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 0));
			first.setOpaque(false);
			first.add(title);
			first.add(removeAction);
			JPanel second = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 0));
			second.setOpaque(false);
			second.add(new JLabel(tr("receiving.orderBy")));
			second.add(sort);
			GridBagConstraints row = new GridBagConstraints();
			row.gridx = 0;
			row.gridy = 0;
			row.weightx = 1;
			row.fill = GridBagConstraints.HORIZONTAL;
			row.anchor = GridBagConstraints.LINE_START;
			listHeader.add(first, row);
			row.gridy = 1;
			listHeader.add(second, row);
			listHeader.revalidate();
			return;
		}
		GridBagConstraints cell = new GridBagConstraints();
		cell.anchor = GridBagConstraints.BASELINE_LEADING;
		cell.insets = new java.awt.Insets(0, 0, 0, 8);
		cell.gridx = 0;
		cell.gridy = 0;
		listHeader.add(title, cell);
		cell.gridx = 1;
		listHeader.add(removeAction, cell);
		cell.gridx = 3;
		cell.weightx = 1;
		cell.anchor = GridBagConstraints.BASELINE_TRAILING;
		listHeader.add(new JLabel(tr("receiving.orderBy")), cell);
		cell.gridx = 4;
		cell.weightx = 0;
		cell.anchor = GridBagConstraints.BASELINE_LEADING;
		cell.insets = new java.awt.Insets(0, 0, 0, 0);
		listHeader.add(sort, cell);
		listHeader.revalidate();
	}

	private void updateHeading() {
		if (session == null || reviewing || getWidth() == 0)
			return;
		String text = session.supplier + " · " + session.note;
		int space = getWidth() - getInsets().left - getInsets().right
				- (compact ? 0 : topActions.getPreferredSize().width + 12);
		String label = heading.getFontMetrics(heading.getFont()).stringWidth(text) > space
				? "<html>" + html(session.supplier) + "<br>" + html(session.note) + "</html>"
				: text;
		if (!label.equals(heading.getText()))
			heading.setText(label);
	}

	@Override
	public void init(AppView app) throws BeanFactoryException {
		this.app = app;
		sales = app.getBean(DataLogicSales.class);
		try {
			repository = new StockSessionRepository(app.getSession().getConnection());
		} catch (SQLException e) {
			throw new BeanFactoryException(e);
		}
	}
	@Override
	public Object getBean() {
		return this;
	}
	@Override
	public JComponent getComponent() {
		return this;
	}
	@Override
	public String getTitle() {
		return tr("receiving.title");
	}
	@Override
	public void activate() throws BasicException {
		refresh();
		if (session != null && !reviewing)
			SwingUtilities.invokeLater(() -> code.requestFocusInWindow());
	}
	@Override
	public boolean deactivate() {
		return true;
	}

	public void startNew() {
		logger.info("event=stock_receipt_open_start");
		JTextField supplier = new JTextField(25);
		JTextField note = new JTextField(18);
		JPanel form = new JPanel(new GridLayout(0, 1, 8, 8));
		form.add(new JLabel(tr("receiving.supplier")));
		form.add(supplier);
		form.add(new JLabel(tr("receiving.note")));
		form.add(note);
		while (true) {
			int answer = JOptionPane.showConfirmDialog(this, form, tr("receiving.open"), JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.PLAIN_MESSAGE);
			if (answer != JOptionPane.OK_OPTION) {
				logger.info("event=stock_receipt_open_cancelled");
				return;
			}
			try {
				session = repository.open(supplier.getText(), note.getText(), app.getInventoryLocation(),
						app.getAppUserView().getUser().getId());
				reviewing = false;
				refresh();
				logger.info("event=stock_receipt_open_success");
				code.requestFocusInWindow();
				return;
			} catch (IllegalArgumentException e) {
				logger.info("event=stock_receipt_open_invalid");
				warn(tr("receiving.requireHeader"));
			} catch (SQLException e) {
				logger.log(Level.SEVERE, "event=stock_receipt_open_failed", e);
				error(e);
				return;
			}
		}
	}

	public void resume(String id) {
		logger.info("event=stock_receipt_resume_start");
		try {
			session = repository.get(id);
			reviewing = false;
			refresh();
			logger.info("event=stock_receipt_resume_success");
			code.requestFocusInWindow();
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "event=stock_receipt_resume_failed", e);
			error(e);
		}
	}

	private void refresh() {
		try {
			if (repository == null)
				return;
			String selectedId = table.getSelectedRow() >= 0 && lines != null && table.getSelectedRow() < lines.size()
					? lines.get(table.getSelectedRow()).id
					: null;
			if (session != null)
				session = repository.get(session.id);
			if (reviewing && table.getColumnCount() == 4) {
				table.addColumn(currentColumn);
				table.addColumn(afterColumn);
			} else if (!reviewing && table.getColumnCount() == 6) {
				table.removeColumn(currentColumn);
				table.removeColumn(afterColumn);
			}
			table.setAutoResizeMode(reviewing || compact ? JTable.AUTO_RESIZE_OFF : JTable.AUTO_RESIZE_ALL_COLUMNS);
			lines = session == null ? List.of() : repository.lines(session.id, sort.getSelectedIndex() == 1);
			heading.setText(session == null
					? tr("receiving.title")
					: reviewing
							? "<html>" + tr("receiving.review") + "<br>" + html(session.supplier) + " · "
									+ html(session.note) + "</html>"
							: session.supplier + " · " + session.note);
			heading.setToolTipText(session == null ? null : session.supplier + " · " + session.note);
			model.setRowCount(0);
			int ticks = 0, unknown = 0;
			double units = 0;
			for (StockSessionRepository.Line line : lines) {
				model.addRow(new Object[]{line.ticked, line.product == null ? tr("receiving.unknown") : line.name,
						line.code + (line.reference == null ? "" : " · " + line.reference), displayUnits(line.units),
						reviewing && line.product != null ? line.stock : "",
						reviewing && line.product != null ? line.stock + line.units : ""});
				if (!reviewing || line.product != null)
					units += line.units;
				if (line.ticked)
					ticks++;
				if (line.product == null)
					unknown++;
			}
			table.getTableHeader().repaint();
			if (selectedId != null)
				for (int i = 0; i < lines.size(); i++)
					if (selectedId.equals(lines.get(i).id)) {
						table.setRowSelectionInterval(i, i);
						break;
					}
			updateSelection();
			totals.setText(tr("receiving.lines") + " " + (reviewing ? lines.size() - unknown : lines.size()) + "    "
					+ tr("receiving.unitsTotal") + " " + NumberFormat.getNumberInstance().format(units));
			checked.setText(reviewing ? "" : tr("receiving.checked") + " " + ticks + " / " + lines.size());
			reviewAction.setEnabled(session != null);
			laterAction.setVisible(session != null && !reviewing);
			removeAction.setVisible(session != null && !reviewing);
			removeAction.setEnabled(session != null && !reviewing && table.getSelectedRow() >= 0);
			reviewAction.setText(tr(reviewing ? "receiving.back" : "receiving.review"));
			updateHeading();
			drawActions(unknown, lines.size() - ticks);
		} catch (SQLException e) {
			error(e);
		}
	}

	private void drawActions(int unknown, int unticked) {
		actions.removeAll();
		if (session == null) {
			actions.add(new JLabel(tr("receiving.empty")), BorderLayout.CENTER);
		} else if (reviewing) {
			JPanel notes = new JPanel(new GridLayout(0, 1, 4, 4));
			if (unknown > 0)
				notes.add(wrapped(tr("receiving.unknownWarning") + " " + unknown));
			if (unticked > 0)
				notes.add(wrapped(tr("receiving.untickedWarning") + " " + unticked));
			notes.add(wrapped(tr("receiving.missingHint")));
			actions.add(notes, BorderLayout.NORTH);
			JPanel buttons = new JPanel(new GridLayout(0, 1, 6, 6));
			if (unknown > 0)
				buttons.add(button("receiving.create", this::createUnknown));
			buttons.add(button("receiving.missing", this::missing));
			buttons.add(button("receiving.back", () -> {
				reviewing = false;
				refresh();
				code.requestFocusInWindow();
			}));
			JButton post = button("receiving.post", this::post);
			post.setEnabled(unknown == 0 && !lines.isEmpty());
			RetailPOSColors.primaryButton(post);
			buttons.add(post);
			actions.add(buttons, BorderLayout.SOUTH);
		} else {
			JPanel scan = new JPanel();
			scan.setLayout(new BoxLayout(scan, BoxLayout.Y_AXIS));
			scan.add(new JLabel(tr("receiving.scan")));
			code.setAlignmentX(LEFT_ALIGNMENT);
			scan.add(code);
			scan.add(searchAction);
			actions.add(scan, BorderLayout.NORTH);
			JPanel selectedPanel = new JPanel(new GridLayout(0, 1, 2, 2));
			selectedPanel.add(new JLabel(tr("receiving.selected")));
			selectedPanel.add(selectedName);
			selectedPanel.add(selectedQuantity);
			JPanel edit = new JPanel(new BorderLayout());
			edit.add(button("receiving.set", this::setQuantity), BorderLayout.CENTER);
			JPanel lower = new JPanel(new BorderLayout(5, 8));
			lower.add(selectedPanel, BorderLayout.NORTH);
			lower.add(numberKeys, BorderLayout.CENTER);
			lower.add(edit, BorderLayout.SOUTH);
			actions.add(lower, BorderLayout.CENTER);
		}
		actions.revalidate();
		actions.repaint();
		int width = reviewing
				? Math.max(scanningToolsWidth,
						actions.getPreferredSize().width + tools.getVerticalScrollBar().getPreferredSize().width + 8)
				: scanningToolsWidth;
		if (tools.getPreferredSize().width != width) {
			tools.setPreferredSize(new Dimension(width, tools.getPreferredSize().height));
			tools.setMinimumSize(new Dimension(compact ? 0 : width, 0));
			workspace.revalidate();
		}
	}

	private void toggleAllTicks() {
		if (session == null || lines == null || lines.isEmpty())
			return;
		try {
			repository.tickAll(session.id, !lines.stream().allMatch(line -> line.ticked));
			refresh();
		} catch (SQLException e) {
			error(e);
		}
	}

	private void numberKey(char key) {
		if (table.getSelectedRow() < 0 || reviewing)
			return;
		if (key == '\u007f') {
			selectedQuantity.setText("");
		} else if (Character.isDigit(key)) {
			if (selectedQuantity.getSelectionStart() != selectedQuantity.getSelectionEnd())
				selectedQuantity.replaceSelection(String.valueOf(key));
			else
				selectedQuantity.setText(selectedQuantity.getText() + key);
		}
		code.requestFocusInWindow();
	}

	private void updateSelection() {
		int row = table.getSelectedRow();
		boolean hasLine = lines != null && row >= 0 && row < lines.size();
		selectedName.setText(hasLine
				? lines.get(row).product == null ? tr("receiving.unknown") : lines.get(row).name
				: tr("receiving.selectHint"));
		selectedName.setToolTipText(hasLine ? selectedName.getText() : null);
		selectedQuantity.setText(hasLine ? displayUnits(lines.get(row).units) : "");
		selectedQuantity.setEnabled(hasLine && !reviewing);
		numberKeys.setEnabled(hasLine && !reviewing);
		removeAction.setEnabled(hasLine && !reviewing);
		if (hasLine && !reviewing)
			selectedQuantity.selectAll();
	}

	private static String displayUnits(double units) {
		return units == Math.rint(units) && units <= Long.MAX_VALUE
				? Long.toString((long) units)
				: Double.toString(units);
	}

	private static double receivedQuantity(String value) {
		String digits = value.trim();
		if (!digits.matches("[0-9]+"))
			throw new IllegalArgumentException("Whole units required");
		return Double.parseDouble(digits);
	}

	private void scan() {
		if (session == null) {
			startNew();
			return;
		}
		try {
			logger.info("event=stock_receipt_scan_start");
			repository.scan(session.id, code.getText(), 1, false);
			code.setText("");
			table.clearSelection();
			refresh();
			logger.info("event=stock_receipt_scan_success lines=" + lines.size());
		} catch (IllegalArgumentException e) {
			logger.info("event=stock_receipt_scan_invalid");
			warn(tr("receiving.invalidScan"));
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "event=stock_receipt_scan_failed", e);
			error(e);
		}
		code.requestFocusInWindow();
	}

	private void searchProduct() {
		if (session == null) {
			startNew();
			return;
		}
		try {
			ProductInfoExt product = JProductFinder.showMessage(this, sales);
			if (product == null)
				return;
			repository.scanProduct(session.id, product.getID(), product.getCode(), 1, false);
			table.clearSelection();
			refresh();
			code.requestFocusInWindow();
		} catch (IllegalArgumentException e) {
			warn(tr("receiving.invalidQuantity"));
		} catch (SQLException e) {
			error(e);
		}
	}

	private StockSessionRepository.Line selected() {
		int row = table.getSelectedRow();
		if (row < 0 || row >= lines.size()) {
			warn(tr("receiving.select"));
			return null;
		}
		return lines.get(row);
	}
	private void setQuantity() {
		StockSessionRepository.Line line = selected();
		if (line == null)
			return;
		try {
			repository.quantity(session.id, line.id, receivedQuantity(selectedQuantity.getText()));
			refresh();
		} catch (IllegalArgumentException e) {
			warn(tr("receiving.invalidQuantity"));
		} catch (SQLException e) {
			error(e);
		}
	}
	private void remove() {
		StockSessionRepository.Line line = selected();
		if (line == null)
			return;
		if (JOptionPane.showConfirmDialog(this, tr("receiving.removeConfirm"), tr("receiving.remove"),
				JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
			return;
		try {
			repository.remove(session.id, line.id);
			refresh();
		} catch (SQLException e) {
			error(e);
		}
	}
	private void post() {
		if (JOptionPane.showConfirmDialog(this, tr("receiving.confirm"), tr("receiving.post"),
				JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
			return;
		try {
			logger.info("event=stock_receipt_post_start lines=" + lines.size());
			repository.post(session.id, app.getAppUserView().getUser().getId());
			logger.info("event=stock_receipt_post_success lines=" + lines.size());
			session = null;
			reviewing = false;
			refresh();
			JOptionPane.showMessageDialog(this, tr("receiving.posted"));
		} catch (IllegalArgumentException | IllegalStateException e) {
			logger.warning("event=stock_receipt_post_rejected reason=" + e.getClass().getSimpleName());
			warn(tr("receiving.postError") + " " + e.getMessage());
			refresh();
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "event=stock_receipt_post_failed", e);
			error(e);
		}
	}
	private void missing() {
		if (app.getAppUserView().getTaskAction("com.openbravo.pos.inventory.ReplenishmentPanel") == null)
			return;
		app.getAppUserView().getTaskAction("com.openbravo.pos.inventory.ReplenishmentPanel").actionPerformed(null);
		try {
			app.getBean(ReplenishmentPanel.class).startManualEntry("", tr("receiving.supplier") + ": "
					+ session.supplier + " · " + tr("receiving.note") + ": " + session.note);
		} catch (BeanFactoryException e) {
			new MessageInf(e).show(this);
		}
	}

	private void createUnknown() {
		StockSessionRepository.Line unresolved = lines.stream().filter(line -> line.product == null).findFirst()
				.orElse(null);
		if (unresolved == null
				|| app.getAppUserView().getTaskAction("com.openbravo.pos.inventory.ProductsPanel") == null)
			return;
		app.getAppUserView().getTaskAction("com.openbravo.pos.inventory.ProductsPanel").actionPerformed(null);
		try {
			app.getBean(ProductsPanel.class).createNewProduct();
		} catch (BeanFactoryException | BasicException e) {
			new MessageInf(e).show(this);
		}
	}
	private void warn(String message) {
		JOptionPane.showMessageDialog(this, message, tr("receiving.title"), JOptionPane.WARNING_MESSAGE);
	}
	private void error(Exception e) {
		logger.log(Level.SEVERE, "event=stock_receipt_ui_failed", e);
		new MessageInf(e).show(this);
	}
	private static String tr(String key) {
		return AppLocal.getIntString(key);
	}
	private static JLabel wrapped(String text) {
		JLabel label = new JLabel("<html><div style='width:290px'>" + text + "</div></html>");
		label.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
		return label;
	}
	private static String html(String text) {
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
	private static Icon magnifierIcon() {
		return new Icon() {
			@Override
			public int getIconWidth() {
				return 20;
			}
			@Override
			public int getIconHeight() {
				return 20;
			}
			@Override
			public void paintIcon(java.awt.Component component, Graphics graphics, int x, int y) {
				Graphics2D g = (Graphics2D) graphics.create();
				try {
					g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					g.setColor(RetailPOSColors.ink());
					g.setStroke(new BasicStroke(2));
					g.drawOval(x + 2, y + 2, 11, 11);
					g.drawLine(x + 12, y + 12, x + 18, y + 18);
				} finally {
					g.dispose();
				}
			}
		};
	}
	private JButton button(String key, Runnable callback) {
		return button(key, callback, true);
	}
	private JButton button(String key, Runnable callback, boolean translate) {
		JButton result = new JButton(translate ? tr(key) : key);
		result.setMargin(new java.awt.Insets(10, 14, 10, 14));
		result.addActionListener(e -> callback.run());
		return result;
	}
}
