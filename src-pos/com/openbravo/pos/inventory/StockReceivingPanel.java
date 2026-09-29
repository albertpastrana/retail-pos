package com.openbravo.pos.inventory;

import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.format.Formats;
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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.RenderingHints;
import java.util.function.Predicate;
import java.awt.Rectangle;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Box;
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
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Icon;
import javax.swing.SwingConstants;
import javax.swing.Scrollable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.JTableHeader;

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
	private final JTextArea selectedName = new JTextArea(3, 12);
	private final JLabel detailBarcode = new JLabel();
	private final JTextArea detailReference = new JTextArea(2, 12);
	private final JLabel detailBrand = new JLabel();
	private final JLabel detailPrice = new JLabel();
	private final JButton tickAction = button("receiving.checkLine", this::toggleSelectedTick);
	private final JPanel brandRow = detailRow("receiving.brand", detailBrand);
	private final JPanel priceRow = detailRow("receiving.retailPrice", detailPrice);
	private final JPanel detailRows = new JPanel(new GridBagLayout());
	private final JPanel cardActions = new JPanel(new GridLayout(0, 1, 6, 6));
	private final JPanel detailCard = new JPanel(new BorderLayout(8, 10)) {
		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = (Graphics2D) graphics.create();
			try {
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				// Design system radius-lg = 16 logical pixels.
				g.setColor(RetailPOSColors.surface100());
				g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 32, 32);
				g.setColor(RetailPOSColors.border());
				g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 32, 32);
			} finally {
				g.dispose();
			}
		}
	};
	private final JLabel heading = new JLabel();
	private final JLabel reviewTitle = new JLabel(tr("receiving.reviewTitle"));
	private final JLabel noteHeading = new JLabel();
	private final JLabel pendingBadge = new JLabel(tr("receiving.pending")) {
		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = (Graphics2D) graphics.create();
			try {
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g.setColor(RetailPOSColors.surface200());
				g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
			} finally {
				g.dispose();
			}
			super.paintComponent(graphics);
		}
	};
	private final JLabel startedHint = new JLabel();
	private final JLabel stockHint = new JLabel(tr("receiving.stockUnchanged"));
	private final JPanel headingLine = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
	private final JPanel noteLine = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
	private final JPanel badgeLine = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
	private final JPanel titleBlock = new JPanel();
	private final JPanel top = new JPanel(new BorderLayout(12, 6));
	private final JPanel topActions = new JPanel(new FlowLayout(FlowLayout.TRAILING, 8, 0));
	private final JPanel listHeader = new JPanel(new GridBagLayout());
	private final JLabel totals = new JLabel();
	private final JLabel checked = new JLabel();
	private final JComboBox<String> sort = new JComboBox<>(
			new String[]{tr("receiving.last"), tr("receiving.nameSort")});
	private final DefaultTableModel model = new DefaultTableModel(new Object[]{tr("receiving.tick"),
			tr("receiving.product"), tr("receiving.barcodeColumn"), tr("receiving.retailPriceColumn"),
			tr("receiving.units"), tr("receiving.current"), tr("receiving.after")}, 0) {
		@Override
		public boolean isCellEditable(int row, int col) {
			return !reviewing && session != null && (col == 0 || col == 4);
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
				else if (col == 4)
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
	private final JPanel list = new JPanel(new BorderLayout());
	private final JPanel actions = new ToolView();
	private final JScrollPane tools = new JScrollPane(actions);
	private final JPanel workspace = new JPanel(new BorderLayout(14, 14));
	private int scanningToolsWidth;
	private int readableTableWidth;
	private int twoColumnWidth;
	private boolean compact;
	private boolean stackedTop;
	private boolean stackedTopButtons;
	private int titleLayout = -1;
	private boolean reviewing;
	private final JButton reviewAction = button("receiving.review", () -> {
		reviewing = !reviewing;
		refresh();
		if (reviewing)
			SwingUtilities.invokeLater(() -> table.requestFocusInWindow());
	});
	private final JButton newAction = button("receiving.new", this::startNew);
	private final JButton laterAction = button("receiving.later", () -> app.getAppUserView()
			.getTaskAction("com.openbravo.pos.forms.MenuStockManagement").actionPerformed(null));
	private final JButton removeAction = button("receiving.remove", this::remove);
	private final JButton discardAction = button("receiving.discard", this::discard);
	private final JButton searchAction = button("receiving.search", this::searchProduct);

	public StockReceivingPanel() {
		setLayout(new BorderLayout(14, 14));
		// The host view title has an 18px inset (8px border + 10px label padding).
		setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
		titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
		heading.setFont(heading.getFont().deriveFont(java.awt.Font.BOLD, 22f));
		reviewTitle.setFont(reviewTitle.getFont().deriveFont(java.awt.Font.BOLD, 18f));
		noteHeading.setFont(RetailPOSTheme.PLEX_MONO_REGULAR.deriveFont(16f));
		noteHeading.setForeground(RetailPOSColors.inkMuted());
		pendingBadge.setFont(RetailPOSTheme.MANROPE_BOLD.deriveFont(12f));
		pendingBadge.setForeground(RetailPOSColors.warning());
		pendingBadge.setOpaque(false);
		pendingBadge.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
		startedHint.setForeground(RetailPOSColors.inkMuted());
		stockHint.setForeground(RetailPOSColors.inkMuted());
		for (JLabel label : new JLabel[]{reviewTitle, startedHint, stockHint})
			label.setAlignmentX(LEFT_ALIGNMENT);
		for (JPanel row : new JPanel[]{headingLine, noteLine, badgeLine}) {
			row.setOpaque(false);
			row.setAlignmentX(LEFT_ALIGNMENT);
		}
		titleBlock.add(reviewTitle);
		titleBlock.add(headingLine);
		titleBlock.add(noteLine);
		titleBlock.add(badgeLine);
		titleBlock.add(startedHint);
		titleBlock.add(stockHint);
		top.add(titleBlock, BorderLayout.CENTER);
		topActions.add(newAction);
		topActions.add(laterAction);
		discardAction.setMargin(new java.awt.Insets(15, 14, 15, 14));
		discardAction.setVisible(false);
		topActions.add(discardAction);
		RetailPOSColors.primaryButton(reviewAction);
		topActions.add(reviewAction);
		top.add(topActions, BorderLayout.EAST);
		add(top, BorderLayout.NORTH);
		removeAction.setIcon(removeIcon());
		discardAction.setForeground(RetailPOSColors.dangerText());
		table.setFont(RetailPOSTheme.PLEX_MONO_REGULAR);
		table.setRowHeight(Math.max(52, table.getFontMetrics(table.getFont()).getHeight() + 20));
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setSelectionBackground(RetailPOSColors.brandSubtle());
		table.setSelectionForeground(RetailPOSColors.ink());
		table.setFillsViewportHeight(true);
		table.setTableHeader(new JTableHeader(table.getColumnModel()) {
			@Override
			public String getToolTipText(java.awt.event.MouseEvent event) {
				int column = columnAtPoint(event.getPoint());
				return column < 0 ? null : stockHeaderHint(getColumnModel().getColumn(column).getModelIndex());
			}
		});
		// Register the header with Swing's tooltip manager; the text is chosen per
		// column above.
		table.getTableHeader().setToolTipText("");
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
				setText(unknown ? tr("receiving.unknownTitle") : String.valueOf(value));
				setToolTipText(unknown ? tr("receiving.unknown") : String.valueOf(value));
				setForeground(selected ? source.getSelectionForeground() : RetailPOSColors.ink());
				if (!selected && unknown)
					setForeground(RetailPOSColors.danger());
				return this;
			}
		});
		table.getColumnModel().getColumn(0).setMaxWidth(44);
		table.getColumnModel().getColumn(0).setPreferredWidth(44);
		table.getColumnModel().getColumn(1).setPreferredWidth(600);
		table.getColumnModel().getColumn(1).setMinWidth(140);
		int barcodeColumnWidth = table.getFontMetrics(table.getFont()).stringWidth("0000000000000") + 36;
		table.getColumnModel().getColumn(2).setPreferredWidth(barcodeColumnWidth);
		table.getColumnModel().getColumn(2)
				.setMinWidth(table.getFontMetrics(table.getFont()).stringWidth("0000000000000") + 12);
		table.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
			@Override
			public java.awt.Component getTableCellRendererComponent(JTable source, Object value, boolean selected,
					boolean focused, int row, int column) {
				super.getTableCellRendererComponent(source, value, selected, focused, row, column);
				setHorizontalAlignment(SwingConstants.CENTER);
				setToolTipText(value == null ? null : value.toString());
				return this;
			}
		});
		table.getColumnModel().getColumn(3).setPreferredWidth(80);
		table.getColumnModel().getColumn(3).setMinWidth(60);
		table.getColumnModel().getColumn(4).setPreferredWidth(60);
		table.getColumnModel().getColumn(5).setPreferredWidth(60);
		table.getColumnModel().getColumn(6).setPreferredWidth(60);
		for (int column = 4; column <= 6; column++)
			table.getColumnModel().getColumn(column).setMinWidth(Math.max(54, table.getTableHeader()
					.getFontMetrics(table.getTableHeader().getFont()).stringWidth(stockHeaderLabel(column)) + 12));
		TableCellRenderer defaultHeader = table.getTableHeader().getDefaultRenderer();
		for (int column = 1; column < 7; column++) {
			int alignment = column == 2
					? SwingConstants.CENTER
					: column >= 3 ? SwingConstants.RIGHT : SwingConstants.LEFT;
			table.getColumnModel().getColumn(column)
					.setHeaderRenderer((source, value, selected, focused, row, viewColumn) -> {
						java.awt.Component component = defaultHeader.getTableCellRendererComponent(source, value,
								selected, focused, row, viewColumn);
						if (component instanceof JLabel label) {
							int modelColumn = source.getColumnModel().getColumn(viewColumn).getModelIndex();
							String shortLabel = stockHeaderLabel(modelColumn);
							label.setIcon(null);
							label.setText(shortLabel == null ? String.valueOf(value) : shortLabel);
							label.setHorizontalAlignment(alignment);
							label.setToolTipText(stockHeaderHint(modelColumn));
							label.getAccessibleContext().setAccessibleName(String.valueOf(value));
						}
						return component;
					});
		}
		DefaultTableCellRenderer quantityRenderer = new DefaultTableCellRenderer();
		quantityRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
		for (int column = 3; column < 7; column++)
			table.getColumnModel().getColumn(column).setCellRenderer(quantityRenderer);
		JTextField quantityEditor = new JTextField();
		quantityEditor.setHorizontalAlignment(SwingConstants.RIGHT);
		quantityEditor.setFont(table.getFont());
		table.getColumnModel().getColumn(4).setCellEditor(new DefaultCellEditor(quantityEditor));
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
		configureDetailCard();
		searchAction.setIcon(magnifierIcon());
		Dimension codeSize = code.getPreferredSize();
		code.setPreferredSize(new Dimension(codeSize.width, Math.max(48, searchAction.getPreferredSize().height)));
		searchAction.setAlignmentX(LEFT_ALIGNMENT);
		searchAction.setMaximumSize(new Dimension(Integer.MAX_VALUE, searchAction.getPreferredSize().height));
		alignToolsWithTable();
		// Reserve room for a complete EAN beside its translated label. The table
		// takes the remaining width, or scrolls inside the compact stacked layout.
		tools.setBorder(null);
		tools.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		int barcodeWidth = new JLabel(tr("receiving.barcode")).getPreferredSize().width
				+ detailBarcode.getFontMetrics(detailBarcode.getFont()).stringWidth("0000000000000") + 8 + 28;
		int reviewButtonWidth = 0;
		for (String key : new String[]{"receiving.create", "receiving.missing", "receiving.back", "receiving.post"})
			reviewButtonWidth = Math.max(reviewButtonWidth, button(key, () -> {
			}).getPreferredSize().width);
		scanningToolsWidth = Math.max(Math.max(code.getPreferredSize().width, barcodeWidth), reviewButtonWidth)
				+ actions.getInsets().left + actions.getInsets().right
				+ tools.getVerticalScrollBar().getPreferredSize().width + 8;
		// All columns fit on a 600px stacked screen. A narrower window retains
		// horizontal access instead of clipping the barcode or stock figures.
		readableTableWidth = tableScroll.getVerticalScrollBar().getPreferredSize().width;
		for (int column = 0; column < 7; column++)
			readableTableWidth += table.getColumnModel().getColumn(column).getMinWidth();
		tools.setPreferredSize(new Dimension(scanningToolsWidth, tools.getPreferredSize().height));
		tools.setMinimumSize(new Dimension(scanningToolsWidth, 0));
		workspace.add(list, BorderLayout.CENTER);
		workspace.add(tools, BorderLayout.EAST);
		add(workspace, BorderLayout.CENTER);
		twoColumnWidth = Math.max(listHeader.getPreferredSize().width + 14, readableTableWidth) + scanningToolsWidth
				+ 14 + getInsets().left + getInsets().right;
		code.addActionListener(e -> scan());
		sort.addActionListener(e -> refresh());
	}

	private static JPanel detailRow(String key, JComponent value) {
		JPanel row = new JPanel(new BorderLayout(8, 0));
		row.setOpaque(false);
		value.setFont(RetailPOSTheme.PLEX_MONO_REGULAR);
		JLabel label = new JLabel(tr(key));
		label.setVerticalAlignment(SwingConstants.TOP);
		if (value instanceof JLabel detail)
			detail.setVerticalAlignment(SwingConstants.TOP);
		row.add(label, BorderLayout.WEST);
		row.add(value, BorderLayout.CENTER);
		return row;
	}

	private static final class ToolView extends JPanel implements Scrollable {
		ToolView() {
			super(new BorderLayout(12, 12));
		}
		@Override
		public Dimension getPreferredScrollableViewportSize() {
			return getPreferredSize();
		}
		@Override
		public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
			return 16;
		}
		@Override
		public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
			return (orientation == SwingConstants.VERTICAL ? visibleRect.height : visibleRect.width) - 16;
		}
		@Override
		public boolean getScrollableTracksViewportWidth() {
			return true;
		}
		@Override
		public boolean getScrollableTracksViewportHeight() {
			return false;
		}
	}

	private void configureDetailCard() {
		detailCard.setOpaque(false);
		detailCard.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
		JPanel name = new JPanel(new BorderLayout(0, 6));
		name.setOpaque(false);
		name.add(new JLabel(tr("receiving.selected")), BorderLayout.NORTH);
		selectedName.setEditable(false);
		selectedName.setFocusable(false);
		selectedName.setOpaque(false);
		selectedName.setLineWrap(true);
		selectedName.setWrapStyleWord(true);
		selectedName.setFont(RetailPOSTheme.MANROPE_BOLD.deriveFont(18f));
		name.add(selectedName, BorderLayout.CENTER);
		detailCard.add(name, BorderLayout.NORTH);
		detailReference.setEditable(false);
		detailReference.setFocusable(false);
		detailReference.setOpaque(false);
		detailReference.setLineWrap(true);
		detailReference.setWrapStyleWord(true);
		detailRows.setOpaque(false);
		GridBagConstraints row = new GridBagConstraints();
		row.gridx = 0;
		row.gridy = 0;
		row.weightx = 1;
		row.fill = GridBagConstraints.HORIZONTAL;
		row.anchor = GridBagConstraints.FIRST_LINE_START;
		row.insets = new java.awt.Insets(2, 0, 2, 0);
		detailRows.add(detailRow("receiving.barcode", detailBarcode), row);
		row.gridy++;
		detailRows.add(detailRow("receiving.reference", detailReference), row);
		row.gridy++;
		detailRows.add(brandRow, row);
		row.gridy++;
		detailRows.add(priceRow, row);
		detailCard.add(detailRows, BorderLayout.CENTER);
		cardActions.setOpaque(false);
		cardActions.add(tickAction);
		cardActions.add(removeAction);
		detailCard.add(cardActions, BorderLayout.SOUTH);
		updateSelection();
	}

	@Override
	public void doLayout() {
		boolean shouldCompact = getWidth() > 0 && getWidth() < twoColumnWidth;
		if (compact != shouldCompact) {
			boolean restoreScanFocus = code.isFocusOwner();
			compact = shouldCompact;
			layoutListHeader(compact);
			alignToolsWithTable();
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
		int buttonsWidth = topButtonRowWidth();
		int supplierWidth = session == null
				? heading.getFontMetrics(heading.getFont()).stringWidth(tr("receiving.title"))
				: heading.getFontMetrics(heading.getFont()).stringWidth(session.supplier);
		boolean shouldStackTop = getWidth() > 0 && getWidth() < buttonsWidth + supplierWidth + 16 + 12;
		boolean shouldStackButtons = getWidth() > 0 && getWidth() < buttonsWidth;
		if (stackedTop != shouldStackTop || stackedTopButtons != shouldStackButtons || topActions.getParent() != top
				|| !((compact || shouldStackTop) ? BorderLayout.SOUTH : BorderLayout.EAST)
						.equals(((BorderLayout) top.getLayout()).getConstraints(topActions))) {
			stackedTop = shouldStackTop;
			stackedTopButtons = shouldStackButtons;
			top.remove(topActions);
			topActions.setLayout(
					stackedTopButtons ? new GridLayout(0, 1, 4, 4) : new FlowLayout(FlowLayout.TRAILING, 8, 0));
			top.add(topActions, compact || stackedTop ? BorderLayout.SOUTH : BorderLayout.EAST);
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
		int listWidth = getWidth() - getInsets().left - getInsets().right - (compact ? 0 : scanningToolsWidth + 14);
		int autoResize = listWidth >= readableTableWidth ? JTable.AUTO_RESIZE_ALL_COLUMNS : JTable.AUTO_RESIZE_OFF;
		if (table.getAutoResizeMode() != autoResize)
			table.setAutoResizeMode(autoResize);
		updateHeading();
		super.doLayout();
	}

	private int topButtonRowWidth() {
		int width = getInsets().left + getInsets().right + topActions.getInsets().left + topActions.getInsets().right;
		int count = 0;
		for (Component button : topActions.getComponents()) {
			if (button.isVisible()) {
				width += button.getPreferredSize().width;
				count++;
			}
		}
		return width + (count + 1) * 8; // FlowLayout's gaps around and between buttons.
	}

	private void alignToolsWithTable() {
		int topInset = compact ? 4 : listHeader.getPreferredSize().height + 4;
		if (actions.getInsets().top != topInset)
			actions.setBorder(BorderFactory.createEmptyBorder(topInset, 14, 4, 4));
	}

	private void layoutListHeader(boolean stacked) {
		listHeader.removeAll();
		JLabel title = new JLabel(tr("receiving.listHeading"));
		title.setToolTipText(title.getText());
		if (stacked) {
			JPanel first = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 0));
			first.setOpaque(false);
			first.add(title);
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
		boolean active = session != null;
		reviewTitle.setVisible(active && reviewing);
		startedHint.setVisible(active);
		stockHint.setVisible(active);
		String supplier = active ? session.supplier : tr("receiving.title");
		int available = getWidth() - getInsets().left - getInsets().right
				- (compact || stackedTop || !topActions.isVisible() ? 0 : topActions.getPreferredSize().width + 12);
		String label = active && available > 0
				&& heading.getFontMetrics(heading.getFont()).stringWidth(supplier) > available
						? "<html><div style='width:" + Math.max(120, available - 8) + "px'>" + html(supplier)
								+ "</div></html>"
						: supplier;
		if (!label.equals(heading.getText()))
			heading.setText(label);
		heading.setToolTipText(active ? supplier : null);
		if (active) {
			noteHeading.setText(AppLocal.getIntString("receiving.noteShort", session.note));
			boolean today = session.started.toLocalDateTime().toLocalDate().equals(LocalDate.now());
			DateFormat date = today
					? DateFormat.getTimeInstance(DateFormat.SHORT)
					: DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
			startedHint.setText(AppLocal.getIntString(today ? "receiving.startedToday" : "receiving.startedEarlier",
					date.format(session.started)));
		}
		int supplierWidth = heading.getFontMetrics(heading.getFont()).stringWidth(supplier);
		int layout = !active
				? 1
				: supplierWidth + noteHeading.getPreferredSize().width + pendingBadge.getPreferredSize().width
						+ 20 <= available
								? 0
								: noteHeading.getPreferredSize().width + pendingBadge.getPreferredSize().width
										+ 10 <= available ? 1 : 2;
		if (titleLayout != layout) {
			titleLayout = layout;
			headingLine.removeAll();
			noteLine.removeAll();
			badgeLine.removeAll();
			headingLine.add(heading);
			if (layout == 0) {
				headingLine.add(Box.createHorizontalStrut(10));
				headingLine.add(noteHeading);
				headingLine.add(Box.createHorizontalStrut(10));
				headingLine.add(pendingBadge);
			} else {
				noteLine.add(noteHeading);
				if (layout == 1) {
					noteLine.add(Box.createHorizontalStrut(10));
					noteLine.add(pendingBadge);
				} else {
					badgeLine.add(pendingBadge);
				}
			}
			titleBlock.revalidate();
		}
		noteLine.setVisible(active && layout != 0);
		badgeLine.setVisible(active && layout == 2);
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
			lines = session == null ? List.of() : repository.lines(session.id, sort.getSelectedIndex() == 1);
			model.setRowCount(0);
			int ticks = 0, unknown = 0;
			double units = 0;
			for (StockSessionRepository.Line line : lines) {
				model.addRow(new Object[]{line.ticked, line.product == null ? tr("receiving.unknown") : line.name,
						text(line.barcode),
						line.retailPrice == null ? "—" : Formats.CURRENCY.formatValue(line.retailPrice),
						displayUnits(line.units), line.product != null ? displayStock(line.stock) : "",
						line.product != null ? displayStock(line.stock + line.units) : ""});
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
			topActions.setVisible(!reviewing);
			laterAction.setVisible(session != null && !reviewing);
			discardAction.setVisible(session != null && !reviewing);
			removeAction.setVisible(session != null && !reviewing && table.getSelectedRow() >= 0);
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
		alignToolsWithTable();
		if (session == null) {
			actions.add(new JLabel(tr("receiving.empty")), BorderLayout.CENTER);
		} else if (reviewing) {
			int viewportWidth = tools.getViewport().getWidth();
			int noteWidth = Math.max(1,
					(viewportWidth > 0 ? viewportWidth : scanningToolsWidth) - actions.getInsets().left
							- actions.getInsets().right - tools.getVerticalScrollBar().getPreferredSize().width);
			JPanel notes = new JPanel(new GridLayout(0, 1, 4, 4));
			if (unknown > 0)
				notes.add(wrapped(tr("receiving.unknownWarning") + " " + unknown, noteWidth));
			if (unticked > 0)
				notes.add(wrapped(tr("receiving.untickedWarning") + " " + unticked, noteWidth));
			notes.add(wrapped(tr("receiving.missingHint"), noteWidth));
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
			JPanel scanAndDetails = new JPanel(new BorderLayout(0, 12));
			scanAndDetails.add(scan, BorderLayout.NORTH);
			scanAndDetails.add(detailCard, BorderLayout.CENTER);
			actions.add(scanAndDetails, BorderLayout.NORTH);
		}
		actions.revalidate();
		actions.repaint();
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

	private void updateSelection() {
		int row = table.getSelectedRow();
		boolean hasLine = lines != null && row >= 0 && row < lines.size();
		StockSessionRepository.Line line = hasLine ? lines.get(row) : null;
		selectedName.setText(hasLine
				? line.product == null ? tr("receiving.unknownTitle") : text(line.name)
				: tr("receiving.selectHint"));
		selectedName.setToolTipText(hasLine ? selectedName.getText() : null);
		detailRows.setVisible(hasLine);
		detailBarcode.setText(hasLine ? text(lines.get(row).barcode) : "");
		detailReference.setText(hasLine ? text(lines.get(row).reference) : "");
		boolean showBrand = hasLine && lines.get(row).brand != null && !lines.get(row).brand.isBlank();
		brandRow.setVisible(showBrand);
		detailBrand.setText(showBrand ? lines.get(row).brand : "");
		boolean showPrice = hasLine && lines.get(row).retailPrice != null;
		priceRow.setVisible(showPrice);
		detailPrice.setText(showPrice ? Formats.CURRENCY.formatValue(lines.get(row).retailPrice) : "");
		removeAction.setEnabled(hasLine && !reviewing);
		removeAction.setVisible(hasLine && !reviewing);
		tickAction.setVisible(hasLine && !reviewing);
		tickAction.setEnabled(hasLine && !reviewing);
		tickAction.setText(tr(hasLine && line.ticked ? "receiving.uncheckLine" : "receiving.checkLine"));
		cardActions.setVisible(hasLine && !reviewing);
		detailCard.revalidate();
	}

	private void toggleSelectedTick() {
		if (reviewing || session == null || lines == null || table.getSelectedRow() < 0)
			return;
		StockSessionRepository.Line line = lines.get(table.getSelectedRow());
		try {
			repository.tick(session.id, line.id, !line.ticked);
			refresh();
		} catch (SQLException e) {
			error(e);
		}
	}

	private static String text(String value) {
		return value == null || value.isBlank() ? "—" : value;
	}

	private static String displayUnits(double units) {
		return units == Math.rint(units) && units <= Long.MAX_VALUE
				? Long.toString((long) units)
				: Double.toString(units);
	}

	private static String displayStock(double units) {
		return Long.toString(Math.round(units));
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
			String scanned = code.getText().trim();
			ProductInfoExt known = sales == null || scanned.isEmpty() ? null : sales.getProductInfoByCode(scanned);
			String importedId = null;
			if (!scanned.isEmpty() && sales != null && (known == null || !scanned.equals(known.getCode()))
					&& sales.getCatalogProductByCode(scanned, null, null) != null) {
				ProductInfoExt imported = CatalogImportDialog.forReceiving(this, app, sales).importIfAbsent(scanned);
				if (imported == null)
					return;
				importedId = imported.getID();
				repository.scanProduct(session.id, imported.getID(), scanned, 1, false);
			} else {
				repository.scan(session.id, scanned, 1, false);
			}
			code.setText("");
			refresh();
			String selectedId = importedId;
			selectLine(line -> selectedId == null ? scanned.equals(line.barcode) : selectedId.equals(line.product));
			logger.info("event=stock_receipt_scan_success lines=" + lines.size());
		} catch (IllegalArgumentException e) {
			logger.info("event=stock_receipt_scan_invalid");
			warn(tr("receiving.invalidScan"));
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "event=stock_receipt_scan_failed", e);
			error(e);
		} catch (BasicException e) {
			error(e);
		} finally {
			code.requestFocusInWindow();
		}
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
			addFoundProduct(product);
			code.requestFocusInWindow();
		} catch (IllegalArgumentException e) {
			warn(tr("receiving.invalidScan"));
		} catch (SQLException e) {
			error(e);
		}
	}

	void addFoundProduct(ProductInfoExt product) throws SQLException {
		repository.scanProduct(session.id, product.getID(), product.getCode(), 1, false);
		refresh();
		selectLine(line -> product.getID().equals(line.product));
	}

	private void selectLine(Predicate<StockSessionRepository.Line> matches) {
		for (int row = 0; row < lines.size(); row++) {
			if (matches.test(lines.get(row))) {
				table.setRowSelectionInterval(row, row);
				table.scrollRectToVisible(table.getCellRect(row, 0, true));
				return;
			}
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

	private static boolean confirmDiscard(Component parent, StockSessionRepository.Session receipt) {
		Object[] options = {tr("receiving.keep"), tr("receiving.discard")};
		// Bound the message width so long supplier names and references wrap on a till.
		JLabel message = new JLabel("<html><div style='width:300px'>"
				+ html(AppLocal.getIntString("receiving.discardConfirm", receipt.supplier, receipt.note))
				+ "</div></html>");
		return JOptionPane.showOptionDialog(parent, message, tr("receiving.discard"), JOptionPane.DEFAULT_OPTION,
				JOptionPane.WARNING_MESSAGE, null, options, options[0]) == 1;
	}

	private void discard() {
		if (session == null || reviewing)
			return;
		if (!confirmDiscard(this, session)) {
			code.requestFocusInWindow();
			return;
		}
		try {
			logger.info("event=stock_receipt_discard_start");
			repository.discard(session.id);
			logger.info("event=stock_receipt_discard_success");
			session = null;
			code.setText("");
			table.clearSelection();
			refresh();
			app.getAppUserView().getTaskAction("com.openbravo.pos.forms.MenuStockManagement").actionPerformed(null);
		} catch (SQLException | IllegalStateException e) {
			error(e);
			refresh();
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
	private static String stockHeaderHint(int column) {
		return switch (column) {
			case 4 -> tr("receiving.unitsHint");
			case 5 -> tr("receiving.currentHint");
			case 6 -> tr("receiving.afterHint");
			default -> null;
		};
	}

	private static String stockHeaderLabel(int column) {
		return switch (column) {
			case 4 -> tr("receiving.unitsShort");
			case 5 -> tr("receiving.currentShort");
			case 6 -> tr("receiving.afterShort");
			default -> null;
		};
	}
	private static JLabel wrapped(String text, int width) {
		JLabel label = new JLabel();
		label.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
		int wrapWidth = width;
		while (true) {
			label.setText("<html><div style='width:" + wrapWidth + "px'>" + text + "</div></html>");
			int excess = label.getPreferredSize().width - width;
			if (excess <= 0 || wrapWidth == 1)
				break;
			wrapWidth = Math.max(1, wrapWidth - excess - 1);
		}
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

	private static Icon removeIcon() {
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
					g.setColor(component.isEnabled() ? RetailPOSColors.dangerText() : RetailPOSColors.inkMuted());
					g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
					g.drawLine(x + 3, y + 5, x + 17, y + 5);
					g.drawLine(x + 8, y + 3, x + 12, y + 3);
					g.drawRoundRect(x + 5, y + 7, 10, 11, 2, 2);
					g.drawLine(x + 9, y + 10, x + 9, y + 15);
					g.drawLine(x + 12, y + 10, x + 12, y + 15);
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
		result.setMargin(new java.awt.Insets(14, 14, 14, 14));
		result.addActionListener(e -> callback.run());
		return result;
	}
}
