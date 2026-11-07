package com.openbravo.pos.inventory;

import com.openbravo.basic.BasicException;
import com.openbravo.pos.customers.CustomerInfo;
import com.openbravo.pos.customers.DataLogicCustomers;
import com.openbravo.pos.customers.JCustomerFinder;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.AppView;
import com.openbravo.pos.forms.BeanFactoryApp;
import com.openbravo.pos.forms.BeanFactoryException;
import com.openbravo.pos.forms.DataLogicSales;
import com.openbravo.pos.forms.JPanelView;
import com.openbravo.pos.panels.JProductFinder;
import com.openbravo.pos.sales.TicketsEditor;
import com.openbravo.pos.sales.JPanelTicketSales;
import com.openbravo.pos.ticket.UserInfo;
import com.openbravo.pos.ticket.ProductInfoExt;
import com.openbravo.pos.theme.RetailPOSColors;
import com.openbravo.pos.theme.RetailPOSTheme;
import com.openbravo.pos.util.PosIcons;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.net.URI;
import java.net.URLEncoder;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.Container;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.sql.SQLException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JFileChooser;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.ButtonGroup;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import com.openbravo.data.gui.JConfirmationDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JDialog;
import javax.swing.BoxLayout;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.KeyStroke;
import javax.swing.AbstractAction;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicSplitPaneUI;

public class ReplenishmentPanel extends JPanel implements JPanelView, BeanFactoryApp {
	private static final Logger logger = Logger.getLogger(ReplenishmentPanel.class.getName());
	private static final Pattern CONSTRAINT = Pattern.compile("(?i)constraint [\"']([a-z0-9_]+)[\"']");
	private AppView app;
	private DataLogicReplenishment data;
	private DataLogicSales sales;
	private DataLogicCustomers customers;
	private final DefaultTableModel model = new DefaultTableModel(new Object[]{"",
			AppLocal.getIntString("Replenishment.RequestColumn"), AppLocal.getIntString("Replenishment.CustomerColumn"),
			AppLocal.getIntString("Replenishment.Status"), AppLocal.getIntString("Replenishment.Added"),
			AppLocal.getIntString("Replenishment.Supplier")}, 0) {
		@Override
		public boolean isCellEditable(int row, int column) {
			return false;
		}
		@Override
		public Class<?> getColumnClass(int column) {
			return column == 0 ? Boolean.class : String.class;
		}
	};
	private final JTable table = new JTable(model) {
		@Override
		protected void processMouseEvent(java.awt.event.MouseEvent event) {
			if (event.getID() == java.awt.event.MouseEvent.MOUSE_PRESSED && isGroupRow(rowAtPoint(event.getPoint())))
				return;
			if (event.getID() == java.awt.event.MouseEvent.MOUSE_PRESSED && isCheckboxColumn(event.getPoint())) {
				int row = rowAtPoint(event.getPoint());
				if (row >= 0) {
					if (isRowSelected(row))
						removeRowSelectionInterval(row, row);
					else
						addRowSelectionInterval(row, row);
					requestFocusInWindow();
				}
				return;
			}
			if (event.getID() == java.awt.event.MouseEvent.MOUSE_RELEASED && isCheckboxColumn(event.getPoint()))
				return;
			super.processMouseEvent(event);
		}
		private boolean isCheckboxColumn(java.awt.Point point) {
			int viewColumn = columnAtPoint(point);
			return administrationMode && viewColumn >= 0 && convertColumnIndexToModel(viewColumn) == 0;
		}
		@Override
		public void doLayout() {
			super.doLayout();
			resizeRows();
		}
		@Override
		public String getToolTipText(java.awt.event.MouseEvent event) {
			int row = rowAtPoint(event.getPoint());
			int column = columnAtPoint(event.getPoint());
			int modelColumn = column < 0 ? -1 : convertColumnIndexToModel(column);
			if (row >= 0 && visibleRows != null && !isGroupRow(row)
					&& (modelColumn == 1 || modelColumn == 2 || modelColumn == 5)) {
				ReplenishmentEntry entry = visibleRows.get(convertRowIndexToModel(row));
				if (modelColumn == 5)
					return supplierName(entry.supplierId);
				return modelColumn == 1
						? entry.productId == null
								? entry.displayName()
								: (displayReference(entry.name, entry.reference) + " "
										+ displayProductName(entry.name, entry.reference)).trim()
						: (valueOrEmpty(entry.customerName) + " " + valueOrEmpty(entry.customerPhone)).trim();
			}
			return super.getToolTipText(event);
		}
	};
	private JButton findProduct;
	private JButton customerButton;
	private JPanel productButtons;
	private JLabel scanLabel;
	private boolean loading;
	private List<ReplenishmentEntry> entries;
	private final List<ReplenishmentEntry> visibleRows = new ArrayList<>();
	private javax.swing.table.TableColumn supplierColumn;
	private javax.swing.table.TableColumn customerColumn;
	private javax.swing.table.TableColumn selectionColumn;
	private javax.swing.table.TableColumn statusColumn;
	private JPanel supplierSidebar;
	private JPanel sidebarItems;
	private JPanel supplierComboRow;
	private JPanel toolbar;
	private JPanel statusToggles;
	private JPanel segmentHolder;
	private JPanel searchRow;
	private boolean toolbarStacked;
	private JButton exportSelection;
	private JButton deselectButton;
	private JButton selectAllButton;
	private JPanel footerActions;
	private JPanel footerSummary;
	private boolean footerStacked;
	private JLabel footerSelection;
	private JLabel footerScope;
	private JButton newRequestButton;
	private JButton cancelNewRequest;
	private JPanel supplierHeading;
	private JPanel supplierHeadingText;
	private JPanel supplierActions;
	private boolean supplierActionsStacked;
	private JLabel supplierHeadingName;
	private JButton sendSupplierOrder;
	private JLabel supplierContact;
	private JLabel supplierPhone;
	private JLabel detailStatus;
	private JTextArea detailNote;
	private JLabel detailCustomer;
	private JLabel detailCustomerPhone;
	private JButton detailCustomerButton;
	private JButton detailSave, detailUndo, nextStep, detailDelete;
	private JTextArea detailProduct;
	private JLabel detailReference, detailCreated;
	private JLabel detailSupplierName;
	private JButton detailSupplierChange;
	private JButton closeDetail;
	private JTextArea detailHint;
	private String draftSupplierId;
	private JPanel selectionSuppliers;
	private boolean loadingDetail;
	private JTextField formCode;
	private JTextComponent formDescription;
	private JTextArea formNoteArea;
	private JLabel formStatusLabel, withoutCodeLabel, manualDescriptionLabel, creatorLabel;
	private JLabel productCodes;
	private JTextArea formDeliveryHint;
	private JButton save;
	private JPanel formStatusButtons;
	private JTextArea selectedProduct;
	private JLabel selectedCustomer;
	private JLabel selectedCustomerPhone;
	private JScrollPane manualDescriptionScroll;
	private JToggleButton formPending, formOrdered, formReceived, formDelivered;
	private JToggleButton pendingStatus;
	private JTextField search;
	private CustomerInfo formCustomer;
	private ProductInfoExt formProduct;
	private ReplenishmentEntry editingEntry;
	private String formStatus = "PENDING";
	private String selectedStatus = "PENDING";
	private String selectedType = "ALL";
	private String displayedSearch = "";
	private javax.swing.Timer filterTimer;
	private final Map<String, JToggleButton> statusButtons = new HashMap<>();
	private JLabel queueFilterLabel;
	private JPanel detailCards;
	private JPanel detailDock;
	private JPanel mainArea;
	private boolean administrationMode;
	private boolean modeInitialized;
	private boolean compactList;
	private JLabel selectionSummary;
	private JComboBox<ReplenishmentSupplier> supplierFilter;
	private String selectedSupplier = "ALL";
	private boolean refreshingSuppliers;
	private JLabel formSupplier;
	private boolean creatingRequest;

	@Override
	public void doLayout() {
		updateResponsiveLayout();
		super.doLayout();
		updateCompactColumns();
	}
	private void updateCompactColumns() {
		if (customerColumn == null || mainArea == null || getWidth() <= 0)
			return;
		int listWidth = getWidth() - (supplierSidebar.isVisible() ? 232 : 0) - (detailDock.isVisible() ? 376 : 0) - 64;
		boolean compact = administrationMode && listWidth < 720;
		if (compactList == compact && (table.convertColumnIndexToView(2) < 0) == compact
				&& (table.convertColumnIndexToView(5) >= 0) == (!compact && administrationMode
						&& "ALL".equals(selectedSupplier)))
			return;
		compactList = compact;
		if (compact && table.convertColumnIndexToView(2) >= 0)
			table.removeColumn(customerColumn);
		if (compact && table.convertColumnIndexToView(5) >= 0)
			table.removeColumn(supplierColumn);
		if (!compact && table.convertColumnIndexToView(2) < 0) {
			table.addColumn(customerColumn);
			table.moveColumn(table.convertColumnIndexToView(2), administrationMode ? 2 : 1);
		}
		if (!compact && administrationMode && "ALL".equals(selectedSupplier) && table.convertColumnIndexToView(5) < 0) {
			table.addColumn(supplierColumn);
			table.moveColumn(table.convertColumnIndexToView(5), 2);
		}
		table.revalidate();
		table.repaint();
	}
	private void updateResponsiveLayout() {
		if (supplierSidebar == null || supplierComboRow == null || getWidth() <= 0)
			return;
		boolean wide = administrationMode && getWidth() >= 980;
		if (supplierSidebar.isVisible() != wide)
			supplierSidebar.setVisible(wide);
		boolean combo = administrationMode && !wide;
		if (supplierComboRow.isVisible() != combo)
			supplierComboRow.setVisible(combo);
		if (toolbar != null)
			arrangeToolbar(toolbarAvailableWidth());
		if (footerActions != null)
			layoutFooter();
		if (supplierHeading != null && supplierActions != null) {
			int available = supplierHeading.getWidth() > 0
					? Math.min(supplierHeading.getWidth(), toolbarAvailableWidth())
					: toolbarAvailableWidth();
			boolean stackActions = available > 0 && available < supplierHeadingText.getPreferredSize().width
					+ supplierActions.getPreferredSize().width + 16;
			boolean vertical = stackActions && available < supplierActions.getComponent(0).getPreferredSize().width
					+ supplierActions.getComponent(1).getPreferredSize().width + 8;
			if (supplierActionsStacked != vertical) {
				supplierActionsStacked = vertical;
				GridBagLayout layout = (GridBagLayout) supplierActions.getLayout();
				GridBagConstraints send = layout.getConstraints(sendSupplierOrder);
				send.gridx = vertical ? 0 : 1;
				send.gridy = vertical ? 1 : 0;
				send.insets = vertical ? new Insets(4, 0, 0, 0) : new Insets(0, 8, 0, 0);
				layout.setConstraints(sendSupplierOrder, send);
				supplierActions.revalidate();
			}
			String position = stackActions ? BorderLayout.SOUTH : BorderLayout.EAST;
			if (supplierHeading.getLayout() instanceof BorderLayout
					&& !position.equals(((BorderLayout) supplierHeading.getLayout()).getConstraints(supplierActions))) {
				supplierHeading.remove(supplierActions);
				supplierHeading.add(supplierActions, position);
				supplierHeading.revalidate();
			}
		}
	}
	private int toolbarAvailableWidth() {
		return getWidth() - (supplierSidebar != null && supplierSidebar.isVisible() ? 248 : 0)
				- (detailDock != null && detailDock.isVisible() ? 376 : 0) - 52;
	}
	private void arrangeToolbar(int availableWidth) {
		if (toolbar == null || availableWidth <= 0)
			return;
		boolean stacked = segmentHolder.getPreferredSize().width + newRequestButton.getPreferredSize().width
				+ com.formdev.flatlaf.util.UIScale.scale(200) + 16 > availableWidth;
		if (stacked == toolbarStacked)
			return;
		toolbarStacked = stacked;
		toolbar.removeAll();
		GridBagConstraints cell = new GridBagConstraints();
		cell.anchor = GridBagConstraints.CENTER;
		cell.weighty = 0;
		cell.gridx = 0;
		cell.gridy = 0;
		if (stacked) {
			cell.weightx = 1;
			cell.fill = GridBagConstraints.HORIZONTAL;
			toolbar.add(segmentHolder, cell);
			cell.gridy = 1;
			cell.insets = new Insets(4, 0, 0, 0);
			toolbar.add(searchRow, cell);
		} else {
			cell.fill = GridBagConstraints.NONE;
			toolbar.add(segmentHolder, cell);
			cell.gridx = 1;
			cell.weightx = 1;
			cell.fill = GridBagConstraints.HORIZONTAL;
			cell.insets = new Insets(0, 8, 0, 0);
			toolbar.add(searchRow, cell);
		}
		toolbar.revalidate();
	}
	private boolean isNamedSupplier() {
		return !"ALL".equals(selectedSupplier) && !"NONE".equals(selectedSupplier);
	}
	private boolean isGroupRow(int row) {
		return row >= 0 && row < visibleRows.size() && visibleRows.get(table.convertRowIndexToModel(row)) == null;
	}
	private static javax.swing.Icon warningDot() {
		return new javax.swing.Icon() {
			@Override
			public int getIconWidth() {
				return 8;
			}
			@Override
			public int getIconHeight() {
				return 8;
			}
			@Override
			public void paintIcon(Component component, java.awt.Graphics graphics, int x, int y) {
				graphics.setColor(RetailPOSColors.warning());
				graphics.fillOval(x, y, 8, 8);
			}
		};
	}
	private static javax.swing.Icon moreIcon() {
		return new javax.swing.Icon() {
			@Override
			public int getIconWidth() {
				return 18;
			}
			@Override
			public int getIconHeight() {
				return 18;
			}
			@Override
			public void paintIcon(Component component, java.awt.Graphics graphics, int x, int y) {
				java.awt.Graphics2D g = (java.awt.Graphics2D) graphics.create();
				g.setColor(RetailPOSColors.ink());
				g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
						java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
				for (int dot = 0; dot < 3; dot++)
					g.fillOval(x + 2 + dot * 6, y + 7, 4, 4);
				g.dispose();
			}
		};
	}
	private static javax.swing.Icon selectionIcon(boolean checked, boolean mixed) {
		return new javax.swing.Icon() {
			@Override
			public int getIconWidth() {
				return 18;
			}
			@Override
			public int getIconHeight() {
				return 18;
			}
			@Override
			public void paintIcon(Component component, java.awt.Graphics graphics, int x, int y) {
				java.awt.Graphics2D g = (java.awt.Graphics2D) graphics.create();
				g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
						java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
				g.setColor(checked || mixed ? RetailPOSColors.brand() : RetailPOSColors.surface200());
				g.fillRoundRect(x, y, 17, 17, 4, 4);
				g.setColor(checked || mixed ? RetailPOSColors.brand() : RetailPOSColors.border());
				g.drawRoundRect(x, y, 17, 17, 4, 4);
				if (checked || mixed) {
					g.setColor(RetailPOSColors.onBrand());
					g.setStroke(new java.awt.BasicStroke(2f, java.awt.BasicStroke.CAP_ROUND,
							java.awt.BasicStroke.JOIN_ROUND));
					if (mixed)
						g.drawLine(x + 5, y + 9, x + 12, y + 9);
					else {
						java.awt.geom.Path2D path = new java.awt.geom.Path2D.Float();
						path.moveTo(x + 4, y + 9);
						path.lineTo(x + 7, y + 12);
						path.lineTo(x + 13, y + 5);
						g.draw(path);
					}
				}
				g.dispose();
			}
		};
	}
	private JPanel createSupplierSidebar() {
		JPanel sidebar = new JPanel(new BorderLayout());
		sidebar.setName("replenishment.supplierSidebar");
		// 232px is the intentional desktop navigation rail width; it collapses below
		// 1200px.
		sidebar.setPreferredSize(new Dimension(232, 0));
		sidebar.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
		sidebarItems = new JPanel();
		sidebarItems.setLayout(new GridBagLayout());
		JScrollPane scroller = new JScrollPane(sidebarItems);
		scroller.setBorder(BorderFactory.createEmptyBorder());
		sidebar.add(scroller, BorderLayout.CENTER);
		return sidebar;
	}
	private void refreshSidebar(List<ReplenishmentEntry> allSuppliers) {
		if (sidebarItems == null)
			return;
		sidebarItems.removeAll();
		for (int i = 0; i < supplierFilter.getItemCount(); i++) {
			ReplenishmentSupplier supplier = supplierFilter.getItemAt(i);
			String id = supplier.id;
			long count = allSuppliers.stream().filter(
					e -> "ALL".equals(id) || ("NONE".equals(id) ? e.supplierId == null : id.equals(e.supplierId)))
					.count();
			JButton button = new JButton(supplier.name) {
				@Override
				public Dimension getPreferredSize() {
					Dimension size = super.getPreferredSize();
					size.height = Math.max(size.height, com.formdev.flatlaf.util.UIScale.scale(44));
					return size;
				}
			};
			button.setHorizontalAlignment(JButton.LEFT);
			button.setForeground(RetailPOSColors.ink());
			button.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(17f));
			button.setContentAreaFilled(false);
			button.setOpaque(false);
			button.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
			button.putClientProperty("FlatLaf.style",
					"background: " + RetailPOSColors.toHex(RetailPOSColors.surface100()) + "; hoverBackground: "
							+ RetailPOSColors.toHex(RetailPOSColors.surface200()));
			button.setToolTipText(supplier.name);
			JLabel number = new JLabel(Long.toString(count));
			number.setFont(RetailPOSTheme.PLEX_MONO_REGULAR.deriveFont(13f));
			number.setForeground(RetailPOSColors.inkMuted());
			number.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
			JButton more = null;
			JPanel moreSlot = null;
			if (i >= 2) {
				more = new JButton(moreIcon()) {
					@Override
					public Dimension getPreferredSize() {
						Dimension size = super.getPreferredSize();
						size.width = Math.max(size.width, com.formdev.flatlaf.util.UIScale.scale(36));
						size.height = Math.max(size.height, com.formdev.flatlaf.util.UIScale.scale(36));
						return size;
					}
				};
				more.setName("replenishment.supplierMore." + id);
				more.setContentAreaFilled(false);
				more.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
				more.getAccessibleContext()
						.setAccessibleName(AppLocal.getIntString("Replenishment.EditSupplier") + ": " + supplier.name);
				JButton menuAnchor = more;
				more.addActionListener(
						e -> createSupplierActionsMenu(supplier).show(menuAnchor, 0, menuAnchor.getHeight()));
				moreSlot = new JPanel(new GridBagLayout()) {
					@Override
					public Dimension getPreferredSize() {
						Dimension size = super.getPreferredSize();
						size.width = Math.max(size.width, com.formdev.flatlaf.util.UIScale.scale(36));
						return size;
					}
				};
				moreSlot.setOpaque(false);
				moreSlot.add(more);
			}
			JPanel line = new JPanel(new GridBagLayout()) {
				@Override
				public Dimension getPreferredSize() {
					Dimension size = super.getPreferredSize();
					size.height = Math.max(size.height, com.formdev.flatlaf.util.UIScale.scale(48));
					return size;
				}
				@Override
				protected void paintComponent(java.awt.Graphics graphics) {
					java.awt.Graphics2D g = (java.awt.Graphics2D) graphics.create();
					boolean highlighted = Boolean.TRUE.equals(getClientProperty("replenishment.highlighted"));
					boolean hovered = Boolean.TRUE.equals(getClientProperty("replenishment.hovered"));
					if (highlighted || hovered) {
						g.setColor(highlighted ? RetailPOSColors.surface100() : RetailPOSColors.surface200());
						g.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
					}
					if (highlighted) {
						g.setColor(RetailPOSColors.borderStrong());
						g.setStroke(new java.awt.BasicStroke(2f));
						g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 10, 10);
					}
					g.dispose();
				}
			};
			line.setOpaque(false);
			GridBagConstraints supplierCell = new GridBagConstraints();
			supplierCell.weightx = 1;
			supplierCell.fill = GridBagConstraints.HORIZONTAL;
			supplierCell.anchor = GridBagConstraints.CENTER;
			line.add(button, supplierCell);
			supplierCell.gridx = 1;
			supplierCell.weightx = 0;
			supplierCell.fill = GridBagConstraints.NONE;
			line.add(number, supplierCell);
			if (moreSlot != null) {
				supplierCell.gridx = 2;
				line.add(moreSlot, supplierCell);
			}
			updateSidebarBorder(line, id.equals(selectedSupplier));
			JButton moreButton = more;
			Runnable updateActions = () -> {
				if (moreButton != null)
					moreButton.setVisible(
							id.equals(selectedSupplier) || button.isFocusOwner() || moreButton.isFocusOwner()
									|| Boolean.TRUE.equals(line.getClientProperty("replenishment.hovered")));
			};
			updateActions.run();
			button.addFocusListener(new java.awt.event.FocusAdapter() {
				@Override
				public void focusGained(java.awt.event.FocusEvent e) {
					updateSidebarBorder(line, true);
					updateActions.run();
				}
				@Override
				public void focusLost(java.awt.event.FocusEvent e) {
					updateSidebarBorder(line, id.equals(selectedSupplier));
					updateActions.run();
				}
			});
			java.awt.event.MouseAdapter hover = new java.awt.event.MouseAdapter() {
				@Override
				public void mouseEntered(java.awt.event.MouseEvent event) {
					line.putClientProperty("replenishment.hovered", true);
					updateActions.run();
					line.repaint();
				}
				@Override
				public void mouseExited(java.awt.event.MouseEvent event) {
					java.awt.Point point = javax.swing.SwingUtilities.convertPoint(event.getComponent(),
							event.getPoint(), line);
					line.putClientProperty("replenishment.hovered", line.contains(point));
					updateActions.run();
					line.repaint();
				}
			};
			line.addMouseListener(hover);
			button.addMouseListener(hover);
			number.addMouseListener(hover);
			if (moreButton != null) {
				moreButton.addMouseListener(hover);
				moreSlot.addMouseListener(hover);
				moreSlot.addMouseListener(new java.awt.event.MouseAdapter() {
					@Override
					public void mouseClicked(java.awt.event.MouseEvent event) {
						moreButton.doClick();
					}
				});
			}
			number.addMouseListener(new java.awt.event.MouseAdapter() {
				@Override
				public void mouseClicked(java.awt.event.MouseEvent event) {
					button.doClick();
				}
			});
			addSidebarItem(line);
			button.addActionListener(e -> selectSupplier(id));
			if (i == 1) {
				JPanel separator = new JPanel();
				separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 12));
				separator.setPreferredSize(new Dimension(0, 12));
				separator.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, RetailPOSColors.border()));
				addSidebarItem(separator);
			}
		}
		if (supplierFilter.getItemCount() == 2) {
			JLabel empty = new JLabel(AppLocal.getIntString("Replenishment.NoSuppliersYet"));
			empty.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f));
			empty.setForeground(RetailPOSColors.inkMuted());
			empty.setBorder(BorderFactory.createEmptyBorder(12, 8, 8, 8));
			empty.setMaximumSize(new Dimension(Integer.MAX_VALUE, empty.getPreferredSize().height));
			addSidebarItem(empty);
		}
		JButton create = new JButton(AppLocal.getIntString("Replenishment.NewSupplier"), PosIcons.plus());
		styleGhost(create);
		create.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(15f));
		create.setIconTextGap(4);
		create.setHorizontalAlignment(JButton.LEFT);
		RetailPOSTheme.touchHeight(create, 44);
		create.addActionListener(e -> manageSuppliers(null));
		addSidebarItem(create);
		GridBagConstraints remaining = new GridBagConstraints();
		remaining.gridx = 0;
		remaining.gridy = sidebarItems.getComponentCount();
		remaining.weighty = 1;
		sidebarItems.add(Box.createVerticalGlue(), remaining);
		sidebarItems.revalidate();
		sidebarItems.repaint();
	}
	private void addSidebarItem(Component item) {
		GridBagConstraints row = new GridBagConstraints();
		row.gridx = 0;
		row.gridy = sidebarItems.getComponentCount();
		row.weightx = 1;
		row.fill = GridBagConstraints.HORIZONTAL;
		row.anchor = GridBagConstraints.NORTHWEST;
		sidebarItems.add(item, row);
	}
	private JPopupMenu createSupplierActionsMenu(ReplenishmentSupplier supplier) {
		JPopupMenu menu = new JPopupMenu();
		menu.setName("replenishment.supplierActions");
		JMenuItem edit = new JMenuItem(AppLocal.getIntString("Replenishment.EditAction"));
		edit.addActionListener(e -> manageSuppliers(supplier.id));
		menu.add(edit);
		JMenuItem delete = new JMenuItem(AppLocal.getIntString("Replenishment.Delete"));
		delete.addActionListener(e -> deleteSupplier(supplier));
		menu.add(delete);
		return menu;
	}
	private void deleteSupplier(ReplenishmentSupplier supplier) {
		if (!administrationMode)
			return;
		try {
			int count = data.list("", "ALL", "ALL", supplier.id).size();
			String message = String.format(AppLocal.getIntString("Replenishment.ConfirmDeleteSupplierRequests"), count,
					supplier.name);
			if (JConfirmationDialog.show(this, message, AppLocal.getIntString("Replenishment.DeleteSupplier"),
					AppLocal.getIntString("Button.Cancel"), AppLocal.getIntString("Replenishment.Delete"),
					true) != JOptionPane.YES_OPTION)
				return;
			removeSupplier(supplier);
		} catch (BasicException ex) {
			JOptionPane.showMessageDialog(this, ex.getMessage());
		}
	}
	private void removeSupplier(ReplenishmentSupplier supplier) throws BasicException {
		data.deleteSupplier(supplier.id, app.getAppUserView().getUser().getId());
		if (supplier.id.equals(selectedSupplier))
			selectedSupplier = "ALL";
		refreshSupplierFilter();
		load(search.getText(), false);
	}
	private static void updateSidebarBorder(JPanel line, boolean highlighted) {
		line.putClientProperty("replenishment.highlighted", highlighted);
		line.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		line.repaint();
	}
	private static void styleGhost(JButton button) {
		button.setContentAreaFilled(false);
		button.setBorder(BorderFactory.createEmptyBorder(com.formdev.flatlaf.util.UIScale.scale(14),
				com.formdev.flatlaf.util.UIScale.scale(8), com.formdev.flatlaf.util.UIScale.scale(14),
				com.formdev.flatlaf.util.UIScale.scale(8)));
		button.setForeground(RetailPOSColors.ink());
		button.setBackground(RetailPOSColors.surface200());
		button.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseEntered(java.awt.event.MouseEvent event) {
				button.setOpaque(true);
				button.repaint();
			}
			@Override
			public void mouseExited(java.awt.event.MouseEvent event) {
				button.setOpaque(button.isFocusOwner());
				button.repaint();
			}
		});
		button.addFocusListener(new java.awt.event.FocusAdapter() {
			@Override
			public void focusGained(java.awt.event.FocusEvent e) {
				button.setOpaque(true);
				button.setBorder(BorderFactory.createCompoundBorder(
						BorderFactory.createLineBorder(RetailPOSColors.borderStrong(), 2),
						BorderFactory.createEmptyBorder(com.formdev.flatlaf.util.UIScale.scale(14) - 2,
								com.formdev.flatlaf.util.UIScale.scale(8) - 2,
								com.formdev.flatlaf.util.UIScale.scale(14) - 2,
								com.formdev.flatlaf.util.UIScale.scale(8) - 2)));
			}
			@Override
			public void focusLost(java.awt.event.FocusEvent e) {
				button.setOpaque(false);
				button.setBorder(BorderFactory.createEmptyBorder(com.formdev.flatlaf.util.UIScale.scale(14),
						com.formdev.flatlaf.util.UIScale.scale(8), com.formdev.flatlaf.util.UIScale.scale(14),
						com.formdev.flatlaf.util.UIScale.scale(8)));
			}
		});
	}
	private static void styleDangerGhost(JButton button) {
		button.setContentAreaFilled(false);
		button.setOpaque(false);
		button.setForeground(RetailPOSColors.dangerText());
		button.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		button.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseEntered(java.awt.event.MouseEvent event) {
				button.setBorder(roundedGhostBorder(RetailPOSColors.border(), 1));
			}
			@Override
			public void mouseExited(java.awt.event.MouseEvent event) {
				button.setBorder(button.isFocusOwner()
						? roundedGhostBorder(RetailPOSColors.borderStrong(), 2)
						: BorderFactory.createEmptyBorder(8, 8, 8, 8));
			}
		});
		button.addFocusListener(new java.awt.event.FocusAdapter() {
			@Override
			public void focusGained(java.awt.event.FocusEvent event) {
				button.setBorder(roundedGhostBorder(RetailPOSColors.borderStrong(), 2));
			}
			@Override
			public void focusLost(java.awt.event.FocusEvent event) {
				button.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
			}
		});
	}
	private static javax.swing.border.Border roundedGhostBorder(java.awt.Color color, int stroke) {
		return new javax.swing.border.AbstractBorder() {
			@Override
			public Insets getBorderInsets(Component component) {
				return new Insets(8, 8, 8, 8);
			}
			@Override
			public void paintBorder(Component component, java.awt.Graphics graphics, int x, int y, int width,
					int height) {
				java.awt.Graphics2D g = (java.awt.Graphics2D) graphics.create();
				g.setColor(color);
				g.setStroke(new java.awt.BasicStroke(stroke));
				g.drawRoundRect(x + stroke / 2, y + stroke / 2, width - stroke, height - stroke, 10, 10);
				g.dispose();
			}
		};
	}
	private void selectSupplier(String id) {
		selectedSupplier = id;
		refreshingSuppliers = true;
		try {
			for (int i = 0; i < supplierFilter.getItemCount(); i++)
				if (id.equals(supplierFilter.getItemAt(i).id)) {
					supplierFilter.setSelectedIndex(i);
					break;
				}
		} finally {
			refreshingSuppliers = false;
		}
		creatingRequest = false;
		clearSelectionForFilter();
		load(search.getText(), false);
	}
	private void selectAllRequests() {
		table.clearSelection();
		for (int row = 0; row < visibleRows.size(); row++)
			if (!isGroupRow(row))
				table.addRowSelectionInterval(row, row);
	}

	@Override
	public void init(AppView app) throws BeanFactoryException {
		this.app = app;
		data = app.getBean(DataLogicReplenishment.class);
		sales = app.getBean(DataLogicSales.class);
		customers = app.getBean(DataLogicCustomers.class);
		build();
	}
	private void build() {
		setLayout(new BorderLayout(16, 16));
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ESCAPE"), "closeDetail");
		getActionMap().put("closeDetail", new AbstractAction() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent event) {
				if (editingEntry != null)
					clearSelectedRows();
			}
		});
		queueFilterLabel = new JLabel(" ");
		queueFilterLabel.setForeground(RetailPOSColors.inkMuted());
		queueFilterLabel.setVisible(false);
		newRequestButton = new JButton(AppLocal.getIntString("Replenishment.NewRequest"), PosIcons.plus()) {
			@Override
			public Dimension getPreferredSize() {
				Dimension size = super.getPreferredSize();
				size.height = Math.max(size.height, com.formdev.flatlaf.util.UIScale.scale(44));
				return size;
			}
			@Override
			public Dimension getMinimumSize() {
				Dimension size = super.getMinimumSize();
				size.height = Math.max(size.height, com.formdev.flatlaf.util.UIScale.scale(44));
				return size;
			}
		};
		newRequestButton.setName("replenishment.newRequest");
		newRequestButton.setIconTextGap(8);
		newRequestButton.setMargin(new Insets(6, 12, 6, 12));
		RetailPOSTheme.touchHeight(newRequestButton, 44);
		newRequestButton.setBackground(RetailPOSColors.surface100());
		JPanel filters = new JPanel(new BorderLayout(0, 4));
		statusToggles = new JPanel(new GridBagLayout()) {
			@Override
			protected void paintComponent(java.awt.Graphics graphics) {
				java.awt.Graphics2D g = (java.awt.Graphics2D) graphics.create();
				g.setColor(RetailPOSColors.surface200());
				g.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
				g.dispose();
			}
		};
		statusToggles.setOpaque(false);
		statusToggles.setName("replenishment.statusSegments");
		statusToggles.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		ButtonGroup group = new ButtonGroup();
		String[] statuses = {"PENDING", "ORDERED", "RECEIVED", "DELIVERED", "ALL"};
		String[] labels = {"Replenishment.Pending", "Replenishment.Ordered", "Replenishment.Received",
				"Replenishment.Delivered", "Replenishment.All"};
		for (int i = 0; i < statuses.length; i++) {
			final String status = statuses[i];
			JToggleButton button = new JToggleButton(AppLocal.getIntString(labels[i])) {
				@Override
				public Dimension getPreferredSize() {
					Dimension size = super.getPreferredSize();
					size.height = Math.max(size.height, com.formdev.flatlaf.util.UIScale.scale(40));
					return size;
				}
				@Override
				protected void paintComponent(java.awt.Graphics graphics) {
					java.awt.Graphics2D g = (java.awt.Graphics2D) graphics.create();
					if (isSelected()) {
						java.awt.Color border = RetailPOSColors.border();
						g.setColor(new java.awt.Color(border.getRed(), border.getGreen(), border.getBlue(), 64));
						g.fillRoundRect(0, 2, getWidth(), getHeight() - 2, 8, 8);
						g.setColor(RetailPOSColors.surface100());
						g.fillRoundRect(0, 0, getWidth(), getHeight() - 2, 8, 8);
					} else if (getModel().isRollover()) {
						g.setColor(RetailPOSColors.surface100());
						g.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
					}
					if (isFocusOwner()) {
						g.setColor(RetailPOSColors.borderStrong());
						g.setStroke(new java.awt.BasicStroke(2f));
						g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 8, 8);
					}
					g.dispose();
					super.paintComponent(graphics);
				}
			};
			styleSegmentToggle(button);
			button.setSelected(i == 0);
			if ("PENDING".equals(status))
				pendingStatus = button;
			statusButtons.put(status, button);
			group.add(button);
			GridBagConstraints segment = new GridBagConstraints();
			segment.gridx = i;
			segment.gridy = 0;
			segment.weightx = 0;
			segment.fill = GridBagConstraints.NONE;
			statusToggles.add(button, segment);
			button.addActionListener(e -> {
				selectedStatus = status;
				updateStatusSegmentStyles();
				selectedType = "ALL";
				queueFilterLabel.setText(" ");
				queueFilterLabel.setVisible(false);
				clearSelectionForFilter();
				creatingRequest = false;
				load("", false);
			});
		}
		updateStatusSegmentStyles();
		search = touchField(0, 44);
		search.putClientProperty("JTextField.placeholderText",
				AppLocal.getIntString("Replenishment.SearchPlaceholder"));
		search.putClientProperty("JTextField.leadingIcon", paddedSearchIcon());
		styleInput(search);
		search.setMargin(new Insets(0, 2, 0, 12));
		RetailPOSTheme.touchHeight(search, 44);
		searchRow = new JPanel(new GridBagLayout());
		searchRow.setName("replenishment.searchRow");
		GridBagConstraints searchCell = new GridBagConstraints();
		searchCell.gridx = 0;
		searchCell.weightx = 1;
		searchCell.fill = GridBagConstraints.HORIZONTAL;
		searchCell.anchor = GridBagConstraints.CENTER;
		searchRow.add(search, searchCell);
		searchCell.gridx = 1;
		searchCell.weightx = 0;
		searchCell.fill = GridBagConstraints.NONE;
		searchCell.insets = new Insets(0, 8, 0, 0);
		searchRow.add(newRequestButton, searchCell);
		segmentHolder = new JPanel(new BorderLayout());
		JScrollPane segmentScroll = new JScrollPane(statusToggles, JScrollPane.VERTICAL_SCROLLBAR_NEVER,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED) {
			@Override
			public Dimension getPreferredSize() {
				Dimension content = statusToggles.getPreferredSize();
				int available = toolbarAvailableWidth();
				int scrollbar = toolbarStacked && (available == 0 || content.width > available)
						? getHorizontalScrollBar().getPreferredSize().height
						: 0;
				return new Dimension(content.width, content.height + scrollbar);
			}
			@Override
			public Dimension getMinimumSize() {
				return new Dimension(0, getPreferredSize().height);
			}
		};
		segmentScroll.setBorder(BorderFactory.createEmptyBorder());
		segmentScroll.getHorizontalScrollBar().setUnitIncrement(24);
		segmentHolder.add(segmentScroll, BorderLayout.CENTER);
		toolbar = new JPanel(new GridBagLayout());
		toolbar.setName("replenishment.toolbar");
		GridBagConstraints top = new GridBagConstraints();
		top.weightx = 1;
		top.fill = GridBagConstraints.HORIZONTAL;
		toolbar.add(segmentHolder, top);
		top.gridy = 1;
		top.insets = new Insets(4, 0, 0, 0);
		toolbar.add(searchRow, top);
		toolbarStacked = true;
		filters.add(toolbar, BorderLayout.NORTH);
		JPanel supplierRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		supplierRow.add(new JLabel(AppLocal.getIntString("Replenishment.Supplier")));
		supplierFilter = new JComboBox<>();
		supplierFilter
				.setPrototypeDisplayValue(filterChoice("ALL", AppLocal.getIntString("Replenishment.AllSuppliers")));
		supplierRow.add(supplierFilter);
		supplierComboRow = supplierRow;
		filters.add(supplierRow, BorderLayout.SOUTH);
		supplierFilter.addActionListener(e -> {
			if (refreshingSuppliers || supplierFilter.getSelectedItem() == null)
				return;
			selectedSupplier = ((ReplenishmentSupplier) supplierFilter.getSelectedItem()).id;
			supplierFilter.setToolTipText(((ReplenishmentSupplier) supplierFilter.getSelectedItem()).name);
			clearSelectionForFilter();
			creatingRequest = false;
			load(search.getText(), false);
		});
		JPanel north = new JPanel(new BorderLayout());
		north.add(filters, BorderLayout.NORTH);
		JPanel contextHeading = new JPanel(new BorderLayout());
		contextHeading.add(queueFilterLabel, BorderLayout.NORTH);
		supplierHeading = new JPanel(new BorderLayout(8, 4));
		supplierHeading.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
		JPanel headingText = new JPanel(new BorderLayout(0, 4));
		supplierHeadingText = headingText;
		supplierHeadingName = new JLabel();
		supplierHeadingName.setFont(RetailPOSTheme.MANROPE_BOLD.deriveFont(20f));
		headingText.add(supplierHeadingName, BorderLayout.NORTH);
		JPanel contactLine = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		supplierContact = new JLabel();
		supplierContact.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f));
		supplierContact.setForeground(RetailPOSColors.inkMuted());
		supplierPhone = new JLabel();
		supplierPhone.setFont(RetailPOSTheme.PLEX_MONO_REGULAR.deriveFont(13f));
		supplierPhone.setForeground(RetailPOSColors.inkMuted());
		contactLine.add(supplierContact);
		contactLine.add(supplierPhone);
		headingText.add(contactLine, BorderLayout.SOUTH);
		supplierHeading.add(headingText, BorderLayout.CENTER);
		JButton editSupplier = new JButton(AppLocal.getIntString("Replenishment.EditSupplier"),
				PosIcons.edit().derive(16, 16));
		styleGhost(editSupplier);
		editSupplier.setIconTextGap(4);
		editSupplier.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f));
		RetailPOSTheme.touchHeight(editSupplier, 44);
		editSupplier.addActionListener(e -> manageSuppliers(selectedSupplier));
		supplierActions = new JPanel(new GridBagLayout());
		GridBagConstraints supplierAction = new GridBagConstraints();
		supplierAction.anchor = GridBagConstraints.LINE_END;
		supplierActions.add(editSupplier, supplierAction);
		sendSupplierOrder = new JButton();
		RetailPOSColors.primaryButton(sendSupplierOrder);
		RetailPOSTheme.touchHeight(sendSupplierOrder, 48);
		sendSupplierOrder.addActionListener(e -> showSendDialog(supplierHasPhone() ? "WHATSAPP" : "TEXT"));
		supplierAction.gridx = 1;
		supplierAction.insets = new Insets(0, 8, 0, 0);
		supplierActions.add(sendSupplierOrder, supplierAction);
		supplierHeading.add(supplierActions, BorderLayout.EAST);
		supplierHeading.setVisible(false);
		contextHeading.add(supplierHeading, BorderLayout.SOUTH);
		north.add(contextHeading, BorderLayout.SOUTH);
		table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		table.setRowHeight(48);
		table.setShowHorizontalLines(true);
		table.setShowVerticalLines(false);
		table.setGridColor(RetailPOSColors.border());
		table.setIntercellSpacing(new java.awt.Dimension(0, 1));
		table.getTableHeader().setDefaultRenderer((t, value, selected, focused, row, column) -> {
			if (administrationMode && table.convertColumnIndexToModel(column) == 0) {
				JCheckBox check = new JCheckBox();
				int count = table.getSelectedRowCount();
				check.setSelected(entries != null && !entries.isEmpty() && count == entries.size());
				if (count > 0 && entries != null && count < entries.size())
					check.putClientProperty("JButton.selectedState", "indeterminate");
				check.setHorizontalAlignment(JCheckBox.CENTER);
				check.setOpaque(true);
				check.setBackground(RetailPOSColors.surface100());
				check.setIcon(
						selectionIcon(check.isSelected(), count > 0 && entries != null && count < entries.size()));
				check.getAccessibleContext().setAccessibleName(AppLocal.getIntString("Replenishment.SelectAll"));
				return check;
			}
			JLabel headerLabel = new JLabel(valueOrEmpty((String) value));
			headerLabel.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f));
			headerLabel.setForeground(RetailPOSColors.inkMuted());
			headerLabel.setBackground(RetailPOSColors.surface100());
			headerLabel.setOpaque(true);
			headerLabel.setHorizontalAlignment(JLabel.LEFT);
			headerLabel.setBorder(
					BorderFactory.createEmptyBorder(0, table.convertColumnIndexToModel(column) == 4 ? 4 : 8, 0, 0));
			return headerLabel;
		});
		table.getTableHeader().addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseClicked(java.awt.event.MouseEvent event) {
				if (!administrationMode || table.columnAtPoint(event.getPoint()) != 0)
					return;
				if (entries != null && table.getSelectedRowCount() == entries.size())
					clearSelectedRows();
				else
					selectAllRequests();
			}
		});
		table.getInputMap().put(KeyStroke.getKeyStroke("SPACE"), "toggleRequest");
		table.getActionMap().put("toggleRequest", new AbstractAction() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent event) {
				int row = table.getSelectionModel().getLeadSelectionIndex();
				if (administrationMode && row >= 0 && row < table.getRowCount() && !isGroupRow(row)) {
					if (table.isRowSelected(row))
						table.removeRowSelectionInterval(row, row);
					else
						table.addRowSelectionInterval(row, row);
				}
			}
		});
		table.getInputMap().put(KeyStroke.getKeyStroke("ESCAPE"), "clearRequests");
		table.getActionMap().put("clearRequests", new AbstractAction() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent event) {
				clearSelectedRows();
			}
		});
		table.getSelectionModel().addListSelectionListener(e -> {
			if (!loading && !e.getValueIsAdjusting()) {
				if (table.getSelectedRowCount() == 0 && editingEntry != null) {
					for (int row = 0; row < model.getRowCount(); row++)
						model.setValueAt(false, row, 0);
					table.getTableHeader().repaint();
					clearForm();
					creatingRequest = false;
					updateSelectionView();
					return;
				}
				for (int row : table.getSelectedRows())
					if (isGroupRow(row)) {
						table.removeRowSelectionInterval(row, row);
						return;
					}
				for (int row = 0; row < model.getRowCount(); row++)
					model.setValueAt(table.isRowSelected(row), row, 0);
				table.getTableHeader().repaint();
				if (table.getSelectedRowCount() == 1 && !visibleRows.isEmpty()) {
					ReplenishmentEntry selectedEntry = visibleRows
							.get(table.convertRowIndexToModel(table.getSelectedRow()));
					if (editingEntry == null || !editingEntry.id.equals(selectedEntry.id))
						loadEntry(selectedEntry);
				}
				updateSelectionView();
			}
		});
		table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
			{
				setVerticalAlignment(JLabel.CENTER);
			}
			@Override
			public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
					boolean focused, int row, int column) {
				java.awt.Component component = super.getTableCellRendererComponent(table, value, selected, focused, row,
						column);
				setFont(RetailPOSTheme.MANROPE_MEDIUM);
				if (isGroupRow(row)) {
					setText("");
					setBackground(RetailPOSColors.surface200());
					setBorder(BorderFactory.createEmptyBorder());
					return component;
				}
				component.setBackground(selected ? RetailPOSColors.surface200() : RetailPOSColors.surface100());
				component.setForeground(RetailPOSColors.ink());
				setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
				return component;
			}
		});
		table.getColumnModel().getColumn(0).setCellRenderer((grid, value, selected, focused, row, column) -> {
			if (isGroupRow(row)) {
				JLabel blank = new JLabel();
				blank.setOpaque(true);
				blank.setBackground(RetailPOSColors.surface200());
				return blank;
			}
			JCheckBox check = new JCheckBox();
			check.setHorizontalAlignment(JLabel.CENTER);
			check.setSelected(Boolean.TRUE.equals(value));
			check.setIcon(selectionIcon(check.isSelected(), false));
			check.setOpaque(true);
			check.setBackground(selected ? RetailPOSColors.surface200() : RetailPOSColors.surface100());
			check.setForeground(RetailPOSColors.ink());
			check.setBorderPainted(true);
			check.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
			return check;
		});
		statusColumn = table.getColumnModel().getColumn(3);
		statusColumn.setCellRenderer(new StatusBadgeRenderer());
		table.getColumnModel().getColumn(1).setCellRenderer(new ProductRenderer());
		table.getColumnModel().getColumn(2).setCellRenderer(new CustomerRenderer());
		selectionColumn = table.getColumnModel().getColumn(0);
		supplierColumn = table.getColumnModel().getColumn(5);
		customerColumn = table.getColumnModel().getColumn(2);
		int supplierWidth = table.getFontMetrics(RetailPOSTheme.MANROPE_MEDIUM)
				.stringWidth(AppLocal.getIntString("Replenishment.NoSupplier")) + 32;
		supplierColumn.setPreferredWidth(supplierWidth);
		supplierColumn.setMinWidth(supplierWidth);
		supplierColumn.setMaxWidth(200);
		supplierColumn.setCellRenderer(new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable grid, Object value, boolean selected, boolean focused,
					int row, int column) {
				super.getTableCellRendererComponent(grid, value, selected, focused, row, column);
				setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(15f));
				setVerticalAlignment(JLabel.CENTER);
				setForeground(value == null || AppLocal.getIntString("Replenishment.NoSupplier").equals(value)
						? RetailPOSColors.inkMuted()
						: RetailPOSColors.ink());
				setBackground(selected ? RetailPOSColors.surface200() : RetailPOSColors.surface100());
				setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
				return this;
			}
		});
		JPanel list = new JPanel(new BorderLayout());
		JScrollPane tableScroll = new JScrollPane(table);
		tableScroll.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
		table.getTableHeader().setPreferredSize(new Dimension(0, 32));
		tableScroll.setColumnHeaderView(table.getTableHeader());
		list.add(tableScroll, BorderLayout.CENTER);
		footerActions = new JPanel(new GridBagLayout());
		footerActions.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
		footerSummary = new JPanel();
		footerSummary.setLayout(new BoxLayout(footerSummary, BoxLayout.Y_AXIS));
		footerSelection = new JLabel();
		footerSelection.setFont(RetailPOSTheme.MANROPE_BOLD.deriveFont(17f));
		footerScope = new JLabel();
		footerScope.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f));
		footerScope.setForeground(RetailPOSColors.inkMuted());
		footerSummary.add(footerSelection);
		footerSummary.add(footerScope);
		selectAllButton = new JButton(AppLocal.getIntString("Replenishment.SelectAll"));
		deselectButton = new JButton(AppLocal.getIntString("Replenishment.ClearSelection"));
		exportSelection = new JButton(AppLocal.getIntString("Replenishment.Export"));
		for (JButton button : new JButton[]{selectAllButton, deselectButton}) {
			styleGhost(button);
			button.setFont(RetailPOSTheme.MANROPE_SEMIBOLD.deriveFont(15f));
			RetailPOSTheme.touchHeight(button, 44);
		}
		exportSelection.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f));
		exportSelection.setMargin(new Insets(8, 4, 8, 4));
		RetailPOSTheme.touchHeight(exportSelection, 48);
		layoutFooter();
		selectAllButton.addActionListener(e -> selectAllRequests());
		deselectButton.addActionListener(e -> clearSelectedRows());
		exportSelection.addActionListener(e -> showSendDialog("TEXT"));
		list.add(footerActions, BorderLayout.SOUTH);
		javax.swing.table.TableColumnModel columns = table.getColumnModel();
		columns.getColumn(0).setMinWidth(48);
		columns.getColumn(0).setPreferredWidth(48);
		columns.getColumn(0).setMaxWidth(48);
		columns.getColumn(1).setMinWidth(110);
		columns.getColumn(1).setPreferredWidth(260);
		columns.getColumn(2).setMinWidth(72);
		columns.getColumn(2).setPreferredWidth(140);
		columns.getColumn(2).setMaxWidth(200);
		columns.getColumn(3).setPreferredWidth(115);
		columns.getColumn(3).setMinWidth(110);
		columns.getColumn(3).setMaxWidth(135);
		int dateWidth = tabularDateWidth(table.getFontMetrics(RetailPOSTheme.PLEX_MONO_REGULAR.deriveFont(13f)),
				"00/00/00") + 8;
		columns.getColumn(4).setMinWidth(dateWidth);
		columns.getColumn(4).setPreferredWidth(dateWidth);
		columns.getColumn(4).setMaxWidth(dateWidth);
		// The optional supplier column is present only in the All suppliers view.
		table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
			@Override
			public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
					boolean focused, int row, int column) {
				super.getTableCellRendererComponent(table, value, selected, focused, row, column);
				setFont(RetailPOSTheme.PLEX_MONO_REGULAR.deriveFont(13f));
				setVerticalAlignment(JLabel.CENTER);
				if (isGroupRow(row))
					setText("");
				setBackground(
						isGroupRow(row) || selected ? RetailPOSColors.surface200() : RetailPOSColors.surface100());
				setForeground(RetailPOSColors.ink());
				setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
				return this;
			}
			@Override
			protected void paintComponent(java.awt.Graphics graphics) {
				java.awt.Graphics2D g = (java.awt.Graphics2D) graphics.create();
				g.setColor(getBackground());
				g.fillRect(0, 0, getWidth(), getHeight());
				g.setFont(getFont());
				g.setColor(getForeground());
				java.awt.FontMetrics metrics = g.getFontMetrics();
				int digitWidth = widestDigit(metrics);
				int x = getInsets().left;
				int y = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent() + 4;
				for (char character : getText().toCharArray()) {
					int width = metrics.charWidth(character);
					int advance = Character.isDigit(character) ? digitWidth : width;
					g.drawString(String.valueOf(character), x + (advance - width) / 2, y);
					x += advance;
				}
				g.dispose();
			}
		});
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		table.getColumnModel().addColumnModelListener(new javax.swing.event.TableColumnModelListener() {
			@Override
			public void columnMarginChanged(javax.swing.event.ChangeEvent event) {
				resizeRows();
			}
			@Override
			public void columnAdded(javax.swing.event.TableColumnModelEvent event) {
			}
			@Override
			public void columnRemoved(javax.swing.event.TableColumnModelEvent event) {
			}
			@Override
			public void columnMoved(javax.swing.event.TableColumnModelEvent event) {
			}
			@Override
			public void columnSelectionChanged(javax.swing.event.ListSelectionEvent event) {
			}
		});
		table.addComponentListener(new java.awt.event.ComponentAdapter() {
			@Override
			public void componentResized(java.awt.event.ComponentEvent event) {
				resizeRows();
			}
		});
		JPanel left = new RaisedCard(new BorderLayout(0, 8));
		left.add(north, BorderLayout.NORTH);
		left.add(list, BorderLayout.CENTER);
		detailCards = new RaisedCard(new CardLayout());
		detailCards.add(createForm(), "new");
		detailCards.add(createDetail(), "detail");
		detailCards.add(createSelectionPanel(), "selection");
		mainArea = new JPanel(new BorderLayout(16, 0));
		mainArea.add(left, BorderLayout.CENTER);
		detailDock = new JPanel(new BorderLayout());
		// The operator's detail stays touch-sized; the list owns all extra width.
		detailDock.setPreferredSize(new Dimension(360, 0));
		detailDock.add(detailCards, BorderLayout.CENTER);
		mainArea.add(detailDock, BorderLayout.EAST);
		supplierSidebar = createSupplierSidebar();
		supplierSidebar.setBorder(BorderFactory.createEmptyBorder());
		applyCardSurface(left);
		applyCardSurface(detailCards);
		add(supplierSidebar, BorderLayout.WEST);
		add(mainArea, BorderLayout.CENTER);
		updateResponsiveLayout();
		search.addActionListener(e -> {
			clearSelectionForFilter();
			load(search.getText(), false);
		});
		filterTimer = new javax.swing.Timer(250, e -> {
			clearSelectionForFilter();
			load(search.getText(), false);
		});
		filterTimer.setRepeats(false);
		search.getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) {
				filterTimer.restart();
			}
			public void removeUpdate(DocumentEvent e) {
				filterTimer.restart();
			}
			public void changedUpdate(DocumentEvent e) {
				filterTimer.restart();
			}
		});
		newRequestButton.addActionListener(e -> clearForm());
		javax.swing.SwingUtilities.invokeLater(() -> search.requestFocusInWindow());
	}
	private void deleteSelected() {
		List<String> ids = selectedIds();
		if (ids.isEmpty())
			return;
		if (editingEntry != null && detailSave.isEnabled()) {
			JOptionPane.showMessageDialog(this, AppLocal.getIntString("Replenishment.UnsavedBulkEdits"));
			return;
		}
		int answer = JConfirmationDialog.show(this,
				ids.size() == 1
						? AppLocal.getIntString("Replenishment.ConfirmDelete")
						: String.format(AppLocal.getIntString("Replenishment.ConfirmDeleteMultiple"), ids.size()),
				AppLocal.getIntString("Replenishment.Delete"), AppLocal.getIntString("Button.Cancel"),
				AppLocal.getIntString("Replenishment.Delete"), true);
		if (answer != JOptionPane.YES_OPTION)
			return;
		try {
			data.deleteAll(ids);
			clearForm();
			load(search.getText(), false);
		} catch (BasicException e) {
			JOptionPane.showMessageDialog(this, e.getMessage());
		}
	}
	private List<ReplenishmentEntry> outgoingEntries(boolean onlySelected) {
		if (editingEntry != null && save.isEnabled()) {
			JOptionPane.showMessageDialog(this, AppLocal.getIntString("Replenishment.UnsavedBulkEdits"));
			return null;
		}
		if (filterTimer.isRunning()) {
			filterTimer.stop();
			if (!load(search.getText(), false))
				return null;
		}
		if (entries == null || entries.isEmpty()) {
			JOptionPane.showMessageDialog(this, AppLocal.getIntString("Replenishment.NothingToExport"));
			return null;
		}
		try {
			List<ReplenishmentEntry> exported = data.listForExport(displayedSearch, selectedType, selectedStatus,
					selectedSupplier);
			exported = ReplenishmentOrderText.scope(exported, entries, new HashSet<>(selectedIds()), onlySelected);
			if (exported.isEmpty()) {
				JOptionPane.showMessageDialog(this, AppLocal.getIntString("Replenishment.NothingToExport"));
				return null;
			}
			return exported;
		} catch (BasicException ex) {
			JOptionPane.showMessageDialog(this, ex.getMessage());
			return null;
		}
	}
	private void showSendDialog(String initialChannel) {
		if (!administrationMode)
			return;
		showOverlayDialog(createSendDialog(initialChannel));
	}
	private void showOverlayDialog(JDialog dialog) {
		if (dialog != null) {
			javax.swing.JRootPane root = javax.swing.SwingUtilities.getRootPane(this);
			Component previous = root == null ? null : root.getGlassPane();
			boolean wasVisible = previous != null && previous.isVisible();
			if (root != null) {
				JPanel scrim = new JPanel() {
					@Override
					protected void paintComponent(java.awt.Graphics graphics) {
						java.awt.Color ink = RetailPOSColors.ink();
						graphics.setColor(new java.awt.Color(ink.getRed(), ink.getGreen(), ink.getBlue(), 110));
						graphics.fillRect(0, 0, getWidth(), getHeight());
					}
				};
				scrim.setOpaque(false);
				root.setGlassPane(scrim);
				scrim.setVisible(true);
			}
			try {
				dialog.setVisible(true);
			} finally {
				if (root != null) {
					root.getGlassPane().setVisible(false);
					root.setGlassPane(previous);
					previous.setVisible(wasVisible);
				}
				dialog.dispose();
			}
		}
	}
	private static JPanel modalSurface(java.awt.LayoutManager layout) {
		JPanel panel = new JPanel(layout) {
			@Override
			protected void paintComponent(java.awt.Graphics graphics) {
				java.awt.Graphics2D g = (java.awt.Graphics2D) graphics.create();
				java.awt.Color border = RetailPOSColors.border();
				g.setColor(new java.awt.Color(border.getRed(), border.getGreen(), border.getBlue(), 64));
				g.fillRoundRect(4, 4, getWidth() - 4, getHeight() - 4, 16, 16);
				g.setColor(RetailPOSColors.surface100());
				g.fillRoundRect(0, 0, getWidth() - 5, getHeight() - 5, 16, 16);
				g.setColor(border);
				g.drawRoundRect(0, 0, getWidth() - 5, getHeight() - 5, 16, 16);
				g.dispose();
			}
		};
		panel.setOpaque(false);
		panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
		return panel;
	}
	private static void styleOverlayDialog(JDialog dialog, JPanel content) {
		dialog.setContentPane(content);
		if (java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
				.isWindowTranslucencySupported(java.awt.GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT)) {
			dialog.setUndecorated(true);
			java.awt.Color surface = RetailPOSColors.surface100();
			dialog.setBackground(new java.awt.Color(surface.getRed(), surface.getGreen(), surface.getBlue(), 0));
		}
	}
	private JDialog createSendDialog(String initialChannel) {
		List<ReplenishmentEntry> all = outgoingEntries(false);
		if (all == null)
			return null;
		Set<String> chosenIds = new HashSet<>(selectedIds());
		List<ReplenishmentEntry> chosen = all.stream().filter(e -> chosenIds.contains(e.id)).toList();
		JDialog dialog = new JDialog(javax.swing.SwingUtilities.getWindowAncestor(this),
				AppLocal.getIntString("Replenishment.ShareOrExport"), java.awt.Dialog.ModalityType.APPLICATION_MODAL);
		JPanel body = modalSurface(new BorderLayout(12, 12));
		JPanel heading = new JPanel(new GridLayout(0, 1, 0, 4));
		heading.setOpaque(false);
		JLabel eyebrow = sectionLabel("Replenishment.OrderEyebrow");
		eyebrow.setForeground(RetailPOSColors.inkMuted());
		JLabel title = new JLabel(AppLocal.getIntString("Replenishment.SendOrderTitle"));
		title.setFont(RetailPOSTheme.MANROPE_BOLD.deriveFont(20f));
		heading.add(eyebrow);
		heading.add(title);
		body.add(heading, BorderLayout.NORTH);
		JPanel center = new JPanel(new BorderLayout(0, 8));
		center.setOpaque(false);
		JPanel options = new JPanel(new GridBagLayout());
		options.setOpaque(false);
		GridBagConstraints optionCell = new GridBagConstraints();
		optionCell.weightx = 1;
		optionCell.fill = GridBagConstraints.HORIZONTAL;
		optionCell.anchor = GridBagConstraints.LINE_START;
		optionCell.insets = new Insets(0, 0, 8, 0);
		JPanel channelRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		channelRow.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		channelRow.setBackground(RetailPOSColors.surface200());
		ButtonGroup channels = new ButtonGroup();
		Map<String, JToggleButton> channelButtons = new HashMap<>();
		for (String channel : new String[]{"WHATSAPP", "TEXT", "PRINT"}) {
			String key = "WHATSAPP".equals(channel)
					? "Replenishment.WhatsAppChannel"
					: "TEXT".equals(channel) ? "Replenishment.TextFile" : "Replenishment.Print";
			JToggleButton button = new JToggleButton(AppLocal.getIntString(key));
			button.setContentAreaFilled(false);
			button.setForeground(RetailPOSColors.ink());
			button.setSelected(channel.equals(initialChannel));
			channels.add(button);
			channelRow.add(button);
			channelButtons.put(channel, button);
		}
		options.add(channelRow, optionCell);
		JLabel scopeLabel = sectionLabel("Replenishment.IncludeEyebrow");
		scopeLabel.setForeground(RetailPOSColors.inkMuted());
		optionCell.gridy = 1;
		options.add(scopeLabel, optionCell);
		JPanel scopeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		scopeRow.setOpaque(false);
		JToggleButton allScope = new JToggleButton(
				String.format(AppLocal.getIntString("Replenishment.AllVisible"), all.size()));
		JToggleButton selectedScope = new JToggleButton(
				String.format(AppLocal.getIntString("Replenishment.OnlySelected"), chosen.size()));
		styleFilterToggle(allScope);
		styleFilterToggle(selectedScope);
		selectedScope.setEnabled(!chosen.isEmpty());
		ButtonGroup scopes = new ButtonGroup();
		scopes.add(allScope);
		scopes.add(selectedScope);
		(chosen.isEmpty() ? allScope : selectedScope).setSelected(true);
		scopeRow.add(allScope);
		scopeRow.add(selectedScope);
		optionCell.gridy = 2;
		options.add(scopeRow, optionCell);
		JLabel messageLabel = sectionLabel("Replenishment.MessageEyebrow");
		messageLabel.setForeground(RetailPOSColors.inkMuted());
		optionCell.gridy = 3;
		options.add(messageLabel, optionCell);
		JLabel phoneLabel = new JLabel();
		phoneLabel.setFont(RetailPOSTheme.PLEX_MONO_REGULAR.deriveFont(13f));
		phoneLabel.setForeground(RetailPOSColors.inkMuted());
		center.add(options, BorderLayout.NORTH);
		JTextArea message = new JTextArea(13, 45);
		message.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(15f));
		message.setLineWrap(true);
		message.setWrapStyleWord(true);
		styleInput(message);
		JScrollPane textScroll = new JScrollPane(message);
		styleAreaScroll(textScroll, message);
		center.add(textScroll, BorderLayout.CENTER);
		JLabel editHint = new JLabel(AppLocal.getIntString("Replenishment.EditMessageHint"));
		editHint.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f));
		editHint.setForeground(RetailPOSColors.inkMuted());
		center.add(editHint, BorderLayout.SOUTH);
		body.add(center, BorderLayout.CENTER);
		JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
		actions.setOpaque(false);
		JButton cancel = new JButton(AppLocal.getIntString("Button.Cancel"));
		JButton confirm = new JButton();
		RetailPOSColors.primaryButton(confirm);
		confirm.setMargin(new Insets(8, 20, 8, 20));
		enableEnter(confirm);
		RetailPOSTheme.touchHeight(cancel, 48);
		RetailPOSTheme.touchHeight(confirm, 48);
		actions.add(cancel);
		actions.add(confirm);
		JPanel footer = new JPanel(new BorderLayout(8, 0));
		footer.setOpaque(false);
		footer.add(phoneLabel, BorderLayout.WEST);
		footer.add(actions, BorderLayout.EAST);
		body.add(footer, BorderLayout.SOUTH);
		styleOverlayDialog(dialog, body);
		dialog.getRootPane().setDefaultButton(confirm);
		Runnable refresh = () -> {
			List<ReplenishmentEntry> scope = selectedScope.isSelected() ? chosen : all;
			ReplenishmentSupplier supplier = singleSupplier(scope);
			eyebrow.setText(String.format(AppLocal.getIntString("Replenishment.OrderSupplierEyebrow"),
					supplier == null ? AppLocal.getIntString("Replenishment.AllSuppliers") : supplier.name));
			String phone = supplier == null ? "" : valueOrEmpty(supplier.whatsapp).replaceAll("\\D", "");
			boolean whatsapp = channelButtons.get("WHATSAPP").isSelected();
			for (JToggleButton button : channelButtons.values())
				updateSegmentStyle(button);
			phoneLabel.setVisible(whatsapp || supplier != null && !hasWhatsAppPhone(supplier.whatsapp));
			phoneLabel.setText(supplier == null
					? AppLocal.getIntString("Replenishment.OneSupplierRequired")
					: AppLocal.getIntString("Replenishment.ToPhone") + " "
							+ (!hasWhatsAppPhone(supplier.whatsapp)
									? AppLocal.getIntString("Replenishment.MissingWhatsApp")
									: formatWhatsApp(supplier.whatsapp)));
			confirm.setText(channelButtons.get("TEXT").isSelected()
					? AppLocal.getIntString("Replenishment.SaveTextFile")
					: whatsapp
							? AppLocal.getIntString("Replenishment.OpenWhatsApp")
							: AppLocal.getIntString("Replenishment.Print"));
			confirm.setEnabled(!scope.isEmpty() && (!whatsapp || phone.length() >= 8 && phone.length() <= 15));
		};
		Runnable scopeChanged = () -> {
			message.setText(ReplenishmentOrderText.format(selectedScope.isSelected() ? chosen : all));
			message.setCaretPosition(0);
			refresh.run();
		};
		for (JToggleButton button : channelButtons.values())
			button.addActionListener(e -> refresh.run());
		allScope.addActionListener(e -> scopeChanged.run());
		selectedScope.addActionListener(e -> scopeChanged.run());
		cancel.addActionListener(e -> dialog.dispose());
		confirm.addActionListener(e -> {
			List<ReplenishmentEntry> scope = selectedScope.isSelected() ? chosen : all;
			if (channelButtons.get("WHATSAPP").isSelected()) {
				ReplenishmentSupplier supplier = singleSupplier(scope);
				if (supplier != null && sendWhatsApp(scope, supplier, message.getText()))
					dialog.dispose();
			} else if (channelButtons.get("TEXT").isSelected()) {
				if (exportVisibleEntries(scope, message.getText()))
					dialog.dispose();
			} else {
				try {
					if (new JTextArea(message.getText()).print())
						dialog.dispose();
				} catch (java.awt.print.PrinterException ex) {
					JOptionPane.showMessageDialog(dialog, ex.getMessage());
				}
			}
		});
		scopeChanged.run();
		dialog.pack();
		dialog.setLocationRelativeTo(this);
		return dialog;
	}
	private ReplenishmentSupplier singleSupplier(List<ReplenishmentEntry> scope) {
		if (scope.isEmpty() || scope.get(0).supplierId == null)
			return null;
		String id = scope.get(0).supplierId;
		if (scope.stream().anyMatch(entry -> !id.equals(entry.supplierId)))
			return null;
		for (int i = 0; i < supplierFilter.getItemCount(); i++)
			if (id.equals(supplierFilter.getItemAt(i).id))
				return supplierFilter.getItemAt(i);
		return null;
	}
	private boolean exportVisibleEntries(List<ReplenishmentEntry> exported, String text) {
		JFileChooser chooser = new JFileChooser();
		chooser.setSelectedFile(new File(String.format(AppLocal.getIntString("Replenishment.ExportFileName"),
				new SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()))));
		if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
			return false;
		File target = chooser.getSelectedFile();
		if (target.exists() && JOptionPane.showConfirmDialog(this,
				AppLocal.getIntString("Replenishment.ConfirmOverwrite"), AppLocal.getIntString("Replenishment.Export"),
				JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
			return false;
		try {
			ReplenishmentSharing.saveTextFile(data, exported, app.getAppUserView().getUser().getId(), target.toPath(),
					text);
			load(search.getText(), false, editingEntry == null ? null : editingEntry.id);
			return true;
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, ex.getMessage());
			return false;
		}
	}
	private boolean sendWhatsApp(List<ReplenishmentEntry> scope, ReplenishmentSupplier supplier, String text) {
		String phone = valueOrEmpty(supplier.whatsapp).replaceAll("\\D", "");
		if (phone.length() < 8 || phone.length() > 15)
			return false;
		try {
			URI link = URI
					.create("https://wa.me/" + phone + "?text=" + URLEncoder.encode(text, StandardCharsets.UTF_8));
			if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
				Desktop.getDesktop().browse(link);
			else {
				JTextField url = new JTextField(link.toString());
				url.setEditable(false);
				JOptionPane.showMessageDialog(this, url, AppLocal.getIntString("Replenishment.SendWhatsApp"),
						JOptionPane.INFORMATION_MESSAGE);
			}
			JDialog confirmation = createWhatsAppConfirmationDialog(supplier, scope);
			confirmation.setVisible(true);
			boolean confirmed = Boolean.TRUE
					.equals(confirmation.getRootPane().getClientProperty("replenishment.confirmed"));
			confirmation.dispose();
			ReplenishmentSharing.afterWhatsApp(data, scope, app.getAppUserView().getUser().getId(), confirmed);
			if (confirmed) {
				load(search.getText(), false, editingEntry == null ? null : editingEntry.id);
			}
			return true;
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, ex.getMessage());
			return false;
		}
	}
	private JDialog createWhatsAppConfirmationDialog(ReplenishmentSupplier supplier, List<ReplenishmentEntry> scope) {
		JDialog dialog = new JDialog(javax.swing.SwingUtilities.getWindowAncestor(this),
				AppLocal.getIntString("Replenishment.SendWhatsApp"), java.awt.Dialog.ModalityType.APPLICATION_MODAL);
		JPanel content = new JPanel(new BorderLayout(0, 16));
		content.setBackground(RetailPOSColors.surface100());
		content.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		JLabel title = new JLabel(String.format(AppLocal.getIntString("Replenishment.ConfirmSentTo"), supplier.name));
		title.setFont(RetailPOSTheme.MANROPE_BOLD.deriveFont(20f));
		content.add(title, BorderLayout.NORTH);
		long pending = scope.stream().filter(entry -> "PENDING".equals(entry.status)).count();
		JTextArea explanation = hintArea();
		explanation.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(15f));
		explanation.setForeground(RetailPOSColors.ink());
		explanation.setText(String.format(AppLocal.getIntString("Replenishment.ConfirmSentExplanation"), pending));
		explanation.setColumns(40);
		content.add(explanation, BorderLayout.CENTER);
		JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
		actions.setOpaque(false);
		JButton notYet = touchButton(AppLocal.getIntString("Replenishment.NotYet"), 48);
		JButton yes = touchButton(AppLocal.getIntString("Replenishment.YesMarkOrdered"), 48);
		RetailPOSColors.primaryButton(yes);
		for (JButton button : new JButton[]{notYet, yes}) {
			button.setMargin(new Insets(8, 20, 8, 20));
			RetailPOSTheme.touchHeight(button, 48);
			actions.add(button);
		}
		notYet.addActionListener(event -> dialog.dispose());
		yes.addActionListener(event -> {
			dialog.getRootPane().putClientProperty("replenishment.confirmed", true);
			dialog.dispose();
		});
		content.add(actions, BorderLayout.SOUTH);
		dialog.setContentPane(content);
		dialog.getRootPane().setDefaultButton(yes);
		dialog.pack();
		dialog.setLocationRelativeTo(this);
		return dialog;
	}
	private static ReplenishmentSupplier filterChoice(String id, String name) {
		ReplenishmentSupplier choice = new ReplenishmentSupplier();
		choice.id = id;
		choice.name = name;
		return choice;
	}
	private void refreshSupplierFilter() throws BasicException {
		refreshingSuppliers = true;
		try {
			supplierFilter.removeAllItems();
			supplierFilter.addItem(filterChoice("ALL", AppLocal.getIntString("Replenishment.AllSuppliers")));
			supplierFilter.addItem(filterChoice("NONE", AppLocal.getIntString("Replenishment.NoSupplier")));
			for (ReplenishmentSupplier supplier : data.suppliers())
				supplierFilter.addItem(supplier);
			for (int i = 0; i < supplierFilter.getItemCount(); i++)
				if (supplierFilter.getItemAt(i).id.equals(selectedSupplier)) {
					supplierFilter.setSelectedIndex(i);
					return;
				}
			selectedSupplier = "ALL";
			supplierFilter.setSelectedIndex(0);
		} finally {
			refreshingSuppliers = false;
		}
	}
	private void manageSuppliers() {
		manageSuppliers(null);
	}
	private void manageSuppliers(String supplierId) {
		manageSuppliers(supplierId, null);
	}
	private void manageSuppliers(String supplierId, java.util.function.Consumer<ReplenishmentSupplier> onCreated) {
		if (!administrationMode)
			return;
		showOverlayDialog(createSupplierFormDialog(supplierId, onCreated));
	}
	private JDialog createSupplierFormDialog(String supplierId,
			java.util.function.Consumer<ReplenishmentSupplier> onCreated) {
		if (!administrationMode)
			return null;
		try {
			ReplenishmentSupplier existing = null;
			if (supplierId != null)
				for (ReplenishmentSupplier candidate : data.suppliers())
					if (supplierId.equals(candidate.id)) {
						existing = candidate;
						break;
					}
			if (supplierId != null && existing == null)
				return null;
			boolean editing = existing != null;
			JDialog dialog = new JDialog(javax.swing.SwingUtilities.getWindowAncestor(this),
					AppLocal.getIntString(editing ? "Replenishment.EditSupplier" : "Replenishment.NewSupplier"),
					java.awt.Dialog.ModalityType.APPLICATION_MODAL);
			JPanel content = modalSurface(new BorderLayout(12, 12));
			JLabel heading = new JLabel(dialog.getTitle());
			heading.setFont(RetailPOSTheme.MANROPE_BOLD.deriveFont(20f));
			content.add(heading, BorderLayout.NORTH);
			JTextField name = touchField(22, 44);
			JTextField first = touchField(22, 44);
			JTextField last = touchField(22, 44);
			JTextField phone = touchField(22, 44);
			phone.setFont(RetailPOSTheme.PLEX_MONO_REGULAR.deriveFont(13f));
			JTextField email = touchField(22, 44);
			JPanel fields = new JPanel(new GridBagLayout());
			fields.setOpaque(false);
			GridBagConstraints cell = new GridBagConstraints();
			cell.gridx = 0;
			cell.weightx = 1;
			cell.fill = GridBagConstraints.HORIZONTAL;
			cell.anchor = GridBagConstraints.LINE_START;
			cell.insets = new Insets(0, 0, 4, 0);
			int row = 0;
			for (Object[] field : new Object[][]{{"Replenishment.SupplierName", name},
					{"Replenishment.ContactFirstName", first}, {"Replenishment.ContactLastName", last},
					{"Replenishment.WhatsAppNumber", phone}, {"Replenishment.SupplierEmail", email}}) {
				cell.gridy = row++;
				fields.add(sectionLabel((String) field[0]), cell);
				JTextField input = (JTextField) field[1];
				styleInput(input);
				input.setBackground(RetailPOSColors.surface100());
				cell.gridy = row++;
				fields.add(input, cell);
				if (input == phone) {
					JTextArea hint = hintArea();
					hint.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f));
					hint.setText(AppLocal.getIntString("Replenishment.SupplierPhoneHint"));
					cell.gridy = row++;
					fields.add(hint, cell);
				}
			}
			content.add(fields, BorderLayout.CENTER);
			JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
			actions.setOpaque(false);
			JButton cancel = new JButton(AppLocal.getIntString("Button.Cancel"));
			JButton saveSupplier = new JButton(AppLocal.getIntString("Replenishment.Save"));
			RetailPOSColors.primaryButton(saveSupplier);
			enableEnter(saveSupplier);
			for (JButton button : new JButton[]{cancel, saveSupplier}) {
				button.setMargin(new Insets(4, 10, 4, 10));
				RetailPOSTheme.touchHeight(button, 44);
				actions.add(button);
			}
			content.add(actions, BorderLayout.SOUTH);
			cancel.addActionListener(e -> dialog.dispose());
			if (editing) {
				name.setText(valueOrEmpty(existing.name));
				first.setText(valueOrEmpty(existing.contactFirstName));
				last.setText(valueOrEmpty(existing.contactLastName));
				phone.setText(formatWhatsApp(existing.whatsapp));
				email.setText(valueOrEmpty(existing.email));
			} else
				phone.setText("+34 ");
			ReplenishmentSupplier original = existing;
			saveSupplier.addActionListener(e -> {
				if (name.getText().isBlank() || first.getText().isBlank() || last.getText().isBlank()) {
					JOptionPane.showMessageDialog(dialog,
							AppLocal.getIntString("Replenishment.SupplierRequiredFields"));
					return;
				}
				ReplenishmentSupplier item = new ReplenishmentSupplier();
				item.id = original == null ? null : original.id;
				item.name = name.getText();
				item.contactFirstName = first.getText();
				item.contactLastName = last.getText();
				item.whatsapp = phone.getText().replaceAll("\\D", "").length() <= 2 ? null : phone.getText();
				item.email = email.getText();
				try {
					data.saveSupplier(item);
					refreshSupplierFilter();
					if (onCreated != null && original == null)
						onCreated.accept(item);
					else
						load(search.getText(), false);
					dialog.dispose();
				} catch (BasicException error) {
					JOptionPane.showMessageDialog(dialog, error.getMessage());
				}
			});
			styleOverlayDialog(dialog, content);
			dialog.getRootPane().setDefaultButton(saveSupplier);
			dialog.pack();
			dialog.setLocationRelativeTo(this);
			return dialog;
		} catch (BasicException ex) {
			JOptionPane.showMessageDialog(this, ex.getMessage());
			return null;
		}
	}
	private void assignPendingSelected(String supplierId) {
		if (!administrationMode)
			return;
		List<String> ids = ReplenishmentOrderText.selectedPendingIds(selectedEntries());
		if (ids.isEmpty()) {
			JOptionPane.showMessageDialog(this, AppLocal.getIntString("Replenishment.SelectPendingToAssign"));
			return;
		}
		if (editingEntry != null && detailSave.isEnabled()) {
			JOptionPane.showMessageDialog(this, AppLocal.getIntString("Replenishment.UnsavedBulkEdits"));
			return;
		}
		try {
			data.assignSupplier(ids, supplierId, app.getAppUserView().getUser().getId());
			load(search.getText(), false);
		} catch (BasicException ex) {
			JOptionPane.showMessageDialog(this, ex.getMessage());
		}
	}
	private UserInfo selectedSeller() {
		try {
			TicketsEditor editor = app.getBean(JPanelTicketSales.class);
			UserInfo seller = ((com.openbravo.pos.sales.JPanelTicket) editor).getSelectedSeller();
			if (seller != null)
				return seller;
		} catch (BeanFactoryException ignored) {
		}
		UserInfo fallback = app.getAppUserView().getUser().getTicketUserInfo();
		return fallback == null ? app.getAppUserView().getUser().getUserInfo() : fallback;
	}
	private String selectedSellerName() {
		UserInfo seller = selectedSeller();
		return seller == null ? "" : valueOrEmpty(seller.getName());
	}
	private static void styleInput(JTextComponent input) {
		input.setBackground(RetailPOSColors.surface200());
		input.setForeground(RetailPOSColors.ink());
		if (input instanceof JTextField) {
			input.putClientProperty("FlatLaf.style",
					"arc: 6; borderColor: " + RetailPOSColors.toHex(RetailPOSColors.border()) + "; focusedBorderColor: "
							+ RetailPOSColors.toHex(RetailPOSColors.borderStrong()));
		}
	}
	private static JTextField touchField(int columns, int minimumHeight) {
		return new JTextField(columns) {
			@Override
			public Dimension getPreferredSize() {
				Dimension size = super.getPreferredSize();
				size.height = Math.max(size.height, com.formdev.flatlaf.util.UIScale.scale(minimumHeight));
				return size;
			}
			@Override
			public Dimension getMinimumSize() {
				Dimension size = super.getMinimumSize();
				size.height = Math.max(size.height, com.formdev.flatlaf.util.UIScale.scale(minimumHeight));
				return size;
			}
		};
	}
	private static JButton touchButton(String text, int minimumHeight) {
		return new JButton(text) {
			@Override
			public Dimension getPreferredSize() {
				Dimension size = super.getPreferredSize();
				size.height = Math.max(size.height, com.formdev.flatlaf.util.UIScale.scale(minimumHeight));
				return size;
			}
			@Override
			public Dimension getMinimumSize() {
				Dimension size = super.getMinimumSize();
				size.height = Math.max(size.height, com.formdev.flatlaf.util.UIScale.scale(minimumHeight));
				return size;
			}
		};
	}
	private static javax.swing.Icon paddedSearchIcon() {
		javax.swing.Icon icon = PosIcons.search();
		return new javax.swing.Icon() {
			@Override
			public int getIconWidth() {
				return icon.getIconWidth() + 10;
			}
			@Override
			public int getIconHeight() {
				return icon.getIconHeight();
			}
			@Override
			public void paintIcon(Component component, java.awt.Graphics graphics, int x, int y) {
				icon.paintIcon(component, graphics, x + 10, y);
			}
		};
	}
	private static void enableEnter(JButton button) {
		button.getInputMap(javax.swing.JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ENTER"), "activate");
		button.getActionMap().put("activate", new AbstractAction() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent event) {
				if (button.isEnabled())
					button.doClick();
			}
		});
	}
	private static void styleAreaScroll(JScrollPane scroll, JTextArea area) {
		scroll.setBorder(BorderFactory.createLineBorder(RetailPOSColors.border()));
		area.addFocusListener(new java.awt.event.FocusAdapter() {
			@Override
			public void focusGained(java.awt.event.FocusEvent event) {
				scroll.setBorder(BorderFactory.createLineBorder(RetailPOSColors.borderStrong()));
			}
			@Override
			public void focusLost(java.awt.event.FocusEvent event) {
				scroll.setBorder(BorderFactory.createLineBorder(RetailPOSColors.border()));
			}
		});
	}
	private static void styleStatusToggle(JToggleButton button) {
		button.putClientProperty("FlatLaf.style",
				"selectedBackground: " + RetailPOSColors.toHex(RetailPOSColors.surface200()) + "; selectedForeground: "
						+ RetailPOSColors.toHex(RetailPOSColors.ink()) + "; selectedBorderColor: "
						+ RetailPOSColors.toHex(RetailPOSColors.borderStrong()));
	}
	private static void styleFilterToggle(JToggleButton button) {
		button.putClientProperty("FlatLaf.style",
				"selectedBackground: " + RetailPOSColors.toHex(RetailPOSColors.surface200()) + "; selectedForeground: "
						+ RetailPOSColors.toHex(RetailPOSColors.ink()) + "; selectedBorderColor: "
						+ RetailPOSColors.toHex(RetailPOSColors.borderStrong()));
	}
	private void styleSegmentToggle(JToggleButton button) {
		button.setContentAreaFilled(false);
		button.setRolloverEnabled(true);
		button.setOpaque(false);
		button.setForeground(RetailPOSColors.ink());
		button.putClientProperty("FlatLaf.style",
				"background: " + RetailPOSColors.toHex(RetailPOSColors.surface100()) + "; hoverBackground: "
						+ RetailPOSColors.toHex(RetailPOSColors.surface200()) + "; focusedBorderColor: "
						+ RetailPOSColors.toHex(RetailPOSColors.borderStrong()));
		button.addFocusListener(new java.awt.event.FocusAdapter() {
			@Override
			public void focusGained(java.awt.event.FocusEvent event) {
				updateStatusSegmentStyles();
			}
			@Override
			public void focusLost(java.awt.event.FocusEvent event) {
				updateStatusSegmentStyles();
			}
		});
		updateSegmentStyle(button);
	}
	private void updateStatusSegmentStyles() {
		for (JToggleButton button : statusButtons.values())
			updateSegmentStyle(button);
	}
	private static void updateSegmentStyle(JToggleButton button) {
		boolean active = button.isSelected();
		button.setFont((active ? RetailPOSTheme.MANROPE_BOLD : RetailPOSTheme.MANROPE_SEMIBOLD).deriveFont(15f));
		button.setBorderPainted(false);
		button.setBorder(BorderFactory.createEmptyBorder(com.formdev.flatlaf.util.UIScale.scale(7),
				com.formdev.flatlaf.util.UIScale.scale(16), com.formdev.flatlaf.util.UIScale.scale(7),
				com.formdev.flatlaf.util.UIScale.scale(16)));
		RetailPOSTheme.touchHeight(button, 40);
		button.repaint();
	}
	private static int widestDigit(java.awt.FontMetrics metrics) {
		int widest = 0;
		for (char digit = '0'; digit <= '9'; digit++)
			widest = Math.max(widest, metrics.charWidth(digit));
		return widest;
	}
	private static int tabularDateWidth(java.awt.FontMetrics metrics, String date) {
		int width = 0;
		int digitWidth = widestDigit(metrics);
		for (char character : date.toCharArray())
			width += Character.isDigit(character) ? digitWidth : metrics.charWidth(character);
		return width;
	}
	private static JTextArea hintArea() {
		JTextArea hint = new JTextArea();
		hint.setEditable(false);
		hint.setFocusable(false);
		hint.setLineWrap(true);
		hint.setWrapStyleWord(true);
		hint.setRows(2);
		hint.setColumns(1);
		hint.setMargin(new Insets(0, 0, 0, 0));
		hint.setBorder(BorderFactory.createEmptyBorder());
		hint.setOpaque(false);
		hint.setForeground(RetailPOSColors.inkMuted());
		return hint;
	}
	private JPanel createForm() {
		JPanel form = new JPanel(new BorderLayout(5, 5));
		form.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
		// Two status buttons per row fit translated labels without a horizontal
		// scrollbar.
		JPanel fields = new FormFields();
		GridBagConstraints outer = new GridBagConstraints();
		outer.gridx = 0;
		outer.weightx = 1;
		outer.fill = GridBagConstraints.HORIZONTAL;
		outer.anchor = GridBagConstraints.NORTHWEST;
		outer.insets = new Insets(4, 4, 4, 4);
		JPanel productGroup = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(2, 4, 2, 4);
		formCode = touchField(0, 48);
		styleInput(formCode);
		formCode.setFont(RetailPOSTheme.PLEX_MONO_REGULAR.deriveFont(13f));
		formDescription = new JTextArea(3, 20);
		styleInput(formDescription);
		((JTextArea) formDescription).setRows(3);
		((JTextArea) formDescription).setLineWrap(true);
		((JTextArea) formDescription).setWrapStyleWord(true);
		c.gridy = 0;
		scanLabel = new JLabel(AppLocal.getIntString("Replenishment.Scan"));
		productGroup.add(scanLabel, c);
		c.gridy = 1;
		productGroup.add(formCode, c);
		RetailPOSTheme.touchHeight(formCode, 48);
		productButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		findProduct = new JButton(AppLocal.getIntString("Replenishment.SearchProduct"));
		findProduct.setMargin(new Insets(6, 10, 6, 10));
		productButtons.add(findProduct);
		c.gridy = 2;
		productGroup.add(productButtons, c);
		selectedProduct = new JTextArea(AppLocal.getIntString("Replenishment.NoProductSelected"));
		selectedProduct.setEditable(false);
		selectedProduct.setFocusable(false);
		selectedProduct.setLineWrap(true);
		selectedProduct.setWrapStyleWord(true);
		selectedProduct.setOpaque(false);
		selectedProduct.setColumns(1);
		c.gridy = 3;
		productGroup.add(selectedProduct, c);
		productCodes = new JLabel();
		productCodes.setForeground(RetailPOSColors.inkMuted());
		productCodes.setFont(RetailPOSTheme.PLEX_MONO_REGULAR.deriveFont(13f));
		c.gridy = 4;
		productGroup.add(productCodes, c);
		withoutCodeLabel = new JLabel(AppLocal.getIntString("Replenishment.WithoutCode"), JLabel.CENTER);
		withoutCodeLabel.setForeground(RetailPOSColors.inkMuted());
		withoutCodeLabel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(1, 0, 0, 0, RetailPOSColors.border()),
				BorderFactory.createEmptyBorder(8, 0, 8, 0)));
		c.gridy = 5;
		productGroup.add(withoutCodeLabel, c);
		manualDescriptionLabel = new JLabel(AppLocal.getIntString("Replenishment.Description"));
		c.gridy = 6;
		productGroup.add(manualDescriptionLabel, c);
		manualDescriptionScroll = new JScrollPane((JTextArea) formDescription);
		styleAreaScroll(manualDescriptionScroll, (JTextArea) formDescription);
		manualDescriptionScroll.setMinimumSize(new java.awt.Dimension(0, 58));
		manualDescriptionScroll.setPreferredSize(new java.awt.Dimension(0, 64));
		c.gridy = 7;
		productGroup.add(manualDescriptionScroll, c);
		JPanel customerGroup = new JPanel(new GridBagLayout()) {
			private boolean stacked;
			@Override
			public void doLayout() {
				if (getWidth() > 0) {
					boolean shouldStack = selectedCustomer.getParent().getPreferredSize().width
							+ customerButton.getPreferredSize().width + 6 > getWidth();
					if (stacked != shouldStack) {
						stacked = shouldStack;
						GridBagLayout layout = (GridBagLayout) getLayout();
						GridBagConstraints cell = layout.getConstraints(customerButton);
						cell.gridx = stacked ? 0 : 1;
						cell.gridy = stacked ? 1 : 0;
						cell.weightx = stacked ? 1 : 0;
						cell.anchor = stacked ? GridBagConstraints.LINE_END : GridBagConstraints.CENTER;
						cell.insets = stacked ? new Insets(4, 0, 0, 0) : new Insets(0, 6, 0, 0);
						layout.setConstraints(customerButton, cell);
						revalidate();
					}
				}
				super.doLayout();
			}
		};
		selectedCustomer = new JLabel();
		selectedCustomerPhone = customerPhoneLabel();
		JPanel customerValue = customerValuePanel(selectedCustomer, selectedCustomerPhone);
		setFormCustomerText(AppLocal.getIntString("Replenishment.NoCustomer"));
		selectedCustomer.setVerticalAlignment(JLabel.CENTER);
		customerButton = new JButton(AppLocal.getIntString("Replenishment.Customer"));
		customerButton.setMargin(new Insets(6, 10, 6, 10));
		GridBagConstraints customerCell = new GridBagConstraints();
		customerCell.weightx = 1;
		customerCell.fill = GridBagConstraints.HORIZONTAL;
		customerCell.anchor = GridBagConstraints.CENTER;
		customerGroup.add(customerValue, customerCell);
		customerCell.gridx = 1;
		customerCell.weightx = 0;
		customerCell.fill = GridBagConstraints.NONE;
		customerCell.insets = new Insets(0, 6, 0, 0);
		customerGroup.add(customerButton, customerCell);
		formNoteArea = new JTextArea(4, 20);
		styleInput(formNoteArea);
		formNoteArea.setLineWrap(true);
		formNoteArea.setWrapStyleWord(true);
		JScrollPane noteScroll = new JScrollPane(formNoteArea) {
			private int contentHeight() {
				return formNoteArea.getPreferredSize().height + getInsets().top + getInsets().bottom;
			}
			@Override
			public Dimension getPreferredSize() {
				Dimension size = super.getPreferredSize();
				size.height = Math.max(size.height, contentHeight());
				return size;
			}
			@Override
			public Dimension getMinimumSize() {
				Dimension size = super.getMinimumSize();
				size.height = Math.max(size.height, contentHeight());
				return size;
			}
		};
		styleAreaScroll(noteScroll, formNoteArea);
		JPanel noteGroup = new JPanel(new BorderLayout());
		noteGroup.add(noteScroll, BorderLayout.CENTER);
		formStatusLabel = sectionLabel("Replenishment.Status");
		formStatusButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		ButtonGroup statusGroup = new ButtonGroup();
		formPending = new JToggleButton(AppLocal.getIntString("Replenishment.FormPending"));
		formOrdered = new JToggleButton(AppLocal.getIntString("Replenishment.FormOrdered"));
		formReceived = new JToggleButton(AppLocal.getIntString("Replenishment.FormReceived"));
		formDelivered = new JToggleButton(AppLocal.getIntString("Replenishment.FormDelivered"));
		for (JToggleButton button : new JToggleButton[]{formPending, formOrdered, formReceived, formDelivered})
			styleStatusToggle(button);
		formPending.setMargin(new Insets(6, 6, 6, 6));
		formOrdered.setMargin(new Insets(6, 6, 6, 6));
		formReceived.setMargin(new Insets(6, 6, 6, 6));
		formDelivered.setMargin(new Insets(6, 6, 6, 6));
		formPending.setSelected(true);
		formDelivered.setEnabled(false);
		formDelivered.setToolTipText(AppLocal.getIntString("Replenishment.DeliveryHint"));
		selectFormStatus();
		statusGroup.add(formPending);
		statusGroup.add(formOrdered);
		statusGroup.add(formReceived);
		statusGroup.add(formDelivered);
		formStatusButtons.add(formPending);
		formStatusButtons.add(formOrdered);
		formStatusButtons.add(formReceived);
		formDeliveryHint = hintArea();
		formDeliveryHint.setText(AppLocal.getIntString("Replenishment.DeliveryHint"));
		formDeliveryHint.setVisible(false);
		formPending.addActionListener(e -> setFormStatus("PENDING"));
		formOrdered.addActionListener(e -> setFormStatus("ORDERED"));
		formReceived.addActionListener(e -> setFormStatus("RECEIVED"));
		formDelivered.addActionListener(e -> setFormStatus("DELIVERED"));
		JPanel customerSection = new JPanel(new BorderLayout(0, 2));
		customerSection.add(sectionLabel("Replenishment.CustomerColumn"), BorderLayout.NORTH);
		customerSection.add(customerGroup, BorderLayout.CENTER);
		JPanel noteSection = new JPanel(new BorderLayout(0, 2));
		noteSection.add(sectionLabel("Replenishment.Note"), BorderLayout.NORTH);
		noteSection.add(noteGroup, BorderLayout.CENTER);
		JPanel statusSection = new JPanel(new BorderLayout(0, 2));
		statusSection.add(formStatusLabel, BorderLayout.NORTH);
		JPanel statusControls = new JPanel(new BorderLayout(0, 4));
		statusControls.add(formStatusButtons, BorderLayout.NORTH);
		statusControls.add(formDeliveryHint, BorderLayout.CENTER);
		statusSection.add(statusControls, BorderLayout.CENTER);
		creatorLabel = new JLabel(AppLocal.getIntString("Replenishment.CreatedBy") + ": " + selectedSellerName());
		formSupplier = new JLabel();
		outer.gridy = 0;
		JLabel newTitle = sectionLabel("Replenishment.NewRequest");
		newTitle.setFont(RetailPOSTheme.MANROPE_BOLD.deriveFont(20f));
		fields.add(newTitle, outer);
		outer.gridy = 1;
		fields.add(productGroup, outer);
		outer.gridy = 2;
		fields.add(customerSection, outer);
		outer.gridy = 3;
		outer.weighty = 0;
		outer.fill = GridBagConstraints.HORIZONTAL;
		fields.add(noteSection, outer);
		outer.gridy = 4;
		outer.weighty = 0;
		outer.fill = GridBagConstraints.HORIZONTAL;
		fields.add(statusSection, outer);
		outer.gridy = 5;
		fields.add(formSupplier, outer);
		outer.gridy = 6;
		fields.add(creatorLabel, outer);
		outer.gridy = 7;
		outer.weighty = 1;
		outer.fill = GridBagConstraints.VERTICAL;
		fields.add(Box.createVerticalGlue(), outer);
		save = new JButton(AppLocal.getIntString("Replenishment.Save"));
		RetailPOSColors.primaryButton(save);
		enableEnter(save);
		save.setMargin(new Insets(6, 24, 6, 24));
		RetailPOSTheme.touchHeight(save, 48);
		cancelNewRequest = new JButton(AppLocal.getIntString("Button.Cancel"));
		cancelNewRequest.setMargin(new Insets(6, 12, 6, 12));
		JPanel formActions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		formActions.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
		formActions.add(cancelNewRequest);
		formActions.add(save);
		JScrollPane fieldsScroll = new JScrollPane(fields);
		fieldsScroll.setBorder(BorderFactory.createEmptyBorder());
		fieldsScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		fieldsScroll.getVerticalScrollBar().setUnitIncrement(16);
		form.add(fieldsScroll, BorderLayout.CENTER);
		form.add(formActions, BorderLayout.SOUTH);
		formCode.addActionListener(e -> findFormProduct());
		DocumentListener draftListener = new DocumentListener() {
			public void insertUpdate(DocumentEvent event) {
				updateSaveState();
			}
			public void removeUpdate(DocumentEvent event) {
				updateSaveState();
			}
			public void changedUpdate(DocumentEvent event) {
				updateSaveState();
			}
		};
		formCode.getDocument().addDocumentListener(draftListener);
		formDescription.getDocument().addDocumentListener(draftListener);
		formNoteArea.getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) {
				updateSaveState();
			}
			public void removeUpdate(DocumentEvent e) {
				updateSaveState();
			}
			public void changedUpdate(DocumentEvent e) {
				updateSaveState();
			}
		});
		findProduct.addActionListener(e -> {
			if (editingEntry != null)
				return;
			formProduct = JProductFinder.showMessage(this, sales);
			showSelectedProduct();
		});
		customerButton.addActionListener(e -> chooseFormCustomer());
		save.addActionListener(e -> saveForm());
		cancelNewRequest.addActionListener(e -> clearForm());
		updateFormDeliveredEnabled();
		updateSaveState();
		return form;
	}
	private JPanel createSelectionPanel() {
		JPanel panel = new JPanel(new BorderLayout(8, 8));
		panel.setBorder(BorderFactory.createEmptyBorder(16, 12, 16, 12));
		JPanel introduction = new JPanel();
		introduction.setLayout(new BoxLayout(introduction, BoxLayout.Y_AXIS));
		JLabel eyebrow = sectionLabel("Replenishment.SelectionEyebrow");
		eyebrow.setForeground(RetailPOSColors.inkMuted());
		introduction.add(eyebrow);
		introduction.add(Box.createVerticalStrut(4));
		selectionSummary = sectionLabel("Replenishment.SelectedCount");
		selectionSummary.setFont(RetailPOSTheme.MANROPE_BOLD.deriveFont(20f));
		introduction.add(selectionSummary);
		introduction.add(Box.createVerticalStrut(8));
		JTextArea selectionHint = hintArea();
		selectionHint.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f));
		selectionHint.setText(AppLocal.getIntString("Replenishment.SelectionHint"));
		introduction.add(selectionHint);
		panel.add(introduction, BorderLayout.NORTH);
		selectionSuppliers = new JPanel();
		selectionSuppliers.setLayout(new BoxLayout(selectionSuppliers, BoxLayout.Y_AXIS));
		JScrollPane scroll = new JScrollPane(selectionSuppliers);
		scroll.setBorder(BorderFactory.createEmptyBorder());
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		JPanel assignments = new JPanel(new BorderLayout(0, 8));
		assignments.add(sectionLabel("Replenishment.AssignToEyebrow"), BorderLayout.NORTH);
		assignments.add(scroll, BorderLayout.CENTER);
		panel.add(assignments, BorderLayout.CENTER);
		JPanel bottom = new JPanel(new BorderLayout(0, 8));
		bottom.add(sectionLabel("Replenishment.OtherActions"), BorderLayout.NORTH);
		JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
		JButton markOrdered = new JButton(AppLocal.getIntString("Replenishment.MarkOrdered"));
		JButton delete = new JButton(AppLocal.getIntString("Replenishment.Delete"));
		delete.setIcon(PosIcons.trash());
		styleDangerGhost(delete);
		for (JButton button : new JButton[]{markOrdered, delete}) {
			button.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f));
			RetailPOSTheme.touchHeight(button, 44);
			actions.add(button);
		}
		markOrdered.addActionListener(e -> markSelectedPending());
		delete.addActionListener(e -> deleteSelected());
		bottom.add(actions, BorderLayout.CENTER);
		panel.add(bottom, BorderLayout.SOUTH);
		return panel;
	}
	private JPanel createDetail() {
		JPanel detail = new JPanel(new BorderLayout(8, 8));
		detail.setBorder(BorderFactory.createEmptyBorder(16, 12, 16, 12));
		closeDetail = new JButton("×");
		closeDetail.setName("replenishment.closeDetail");
		styleGhost(closeDetail);
		closeDetail.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(20f));
		closeDetail.setToolTipText(AppLocal.getIntString("Button.Close"));
		closeDetail.getAccessibleContext().setAccessibleName(AppLocal.getIntString("Button.Close"));
		closeDetail.addActionListener(e -> clearSelectedRows());
		JPanel closeRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		closeRow.add(closeDetail);
		detail.add(closeRow, BorderLayout.NORTH);
		JPanel contents = new JPanel();
		contents.setLayout(new BoxLayout(contents, BoxLayout.Y_AXIS));
		detailReference = new JLabel();
		detailReference.setText(" ");
		detailReference.setFont(RetailPOSTheme.PLEX_MONO_REGULAR.deriveFont(13f));
		detailReference.setForeground(RetailPOSColors.inkMuted());
		detailProduct = new JTextArea(1, 1);
		detailProduct.setFont(RetailPOSTheme.MANROPE_BOLD.deriveFont(20f));
		detailProduct.setMargin(new Insets(0, 0, 0, 0));
		detailProduct.setBorder(BorderFactory.createEmptyBorder());
		detailProduct.setEditable(false);
		detailProduct.setFocusable(false);
		detailProduct.setLineWrap(true);
		detailProduct.setWrapStyleWord(true);
		detailProduct.setOpaque(false);
		detailCreated = new JLabel();
		detailCreated.setText(" ");
		detailCreated.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f));
		detailCreated.setForeground(RetailPOSColors.inkMuted());
		detailStatus = new JLabel() {
			@Override
			protected void paintComponent(java.awt.Graphics graphics) {
				java.awt.Graphics2D g = (java.awt.Graphics2D) graphics.create();
				g.setColor(RetailPOSColors.surface200());
				g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
				g.dispose();
				super.paintComponent(graphics);
			}
		};
		detailStatus.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
		detailStatus.setForeground(RetailPOSColors.ink());
		detailHint = hintArea();
		detailHint.setRows(3);
		detailHint.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f));
		detailHint.setForeground(RetailPOSColors.inkMuted());
		nextStep = new JButton() {
			@Override
			public Dimension getPreferredSize() {
				Dimension size = super.getPreferredSize();
				size.height = Math.max(size.height, com.formdev.flatlaf.util.UIScale.scale(48));
				return size;
			}
			@Override
			public Dimension getMinimumSize() {
				Dimension size = super.getMinimumSize();
				size.height = Math.max(size.height, com.formdev.flatlaf.util.UIScale.scale(48));
				return size;
			}
			@Override
			public Dimension getMaximumSize() {
				return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
			}
		};
		RetailPOSColors.primaryButton(nextStep);
		nextStep.setMargin(new Insets(4, 12, 4, 12));
		enableEnter(nextStep);
		RetailPOSTheme.touchHeight(nextStep, 48);
		nextStep.addActionListener(e -> {
			String next = nextStatus();
			if (next != null)
				applyDetailStep(next);
			else if (editingEntry != null && "RECEIVED".equals(editingEntry.status) && formCustomer == null)
				chooseDetailCustomer();
		});
		JButton changeStatus = new JButton(AppLocal.getIntString("Replenishment.ChangeStatus"), PosIcons.chevronDown());
		styleGhost(changeStatus);
		changeStatus.setHorizontalTextPosition(JButton.LEFT);
		changeStatus.setIconTextGap(4);
		changeStatus.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f));
		RetailPOSTheme.touchHeight(changeStatus, 44);
		changeStatus.addActionListener(e -> showStatusMenu(changeStatus));
		detailSupplierName = new JLabel();
		detailSupplierChange = new JButton(AppLocal.getIntString("Replenishment.Change"));
		detailSupplierChange.addActionListener(e -> showSupplierMenu(detailSupplierChange));
		JPanel supplierLine = detailLine("Replenishment.Supplier", detailSupplierName, detailSupplierChange);
		detailCustomer = new JLabel();
		detailCustomerPhone = customerPhoneLabel();
		detailCustomer.setVerticalAlignment(JLabel.CENTER);
		detailCustomer.setForeground(RetailPOSColors.ink());
		detailCustomerButton = new JButton(AppLocal.getIntString("Replenishment.Change"));
		detailCustomerButton.addActionListener(e -> chooseDetailCustomer());
		JPanel customerLine = detailLine("Replenishment.CustomerColumn",
				customerValuePanel(detailCustomer, detailCustomerPhone), detailCustomerButton);
		JPanel statusLine = new JPanel(new GridBagLayout());
		GridBagConstraints statusCell = new GridBagConstraints();
		statusCell.anchor = GridBagConstraints.LINE_START;
		statusCell.weighty = 0;
		statusLine.add(detailStatus, statusCell);
		statusCell.gridx = 1;
		statusCell.weightx = 1;
		statusCell.fill = GridBagConstraints.HORIZONTAL;
		statusLine.add(Box.createHorizontalGlue(), statusCell);
		statusCell.gridx = 2;
		statusCell.weightx = 0;
		statusCell.fill = GridBagConstraints.NONE;
		statusCell.anchor = GridBagConstraints.CENTER;
		statusCell.insets = new Insets(0, 4, 0, 0);
		statusLine.add(changeStatus, statusCell);
		detailNote = new JTextArea(4, 20);
		styleInput(detailNote);
		detailNote.setLineWrap(true);
		detailNote.setWrapStyleWord(true);
		detailNote.getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) {
				updateDetailSaveState();
			}
			public void removeUpdate(DocumentEvent e) {
				updateDetailSaveState();
			}
			public void changedUpdate(DocumentEvent e) {
				updateDetailSaveState();
			}
		});
		JScrollPane noteScroll = new JScrollPane(detailNote);
		styleAreaScroll(noteScroll, detailNote);
		for (javax.swing.JComponent component : new javax.swing.JComponent[]{detailReference, detailProduct,
				detailCreated, statusLine, nextStep, detailHint, supplierLine, customerLine,
				sectionLabel("Replenishment.Note"), noteScroll}) {
			component.setMaximumSize(new Dimension(Integer.MAX_VALUE, component.getPreferredSize().height));
			component.setAlignmentX(Component.LEFT_ALIGNMENT);
			contents.add(component);
			contents.add(Box.createVerticalStrut(component == detailReference || component == detailProduct ? 4 : 8));
		}
		JScrollPane scroll = new JScrollPane(contents);
		scroll.setBorder(BorderFactory.createEmptyBorder());
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		detail.add(scroll, BorderLayout.CENTER);
		JPanel buttons = new JPanel(new GridBagLayout());
		detailUndo = new JButton(AppLocal.getIntString("Replenishment.Undo"));
		detailSave = new JButton(AppLocal.getIntString("Replenishment.Save"));
		enableEnter(detailSave);
		detailDelete = new JButton(AppLocal.getIntString("Replenishment.Delete"));
		detailDelete.setIcon(PosIcons.trash());
		styleDangerGhost(detailDelete);
		for (JButton button : new JButton[]{detailUndo, detailSave, detailDelete}) {
			button.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f));
			RetailPOSTheme.touchHeight(button, 48);
		}
		GridBagConstraints buttonCell = new GridBagConstraints();
		buttonCell.gridx = 0;
		buttons.add(detailDelete, buttonCell);
		buttonCell.gridx = 1;
		buttonCell.weightx = 1;
		buttonCell.fill = GridBagConstraints.HORIZONTAL;
		buttons.add(Box.createHorizontalGlue(), buttonCell);
		buttonCell.weightx = 0;
		buttonCell.fill = GridBagConstraints.NONE;
		buttonCell.insets = new Insets(0, 4, 0, 0);
		buttonCell.gridx = 2;
		buttons.add(detailUndo, buttonCell);
		buttonCell.gridx = 3;
		buttons.add(detailSave, buttonCell);
		detailUndo.addActionListener(e -> {
			if (editingEntry != null)
				loadEntry(editingEntry);
		});
		detailSave.addActionListener(e -> saveDetail());
		detailDelete.addActionListener(e -> deleteSelected());
		detail.add(buttons, BorderLayout.SOUTH);
		return detail;
	}
	private JPanel detailLine(String key, Component value, JButton change) {
		JPanel line = new JPanel(new BorderLayout(4, 4));
		line.add(sectionLabel(key), BorderLayout.NORTH);
		JPanel inner = new JPanel(new GridBagLayout());
		inner.setBorder(BorderFactory.createLineBorder(RetailPOSColors.border()));
		if (value instanceof javax.swing.JComponent)
			((javax.swing.JComponent) value).setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 0));
		styleGhost(change);
		change.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f));
		RetailPOSTheme.touchHeight(change, 44);
		GridBagConstraints cell = new GridBagConstraints();
		cell.weightx = 1;
		cell.fill = GridBagConstraints.HORIZONTAL;
		cell.anchor = GridBagConstraints.CENTER;
		inner.add(value, cell);
		cell.gridx = 1;
		cell.weightx = 0;
		cell.fill = GridBagConstraints.NONE;
		cell.insets = new Insets(0, 4, 0, 0);
		inner.add(change, cell);
		line.add(inner, BorderLayout.CENTER);
		return line;
	}
	private static JLabel customerPhoneLabel() {
		JLabel label = new JLabel();
		label.setFont(RetailPOSTheme.PLEX_MONO_REGULAR.deriveFont(13f));
		label.setForeground(RetailPOSColors.inkMuted());
		return label;
	}
	private static JPanel customerValuePanel(JLabel name, JLabel phone) {
		JPanel value = new JPanel();
		value.setLayout(new BoxLayout(value, BoxLayout.Y_AXIS));
		value.setOpaque(false);
		name.setAlignmentX(Component.LEFT_ALIGNMENT);
		phone.setAlignmentX(Component.LEFT_ALIGNMENT);
		value.add(name);
		value.add(phone);
		return value;
	}
	private static void showCustomerPhone(JLabel label, String phone) {
		String text = valueOrEmpty(phone).trim();
		label.setText(text);
		label.setToolTipText(text.isEmpty() ? null : text);
		label.setVisible(!text.isEmpty());
		label.getParent().revalidate();
	}
	private void updateSupplierHeading() {
		supplierHeadingName.setText(supplierName(selectedSupplier));
		for (int i = 0; i < supplierFilter.getItemCount(); i++) {
			ReplenishmentSupplier supplier = supplierFilter.getItemAt(i);
			if (supplier.id.equals(selectedSupplier)) {
				boolean hasPhone = hasWhatsAppPhone(supplier.whatsapp);
				supplierContact.setText(supplier.contactFirstName + " " + supplier.contactLastName + " · "
						+ (hasPhone ? AppLocal.getIntString("Replenishment.WhatsAppNumber") : ""));
				supplierPhone.setFont(hasPhone
						? RetailPOSTheme.PLEX_MONO_REGULAR.deriveFont(13f)
						: RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f));
				supplierPhone.setIcon(hasPhone ? null : warningDot());
				supplierPhone.setIconTextGap(4);
				supplierPhone.setText(hasPhone
						? formatWhatsApp(supplier.whatsapp)
						: AppLocal.getIntString("Replenishment.NoWhatsAppShort"));
				return;
			}
		}
	}
	private boolean supplierHasPhone() {
		for (int i = 2; i < supplierFilter.getItemCount(); i++) {
			ReplenishmentSupplier supplier = supplierFilter.getItemAt(i);
			if (supplier.id.equals(selectedSupplier))
				return hasWhatsAppPhone(supplier.whatsapp);
		}
		return false;
	}
	private static String formatWhatsApp(String phone) {
		String digits = valueOrEmpty(phone).replaceAll("\\D", "");
		if (digits.startsWith("34") && digits.length() == 11)
			return "+34 " + digits.substring(2, 5) + " " + digits.substring(5, 7) + " " + digits.substring(7, 9) + " "
					+ digits.substring(9, 11);
		return valueOrEmpty(phone).trim();
	}
	private static boolean hasWhatsAppPhone(String phone) {
		int digits = valueOrEmpty(phone).replaceAll("\\D", "").length();
		return digits >= 8 && digits <= 15;
	}
	private static String pendingCountLabel(long count) {
		return String.format(
				AppLocal.getIntString(count == 1 ? "Replenishment.PendingCountOne" : "Replenishment.PendingCount"),
				count);
	}
	private void refreshSelectionSuppliers() {
		if (selectionSuppliers == null)
			return;
		selectionSuppliers.removeAll();
		List<ReplenishmentEntry> pending;
		try {
			pending = data.list("", "ALL", "PENDING", "ALL");
		} catch (BasicException ex) {
			JOptionPane.showMessageDialog(this, ex.getMessage());
			return;
		}
		for (int i = 2; i < supplierFilter.getItemCount(); i++) {
			ReplenishmentSupplier supplier = supplierFilter.getItemAt(i);
			long count = pending.stream().filter(entry -> supplier.id.equals(entry.supplierId)).count();
			String countText = pendingCountLabel(count);
			JButton assign = new JButton(supplier.name) {
				@Override
				protected void paintComponent(java.awt.Graphics graphics) {
					super.paintComponent(graphics);
					java.awt.Graphics2D g = (java.awt.Graphics2D) graphics.create();
					g.setFont(RetailPOSTheme.PLEX_MONO_REGULAR.deriveFont(13f));
					g.setColor(RetailPOSColors.inkMuted());
					java.awt.FontMetrics metrics = g.getFontMetrics();
					g.drawString(countText, getWidth() - metrics.stringWidth(countText) - 12,
							(getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
					g.dispose();
				}
			};
			assign.setHorizontalAlignment(JButton.LEFT);
			assign.putClientProperty("FlatLaf.style",
					"background: " + RetailPOSColors.toHex(RetailPOSColors.surface100()) + "; hoverBackground: "
							+ RetailPOSColors.toHex(RetailPOSColors.surface200()) + "; focusedBorderColor: "
							+ RetailPOSColors.toHex(RetailPOSColors.borderStrong()));
			assign.setMargin(new Insets(8, 12, 8,
					new JLabel().getFontMetrics(RetailPOSTheme.PLEX_MONO_REGULAR.deriveFont(13f)).stringWidth(countText)
							+ 24));
			assign.setToolTipText(supplier.name);
			RetailPOSTheme.touchHeight(assign, 56);
			assign.setMaximumSize(new Dimension(Integer.MAX_VALUE, assign.getPreferredSize().height));
			assign.setAlignmentX(Component.LEFT_ALIGNMENT);
			selectionSuppliers.add(assign);
			selectionSuppliers.add(Box.createVerticalStrut(8));
			assign.addActionListener(e -> assignPendingSelected(supplier.id));
		}
		if (!"NONE".equals(selectedSupplier)) {
			JButton remove = new JButton(AppLocal.getIntString("Replenishment.RemoveSupplier"));
			RetailPOSTheme.touchHeight(remove, 48);
			remove.setMaximumSize(new Dimension(Integer.MAX_VALUE, remove.getPreferredSize().height));
			remove.setAlignmentX(Component.LEFT_ALIGNMENT);
			remove.addActionListener(e -> assignPendingSelected(null));
			selectionSuppliers.add(remove);
			selectionSuppliers.add(Box.createVerticalStrut(8));
		}
		JButton create = new JButton(AppLocal.getIntString("Replenishment.NewSupplier"), PosIcons.plus());
		styleGhost(create);
		create.setHorizontalAlignment(JButton.LEFT);
		RetailPOSTheme.touchHeight(create, 48);
		create.setMaximumSize(new Dimension(Integer.MAX_VALUE, create.getPreferredSize().height));
		create.setAlignmentX(Component.LEFT_ALIGNMENT);
		create.addActionListener(e -> manageSuppliers(null, saved -> assignPendingSelected(saved.id)));
		selectionSuppliers.add(create);
		selectionSuppliers.revalidate();
		selectionSuppliers.repaint();
	}
	private void updateDetailSaveState() {
		if (loadingDetail || editingEntry == null || detailSave == null)
			return;
		boolean dirty = !detailNote.getText().trim().equals(valueOrEmpty(editingEntry.note)) || !java.util.Objects
				.equals(formCustomer == null ? null : formCustomer.getId(), editingEntry.customerId)
				|| !java.util.Objects.equals(draftSupplierId, editingEntry.supplierId);
		detailSave.setEnabled(dirty);
		detailUndo.setEnabled(dirty);
		detailSave.setVisible(dirty);
		detailUndo.setVisible(dirty);
		detailSave.getParent().revalidate();
	}
	private void showStatusMenu(JButton anchor) {
		JPopupMenu menu = new JPopupMenu();
		menu.setName("replenishment.statusMenu");
		for (String status : new String[]{"PENDING", "ORDERED", "RECEIVED", "DELIVERED"}) {
			boolean current = editingEntry != null && status.equals(editingEntry.status);
			JMenuItem item = new JMenuItem((current ? "✓  " : "    ") + statusLabel(status));
			item.setName("replenishment.status." + status);
			boolean blocked = "DELIVERED".equals(status) && editingEntry != null && editingEntry.customerId == null
					&& formCustomer == null;
			item.setEnabled(!current && !blocked);
			if (blocked)
				item.setToolTipText(AppLocal.getIntString("Replenishment.NeedCustomerForDelivery"));
			if (blocked)
				item.getAccessibleContext()
						.setAccessibleDescription(AppLocal.getIntString("Replenishment.NeedCustomerForDelivery"));
			item.addActionListener(e -> applyDetailStep(status));
			menu.add(item);
		}
		menu.show(anchor, 0, anchor.getHeight());
	}
	private void showSupplierMenu(JButton anchor) {
		if (!administrationMode)
			return;
		JPopupMenu menu = new JPopupMenu();
		menu.setName("replenishment.supplierMenu");
		List<ReplenishmentSupplier> suppliers = new ArrayList<>();
		for (int i = 2; i < supplierFilter.getItemCount(); i++)
			suppliers.add(supplierFilter.getItemAt(i));
		List<ReplenishmentEntry> pending;
		try {
			pending = data.list("", "ALL", "PENDING", "ALL");
		} catch (BasicException ex) {
			JOptionPane.showMessageDialog(this, ex.getMessage());
			return;
		}
		JTextField filter = new JTextField(20);
		filter.putClientProperty("JTextField.placeholderText", AppLocal.getIntString("Replenishment.Search"));
		if (suppliers.size() > 8) {
			menu.add(filter);
		}
		List<JMenuItem> supplierItems = new ArrayList<>();
		for (ReplenishmentSupplier supplier : suppliers) {
			long count = pending.stream().filter(entry -> supplier.id.equals(entry.supplierId)).count();
			JMenuItem item = new JMenuItem(supplier.name + "  ·  " + pendingCountLabel(count));
			item.setName("replenishment.supplier." + supplier.id);
			item.addActionListener(e -> setDetailSupplier(supplier.id));
			menu.add(item);
			supplierItems.add(item);
		}
		if (suppliers.size() > 8) {
			Runnable update = () -> {
				String query = filter.getText().toLowerCase(java.util.Locale.ROOT);
				for (int i = 0; i < suppliers.size(); i++)
					supplierItems.get(i)
							.setVisible(suppliers.get(i).name.toLowerCase(java.util.Locale.ROOT).contains(query));
				menu.pack();
			};
			filter.getDocument().addDocumentListener(new DocumentListener() {
				public void insertUpdate(DocumentEvent e) {
					update.run();
				}
				public void removeUpdate(DocumentEvent e) {
					update.run();
				}
				public void changedUpdate(DocumentEvent e) {
					update.run();
				}
			});
			filter.getInputMap().put(KeyStroke.getKeyStroke("DOWN"), "selectSupplier");
			filter.getActionMap().put("selectSupplier", new AbstractAction() {
				@Override
				public void actionPerformed(java.awt.event.ActionEvent event) {
					for (JMenuItem item : supplierItems)
						if (item.isVisible()) {
							javax.swing.MenuSelectionManager.defaultManager()
									.setSelectedPath(new javax.swing.MenuElement[]{menu, item});
							break;
						}
				}
			});
		}
		menu.addSeparator();
		JMenuItem remove = new JMenuItem(AppLocal.getIntString("Replenishment.RemoveSupplier"));
		remove.setEnabled(draftSupplierId != null);
		remove.addActionListener(e -> setDetailSupplier(null));
		menu.add(remove);
		JMenuItem create = new JMenuItem(AppLocal.getIntString("Replenishment.NewSupplier"));
		create.addActionListener(e -> manageSuppliers(null, saved -> setDetailSupplier(saved.id)));
		menu.add(create);
		menu.show(anchor, 0, anchor.getHeight());
		if (suppliers.size() > 8)
			filter.requestFocusInWindow();
	}
	private void setDetailSupplier(String id) {
		if (!administrationMode || editingEntry == null || java.util.Objects.equals(draftSupplierId, id))
			return;
		draftSupplierId = id;
		detailSupplierName.setText(supplierName(id));
		applyDetailStep(editingEntry.status);
	}
	private void chooseDetailCustomer() {
		JCustomerFinder finder = JCustomerFinder.getCustomerFinder(this, customers);
		finder.search(null);
		finder.setVisible(true);
		CustomerInfo selected = finder.getSelectedCustomer();
		if (selected != null) {
			formCustomer = selected;
			showDetailCustomer(customerDisplayName(selected), selected.getPhone());
			String next = nextStatus();
			nextStep.setVisible(next != null || "RECEIVED".equals(editingEntry.status));
			nextStep.setText(next == null ? AppLocal.getIntString("Replenishment.Customer") : nextStepLabel(next));
			detailHint.setText(next == null && "RECEIVED".equals(editingEntry.status)
					? AppLocal.getIntString("Replenishment.NeedCustomerForDelivery")
					: "");
			detailHint.setVisible(!detailHint.getText().isBlank());
			updateDetailSaveState();
		}
	}
	private void saveDetail() {
		if (editingEntry == null)
			return;
		try {
			data.updateDetailsSupplierAndStatus(editingEntry.id, detailNote.getText().trim(),
					formCustomer == null ? null : formCustomer.getId(),
					formCustomer == null ? null : customerDisplayName(formCustomer), draftSupplierId,
					editingEntry.status, app.getAppUserView().getUser().getId());
			load(search.getText(), false, editingEntry.id);
		} catch (BasicException ex) {
			JOptionPane.showMessageDialog(this, AppLocal.getIntString("Replenishment.StatusUpdateFailed"));
		}
	}
	private String nextStatus() {
		if (editingEntry == null)
			return null;
		return switch (editingEntry.status) {
			case "PENDING" -> "ORDERED";
			case "ORDERED" -> "RECEIVED";
			case "RECEIVED" -> editingEntry.customerId != null || formCustomer != null ? "DELIVERED" : null;
			default -> null;
		};
	}
	private static String nextStepLabel(String status) {
		return AppLocal.getIntString("DELIVERED".equals(status)
				? "Replenishment.MarkDelivered"
				: "RECEIVED".equals(status) ? "Replenishment.MarkReceived" : "Replenishment.MarkOrdered");
	}
	private void applyDetailStep(String status) {
		if (status == null || editingEntry == null)
			return;
		String id = editingEntry.id;
		try {
			data.updateDetailsSupplierAndStatus(id, detailNote.getText().trim(),
					formCustomer == null ? null : formCustomer.getId(),
					formCustomer == null ? null : customerDisplayName(formCustomer), draftSupplierId, status,
					app.getAppUserView().getUser().getId());
			load(search.getText(), false, id);
		} catch (BasicException ex) {
			logStatusFailure("next-step", status, id, ex);
			JOptionPane.showMessageDialog(this, AppLocal.getIntString("Replenishment.StatusUpdateFailed"));
		}
	}
	private void markSelectedPending() {
		List<ReplenishmentEntry> selected = selectedEntries();
		try {
			data.markExportedPending(selected, app.getAppUserView().getUser().getId());
			load(search.getText(), false);
		} catch (BasicException ex) {
			JOptionPane.showMessageDialog(this, AppLocal.getIntString("Replenishment.StatusUpdateFailed"));
		}
	}
	private static JLabel sectionLabel(String key) {
		JLabel label = new JLabel(AppLocal.getIntString(key));
		label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD, 13f));
		return label;
	}
	private void findFormProduct() {
		if (editingEntry != null)
			return;
		try {
			formProduct = sales.getProductInfoByCode(formCode.getText().trim());
			if (formProduct == null)
				formProduct = JProductFinder.showMessage(this, sales);
			showSelectedProduct();
		} catch (BasicException e) {
			JOptionPane.showMessageDialog(this, e.getMessage());
		}
	}
	private void showSelectedProduct() {
		boolean manual = formProduct == null;
		if (manual) {
			selectedProduct.setText(AppLocal.getIntString("Replenishment.UnknownProduct"));
			productCodes.setText("");
			formDescription.setEditable(true);
		} else {
			selectedProduct.setText(displayProductName(formProduct.getName(), formProduct.getReference()));
			productCodes.setText(displayReference(formProduct.getName(), formProduct.getReference()) + " · EAN "
					+ valueOrEmpty(formProduct.getCode()));
			formDescription.setText(valueOrEmpty(formProduct.getName()));
			formDescription.setEditable(false);
		}
		if (editingEntry != null)
			formDescription.setEditable(false);
		updateSaveState();
		withoutCodeLabel.setVisible(manual && editingEntry == null);
		manualDescriptionLabel.setVisible(manual && editingEntry == null);
		manualDescriptionScroll.setVisible(manual && editingEntry == null);
		manualDescriptionScroll.getParent().revalidate();
		manualDescriptionScroll.getParent().repaint();
	}
	private void chooseFormCustomer() {
		JCustomerFinder finder = JCustomerFinder.getCustomerFinder(this, customers);
		finder.search(null);
		finder.setVisible(true);
		formCustomer = finder.getSelectedCustomer();
		setFormCustomerText(formCustomer == null
				? AppLocal.getIntString("Replenishment.NoCustomer")
				: customerDisplayName(formCustomer), formCustomer == null ? null : formCustomer.getPhone());
		updateCustomerButton();
		updateFormDeliveredEnabled();
		updateSaveState();
	}
	private void updateSaveState() {
		if (save == null)
			return;
		boolean dirty = editingEntry == null
				|| !formNoteArea.getText().trim().equals(valueOrEmpty(editingEntry.note)) || !java.util.Objects
						.equals(formCustomer == null ? null : formCustomer.getId(), editingEntry.customerId)
				|| !formStatus.equals(editingEntry.status);
		save.setEnabled(dirty);
		if (cancelNewRequest != null)
			cancelNewRequest.setEnabled(editingEntry == null && hasDraftContent());
		if (dirty) {
			RetailPOSColors.primaryButton(save);
		} else {
			save.putClientProperty("FlatLaf.style", null);
			save.setOpaque(false);
			save.setBackground(RetailPOSColors.surface100());
			save.setForeground(RetailPOSColors.ink());
		}
	}
	private void updateCustomerButton() {
		customerButton.setText(AppLocal
				.getIntString(formCustomer == null ? "Replenishment.Customer" : "Replenishment.ChangeCustomer"));
	}
	private void setFormCustomerText(String value) {
		setFormCustomerText(value, null);
	}
	private void setFormCustomerText(String value, String phone) {
		selectedCustomer.setText("<html>" + htmlText(value) + "</html>");
		selectedCustomer.setToolTipText(valueOrEmpty(phone).isBlank() ? value : value + " · " + phone);
		showCustomerPhone(selectedCustomerPhone, phone);
	}
	private void showDetailCustomer(String name, String phone) {
		detailCustomer.setText(name);
		detailCustomer.setToolTipText(name);
		showCustomerPhone(detailCustomerPhone, phone);
	}
	private void saveForm() {
		if (editingEntry == null && formProduct == null && formCode.getText().trim().isEmpty()
				&& formDescription.getText().trim().isEmpty()) {
			JOptionPane.showMessageDialog(this, AppLocal.getIntString("Replenishment.SelectProduct"));
			return;
		}
		String customerId = formCustomer == null ? null : formCustomer.getId();
		if (editingEntry != null && "DELIVERED".equals(formStatus) && customerId == null) {
			JOptionPane.showMessageDialog(this, AppLocal.getIntString("Replenishment.KeepDeliveredCustomer"));
			return;
		}
		try {
			if (editingEntry == null) {
				ReplenishmentEntry e = new ReplenishmentEntry();
				if (formProduct != null) {
					e.productId = formProduct.getID();
					e.reference = formProduct.getReference();
					e.name = formProduct.getName();
					e.ean = formProduct.getCode();
				} else {
					e.manualEan = formCode.getText().trim();
					e.manualDescription = formDescription.getText().trim();
				}
				e.note = formNoteArea.getText().trim();
				e.customerId = customerId;
				e.customerName = formCustomer == null ? null : customerDisplayName(formCustomer);
				e.createdBy = selectedSeller() == null ? null : selectedSeller().getId();
				ReplenishmentEntry saved = data.add(e);
				if (!"PENDING".equals(formStatus))
					data.updateStatus(saved.id, formStatus, app.getAppUserView().getUser().getId());
				selectedStatus = formStatus;
				selectedType = "ALL";
				selectedSupplier = "ALL";
				statusButtons.get(selectedStatus).setSelected(true);
				updateStatusSegmentStyles();
				refreshingSuppliers = true;
				supplierFilter.setSelectedIndex(0);
				refreshingSuppliers = false;
				clearForm();
				if (administrationMode)
					creatingRequest = false;
				load("", false);
			} else {
				String id = editingEntry.id;
				data.updateDetailsAndStatus(id, formNoteArea.getText().trim(), customerId,
						formCustomer == null ? null : customerDisplayName(formCustomer), formStatus,
						app.getAppUserView().getUser().getId());
				load("", false, id);
			}
		} catch (BasicException ex) {
			logStatusFailure("save", formStatus, editingEntry == null ? null : editingEntry.id, ex);
			String message = DataLogicReplenishment.STORE_REPLENISHMENT_EXISTS.equals(ex.getMessage())
					? AppLocal.getIntString("Replenishment.StoreAlreadyExists")
					: AppLocal.getIntString("Replenishment.StatusUpdateFailed");
			JOptionPane.showMessageDialog(this, message);
		}
	}
	private void loadEntry(ReplenishmentEntry entry) {
		loadingDetail = true;
		editingEntry = entry;
		creatingRequest = false;
		draftSupplierId = entry.supplierId;
		detailReference.setText(entry.productId == null
				? valueOrEmpty(entry.manualEan).isBlank()
						? AppLocal.getIntString("Replenishment.Manual")
						: entry.manualEan
				: displayReference(entry.name, entry.reference));
		detailProduct.setText(entry.productId == null
				? valueOrEmpty(entry.manualDescription)
				: displayProductName(entry.name, entry.reference));
		resizeDetailHeading();
		detailProduct.setToolTipText(entry.displayName());
		detailCreated.setText(String.format(AppLocal.getIntString("Replenishment.DetailAddedBy"),
				new SimpleDateFormat("dd/MM/yy").format(entry.createdAt), creatorName(entry)));
		detailStatus.setText(statusLabel(entry.status));
		detailStatus.setForeground(RetailPOSColors.ink());
		detailStatus.setIcon(new javax.swing.Icon() {
			@Override
			public int getIconWidth() {
				return 8;
			}
			@Override
			public int getIconHeight() {
				return 8;
			}
			@Override
			public void paintIcon(Component component, java.awt.Graphics graphics, int x, int y) {
				graphics.setColor("PENDING".equals(entry.status) || "DELIVERED".equals(entry.status)
						? RetailPOSColors.inkMuted()
						: statusColor(entry.status));
				graphics.fillOval(x, y, 8, 8);
			}
		});
		detailStatus.setIconTextGap(6);
		detailSupplierName.setText(supplierName(entry.supplierId));
		detailNote.setText(valueOrEmpty(entry.note));
		showDetailCustomer(entry.customerId == null
				? AppLocal.getIntString("Replenishment.NoCustomer")
				: valueOrEmpty(entry.customerName), entry.customerPhone);
		boolean pending = "PENDING".equals(entry.status);
		Component supplierControl = detailSupplierName.getParent().getComponent(1);
		if (supplierControl instanceof JButton)
			((JButton) supplierControl).setEnabled(pending);
		formSupplier.setText(AppLocal.getIntString("Replenishment.Supplier") + ": " + supplierName(entry.supplierId));
		formStatus = entry.status;
		creatorLabel.setText(AppLocal.getIntString("Replenishment.CreatedBy") + ": " + creatorName(entry));
		formCode.setText(entry.productId == null ? valueOrEmpty(entry.manualEan) : valueOrEmpty(entry.ean));
		formCode.setEditable(false);
		findProduct.setEnabled(false);
		productButtons.setVisible(false);
		scanLabel.setVisible(false);
		formCode.setVisible(false);
		formProduct = entry.productId == null ? null : new ProductInfoExt();
		if (formProduct != null) {
			formProduct.setID(entry.productId);
			formProduct.setReference(entry.reference);
			formProduct.setName(entry.name);
			formProduct.setCode(entry.ean);
		}
		formDescription.setText(entry.productId == null
				? (entry.manualDescription == null ? "" : entry.manualDescription)
				: entry.name);
		formDescription.setEditable(entry.productId == null);
		formNoteArea.setText(entry.note == null ? "" : entry.note);
		formCustomer = entry.customerId == null ? null : new CustomerInfo(entry.customerId);
		if (formCustomer != null) {
			formCustomer.setName(entry.customerName);
			formCustomer.setPhone(entry.customerPhone);
		}
		setFormCustomerText(entry.customerId == null
				? AppLocal.getIntString("Replenishment.NoCustomer")
				: customerDisplayName(formCustomer, entry.customerName), entry.customerPhone);
		updateCustomerButton();
		showSelectedProduct();
		if (entry.productId == null)
			selectedProduct.setText(valueOrEmpty(entry.manualDescription));
		if (entry.productId == null)
			productCodes.setText(valueOrEmpty(entry.manualEan));
		selectFormStatus();
		updateFormDeliveredEnabled();
		String next = nextStatus();
		nextStep.setVisible(next != null || "RECEIVED".equals(entry.status));
		nextStep.setText(next == null ? AppLocal.getIntString("Replenishment.Customer") : nextStepLabel(next));
		detailHint.setText("RECEIVED".equals(entry.status) && next == null
				? AppLocal.getIntString("Replenishment.NeedCustomerForDelivery")
				: "");
		detailHint.setVisible(!detailHint.getText().isBlank());
		updateSaveState();
		loadingDetail = false;
		updateDetailSaveState();
		updateSelectionView();
	}
	private void resizeDetailHeading() {
		java.awt.FontMetrics metrics = detailProduct.getFontMetrics(detailProduct.getFont());
		int width = Math.max(180, detailCards.getWidth() - 48);
		int lines = 1;
		String current = "";
		for (String word : detailProduct.getText().split("\\s+")) {
			String candidate = current.isEmpty() ? word : current + " " + word;
			if (!current.isEmpty() && metrics.stringWidth(candidate) > width) {
				lines++;
				current = word;
			} else
				current = candidate;
		}
		int height = metrics.getHeight() * lines + 4;
		detailProduct.setPreferredSize(new Dimension(0, height));
		detailProduct.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
		detailProduct.revalidate();
	}
	private void clearForm() {
		creatingRequest = true;
		editingEntry = null;
		formSupplier.setText("");
		formStatus = "PENDING";
		creatorLabel.setText(AppLocal.getIntString("Replenishment.CreatedBy") + ": " + selectedSellerName());
		formCode.setText("");
		formDescription.setText("");
		formDescription.setEditable(true);
		formNoteArea.setText("");
		formCustomer = null;
		formProduct = null;
		formCode.setEditable(true);
		formCode.setVisible(true);
		findProduct.setEnabled(true);
		productButtons.setVisible(true);
		scanLabel.setVisible(true);
		selectFormStatus();
		updateFormDeliveredEnabled();
		showSelectedProduct();
		selectedProduct.setText(AppLocal.getIntString("Replenishment.NoProductSelected"));
		setFormCustomerText(AppLocal.getIntString("Replenishment.NoCustomer"));
		updateCustomerButton();
		table.clearSelection();
		updateSaveState();
		formCode.requestFocusInWindow();
		updateSelectionView();
	}
	private boolean load(String search, boolean received) {
		return load(search, received, editingEntry == null ? null : editingEntry.id, false);
	}
	private boolean load(String search, boolean received, String selectId) {
		return load(search, received, selectId, true);
	}
	private boolean load(String search, boolean received, String selectId, boolean reloadForm) {
		try {
			List<ReplenishmentEntry> refreshed = data.list(search, selectedType, selectedStatus, selectedSupplier);
			updateTableColumnWidths(refreshed);
			List<ReplenishmentEntry> all = "ALL".equals(selectedStatus)
					? refreshed
					: data.list(search, selectedType, "ALL", selectedSupplier);
			List<ReplenishmentEntry> supplierCounts = "ALL".equals(selectedSupplier)
					? refreshed
					: data.list(search, selectedType, selectedStatus, "ALL");
			for (Map.Entry<String, JToggleButton> filter : statusButtons.entrySet()) {
				String key = filter.getKey();
				String labelKey = "ALL".equals(key)
						? "Replenishment.All"
						: "Replenishment." + key.charAt(0) + key.substring(1).toLowerCase(java.util.Locale.ROOT);
				long count = all.stream().filter(e -> "ALL".equals(key) || key.equals(e.status)).count();
				filter.getValue().setText(AppLocal.getIntString(labelKey) + " (" + count + ")");
			}
			updateStatusSegmentStyles();
			refreshSidebar(supplierCounts);
			supplierHeading.setVisible(administrationMode && isNamedSupplier());
			if (isNamedSupplier())
				updateSupplierHeading();
			sendSupplierOrder
					.setText(String.format(AppLocal.getIntString("Replenishment.SendOrderCount"), refreshed.size()));
			sendSupplierOrder.setEnabled(isNamedSupplier() && !refreshed.isEmpty());
			if (administrationMode && table.convertColumnIndexToView(0) < 0) {
				table.addColumn(selectionColumn);
				table.moveColumn(table.convertColumnIndexToView(0), 0);
			}
			if (!administrationMode && table.convertColumnIndexToView(0) >= 0)
				table.removeColumn(selectionColumn);
			if (administrationMode && !compactList && "ALL".equals(selectedSupplier)
					&& table.convertColumnIndexToView(5) < 0)
				table.addColumn(supplierColumn);
			if ((!administrationMode || compactList || !"ALL".equals(selectedSupplier))
					&& table.convertColumnIndexToView(5) >= 0)
				table.removeColumn(supplierColumn);
			boolean mixedStatuses = "ALL".equals(selectedStatus) || "OPEN".equals(selectedStatus);
			if (mixedStatuses && table.convertColumnIndexToView(3) < 0)
				table.addColumn(statusColumn);
			if (!mixedStatuses && table.convertColumnIndexToView(3) >= 0)
				table.removeColumn(statusColumn);
			if (administrationMode && !compactList && "ALL".equals(selectedSupplier)
					&& table.convertColumnIndexToView(5) != 2)
				table.moveColumn(table.convertColumnIndexToView(5), 2);
			int statusPosition = 1 + (table.convertColumnIndexToView(0) >= 0 ? 1 : 0)
					+ (table.convertColumnIndexToView(2) >= 0 ? 1 : 0)
					+ (table.convertColumnIndexToView(5) >= 0 ? 1 : 0);
			if (mixedStatuses && table.convertColumnIndexToView(3) != statusPosition)
				table.moveColumn(table.convertColumnIndexToView(3), statusPosition);
			loading = true;
			table.clearSelection();
			entries = refreshed;
			visibleRows.clear();
			model.setRowCount(0);
			DateFormat f = new SimpleDateFormat("dd/MM/yy");
			int selectedRow = -1;
			String previousGroup = null;
			for (ReplenishmentEntry e : entries) {
				if (isNamedSupplier()) {
					String groupLabel = groupLabel(e);
					if (!groupLabel.equals(previousGroup)) {
						visibleRows.add(null);
						model.addRow(new Object[]{false, groupLabel, "", "", "", ""});
						previousGroup = groupLabel;
					}
				}
				if (e.id.equals(selectId))
					selectedRow = model.getRowCount();
				visibleRows.add(e);
				model.addRow(new Object[]{false, e.displayName(), valueOrEmpty(e.customerName), statusLabel(e.status),
						f.format(e.createdAt), supplierName(e.supplierId)});
			}
			resizeRows();
			displayedSearch = search;
			if (selectedRow >= 0) {
				table.setRowSelectionInterval(selectedRow, selectedRow);
				if (reloadForm)
					loadEntry(visibleRows.get(selectedRow));
			} else {
				if (editingEntry != null) {
					clearForm();
					creatingRequest = false;
				}
			}
			loading = false;
			updateSelectionView();
			return true;
		} catch (BasicException e) {
			loading = false;
			table.clearSelection();
			model.setRowCount(0);
			visibleRows.clear();
			entries = null;
			JOptionPane.showMessageDialog(this, e.getMessage());
			return false;
		}
	}
	private void updateTableColumnWidths(List<ReplenishmentEntry> refreshed) {
		java.awt.FontMetrics metrics = table.getFontMetrics(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(15f));
		int supplierWidth = metrics.stringWidth(AppLocal.getIntString("Replenishment.NoSupplier")) + 24;
		for (int i = 2; i < supplierFilter.getItemCount(); i++)
			supplierWidth = Math.max(supplierWidth, metrics.stringWidth(supplierFilter.getItemAt(i).name) + 24);
		supplierColumn.setPreferredWidth(Math.min(200, supplierWidth));
		int customerWidth = 90;
		java.awt.FontMetrics phoneMetrics = table.getFontMetrics(RetailPOSTheme.PLEX_MONO_REGULAR.deriveFont(13f));
		for (ReplenishmentEntry entry : refreshed) {
			customerWidth = Math.max(customerWidth, metrics.stringWidth(valueOrEmpty(entry.customerName)) + 16);
			customerWidth = Math.max(customerWidth, phoneMetrics.stringWidth(valueOrEmpty(entry.customerPhone)) + 16);
		}
		if (table.convertColumnIndexToView(2) >= 0)
			table.getColumnModel().getColumn(table.convertColumnIndexToView(2))
					.setPreferredWidth(Math.min(200, customerWidth));
	}
	private String groupLabel(ReplenishmentEntry entry) {
		String brand = valueOrEmpty(entry.brand).trim();
		String family = valueOrEmpty(entry.family).trim();
		if (brand.isEmpty() && family.isEmpty())
			return AppLocal.getIntString("Replenishment.UnbrandedFamily");
		return (brand.isEmpty() ? AppLocal.getIntString("Replenishment.Unbranded") : brand) + " · "
				+ (family.isEmpty() ? AppLocal.getIntString("Replenishment.NoFamily") : family);
	}
	private String supplierName(String id) {
		if (id == null)
			return AppLocal.getIntString("Replenishment.NoSupplier");
		for (int i = 0; i < supplierFilter.getItemCount(); i++)
			if (id.equals(supplierFilter.getItemAt(i).id))
				return supplierFilter.getItemAt(i).name;
		return AppLocal.getIntString("Replenishment.NoSupplier");
	}
	private String statusLabel(String status) {
		return "DELIVERED".equals(status)
				? AppLocal.getIntString("Replenishment.FormDelivered")
				: "RECEIVED".equals(status)
						? AppLocal.getIntString("Replenishment.FormReceived")
						: "ORDERED".equals(status)
								? AppLocal.getIntString("Replenishment.FormOrdered")
								: AppLocal.getIntString("Replenishment.FormPending");
	}
	private static java.awt.Color statusColor(String state) {
		return "DELIVERED".equals(state)
				? RetailPOSColors.surface200()
				: "RECEIVED".equals(state)
						? RetailPOSColors.success()
						: "ORDERED".equals(state) ? RetailPOSColors.info() : RetailPOSColors.warning();
	}
	private static java.awt.Color badgeFill(String state) {
		java.awt.Color surface = RetailPOSColors.surface100();
		java.awt.Color accent = statusColor(state);
		return new java.awt.Color((surface.getRed() * 3 + accent.getRed()) / 4,
				(surface.getGreen() * 3 + accent.getGreen()) / 4, (surface.getBlue() * 3 + accent.getBlue()) / 4);
	}
	private void setFormStatus(String status) {
		formStatus = status;
		selectFormStatus();
		updateSaveState();
	}
	private void resizeRows() {
		int productWidth = table.getColumnModel().getColumn(table.convertColumnIndexToView(1)).getWidth();
		int customerWidth = table.convertColumnIndexToView(2) >= 0
				? table.getColumnModel().getColumn(table.convertColumnIndexToView(2)).getWidth()
				: productWidth;
		if (productWidth <= 0 || customerWidth <= 0 || entries == null)
			return;
		for (int row = 0; row < table.getRowCount(); row++) {
			ReplenishmentEntry entry = visibleRows.get(table.convertRowIndexToModel(row));
			if (entry == null) {
				table.setRowHeight(row, 36);
				continue;
			}
			String productName = entry.productId == null
					? valueOrEmpty(entry.manualDescription)
					: displayProductName(entry.name, entry.reference);
			int productHeight = wrappedHeight(productName, productWidth);
			productHeight += table.getFontMetrics(RetailPOSTheme.PLEX_MONO_REGULAR.deriveFont(13f)).getHeight();
			if (compactList)
				productHeight += table.getFontMetrics(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f)).getHeight() + 4;
			int customerHeight = table.getFontMetrics(RetailPOSTheme.MANROPE_MEDIUM).getHeight();
			if (!valueOrEmpty(entry.customerPhone).isBlank())
				customerHeight += table.getFontMetrics(RetailPOSTheme.PLEX_MONO_REGULAR.deriveFont(13f)).getHeight()
						+ 2;
			int height = Math.max(48, Math.max(productHeight, customerHeight) + 8);
			if (table.getRowHeight(row) != height)
				table.setRowHeight(row, height);
		}
	}
	static String displayProductName(String name, String reference) {
		String text = valueOrEmpty(name).trim();
		String code = valueOrEmpty(reference).trim();
		if (!code.isEmpty() && text.endsWith(" [" + code + "]"))
			return text.substring(0, text.length() - code.length() - 3).stripTrailing();
		return text;
	}
	static String displayReference(String name, String reference) {
		String code = valueOrEmpty(reference).trim();
		String displayName = displayProductName(name, reference);
		int separator = displayName.lastIndexOf(" — ");
		if (separator >= 0) {
			String variant = displayName.substring(separator + 3).trim();
			if (!variant.isEmpty() && code.endsWith(" " + variant))
				return code.substring(0, code.length() - variant.length() - 1);
		}
		return code;
	}
	private static int wrappedHeight(String text, int columnWidth) {
		String visible = preview(text, columnWidth);
		int lines = (int) visible.chars().filter(ch -> ch == '\n').count() + 1;
		return lines * new JLabel().getFontMetrics(RetailPOSTheme.MANROPE_MEDIUM).getHeight();
	}
	private static String preview(String text, int columnWidth) {
		java.awt.FontMetrics metrics = new JLabel().getFontMetrics(RetailPOSTheme.MANROPE_MEDIUM);
		int width = Math.max(1, columnWidth - 20);
		StringBuilder visible = new StringBuilder();
		StringBuilder line = new StringBuilder();
		int lines = 1;
		for (String word : valueOrEmpty(text).trim().split("\\s+")) {
			if (line.length() == 0 && metrics.stringWidth(word) > width) {
				StringBuilder shortened = new StringBuilder(word);
				while (shortened.length() > 0 && metrics.stringWidth(shortened + "…") > width)
					shortened.deleteCharAt(shortened.length() - 1);
				return visible.append(shortened).append('…').toString();
			}
			String candidate = line.length() == 0 ? word : line + " " + word;
			if (line.length() > 0 && metrics.stringWidth(candidate) > width) {
				if (lines == 2) {
					while (line.length() > 0 && metrics.stringWidth(line + "…") > width)
						line.deleteCharAt(line.length() - 1);
					return visible.append(line).append('…').toString();
				}
				visible.append(line).append('\n');
				line.setLength(0);
				lines++;
			}
			line.append(line.length() == 0 ? word : " " + word);
		}
		return visible.append(line).toString();
	}
	private final class CustomerRenderer extends JPanel implements javax.swing.table.TableCellRenderer {
		private final JLabel customerName = new JLabel();
		private final JLabel phone = new JLabel();
		CustomerRenderer() {
			super(new GridBagLayout());
			setOpaque(true);
			customerName.setFont(RetailPOSTheme.MANROPE_MEDIUM);
			phone.setFont(RetailPOSTheme.PLEX_MONO_REGULAR.deriveFont(13f));
			phone.setForeground(RetailPOSColors.inkMuted());
			GridBagConstraints line = new GridBagConstraints();
			line.weightx = 1;
			line.fill = GridBagConstraints.HORIZONTAL;
			line.anchor = GridBagConstraints.LINE_START;
			add(customerName, line);
			line.gridy = 1;
			add(phone, line);
		}
		@Override
		public void paint(java.awt.Graphics graphics) {
			doLayout();
			super.paint(graphics);
		}
		@Override
		public Component getTableCellRendererComponent(JTable grid, Object value, boolean selected, boolean focused,
				int row, int column) {
			ReplenishmentEntry entry = visibleRows.get(grid.convertRowIndexToModel(row));
			setBackground(entry == null || selected ? RetailPOSColors.surface200() : RetailPOSColors.surface100());
			customerName.setForeground(RetailPOSColors.ink());
			customerName.setText(entry == null ? "" : valueOrEmpty(entry.customerName));
			phone.setText(entry == null ? "" : valueOrEmpty(entry.customerPhone).trim());
			phone.setVisible(!phone.getText().isEmpty());
			setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 4));
			return this;
		}
	}
	private final class ProductRenderer extends JPanel implements javax.swing.table.TableCellRenderer {
		private final JLabel reference = new JLabel();
		private final JTextArea name = new JTextArea();
		private final JLabel metadata = new JLabel();
		ProductRenderer() {
			super(new BorderLayout());
			reference.setFont(RetailPOSTheme.PLEX_MONO_REGULAR.deriveFont(13f));
			name.setFont(RetailPOSTheme.MANROPE_MEDIUM);
			name.setLineWrap(false);
			// The renderer panel provides the cell padding for both lines.
			name.setMargin(new Insets(0, 0, 0, 0));
			name.setBorder(BorderFactory.createEmptyBorder());
			name.setEditable(false);
			name.setFocusable(false);
			add(reference, BorderLayout.NORTH);
			add(name, BorderLayout.CENTER);
			metadata.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f));
			metadata.setForeground(RetailPOSColors.inkMuted());
			add(metadata, BorderLayout.SOUTH);
		}
		@Override
		public void paint(java.awt.Graphics graphics) {
			doLayout();
			super.paint(graphics);
		}
		@Override
		public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
				boolean focused, int row, int column) {
			ReplenishmentEntry entry = visibleRows.get(table.convertRowIndexToModel(row));
			if (entry == null) {
				setBackground(RetailPOSColors.surface200());
				name.setBackground(RetailPOSColors.surface200());
				name.setForeground(RetailPOSColors.ink());
				name.setFont(RetailPOSTheme.MANROPE_BOLD.deriveFont(13f));
				name.setText(valueOrEmpty((String) value));
				reference.setVisible(false);
				metadata.setVisible(false);
				setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 4));
				return this;
			}
			name.setFont(RetailPOSTheme.MANROPE_MEDIUM);
			java.awt.Color background = selected ? RetailPOSColors.surface200() : RetailPOSColors.surface100();
			setBackground(background);
			name.setBackground(background);
			name.setForeground(RetailPOSColors.ink());
			reference.setBackground(background);
			reference.setForeground(RetailPOSColors.inkMuted());
			reference.setText(entry.productId == null
					? AppLocal.getIntString("Replenishment.Manual")
					: displayReference(entry.name, entry.reference));
			reference.setVisible(true);
			metadata.setVisible(compactList);
			metadata.setBackground(background);
			String caption = (!isNamedSupplier() ? supplierName(entry.supplierId) : "");
			if (!valueOrEmpty(entry.customerName).isBlank())
				caption += (caption.isEmpty() ? "" : " · ") + entry.customerName;
			if (!valueOrEmpty(entry.customerPhone).isBlank())
				caption += (caption.isEmpty() ? "" : " · ") + entry.customerPhone;
			metadata.setText(caption);
			metadata.setToolTipText(caption);
			name.setText(preview(
					entry.productId == null
							? valueOrEmpty(entry.manualDescription)
							: displayProductName(entry.name, entry.reference),
					table.getColumnModel().getColumn(column).getWidth()));
			setBorder(BorderFactory.createEmptyBorder(4, 8, 0, 4));
			return this;
		}
	}
	private final class StatusBadgeRenderer extends DefaultTableCellRenderer {
		private java.awt.Color badgeColor;
		private java.awt.Color fillColor;
		private String state;
		@Override
		public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
				boolean focused, int row, int column) {
			super.getTableCellRendererComponent(table, value, selected, focused, row, column);
			ReplenishmentEntry entry = visibleRows.get(table.convertRowIndexToModel(row));
			if (entry == null) {
				state = "";
				setText("");
				badgeColor = RetailPOSColors.surface200();
				fillColor = RetailPOSColors.surface200();
				setBackground(RetailPOSColors.surface200());
				return this;
			}
			state = entry.status;
			badgeColor = "PENDING".equals(state)
					? RetailPOSColors.inkMuted()
					: "DELIVERED".equals(state) ? RetailPOSColors.success() : statusColor(state);
			fillColor = RetailPOSColors.surface200();
			setBackground(selected ? RetailPOSColors.surface200() : RetailPOSColors.surface100());
			setForeground(RetailPOSColors.ink());
			setFont(RetailPOSTheme.MANROPE_SEMIBOLD.deriveFont(13f));
			setVerticalAlignment(JLabel.CENTER);
			setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 4));
			setOpaque(false);
			return this;
		}
		@Override
		protected void paintComponent(java.awt.Graphics graphics) {
			java.awt.Graphics2D g = (java.awt.Graphics2D) graphics.create();
			g.setColor(getBackground());
			g.fillRect(0, 0, getWidth(), getHeight());
			if (state.isEmpty()) {
				g.dispose();
				return;
			}
			g.setColor(fillColor);
			int badgeWidth = Math.min(getWidth() - 8, getFontMetrics(getFont()).stringWidth(getText()) + 32);
			int badgeHeight = 28;
			int badgeY = (getHeight() - badgeHeight) / 2;
			g.fillRoundRect(4, badgeY, badgeWidth, badgeHeight, badgeHeight, badgeHeight);
			g.setColor(badgeColor);
			if ("DELIVERED".equals(state)) {
				g.setStroke(new java.awt.BasicStroke(2f));
				g.drawLine(11, badgeY + 15, 14, badgeY + 18);
				g.drawLine(14, badgeY + 18, 20, badgeY + 10);
			} else
				g.fillOval(12, badgeY + 10, 8, 8);
			g.setColor(getForeground());
			java.awt.FontMetrics metrics = g.getFontMetrics(getFont());
			g.drawString(getText(), 27, badgeY + (badgeHeight - metrics.getHeight()) / 2 + metrics.getAscent());
			g.dispose();
		}
	}
	private static String valueOrEmpty(String value) {
		return value == null ? "" : value;
	}
	private static String htmlText(String value) {
		return valueOrEmpty(value).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"",
				"&quot;");
	}
	private void selectFormStatus() {
		formPending.setSelected("PENDING".equals(formStatus));
		formOrdered.setSelected("ORDERED".equals(formStatus));
		formReceived.setSelected("RECEIVED".equals(formStatus));
		formDelivered.setSelected("DELIVERED".equals(formStatus));
	}
	private void updateFormDeliveredEnabled() {
		formDelivered.setVisible(editingEntry != null);
		formDelivered.setEnabled(editingEntry != null && formCustomer != null
				&& ("RECEIVED".equals(editingEntry.status) || "DELIVERED".equals(editingEntry.status)));
		formDeliveryHint.setVisible(
				editingEntry != null && !formDelivered.isEnabled() && !"DELIVERED".equals(editingEntry.status));
		formStatusButtons.revalidate();
	}
	private static String customerDisplayName(CustomerInfo customer) {
		return customerDisplayName(customer, customer == null ? null : customer.getName());
	}
	private static String customerDisplayName(CustomerInfo customer, String name) {
		if (name != null && !name.isBlank())
			return name;
		return valueOrEmpty(customer == null ? null : customer.getId());
	}
	private static void applyCardSurface(Container component) {
		if (component instanceof JPanel)
			((JPanel) component).setBackground(RetailPOSColors.surface100());
		if (component instanceof JScrollPane)
			((JScrollPane) component).getViewport().setBackground(RetailPOSColors.surface100());
		if (component instanceof JTable)
			((JTable) component).setBackground(RetailPOSColors.surface100());
		for (Component child : component.getComponents())
			if (child instanceof Container)
				applyCardSurface((Container) child);
	}
	private static final class RaisedCard extends JPanel {
		RaisedCard(java.awt.LayoutManager layout) {
			super(layout);
			setOpaque(false);
			setBorder(BorderFactory.createEmptyBorder(4, 16, 8, 20));
		}
		@Override
		protected void paintComponent(java.awt.Graphics graphics) {
			java.awt.Graphics2D g = (java.awt.Graphics2D) graphics.create();
			g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
			java.awt.Color border = RetailPOSColors.border();
			g.setColor(new java.awt.Color(border.getRed(), border.getGreen(), border.getBlue(), 48));
			g.fillRoundRect(4, 4, getWidth() - 4, getHeight() - 4, 16, 16);
			g.setColor(RetailPOSColors.surface100());
			g.fillRoundRect(0, 0, getWidth() - 5, getHeight() - 5, 16, 16);
			g.dispose();
		}
	}
	private String creatorName(ReplenishmentEntry entry) {
		if (entry.creatorName != null && !entry.creatorName.isBlank())
			return entry.creatorName;
		UserInfo seller = selectedSeller();
		return seller != null && seller.getId() != null && seller.getId().equals(entry.createdBy)
				? seller.getName()
				: "--";
	}
	/**
	 * Follow the viewport width while allowing the right form to scroll vertically.
	 */
	private static class FormFields extends JPanel implements javax.swing.Scrollable {
		FormFields() {
			super(new GridBagLayout());
		}
		@Override
		public java.awt.Dimension getPreferredScrollableViewportSize() {
			return getPreferredSize();
		}
		@Override
		public int getScrollableUnitIncrement(java.awt.Rectangle visibleRect, int orientation, int direction) {
			return 16;
		}
		@Override
		public int getScrollableBlockIncrement(java.awt.Rectangle visibleRect, int orientation, int direction) {
			return visibleRect.height - 16;
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
	private List<ReplenishmentEntry> selectedEntries() {
		List<ReplenishmentEntry> selected = new ArrayList<>();
		if (entries != null)
			for (int row : table.getSelectedRows())
				if (!isGroupRow(row))
					selected.add(visibleRows.get(table.convertRowIndexToModel(row)));
		return selected;
	}
	private List<String> selectedIds() {
		List<String> ids = new ArrayList<>();
		for (ReplenishmentEntry entry : selectedEntries())
			ids.add(entry.id);
		return ids;
	}
	private void clearSelectedRows() {
		if (editingEntry != null) {
			clearForm();
			creatingRequest = false;
			updateSelectionView();
		} else
			table.clearSelection();
	}
	private void clearSelectionForFilter() {
		creatingRequest = false;
		boolean previousLoading = loading;
		loading = true;
		table.clearSelection();
		loading = previousLoading;
	}
	private void updateSelectionView() {
		int count = table.getSelectedRowCount();
		if (detailCards == null)
			return;
		String card = count > 1 && administrationMode ? "selection" : count == 1 ? "detail" : "new";
		((CardLayout) detailCards.getLayout()).show(detailCards, card);
		boolean dockVisible = !administrationMode || count > 0 || creatingRequest;
		if (detailDock.isVisible() != dockVisible) {
			detailDock.setVisible(dockVisible);
			mainArea.revalidate();
		}
		selectionSummary.setText(String.format(AppLocal.getIntString("Replenishment.SelectedCount"), count));
		footerSelection.setText(String.format(AppLocal.getIntString("Replenishment.SelectedOf"), count,
				entries == null ? 0 : entries.size()));
		footerSelection.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		footerScope.setText(String.format(AppLocal.getIntString("Replenishment.ExportVisibleHint"),
				entries == null ? 0 : entries.size()));
		footerScope.setVisible(count == 0);
		deselectButton.setVisible(count > 0);
		exportSelection.setText(count == 0
				? AppLocal.getIntString("Replenishment.Export")
				: String.format(AppLocal.getIntString("Replenishment.ExportSelectionCount"), count));
		exportSelection.setVisible(administrationMode && !isNamedSupplier());
		layoutFooter();
		if (count > 1 && administrationMode)
			refreshSelectionSuppliers();
	}
	private void layoutFooter() {
		if (footerActions == null || selectAllButton == null)
			return;
		boolean stacked = getWidth() > 0 && getWidth() < 1200 && table.getSelectedRowCount() > 0;
		if (footerActions.getComponentCount() > 0 && footerStacked == stacked)
			return;
		footerStacked = stacked;
		footerActions.removeAll();
		if (stacked) {
			GridBagConstraints row = new GridBagConstraints();
			row.gridx = 0;
			row.fill = GridBagConstraints.HORIZONTAL;
			row.weightx = 1;
			footerActions.add(footerSummary, row);
			row.gridy = 1;
			row.fill = GridBagConstraints.NONE;
			row.anchor = GridBagConstraints.EAST;
			footerActions.add(selectAllButton, row);
			row.gridy = 2;
			footerActions.add(deselectButton, row);
			row.gridy = 3;
			footerActions.add(exportSelection, row);
			footerActions.revalidate();
			return;
		}
		GridBagConstraints cell = new GridBagConstraints();
		cell.gridx = 0;
		cell.gridy = 0;
		cell.weightx = 1;
		cell.fill = GridBagConstraints.HORIZONTAL;
		cell.anchor = GridBagConstraints.WEST;
		cell.gridwidth = 1;
		footerActions.add(footerSummary, cell);
		cell.gridwidth = 1;
		cell.gridy = 0;
		cell.weightx = 0;
		cell.fill = GridBagConstraints.NONE;
		cell.insets = new Insets(0, 0, 0, 4);
		cell.gridx = 1;
		footerActions.add(selectAllButton, cell);
		cell.gridx = 2;
		footerActions.add(deselectButton, cell);
		cell.gridx = 3;
		cell.gridy = 0;
		cell.weightx = 0;
		cell.fill = GridBagConstraints.NONE;
		cell.anchor = GridBagConstraints.EAST;
		cell.insets = new Insets(0, 0, 0, 0);
		footerActions.add(exportSelection, cell);
		footerActions.revalidate();
		footerActions.repaint();
	}
	static void logStatusFailure(String action, String status, String id, BasicException failure) {
		Throwable cause = failure;
		while (cause.getCause() != null)
			cause = cause.getCause();
		String detail = "";
		if (cause instanceof SQLException) {
			SQLException sql = (SQLException) cause;
			detail = " sqlState=" + sql.getSQLState() + " errorCode=" + sql.getErrorCode();
			Matcher constraint = CONSTRAINT.matcher(sql.getMessage() == null ? "" : sql.getMessage());
			if (constraint.find())
				detail += " constraint=" + constraint.group(1);
		}
		logger.warning("event=replenishment_status_update_failed action=" + action + " status=" + status + " entryId="
				+ id + " cause=" + cause.getClass().getSimpleName() + detail);
	}
	@Override
	public String getTitle() {
		return AppLocal.getIntString("Replenishment.ScreenTitle");
	}
	@Override
	public void activate() throws BasicException {
		boolean nextMode = app.getAppUserView().getUser().isAdministrationMode();
		if (!modeInitialized || administrationMode != nextMode) {
			modeInitialized = true;
			administrationMode = nextMode;
			table.setSelectionMode(administrationMode
					? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
					: ListSelectionModel.SINGLE_SELECTION);
			detailSupplierChange.setVisible(administrationMode);
			footerActions.setVisible(administrationMode);
			mainArea.revalidate();
		}
		refreshSupplierFilter();
		search.setText("");
		selectedStatus = "PENDING";
		selectedType = "ALL";
		selectedSupplier = "ALL";
		creatingRequest = false;
		supplierFilter.setSelectedIndex(0);
		queueFilterLabel.setText(" ");
		queueFilterLabel.setVisible(false);
		pendingStatus.setSelected(true);
		updateStatusSegmentStyles();
		if (!hasDraftContent())
			creatorLabel.setText(AppLocal.getIntString("Replenishment.CreatedBy") + ": " + selectedSellerName());
		load("", false);
		javax.swing.SwingUtilities.invokeLater(() -> search.requestFocusInWindow());
	}

	/** Open the list for one of the landing screen's waiting-work figures. */
	public void showQueue(String filter) {
		selectedType = "ENCARGO".equals(filter) ? "ENCARGO" : "ALL";
		selectedStatus = "ENCARGO".equals(filter) ? "OPEN" : filter;
		statusButtons.get("ENCARGO".equals(filter) ? "ALL" : filter).setSelected(true);
		updateStatusSegmentStyles();
		queueFilterLabel.setText("ENCARGO".equals(filter) ? AppLocal.getIntString("stock.welcome.customers") : " ");
		queueFilterLabel.setVisible("ENCARGO".equals(filter));
		load("", false);
	}

	/** Start a manual request from a code with no matching product. */
	public void startManualEntry(String code) {
		clearForm();
		formCode.setText(code);
		formDescription.requestFocusInWindow();
	}

	/**
	 * Prepare a missing delivery-note item with the supplier recorded in its note.
	 */
	public void startManualEntry(String code, String note) {
		startManualEntry(code);
		formNoteArea.setText(note);
	}

	/** Locate an already-open replenishment entry without adding a duplicate. */
	public void showProductOnList(String code) {
		search.setText(code);
		load(code, false);
	}
	private boolean hasDraftContent() {
		return formProduct != null || !formCode.getText().trim().isEmpty()
				|| !formDescription.getText().trim().isEmpty() || !formNoteArea.getText().trim().isEmpty()
				|| formCustomer != null || !"PENDING".equals(formStatus);
	}
	@Override
	public boolean deactivate() {
		return true;
	}
	@Override
	public javax.swing.JComponent getComponent() {
		return this;
	}
	@Override
	public Object getBean() {
		return this;
	}

}
