package com.openbravo.pos.inventory;

import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.AppView;
import com.openbravo.pos.forms.BeanFactoryApp;
import com.openbravo.pos.forms.BeanFactoryException;
import com.openbravo.pos.forms.DataLogicSales;
import com.openbravo.pos.forms.JPanelView;
import com.openbravo.pos.forms.MenuDefinition;
import com.openbravo.pos.forms.MenuItemDefinition;
import com.openbravo.pos.theme.RetailPOSColors;
import com.openbravo.pos.theme.RetailPOSTheme;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Start with a product or with the job to be done, while retaining every stock
 * menu entry.
 */
public final class JPanelStockWelcome extends JPanel implements JPanelView, BeanFactoryApp {
	private static final String PREFIX = "com.openbravo.pos.inventory.";
	private static final String REPLENISHMENT = PREFIX + "ReplenishmentPanel";
	private final MenuDefinition menu;
	private final JTextField search = new JTextField();
	private final JPanel results = new JPanel();
	private final JPanel queue = new JPanel();
	private JPanel jobsHeading;
	private JPanel jobGrid;
	private JScrollPane scroll;
	private Timer debounce;
	private boolean built;
	private AppView app;
	private int generation;
	private List<StockWelcomeRepository.Product> matches;
	private boolean knownToImport;
	private static final class SearchResult {
		final List<StockWelcomeRepository.Product> products;
		final boolean importKnown;
		SearchResult(List<StockWelcomeRepository.Product> products, boolean importKnown) {
			this.products = products;
			this.importKnown = importKnown;
		}
	}

	public JPanelStockWelcome(AppView app, MenuDefinition menu) {
		this.app = app;
		this.menu = menu;
	}

	private void build() {
		built = true;
		setLayout(new BorderLayout(0, 12));
		setBackground(RetailPOSColors.surface0());
		setBorder(BorderFactory.createEmptyBorder(16, 28, 20, 28));

		JPanel header = new JPanel(new BorderLayout(12, 0));
		header.setOpaque(false);
		JPanel heading = column();
		heading.add(label("Menu.StockManagement", 12, true));
		heading.add(label("stock.welcome.heading", 26, true));
		header.add(heading, BorderLayout.CENTER);
		JButton all = button("stock.welcome.allScreens",
				() -> scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum()));
		header.add(all, BorderLayout.EAST);
		add(header, BorderLayout.NORTH);

		JPanel page = new ScrollPage();
		page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));
		page.setOpaque(false);
		search.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(17f));
		search.setBackground(RetailPOSColors.surface200());
		search.setForeground(RetailPOSColors.ink());
		search.setBorder(
				BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(RetailPOSColors.borderStrong()),
						BorderFactory.createEmptyBorder(12, 14, 12, 14)));
		search.putClientProperty("JTextField.placeholderText", tr("stock.welcome.search"));
		search.setToolTipText(tr("stock.welcome.search"));
		search.setMaximumSize(new Dimension(Integer.MAX_VALUE, search.getPreferredSize().height));
		page.add(search);
		jobsHeading = section("stock.welcome.jobs");
		page.add(jobsHeading);
		String[][] jobs = {{"stock.welcome.new", "ProductsPanel"}, {"stock.welcome.sale", "SaleMarkPanel"},
				{"stock.welcome.receive", "StockDiaryPanel"}, {"stock.welcome.correct", "StockDiaryPanel"},
				{"stock.welcome.priceRules", "PriceRulesPanel"}, {"stock.welcome.organize", "CategoriesPanel"}};
		jobGrid = grid(2, 12);
		for (String[] job : jobs) {
			Action destination = action(PREFIX + job[1]);
			if (destination != null) {
				JButton card = button(job[0], () -> {
					if ("stock.welcome.new".equals(job[0]))
						createProduct();
					else
						destination.actionPerformed(null);
				});
				card.setFont(RetailPOSTheme.MANROPE_SEMIBOLD.deriveFont(15f));
				jobGrid.add(card);
			}
		}
		page.add(jobGrid);
		results.setLayout(new BoxLayout(results, BoxLayout.Y_AXIS));
		results.setOpaque(false);
		results.setAlignmentX(Component.LEFT_ALIGNMENT);
		page.add(results);
		queue.setLayout(new BoxLayout(queue, BoxLayout.Y_AXIS));
		queue.setOpaque(false);
		queue.setAlignmentX(Component.LEFT_ALIGNMENT);
		page.add(queue);
		page.add(section("stock.welcome.screens"));
		JPanel entries = grid(2, 8);
		for (int i = 0; i < menu.countMenuElements(); i++) {
			if (menu.getMenuElement(i) instanceof MenuItemDefinition) {
				Action entry = ((MenuItemDefinition) menu.getMenuElement(i)).getAction();
				if (action((String) entry.getValue(com.openbravo.pos.forms.AppUserView.ACTION_TASKNAME)) != null) {
					JButton link = new JButton(entry);
					style(link);
					entries.add(link);
				}
			}
		}
		page.add(entries);
		page.add(Box.createVerticalGlue());
		scroll = new JScrollPane(page);
		scroll.setBorder(null);
		scroll.setOpaque(false);
		scroll.getViewport().setOpaque(false);
		scroll.getVerticalScrollBar().setUnitIncrement(18);
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		add(scroll, BorderLayout.CENTER);

		debounce = new Timer(300, e -> lookup());
		debounce.setRepeats(false);
		search.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				changed();
			}
			@Override
			public void removeUpdate(DocumentEvent e) {
				changed();
			}
			@Override
			public void changedUpdate(DocumentEvent e) {
				changed();
			}
		});
		search.addActionListener(e -> {
			debounce.stop();
			if (matches != null && !matches.isEmpty()) {
				openProduct(matches.get(0));
			} else {
				lookup(true);
			}
		});
		getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ESCAPE"), "clear");
		getActionMap().put("clear", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				search.setText("");
				search.requestFocusInWindow();
			}
		});
	}

	@Override
	public void init(AppView app) throws BeanFactoryException {
		this.app = app;
	}
	@Override
	public Object getBean() {
		return this;
	}
	@Override
	public JComponent getComponent() {
		if (!built)
			build();
		return this;
	}
	@Override
	public String getTitle() {
		return null;
	}
	@Override
	public void activate() throws BasicException {
		SwingUtilities.invokeLater(() -> {
			scroll.getVerticalScrollBar().setValue(0);
			search.requestFocusInWindow();
		});
		if (!search.getText().trim().isEmpty())
			lookup();
		else
			checkCatalogue();
		loadQueue();
	}
	@Override
	public boolean deactivate() {
		generation++;
		debounce.stop();
		return true;
	}

	private void changed() {
		generation++;
		matches = null;
		debounce.stop();
		boolean empty = search.getText().trim().isEmpty();
		jobsHeading.setVisible(empty);
		jobGrid.setVisible(empty);
		results.removeAll();
		if (empty) {
			checkCatalogue();
		} else {
			results.add(text(tr("stock.welcome.searching")));
			debounce.restart();
		}
		results.revalidate();
		results.repaint();
	}

	private void lookup() {
		lookup(false);
	}

	private void lookup(boolean openFirst) {
		String text = search.getText().trim();
		if (text.isEmpty())
			return;
		int request = ++generation;
		new SwingWorker<SearchResult, Void>() {
			@Override
			protected SearchResult doInBackground() throws Exception {
				List<StockWelcomeRepository.Product> found = new StockWelcomeRepository(app).search(text);
				boolean known = found.isEmpty() && text.matches("[^\\s]{6,30}")
						&& app.getBean(DataLogicSales.class).getCatalogProductByCode(text, null, null) != null;
				return new SearchResult(found, known);
			}
			@Override
			protected void done() {
				if (request != generation)
					return;
				try {
					SearchResult result = get();
					matches = result.products;
					knownToImport = result.importKnown;
					showMatches(text);
					if (openFirst && !matches.isEmpty())
						openProduct(matches.get(0));
				} catch (Exception ex) {
					showError();
				}
			}
		}.execute();
	}

	private void showMatches(String text) {
		results.removeAll();
		results.add(section("stock.welcome.results"));
		if (matches.isEmpty()) {
			results.add(text(
					tr(knownToImport ? "stock.welcome.importKnown" : "stock.welcome.unknown") + " «" + text + "»"));
			Action products = action(PREFIX + "ProductsPanel");
			if (products != null)
				results.add(button("stock.welcome.create", this::createProduct));
			if (knownToImport && products != null)
				results.add(button("stock.welcome.import", () -> importProduct(text)));
			Action replenishment = action(REPLENISHMENT);
			if (replenishment != null)
				results.add(button("stock.welcome.note", () -> manualRequest(text)));
		} else {
			for (StockWelcomeRepository.Product product : matches)
				results.add(productCard(product));
		}
		results.revalidate();
		results.repaint();
	}

	private JPanel productCard(StockWelcomeRepository.Product product) {
		JPanel card = column();
		card.setBackground(RetailPOSColors.surface100());
		card.setOpaque(true);
		card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(RetailPOSColors.border()),
				BorderFactory.createEmptyBorder(12, 16, 12, 16)));
		card.add(text(product.name + " · " + product.reference + " · " + product.code));
		card.add(fact("stock.welcome.price", NumberFormat.getCurrencyInstance().format(product.price), "ProductsPanel",
				product));
		if (product.salePercent > 0)
			card.add(fact("stock.welcome.markdown", product.salePercent + "%", "SaleMarkPanel"));
		card.add(fact("stock.welcome.category", product.category, "CategoriesPanel"));
		card.add(fact("stock.welcome.tax", product.tax, "TaxPanel"));
		JPanel stock = fact("stock.welcome.units", NumberFormat.getNumberInstance().format(product.units)
				+ (product.units < 0 ? " · " + tr("stock.welcome.negative") : ""), "StockDiaryPanel");
		if (product.units < 0)
			stock.getComponent(1).setForeground(RetailPOSColors.danger());
		card.add(stock);
		if (product.lastMovement != null)
			card.add(fact("stock.welcome.lastMovement",
					DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(product.lastMovement),
					"StockDiaryPanel"));
		if (product.brand != null)
			card.add(fact("stock.welcome.brand", product.brand + (product.rule == null ? "" : " · " + product.rule),
					"PriceRulesPanel"));
		if (product.status != null && action(REPLENISHMENT) != null) {
			card.add(button("stock.welcome.onList", () -> openReplenishmentProduct(product.code)));
			card.add(text(tr("Replenishment." + ("ORDERED".equals(product.status) ? "Ordered" : "Pending"))
					+ (product.customer == null ? "" : " · " + product.customer)));
		}
		return card;
	}

	private JPanel fact(String key, String value, String destination) {
		return fact(key, value, destination, null);
	}

	private JPanel fact(String key, String value, String destination, StockWelcomeRepository.Product product) {
		JPanel row = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 0));
		row.setOpaque(false);
		Action target = action(PREFIX + destination);
		if (target != null)
			row.add(button(key, () -> {
				if (product != null)
					openProduct(product);
				else
					target.actionPerformed(null);
			}));
		else
			row.add(label(key, 14, false));
		JLabel figure = text(value);
		figure.setFont(RetailPOSTheme.PLEX_MONO_REGULAR.deriveFont(14f));
		row.add(figure);
		return row;
	}

	private void loadQueue() {
		if (action(REPLENISHMENT) == null)
			return;
		new SwingWorker<StockWelcomeRepository.Queue, Void>() {
			@Override
			protected StockWelcomeRepository.Queue doInBackground() throws SQLException {
				return new StockWelcomeRepository(app).queue();
			}
			@Override
			protected void done() {
				try {
					StockWelcomeRepository.Queue data = get();
					queue.removeAll();
					queue.add(section("stock.welcome.replenishment"));
					if (data.pending + data.ordered + data.customers == 0)
						queue.add(text(tr("stock.welcome.none")));
					else {
						JPanel figures = grid(3, 8);
						JButton pending = button("stock.welcome.pending", () -> openQueue("PENDING"), data.pending);
						if (data.oldest != null)
							pending.setToolTipText(tr("stock.welcome.oldest") + " "
									+ DateFormat.getDateInstance(DateFormat.SHORT).format(data.oldest));
						figures.add(pending);
						figures.add(button("stock.welcome.ordered", () -> openQueue("ORDERED"), data.ordered));
						figures.add(button("stock.welcome.customers", () -> openQueue("ENCARGO"), data.customers));
						queue.add(figures);
					}
					queue.add(button("stock.welcome.openList", () -> open(REPLENISHMENT)));
					queue.revalidate();
					queue.repaint();
				} catch (Exception e) {
					queue.removeAll();
					queue.add(section("stock.welcome.replenishment"));
					queue.add(text(tr("stock.welcome.error")));
					queue.add(button("stock.welcome.openList", () -> open(REPLENISHMENT)));
					queue.revalidate();
					queue.repaint();
				}
			}
		}.execute();
	}

	private void checkCatalogue() {
		int request = generation;
		new SwingWorker<Boolean, Void>() {
			@Override
			protected Boolean doInBackground() throws SQLException {
				return new StockWelcomeRepository(app).emptyCatalogue();
			}
			@Override
			protected void done() {
				if (request != generation || !search.getText().trim().isEmpty())
					return;
				try {
					results.removeAll();
					if (get())
						results.add(text(tr("stock.welcome.empty")));
					results.revalidate();
					results.repaint();
				} catch (Exception e) {
					showError();
				}
			}
		}.execute();
	}

	private void showError() {
		results.removeAll();
		results.add(text(tr("stock.welcome.error")));
		results.revalidate();
		results.repaint();
	}
	private Action action(String task) {
		return app.getAppUserView().getTaskAction(task);
	}
	private void open(String task) {
		Action target = action(task);
		if (target != null)
			target.actionPerformed(null);
	}
	private void createProduct() {
		if (action(PREFIX + "ProductsPanel") == null)
			return;
		open(PREFIX + "ProductsPanel");
		try {
			app.getBean(ProductsPanel.class).createNewProduct();
		} catch (BeanFactoryException | BasicException e) {
			new MessageInf(e).show(this);
		}
	}
	private void openProduct(StockWelcomeRepository.Product product) {
		if (action(PREFIX + "ProductsPanel") == null)
			return;
		open(PREFIX + "ProductsPanel");
		try {
			app.getBean(ProductsPanel.class).showProduct(product.code);
		} catch (BeanFactoryException e) {
			new MessageInf(e).show(this);
		}
	}
	private void openQueue(String filter) {
		openReplenishment(panel -> panel.showQueue(filter));
	}
	private void openReplenishmentProduct(String code) {
		openReplenishment(panel -> panel.showProductOnList(code));
	}
	private void manualRequest(String code) {
		openReplenishment(panel -> panel.startManualEntry(code));
	}
	private void openReplenishment(java.util.function.Consumer<ReplenishmentPanel> select) {
		if (action(REPLENISHMENT) == null)
			return;
		open(REPLENISHMENT);
		try {
			select.accept(app.getBean(ReplenishmentPanel.class));
		} catch (BeanFactoryException e) {
			new MessageInf(e).show(this);
		}
	}
	private void importProduct(String code) {
		try {
			if (CatalogImportDialog.forStock(this, app, app.getBean(DataLogicSales.class)).importIfAbsent(code) != null)
				lookup();
		} catch (BasicException | BeanFactoryException e) {
			new MessageInf(e).show(this);
		}
	}
	private static String tr(String key) {
		return AppLocal.getIntString(key);
	}
	private static JPanel column() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setOpaque(false);
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		return panel;
	}
	private static JPanel grid(int columns, int gap) {
		JPanel panel = new JPanel(new GridLayout(0, columns, gap, gap));
		panel.setOpaque(false);
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		return panel;
	}

	private static final class ScrollPage extends JPanel implements Scrollable {
		@Override
		public Dimension getPreferredScrollableViewportSize() {
			return getPreferredSize();
		}
		@Override
		public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
			return 16;
		}
		@Override
		public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
			return visible.height - 16;
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
	private static JLabel text(String value) {
		JLabel label = new JLabel(value);
		label.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(14f));
		label.setForeground(RetailPOSColors.ink());
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		return label;
	}
	private static JLabel label(String key, int size, boolean bold) {
		JLabel label = text(tr(key));
		label.setFont((bold ? RetailPOSTheme.MANROPE_BOLD : RetailPOSTheme.MANROPE_MEDIUM).deriveFont((float) size));
		return label;
	}
	private static JPanel section(String key) {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setOpaque(false);
		panel.setBorder(BorderFactory.createEmptyBorder(18, 0, 8, 0));
		panel.add(label(key, 13, true));
		return panel;
	}
	private static JButton button(String key, Runnable action) {
		return button(key, action, -1);
	}
	private static JButton button(String key, Runnable action, int number) {
		JButton button = new JButton((number < 0 ? "" : number + "  ") + tr(key));
		style(button);
		button.addActionListener(e -> action.run());
		return button;
	}
	private static void style(JButton button) {
		button.setFont(RetailPOSTheme.MANROPE_SEMIBOLD.deriveFont(14f));
		button.setBackground(RetailPOSColors.surface100());
		button.setForeground(RetailPOSColors.ink());
		button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(RetailPOSColors.border()),
				BorderFactory.createEmptyBorder(14, 14, 14, 14)));
		button.setHorizontalAlignment(JButton.LEADING);
	}
}
