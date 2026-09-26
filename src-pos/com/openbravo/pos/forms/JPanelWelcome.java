package com.openbravo.pos.forms;

import com.openbravo.basic.BasicException;
import com.openbravo.pos.util.HiDpiIcon;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.AbstractBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class JPanelWelcome extends JPanel implements JPanelView, BeanFactoryApp {

	private static final Color BACKGROUND = new Color(245, 241, 234);
	private static final Color SURFACE = new Color(255, 253, 249);
	private static final Color TEXT = new Color(36, 28, 20);
	private static final Color MUTED = new Color(107, 97, 84);
	private static final Color BORDER = new Color(150, 137, 111);
	private static final int CARD_RADIUS = 22;

	private AppView app;
	private JTextField search;
	private JLabel greetingTitle;
	private JPanel content;

	@Override
	public void init(AppView app) throws BeanFactoryException {
		this.app = app;
		build();
	}

	@Override
	public Object getBean() {
		return this;
	}

	private void build() {
		setLayout(new BorderLayout());
		setBackground(BACKGROUND);
		setBorder(null);

		JPanel page = new JPanel(new BorderLayout(0, 18));
		page.setOpaque(false);
		page.setBorder(BorderFactory.createEmptyBorder(28, 0, 0, 36));

		JPanel header = new JPanel();
		header.setOpaque(false);
		header.setLayout(new BorderLayout(0, 12));
		JPanel greeting = new JPanel();
		greeting.setOpaque(false);
		greeting.setLayout(new javax.swing.BoxLayout(greeting, javax.swing.BoxLayout.Y_AXIS));
		JLabel date = label(DateFormat.getDateInstance(DateFormat.FULL).format(new Date()).toUpperCase(), 12, Font.BOLD,
				MUTED);
		greetingTitle = label(AppLocal.getIntString("Workflow.Greeting", firstName()), 28, Font.BOLD, TEXT);
		greeting.add(date);
		greeting.add(greetingTitle);
		header.add(greeting, BorderLayout.NORTH);

		JPanel searchPanel = new JPanel(new BorderLayout(10, 0));
		searchPanel.setOpaque(false);
		search = new JTextField();
		search.setFont(search.getFont().deriveFont(15f));
		search.setToolTipText(AppLocal.getIntString("Workflow.Search"));
		search.putClientProperty("JTextField.placeholderText", AppLocal.getIntString("Workflow.SearchHint"));
		search.setBorder(BorderFactory.createCompoundBorder(new RoundedLineBorder(BORDER, 16),
				BorderFactory.createEmptyBorder(8, 12, 8, 12)));
		searchPanel.add(search, BorderLayout.CENTER);
		header.add(searchPanel, BorderLayout.SOUTH);
		page.add(header, BorderLayout.NORTH);

		content = new JPanel(new BorderLayout());
		content.setOpaque(false);
		page.add(content, BorderLayout.CENTER);

		JScrollPane scroll = new JScrollPane(page);
		scroll.setBorder(null);
		scroll.setOpaque(false);
		scroll.getViewport().setOpaque(false);
		scroll.getVerticalScrollBar().setUnitIncrement(18);
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		add(scroll, BorderLayout.CENTER);

		search.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent event) {
				render();
			}

			@Override
			public void removeUpdate(DocumentEvent event) {
				render();
			}

			@Override
			public void changedUpdate(DocumentEvent event) {
				render();
			}
		});
		search.addActionListener(event -> focusFirstWorkflow());
		getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ESCAPE"), "clearSearch");
		getActionMap().put("clearSearch", new javax.swing.AbstractAction() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent event) {
				search.setText("");
				search.requestFocusInWindow();
			}
		});
		render();
	}

	private void focusFirstWorkflow() {
		if (focusFirstWorkflow(content)) {
			return;
		}
		search.requestFocusInWindow();
	}

	private boolean focusFirstWorkflow(Component component) {
		if (component instanceof WorkflowButton) {
			component.requestFocusInWindow();
			return true;
		}
		if (component instanceof java.awt.Container) {
			for (Component child : ((java.awt.Container) component).getComponents()) {
				if (focusFirstWorkflow(child)) {
					return true;
				}
			}
		}
		return false;
	}

	private void render() {
		if (content == null) {
			return;
		}
		content.removeAll();
		String query = search.getText().trim().toLowerCase(Locale.ROOT);
		if (query.isEmpty()) {
			content.add(defaultContent(), BorderLayout.NORTH);
		} else {
			content.add(searchResults(query), BorderLayout.NORTH);
		}
		content.revalidate();
		content.repaint();
	}

	private JPanel defaultContent() {
		JPanel all = new JPanel(new GridBagLayout());
		all.setOpaque(false);
		int row = 0;
		all.add(sectionHeading(AppLocal.getIntString("Workflow.Common")), fullWidthRow(row++));
		all.add(grid(commonEntries(), true), fullWidthRow(row++));
		all.add(sectionHeading(AppLocal.getIntString("Workflow.Everything")), fullWidthRow(row++));
		for (WorkflowCategory category : catalogue()) {
			List<JPanel> entries = usablePanels(category.entries, false);
			if (!entries.isEmpty()) {
				all.add(categoryHeading(category.labelKey), fullWidthRow(row++));
				all.add(responsiveLinks(entries, 3, 0), fullWidthRow(row++));
			}
		}
		return all;
	}

	private GridBagConstraints fullWidthRow(int row) {
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = row;
		constraints.gridwidth = 1;
		constraints.weightx = 1;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.anchor = GridBagConstraints.LINE_START;
		return constraints;
	}

	private JPanel searchResults(String query) {
		List<JPanel> commonResults = new ArrayList<JPanel>();
		List<JPanel> catalogueResults = new ArrayList<JPanel>();
		for (WorkflowCategory category : catalogue()) {
			for (WorkflowEntry entry : category.entries) {
				if (entry.matches(query) && usable(entry)) {
					if (entry.common) {
						commonResults.add(workflowButton(entry, true));
					} else {
						catalogueResults.add(workflowButton(entry, false));
					}
				}
			}
		}
		JPanel result = new JPanel();
		result.setOpaque(false);
		result.setLayout(new javax.swing.BoxLayout(result, javax.swing.BoxLayout.Y_AXIS));
		result.add(sectionHeading(AppLocal.getIntString("Workflow.Results")));
		if (commonResults.isEmpty() && catalogueResults.isEmpty()) {
			result.add(label(AppLocal.getIntString("Workflow.NoResults"), 14, Font.PLAIN, MUTED));
		} else {
			if (!commonResults.isEmpty()) {
				result.add(responsivePanels(commonResults, 3, 16));
			}
			if (!catalogueResults.isEmpty()) {
				result.add(responsiveLinks(catalogueResults, 3, 0));
			}
		}
		return result;
	}

	private List<JPanel> usablePanels(List<WorkflowEntry> entries, boolean common) {
		List<JPanel> result = new ArrayList<JPanel>();
		for (WorkflowEntry entry : entries) {
			if (usable(entry)) {
				result.add(workflowButton(entry, common));
			}
		}
		return result;
	}

	private boolean usable(WorkflowEntry entry) {
		return isSalesWorkflow(entry) || app.getAppUserView().getTaskAction(entry.taskName) != null;
	}

	private boolean isSalesWorkflow(WorkflowEntry entry) {
		return "com.openbravo.pos.sales.JPanelTicketSales".equals(entry.taskName) && app instanceof JRootApp;
	}

	private JPanel grid(List<WorkflowEntry> entries, boolean common) {
		return responsivePanels(usablePanels(entries, common), 3, 16);
	}

	private JPanel responsivePanels(final List<JPanel> panels, final int wideColumns, int gap) {
		final JPanel grid = new JPanel(new GridLayout(0, wideColumns, gap, gap));
		grid.setOpaque(false);
		for (JPanel panel : panels) {
			grid.add(panel);
		}
		grid.addComponentListener(new java.awt.event.ComponentAdapter() {
			@Override
			public void componentResized(java.awt.event.ComponentEvent event) {
				int columns = grid.getWidth() < 620 ? 2 : wideColumns;
				if (grid.getWidth() < 390) {
					columns = 1;
				}
				if (((GridLayout) grid.getLayout()).getColumns() != columns) {
					grid.setLayout(new GridLayout(0, columns, gap, gap));
					grid.revalidate();
				}
			}
		});
		return grid;
	}

	private JPanel responsiveLinks(final List<JPanel> panels, final int wideColumns, int gap) {
		final JPanel grid = new JPanel(new GridLayout(0, wideColumns, gap, gap));
		grid.setOpaque(false);
		for (JPanel panel : panels) {
			grid.add(asCatalogLink(panel));
		}
		grid.addComponentListener(new java.awt.event.ComponentAdapter() {
			@Override
			public void componentResized(java.awt.event.ComponentEvent event) {
				int columns = grid.getWidth() < 620 ? 2 : wideColumns;
				if (grid.getWidth() < 390) {
					columns = 1;
				}
				if (((GridLayout) grid.getLayout()).getColumns() != columns) {
					grid.setLayout(new GridLayout(0, columns, gap, gap));
					grid.revalidate();
				}
			}
		});
		return grid;
	}

	private JPanel asCatalogLink(JPanel panel) {
		WorkflowButton button = (WorkflowButton) panel.getComponent(0);
		return new JPanel(new BorderLayout()) {
			{
				setOpaque(false);
				add(button.asLink(), BorderLayout.CENTER);
			}
		};
	}

	private JPanel workflowButton(final WorkflowEntry entry, boolean common) {
		final javax.swing.Action action = isSalesWorkflow(entry)
				? new javax.swing.AbstractAction(AppLocal.getIntString(entry.labelKey)) {
					@Override
					public void actionPerformed(java.awt.event.ActionEvent event) {
						((JRootApp) app).openSalesView();
					}
				}
				: app.getAppUserView().getTaskAction(entry.taskName);
		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setOpaque(false);
		WorkflowButton button = new WorkflowButton(entry, action, common);
		wrapper.add(button, BorderLayout.CENTER);
		return wrapper;
	}

	private JPanel sectionHeading(String text) {
		JPanel panel = new JPanel();
		panel.setLayout(new javax.swing.BoxLayout(panel, javax.swing.BoxLayout.X_AXIS));
		panel.setOpaque(false);
		panel.setBorder(BorderFactory.createEmptyBorder(28, 0, 10, 0));
		panel.add(label(text.toUpperCase(Locale.getDefault()), 12, Font.BOLD, MUTED));
		panel.add(Box.createHorizontalStrut(12));
		JPanel line = new JPanel();
		line.setBackground(BORDER);
		line.setPreferredSize(new Dimension(1, 1));
		line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
		panel.add(line);
		return panel;
	}

	private JLabel categoryHeading(String key) {
		JLabel heading = label(AppLocal.getIntString(key).toUpperCase(Locale.getDefault()), 12, Font.BOLD, MUTED);
		heading.setHorizontalAlignment(SwingConstants.LEFT);
		heading.setBorder(BorderFactory.createEmptyBorder(16, 0, 4, 0));
		heading.setAlignmentX(Component.LEFT_ALIGNMENT);
		heading.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
		return heading;
	}

	private static JLabel label(String text, int size, int style, Color color) {
		JLabel label = new JLabel(text);
		label.setFont(label.getFont().deriveFont(style, (float) size));
		label.setForeground(color);
		return label;
	}

	private List<WorkflowEntry> commonEntries() {
		List<WorkflowEntry> result = new ArrayList<WorkflowEntry>();
		String[] commonTasks = {"com.openbravo.pos.sales.JPanelTicketSales",
				"com.openbravo.pos.inventory.ReplenishmentPanel", "com.openbravo.pos.forms.MenuStockManagement",
				"com.openbravo.pos.reports.JPanelCashClosing", "com.openbravo.pos.customers.CustomersPanel",
				"com.openbravo.pos.forms.MenuSalesManagement"};
		for (String task : commonTasks) {
			for (WorkflowCategory category : catalogue()) {
				for (WorkflowEntry entry : category.entries) {
					if (entry.common && entry.taskName.equals(task)) {
						result.add(entry);
					}
				}
			}
		}
		return result;
	}

	private static List<WorkflowCategory> catalogue() {
		List<WorkflowCategory> result = new ArrayList<WorkflowCategory>();
		result.add(category("Workflow.Category.Selling",
				entry("Workflow.Sell", "com.openbravo.pos.sales.JPanelTicketSales",
						"/com/openbravo/images/menu-sales.png", true, "sell", "sale", "vendre"),
				entry("Workflow.EditSales", "com.openbravo.pos.sales.JPanelTicketEdits",
						"/com/openbravo/images/menu-edit-sales.png", false, "edit", "sale"),
				entry("Workflow.CashMovements", "com.openbravo.pos.panels.JPanelPayments",
						"/com/openbravo/images/menu-payments.png", false, "cash", "payment", "caixa"),
				entry("Workflow.CloseCash", "com.openbravo.pos.panels.JPanelCloseMoney",
						"/com/openbravo/images/menu-close-cash.png", false, "close", "cash", "caixa"),
				entry("Workflow.ClosedCash", "com.openbravo.pos.panels.JPanelClosedCash",
						"/com/openbravo/images/menu-cash-closed.png", false, "closed", "cash", "caixa")));
		result.add(category("Workflow.Category.Catalogue",
				entry("Workflow.CataloguePrices", "com.openbravo.pos.forms.MenuStockManagement",
						"/com/openbravo/images/menu-stock.png", true, "catalogue", "catalog", "stock", "inventory",
						"cataleg", "preus", "prices", "productes"),
				entry("Workflow.Products", "com.openbravo.pos.inventory.ProductsPanel",
						"/com/openbravo/images/menu-products.png", false, "product", "products", "article", "producte"),
				entry("Workflow.Categories", "com.openbravo.pos.inventory.CategoriesPanel",
						"/com/openbravo/images/menu-categories.png", false, "category", "categories"),
				entry("Workflow.PriceRules", "com.openbravo.pos.inventory.PriceRulesPanel",
						"/com/openbravo/images/menu-products.png", false, "price", "prices", "preu"),
				entry("Workflow.Markdowns", "com.openbravo.pos.inventory.SaleMarkPanel",
						"/com/openbravo/images/menu-products.png", false, "markdown", "discount", "rebaixa"),
				entry("Workflow.Taxes", "com.openbravo.pos.inventory.TaxPanel", "/com/openbravo/images/menu-taxes.png",
						false, "tax", "vat", "iva")));
		result.add(category("Workflow.Category.Stock",
				entry("Workflow.Replenishment", "com.openbravo.pos.inventory.ReplenishmentPanel",
						"/com/openbravo/images/menu-package-plus.png", true, "replenishment", "orders", "delivery",
						"restock", "reposicio"),
				entry("Workflow.LowStock", "com.openbravo.pos.reports.JPanelLowStock",
						"/com/openbravo/images/menu-inventory-current.png", false, "low", "stock", "ending", "acabant"),
				entry("Workflow.StockDiary", "com.openbravo.pos.inventory.StockDiaryPanel",
						"/com/openbravo/images/menu-stock-diary.png", false, "stock", "diary", "movement")));
		result.add(category("Workflow.Category.Customers",
				entry("Workflow.Customers", "com.openbravo.pos.customers.CustomersPanel",
						"/com/openbravo/images/menu-customers-payment.png", true, "customer", "customers", "debt",
						"clients", "deutes"),
				entry("Workflow.CustomerDebt", "com.openbravo.pos.reports.JPanelCustomerDebt",
						"/com/openbravo/images/menu-customers-report.png", false, "customer", "debt", "clients",
						"deute")));
		result.add(category("Workflow.Category.Reports",
				entry("Workflow.ReportsWelcome", "com.openbravo.pos.forms.MenuSalesManagement",
						"/com/openbravo/images/menu-sales-reports.png", true, "reports", "sales", "informes", "vendes"),
				entry("Workflow.SalesSummary", "com.openbravo.pos.reports.JPanelSalesSummary",
						"/com/openbravo/images/menu-sales-reports.png", false, "sales", "summary", "vendes"),
				entry("Workflow.ProductSales", "com.openbravo.pos.reports.JPanelProductSales",
						"/com/openbravo/images/menu-product-sales.png", false, "sales", "product"),
				entry("Workflow.PaymentSales", "com.openbravo.pos.reports.JPanelPaymentSales",
						"/com/openbravo/images/menu-sales-reports.png", false, "sales", "payment"),
				entry("Workflow.TaxSummary", "com.openbravo.pos.reports.JPanelTaxSummary",
						"/com/openbravo/images/menu-taxes-report.png", false, "tax", "vat", "iva"),
				entry("Workflow.CashClosing", "com.openbravo.pos.reports.JPanelCashClosing",
						"/com/openbravo/images/menu-cash-closed.png", true, "cash", "closing", "close", "caixa")));
		result.add(category("Workflow.Category.ShopTeam",
				entry("Workflow.Users", "com.openbravo.pos.admin.PeoplePanel", "/com/openbravo/images/menu-users.png",
						false, "users", "people", "usuaris"),
				entry("Workflow.Roles", "com.openbravo.pos.admin.RolesPanel", "/com/openbravo/images/menu-roles.png",
						false, "roles", "rols"),
				entry("Workflow.ChangePassword", "Menu.ChangePassword",
						"/com/openbravo/images/menu-change-password.png", false, "password", "contrasenya")));
		result.add(category("Workflow.Category.System",
				entry("Workflow.Resources", "com.openbravo.pos.admin.ResourcesPanel",
						"/com/openbravo/images/menu-resources.png", false, "resources"),
				entry("Workflow.Configuration", "com.openbravo.pos.config.JPanelConfiguration",
						"/com/openbravo/images/menu-configuration.png", false, "configuration", "settings",
						"configuracio"),
				entry("Workflow.Printer", "com.openbravo.pos.panels.JPanelPrinter",
						"/com/openbravo/images/menu-printer.png", false, "printer", "impressora"),
				entry("Workflow.DatabaseBackup", "com.openbravo.pos.admin.BackupDatabaseAction",
						"/com/openbravo/images/ark2.png", false, "backup", "database")));
		return result;
	}

	static List<String> workflowTaskNames() {
		List<String> names = new ArrayList<String>();
		for (WorkflowCategory category : catalogue()) {
			for (WorkflowEntry entry : category.entries) {
				if (!names.contains(entry.taskName)) {
					names.add(entry.taskName);
				}
			}
		}
		return names;
	}

	private static WorkflowCategory category(String key, WorkflowEntry... entries) {
		return new WorkflowCategory(key, Arrays.asList(entries));
	}

	private static WorkflowEntry entry(String key, String task, String icon, boolean common, String... synonyms) {
		return new WorkflowEntry(key, key + ".Hint", task, icon, common, Arrays.asList(synonyms));
	}

	@Override
	public String getTitle() {
		return null;
	}

	@Override
	public void activate() throws BasicException {
		greetingTitle.setText(AppLocal.getIntString("Workflow.Greeting", firstName()));
		search.setText("");
		SwingUtilities.invokeLater(() -> search.requestFocusInWindow());
	}

	private String firstName() {
		String name = app.getAppUserView().getUser().getName().trim();
		int separator = name.indexOf(' ');
		return separator < 0 ? name : name.substring(0, separator);
	}

	@Override
	public boolean deactivate() {
		return true;
	}

	@Override
	public javax.swing.JComponent getComponent() {
		return this;
	}

	private static final class WorkflowCategory {
		private final String labelKey;
		private final List<WorkflowEntry> entries;

		private WorkflowCategory(String labelKey, List<WorkflowEntry> entries) {
			this.labelKey = labelKey;
			this.entries = entries;
		}
	}

	private static final class WorkflowEntry {
		private final String labelKey;
		private final String hintKey;
		private final String taskName;
		private final String icon;
		private final boolean common;
		private final List<String> synonyms;

		private WorkflowEntry(String labelKey, String hintKey, String taskName, String icon, boolean common,
				List<String> synonyms) {
			this.labelKey = labelKey;
			this.hintKey = hintKey;
			this.taskName = taskName;
			this.icon = icon;
			this.common = common;
			this.synonyms = synonyms;
		}

		private boolean matches(String query) {
			if (AppLocal.getIntString(labelKey).toLowerCase(Locale.ROOT).contains(query)
					|| AppLocal.getIntString(hintKey).toLowerCase(Locale.ROOT).contains(query)) {
				return true;
			}
			for (String synonym : synonyms) {
				if (synonym.contains(query)) {
					return true;
				}
			}
			return false;
		}
	}

	private static final class WorkflowButton extends JButton {
		private WorkflowButton(WorkflowEntry entry, javax.swing.Action action, boolean common) {
			setAction(action);
			setText(null);
			setFocusable(true);
			setRequestFocusEnabled(true);
			setHorizontalAlignment(SwingConstants.LEADING);
			setVerticalAlignment(common ? SwingConstants.CENTER : SwingConstants.TOP);
			setLayout(common ? new BorderLayout(12, 0) : new BorderLayout(0, 7));
			setBorder(BorderFactory.createCompoundBorder(new RoundedLineBorder(BORDER, CARD_RADIUS), BorderFactory
					.createEmptyBorder(common ? 16 : 6, common ? 16 : 8, common ? 16 : 6, common ? 16 : 8)));
			setBackground(SURFACE);
			setOpaque(false);
			setMargin(new Insets(0, 0, 0, 0));
			JLabel iconLabel = new JLabel(new HiDpiIcon(JPanelWelcome.class.getResource(entry.icon)));
			add(iconLabel, common ? BorderLayout.WEST : BorderLayout.NORTH);
			JPanel text = new JPanel();
			text.setOpaque(false);
			text.setLayout(new javax.swing.BoxLayout(text, javax.swing.BoxLayout.Y_AXIS));
			JLabel title = label(AppLocal.getIntString(entry.labelKey), common ? 16 : 14, Font.BOLD, TEXT);
			text.add(title);
			if (common) {
				text.add(label(AppLocal.getIntString(entry.hintKey), 13, Font.PLAIN, MUTED));
			}
			add(text, BorderLayout.CENTER);
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g2 = (Graphics2D) graphics.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(getBackground());
			g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, CARD_RADIUS, CARD_RADIUS);
			g2.clip(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), CARD_RADIUS, CARD_RADIUS));
			super.paintComponent(g2);
			g2.dispose();
		}

		private JButton asLink() {
			JButton link = new JButton(getAction());
			link.setText("\u203a  " + getAction().getValue(javax.swing.Action.NAME));
			link.setIcon(null);
			link.setDisabledIcon(null);
			link.setFont(link.getFont().deriveFont(Font.PLAIN, 14f));
			link.setForeground(TEXT);
			link.setHorizontalAlignment(SwingConstants.LEADING);
			link.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
			link.setContentAreaFilled(false);
			link.setFocusPainted(true);
			link.setMargin(new Insets(0, 0, 0, 0));
			return link;
		}
	}

	private static final class RoundedLineBorder extends AbstractBorder {
		private final Color color;
		private final int radius;

		private RoundedLineBorder(Color color, int radius) {
			this.color = color;
			this.radius = radius;
		}

		@Override
		public void paintBorder(Component component, Graphics graphics, int x, int y, int width, int height) {
			Graphics2D g2 = (Graphics2D) graphics.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(color);
			g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
			g2.dispose();
		}

		@Override
		public Insets getBorderInsets(Component component) {
			return new Insets(1, 1, 1, 1);
		}
	}
}
