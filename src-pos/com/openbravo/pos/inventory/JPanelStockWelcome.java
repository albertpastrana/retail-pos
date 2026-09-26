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
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.Path2D;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.JTextArea;
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
	private JLabel searchHint;
	private JPanel entries;
	private RoundedSurface searchWell;
	private JScrollPane scroll;
	private Timer debounce;
	private boolean built;
	private AppView app;
	private int generation;
	private List<StockWelcomeRepository.Product> matches;
	private StockWelcomeRepository.Queue latestQueue;
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
		setLayout(new BorderLayout(0, 16));
		setBackground(RetailPOSColors.surface0());
		setBorder(BorderFactory.createEmptyBorder(20, 28, 24, 28));

		JPanel header = new JPanel(new BorderLayout(12, 0));
		header.setOpaque(false);
		JPanel heading = column();
		JLabel eyebrow = label("Menu.StockManagement", 12, true);
		eyebrow.setForeground(RetailPOSColors.inkMuted());
		heading.add(eyebrow);
		heading.add(Box.createVerticalStrut(4));
		heading.add(label("stock.welcome.heading", 26, true));
		header.add(heading, BorderLayout.CENTER);
		JButton all = button("stock.welcome.allScreens",
				() -> scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum()));
		all.setIcon(new LineIcon("down", RetailPOSColors.inkMuted(), 16));
		all.setHorizontalTextPosition(JButton.LEFT);
		all.setIconTextGap(12);
		header.add(all, BorderLayout.EAST);
		add(header, BorderLayout.NORTH);

		JPanel page = new ScrollPage();
		page.setLayout(new GridBagLayout());
		page.setOpaque(false);
		int row = 0;
		searchWell = new RoundedSurface(12, RetailPOSColors.surface200());
		searchWell.setLayout(new BorderLayout(12, 0));
		searchWell.setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 18));
		searchWell.setPreferredSize(new Dimension(0, 64));
		searchWell.add(new JLabel(new LineIcon("scan", RetailPOSColors.ink(), 24)), BorderLayout.WEST);
		search.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(17f));
		search.setOpaque(false);
		search.setForeground(RetailPOSColors.ink());
		search.setBorder(BorderFactory.createEmptyBorder());
		search.putClientProperty("JTextField.placeholderText", tr("stock.welcome.search"));
		search.setToolTipText(tr("stock.welcome.search"));
		search.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent event) {
				searchWell.repaint();
			}
			@Override
			public void focusLost(FocusEvent event) {
				searchWell.repaint();
			}
		});
		searchWell.add(search, BorderLayout.CENTER);
		JPanel scanBlock = new JPanel(new BorderLayout(0, 6));
		scanBlock.setOpaque(false);
		scanBlock.add(searchWell, BorderLayout.CENTER);
		searchHint = label("stock.welcome.searchHint", 12, false);
		searchHint.setForeground(RetailPOSColors.inkMuted());
		scanBlock.add(searchHint, BorderLayout.SOUTH);
		addRow(page, scanBlock, row++);
		jobsHeading = section("stock.welcome.jobs");
		addRow(page, jobsHeading, row++);
		String[][] jobs = {{"stock.welcome.new", "ProductsPanel"}, {"stock.welcome.sale", "SaleMarkPanel"},
				{"stock.welcome.receive", "StockDiaryPanel"}, {"stock.welcome.correct", "StockDiaryPanel"},
				{"stock.welcome.priceRules", "PriceRulesPanel"}, {"stock.welcome.organize", "CategoriesPanel"}};
		jobGrid = grid(3, 12, 480, 860);
		for (String[] job : jobs) {
			Action destination = action(PREFIX + job[1]);
			if (destination != null) {
				JButton card = jobCard(job[0], () -> {
					if ("stock.welcome.new".equals(job[0]))
						createProduct();
					else
						destination.actionPerformed(null);
				});
				jobGrid.add(card);
			}
		}
		addRow(page, jobGrid, row++);
		results.setLayout(new GridBagLayout());
		results.setOpaque(false);
		results.setAlignmentX(Component.LEFT_ALIGNMENT);
		addRow(page, results, row++);
		queue.setLayout(new GridBagLayout());
		queue.setOpaque(false);
		queue.setAlignmentX(Component.LEFT_ALIGNMENT);
		addRow(page, queue, row++);
		addRow(page, section("stock.welcome.screens"), row++);
		entries = grid(6, 8, 510, 1480);
		for (int i = 0; i < menu.countMenuElements(); i++) {
			if (menu.getMenuElement(i) instanceof MenuItemDefinition) {
				Action entry = ((MenuItemDefinition) menu.getMenuElement(i)).getAction();
				if (action((String) entry.getValue(com.openbravo.pos.forms.AppUserView.ACTION_TASKNAME)) != null) {
					JButton link = navigationButton(entry);
					entries.add(link);
				}
			}
		}
		addRow(page, entries, row++);
		GridBagConstraints fill = new GridBagConstraints();
		fill.gridx = 0;
		fill.gridy = row;
		fill.weightx = 1;
		fill.weighty = 1;
		fill.fill = GridBagConstraints.BOTH;
		JPanel spacer = new JPanel();
		spacer.setOpaque(false);
		page.add(spacer, fill);
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
		searchHint.setVisible(empty);
		jobsHeading.setVisible(empty);
		jobGrid.setVisible(empty);
		results.removeAll();
		if (empty) {
			if (latestQueue != null)
				renderQueue(latestQueue);
			else {
				queue.removeAll();
				queue.revalidate();
				queue.repaint();
			}
			checkCatalogue();
		} else {
			showResultJobs();
			resultRow(text(tr("stock.welcome.searching")));
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
					renderMatches(text, result.products, result.importKnown);
					if (openFirst && !matches.isEmpty())
						openProduct(matches.get(0));
				} catch (Exception ex) {
					showError();
				}
			}
		}.execute();
	}

	void renderMatches(String text, List<StockWelcomeRepository.Product> products, boolean inImportCatalogue) {
		if (!search.getText().equals(text)) {
			search.setText(text);
			debounce.stop();
		}
		matches = products;
		knownToImport = inImportCatalogue;
		jobsHeading.setVisible(false);
		jobGrid.setVisible(false);
		showResultJobs();
		showMatches(text);
	}

	private void showMatches(String text) {
		results.removeAll();
		if (matches.size() != 1)
			resultRow(section("stock.welcome.results"));
		if (matches.isEmpty()) {
			resultRow(text(
					tr(knownToImport ? "stock.welcome.importKnown" : "stock.welcome.unknown") + " «" + text + "»"));
			JPanel choices = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 0));
			choices.setOpaque(false);
			Action products = action(PREFIX + "ProductsPanel");
			if (products != null)
				choices.add(button("stock.welcome.create", this::createProduct));
			if (knownToImport && products != null)
				choices.add(button("stock.welcome.import", () -> importProduct(text)));
			Action replenishment = action(REPLENISHMENT);
			if (replenishment != null)
				choices.add(button("stock.welcome.note", () -> manualRequest(text)));
			resultRow(choices);
		} else {
			for (StockWelcomeRepository.Product product : matches)
				resultRow(productCard(product));
		}
		results.revalidate();
		results.repaint();
	}

	private void resultRow(JComponent component) {
		addRow(results, component, results.getComponentCount());
	}

	private JPanel productCard(StockWelcomeRepository.Product product) {
		JPanel card = new RoundedSurface(32, RetailPOSColors.surface100());
		card.setLayout(new GridBagLayout());
		card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
		int row = 0;
		JPanel header = new JPanel(new BorderLayout(12, 0));
		header.setOpaque(false);
		JPanel identity = new JPanel(new GridBagLayout());
		identity.setOpaque(false);
		JPanel badges = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 0));
		badges.setOpaque(false);
		if (product.salePercent > 0)
			badges.add(new Badge(tr("stock.welcome.discounted") + " " + percent(product.salePercent / 100),
					RetailPOSColors.warning()));
		if (product.status != null && action(REPLENISHMENT) != null)
			badges.add(new Badge(tr("stock.welcome.onList"), RetailPOSColors.info()));
		int identityRow;
		if (getWidth() >= 1300) {
			JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 0));
			titleRow.setOpaque(false);
			JLabel title = text(product.name);
			title.setFont(RetailPOSTheme.MANROPE_BOLD.deriveFont(21f));
			titleRow.add(title);
			for (Component badge : badges.getComponents())
				titleRow.add(badge);
			addRow(identity, titleRow, 0);
			identityRow = 1;
		} else {
			JTextArea title = wrapped(product.name, RetailPOSTheme.MANROPE_BOLD.deriveFont(21f), RetailPOSColors.ink());
			addRow(identity, title, 0);
			identityRow = 1;
			if (badges.getComponentCount() > 0)
				addRow(identity, badges, identityRow++);
		}
		String metadata = tr("stock.welcome.reference") + " " + product.reference + " · " + tr("stock.welcome.barcode")
				+ " " + product.code
				+ (product.brand == null ? "" : " · " + tr("stock.welcome.brandName") + " " + product.brand);
		JTextArea details = wrapped(metadata, RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f),
				RetailPOSColors.inkMuted());
		addRow(identity, details, identityRow);
		header.add(identity, BorderLayout.CENTER);
		if (action(PREFIX + "ProductsPanel") != null) {
			JButton openProduct = button("stock.welcome.openProduct", () -> openProduct(product));
			openProduct.setIcon(new LineIcon("next", RetailPOSColors.inkMuted(), 16));
			openProduct.setHorizontalTextPosition(JButton.LEFT);
			JPanel openAtTop = new JPanel(new BorderLayout());
			openAtTop.setOpaque(false);
			openAtTop.add(openProduct, BorderLayout.NORTH);
			header.add(openAtTop, BorderLayout.EAST);
		}
		addRow(card, header, row++);

		JPanel facets = grid(5, 10, 510, 1340);
		facets.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));
		boolean markdown = product.salePercent > 0;
		String priceHint = markdown
				? tr("stock.welcome.before") + " " + NumberFormat.getCurrencyInstance().format(product.price)
				: "";
		facets.add(facet("stock.welcome.price",
				NumberFormat.getCurrencyInstance().format(product.price * (1 - product.salePercent / 100)), priceHint,
				"stock.welcome.changePrice", "ProductsPanel", () -> openProduct(product),
				RetailPOSTheme.PLEX_MONO_SEMIBOLD.deriveFont(21f), RetailPOSColors.ink()));
		facets.add(facet("stock.welcome.markdown",
				markdown
						? tr("stock.welcome.activeMarkdown") + " −" + percent(product.salePercent / 100)
						: tr("stock.welcome.noMarkdown"),
				"", "stock.welcome.manageMarkdowns", "SaleMarkPanel",
				() -> open("com.openbravo.pos.inventory.SaleMarkPanel"), RetailPOSTheme.MANROPE_BOLD.deriveFont(16f),
				markdown ? RetailPOSColors.warning() : RetailPOSColors.ink()));
		facets.add(facet("stock.welcome.category",
				product.categoryParent == null ? product.category : product.categoryParent,
				product.categoryParent == null ? "" : product.category, "stock.welcome.changeCategory",
				"CategoriesPanel", () -> open(PREFIX + "CategoriesPanel"), RetailPOSTheme.MANROPE_BOLD.deriveFont(17f),
				RetailPOSColors.ink()));
		facets.add(facet("stock.welcome.vat",
				product.taxRate == null ? tr("stock.welcome.noTaxRate") : percent(product.taxRate), product.tax,
				"stock.welcome.viewTaxes", "TaxPanel", () -> open(PREFIX + "TaxPanel"),
				RetailPOSTheme.PLEX_MONO_SEMIBOLD.deriveFont(19f), RetailPOSColors.ink()));
		facets.add(facet("stock.welcome.units", NumberFormat.getNumberInstance().format(product.units),
				(product.units < 0 ? tr("stock.welcome.negative") + " · " : "") + (product.lastMovement == null
						? tr("stock.welcome.noMovement")
						: tr("stock.welcome.lastMovement") + " "
								+ DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
										.format(product.lastMovement)),
				"stock.welcome.adjustStock", "StockDiaryPanel", () -> open(PREFIX + "StockDiaryPanel"),
				RetailPOSTheme.PLEX_MONO_SEMIBOLD.deriveFont(21f),
				product.units < 0 ? RetailPOSColors.danger() : RetailPOSColors.ink()));
		addRow(card, facets, row++);

		if (product.status != null && action(REPLENISHMENT) != null)
			addRow(card, replenishmentNotice(product), row++);
		if (product.brand != null)
			addRow(card, brandRule(product), row++);
		return card;
	}

	private JComponent facet(String captionKey, String value, String hint, String linkKey, String task,
			Runnable navigate, java.awt.Font valueFont, Color valueColor) {
		boolean permitted = action(PREFIX + task) != null;
		JComponent tile = permitted ? new RoundedButton(20) : new RoundedSurface(20, RetailPOSColors.surface100());
		if (tile instanceof JButton button) {
			style(button);
			button.addActionListener(e -> navigate.run());
			button.getAccessibleContext().setAccessibleName(tr(captionKey) + ": " + value + ". " + tr(linkKey));
		}
		tile.setLayout(new GridBagLayout());
		tile.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
		tile.setPreferredSize(new Dimension(0, 124));
		JLabel caption = label(captionKey, 12, true);
		caption.setForeground(RetailPOSColors.inkMuted());
		caption.setText(caption.getText().toUpperCase(Locale.getDefault()));
		addRow(tile, caption, 0);
		JTextArea amount = wrapped(value, valueFont, valueColor, value.length() > 24 ? 2 : 1);
		addRow(tile, amount, 1);
		if (!hint.isEmpty()) {
			JTextArea note = wrapped(hint, RetailPOSTheme.MANROPE_MEDIUM.deriveFont(12f), RetailPOSColors.inkMuted(),
					hint.length() > 32 ? 2 : 1);
			addRow(tile, note, 2);
		}
		GridBagConstraints filler = new GridBagConstraints();
		filler.gridx = 0;
		filler.gridy = 3;
		filler.weighty = 1;
		tile.add(Box.createVerticalGlue(), filler);
		if (permitted) {
			JPanel link = new JPanel(new BorderLayout(6, 0));
			link.setOpaque(false);
			link.add(label(linkKey, 13, true), BorderLayout.CENTER);
			link.add(new JLabel(new LineIcon("next", RetailPOSColors.inkMuted(), 14)), BorderLayout.EAST);
			addRow(tile, link, 4);
		}
		return tile;
	}

	private JPanel replenishmentNotice(StockWelcomeRepository.Product product) {
		JPanel banner = new RoundedSurface(20, RetailPOSColors.surface200());
		banner.setLayout(new BorderLayout(12, 0));
		banner.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
		banner.add(new JLabel(new LineIcon("box", RetailPOSColors.info(), 20)), BorderLayout.WEST);
		String state = tr("Replenishment." + ("ORDERED".equals(product.status) ? "Ordered" : "Pending"));
		String since = product.replenishmentCreated == null
				? ""
				: " · " + DateFormat.getDateInstance(DateFormat.SHORT).format(product.replenishmentCreated);
		String customer = product.customer == null ? "" : " · " + product.customer;
		banner.add(wrapped(
				tr("stock.welcome.onList") + ": " + state + since + customer + ". " + tr("stock.welcome.noDuplicate"),
				RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f), RetailPOSColors.ink()), BorderLayout.CENTER);
		JButton view = new JButton(tr("stock.welcome.viewList"));
		view.setContentAreaFilled(false);
		view.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		view.setFont(RetailPOSTheme.MANROPE_BOLD.deriveFont(13f));
		view.setForeground(RetailPOSColors.ink());
		view.addActionListener(e -> openReplenishmentProduct(product.code));
		banner.add(view, BorderLayout.EAST);
		return banner;
	}

	private JPanel brandRule(StockWelcomeRepository.Product product) {
		JPanel footer = new JPanel(new BorderLayout(12, 0));
		footer.setOpaque(false);
		footer.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(1, 0, 0, 0, RetailPOSColors.border()),
				BorderFactory.createEmptyBorder(12, 0, 0, 0)));
		String rule = product.ruleMarkup == null
				? ""
				: " · " + tr("stock.welcome.markup") + " " + NumberFormat.getNumberInstance().format(product.ruleMarkup)
						+ "%"
						+ (product.ruleRounding == null
								? ""
								: " · " + tr("stock.welcome.rounding") + " " + roundingLabel(product.ruleRounding));
		footer.add(
				wrapped(tr("stock.welcome.brandName") + " " + product.brand + rule,
						RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f), RetailPOSColors.inkMuted()),
				BorderLayout.CENTER);
		if (action(PREFIX + "PriceRulesPanel") != null) {
			JButton view = new JButton(tr("stock.welcome.viewRule"));
			view.setContentAreaFilled(false);
			view.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
			view.setForeground(RetailPOSColors.ink());
			view.setFont(RetailPOSTheme.MANROPE_BOLD.deriveFont(13f));
			view.addActionListener(e -> open(PREFIX + "PriceRulesPanel"));
			footer.add(view, BorderLayout.EAST);
		}
		return footer;
	}

	private static String percent(double fraction) {
		NumberFormat format = NumberFormat.getPercentInstance();
		format.setMaximumFractionDigits(2);
		return format.format(fraction);
	}

	private static String roundingLabel(String rounding) {
		return switch (rounding) {
			case "NONE", "CHARM", "ALWAYS_95" -> tr("stock.welcome.rounding." + rounding);
			default -> rounding;
		};
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
					renderQueue(get());
				} catch (Exception e) {
					queue.removeAll();
					queueRow(section("stock.welcome.replenishment"));
					queueRow(text(tr("stock.welcome.error")));
					queueRow(button("stock.welcome.openList", () -> open(REPLENISHMENT)));
					queue.revalidate();
					queue.repaint();
				}
			}
		}.execute();
	}

	void renderQueue(StockWelcomeRepository.Queue data) {
		latestQueue = data;
		if (!search.getText().trim().isEmpty()) {
			showResultJobs();
			return;
		}
		queue.removeAll();
		queueRow(section("stock.welcome.replenishment"));
		JPanel figures = new RoundedSurface(32, RetailPOSColors.surface100());
		figures.setLayout(new BorderLayout(12, 0));
		figures.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
		if (data.pending + data.ordered + data.customers == 0) {
			figures.add(text(tr("stock.welcome.none")), BorderLayout.CENTER);
		} else {
			JPanel metrics = grid(3, 0, 560, 820);
			String oldest = data.oldest == null
					? ""
					: tr("stock.welcome.oldest") + " "
							+ DateFormat.getDateInstance(DateFormat.SHORT).format(data.oldest);
			metrics.add(metric("stock.welcome.pending", "box", data.pending, oldest, () -> openQueue("PENDING")));
			metrics.add(metric("stock.welcome.ordered", "truck", data.ordered, tr("stock.welcome.awaiting"),
					() -> openQueue("ORDERED")));
			metrics.add(metric("stock.welcome.customers", "person", data.customers, tr("stock.welcome.customerHint"),
					() -> openQueue("ENCARGO")));
			figures.add(metrics, BorderLayout.CENTER);
		}
		JButton openList = button("stock.welcome.openList", () -> open(REPLENISHMENT));
		openList.setIcon(new LineIcon("next", RetailPOSColors.inkMuted(), 16));
		openList.setHorizontalTextPosition(JButton.LEFT);
		figures.add(openList, BorderLayout.EAST);
		queueRow(figures);
		queue.revalidate();
		queue.repaint();
	}

	private void showResultJobs() {
		queue.removeAll();
		JPanel shortcuts = grid(3, 10, 470, 860);
		String[][] jobs = {{"stock.welcome.new", "ProductsPanel", "new"},
				{"stock.welcome.receive", "StockDiaryPanel", "receive"},
				{"stock.welcome.sale", "SaleMarkPanel", "sale"}};
		for (String[] job : jobs) {
			if (action(PREFIX + job[1]) == null)
				continue;
			JButton shortcut = new RoundedButton(32);
			shortcut.setLayout(new BorderLayout(10, 0));
			style(shortcut);
			shortcut.setPreferredSize(new Dimension(0, 60));
			shortcut.add(new JLabel(new LineIcon(job[2], RetailPOSColors.ink(), 20)), BorderLayout.WEST);
			shortcut.add(label(job[0], 14, true), BorderLayout.CENTER);
			shortcut.getAccessibleContext().setAccessibleName(tr(job[0]));
			shortcut.addActionListener(e -> {
				if ("ProductsPanel".equals(job[1]))
					createProduct();
				else
					open(PREFIX + job[1]);
			});
			shortcuts.add(shortcut);
		}
		if (shortcuts.getComponentCount() > 0) {
			queueRow(section("stock.welcome.jobs"));
			queueRow(shortcuts);
		}
		queue.revalidate();
		queue.repaint();
	}

	private void queueRow(JComponent component) {
		addRow(queue, component, queue.getComponentCount());
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
						resultRow(text(tr("stock.welcome.empty")));
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
		resultRow(text(tr("stock.welcome.error")));
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
	private static JPanel grid(int columns, int gap, int twoColumnWidth, int wideWidth) {
		JPanel panel = new JPanel(new GridLayout(0, Math.min(columns, 2), gap, gap)) {
			private void updateColumns(int width) {
				int count = width < twoColumnWidth
						? 1
						: width >= wideWidth ? columns : columns >= 5 && width >= 940 ? 3 : 2;
				GridLayout layout = (GridLayout) getLayout();
				if (layout.getColumns() != count)
					layout.setColumns(count);
			}
			@Override
			public Dimension getPreferredSize() {
				int width = 0;
				Component ancestor = getParent();
				while (ancestor != null) {
					if (ancestor instanceof JPanelStockWelcome view && view.getWidth() > 0) {
						width = view.getWidth() - view.getInsets().left - view.getInsets().right;
						if (view.scroll != null)
							width -= view.scroll.getVerticalScrollBar().getPreferredSize().width;
						break;
					}
					ancestor = ancestor.getParent();
				}
				ancestor = getParent();
				while (width == 0 && ancestor != null) {
					width = ancestor.getWidth();
					ancestor = ancestor.getParent();
				}
				if (getParent() instanceof RoundedSurface && getParent().getLayout() instanceof BorderLayout layout
						&& width > 0) {
					Component east = layout.getLayoutComponent(BorderLayout.EAST);
					if (east != null)
						width -= east.getPreferredSize().width + 44;
				}
				if (width > 0)
					updateColumns(width);
				return super.getPreferredSize();
			}
			@Override
			public void doLayout() {
				updateColumns(getWidth());
				super.doLayout();
			}
		};
		panel.setOpaque(false);
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		return panel;
	}

	private static void addRow(JComponent page, JComponent child, int row) {
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = row;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.NORTHWEST;
		page.add(child, c);
	}

	private JButton jobCard(String key, Runnable navigate) {
		String name = key.substring("stock.welcome.".length());
		JButton card = new RoundedButton(32);
		card.setLayout(new BorderLayout(12, 0));
		style(card);
		card.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
		card.setPreferredSize(new Dimension(0, 112));
		card.add(topIcon(name, RetailPOSColors.ink(), 22), BorderLayout.WEST);
		JPanel words = new JPanel(new BorderLayout(0, 4));
		words.setOpaque(false);
		JTextArea title = wrapped(tr(key), RetailPOSTheme.MANROPE_BOLD.deriveFont(16f), RetailPOSColors.ink());
		words.add(title, BorderLayout.NORTH);
		JTextArea hint = wrapped(tr(key + ".hint"), RetailPOSTheme.MANROPE_MEDIUM.deriveFont(12f),
				RetailPOSColors.inkMuted());
		words.add(hint, BorderLayout.CENTER);
		card.add(words, BorderLayout.CENTER);
		card.getAccessibleContext().setAccessibleName(tr(key));
		card.addActionListener(e -> navigate.run());
		return card;
	}

	private static JTextArea wrapped(String value, java.awt.Font font, Color color) {
		return wrapped(value, font, color, 2);
	}

	private static JTextArea wrapped(String value, java.awt.Font font, Color color, int lines) {
		JTextArea text = new JTextArea(value) {
			@Override
			public Dimension getPreferredSize() {
				return new Dimension(0, getFontMetrics(getFont()).getHeight() * lines);
			}
			@Override
			public Dimension getMinimumSize() {
				return getPreferredSize();
			}
		};
		text.setFont(font);
		text.setForeground(color);
		text.setOpaque(false);
		text.setEditable(false);
		text.setFocusable(false);
		text.setLineWrap(true);
		text.setWrapStyleWord(true);
		text.setBorder(null);
		text.setAlignmentX(Component.LEFT_ALIGNMENT);
		return text;
	}

	private static JButton metric(String key, String icon, int count, String hint, Runnable navigate) {
		JButton cell = new JButton();
		cell.setLayout(new BorderLayout(10, 0));
		cell.setOpaque(false);
		cell.setContentAreaFilled(false);
		cell.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
		cell.add(topIcon(icon, RetailPOSColors.inkMuted(), 22), BorderLayout.WEST);
		JPanel content = new JPanel(new BorderLayout(0, 2));
		content.setOpaque(false);
		JPanel title = new JPanel(new FlowLayout(FlowLayout.LEADING, 6, 0));
		title.setOpaque(false);
		JLabel number = text(String.valueOf(count));
		number.setFont(RetailPOSTheme.PLEX_MONO_SEMIBOLD.deriveFont(19f));
		title.add(number);
		title.add(label(key, 13, true));
		content.add(title, BorderLayout.NORTH);
		JLabel caption = text(hint);
		caption.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(12f));
		caption.setForeground(RetailPOSColors.inkMuted());
		content.add(caption, BorderLayout.CENTER);
		cell.add(content, BorderLayout.CENTER);
		cell.setMinimumSize(new Dimension(0, 48));
		cell.getAccessibleContext().setAccessibleName(count + " " + tr(key));
		cell.addActionListener(e -> navigate.run());
		return cell;
	}

	private static JPanel topIcon(String icon, Color color, int size) {
		JPanel wrap = new JPanel(new BorderLayout());
		wrap.setOpaque(false);
		wrap.add(new JLabel(new LineIcon(icon, color, size)), BorderLayout.NORTH);
		return wrap;
	}

	private static JButton navigationButton(Action action) {
		JButton link = new RoundedButton(20);
		link.setAction(action);
		style(link);
		String name = String.valueOf(action.getValue(Action.NAME));
		link.setText(null);
		link.setIcon(null);
		link.setLayout(new BorderLayout(8, 0));
		link.add(text(name), BorderLayout.CENTER);
		link.add(new JLabel(new LineIcon("next", RetailPOSColors.inkMuted(), 16)), BorderLayout.EAST);
		link.getAccessibleContext().setAccessibleName(name);
		return link;
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
		JPanel panel = new JPanel(new BorderLayout(12, 0));
		panel.setOpaque(false);
		panel.setBorder(BorderFactory.createEmptyBorder(22, 0, 10, 0));
		JLabel heading = label(key, 12, true);
		heading.setForeground(RetailPOSColors.inkMuted());
		heading.setText(heading.getText().toUpperCase(Locale.getDefault()));
		panel.add(heading, BorderLayout.WEST);
		JPanel rule = new JPanel();
		rule.setBackground(RetailPOSColors.border());
		rule.setPreferredSize(new Dimension(0, 1));
		JPanel line = new JPanel(new GridBagLayout());
		line.setOpaque(false);
		GridBagConstraints constraint = new GridBagConstraints();
		constraint.weightx = 1;
		constraint.fill = GridBagConstraints.HORIZONTAL;
		line.add(rule, constraint);
		panel.add(line, BorderLayout.CENTER);
		return panel;
	}
	private static JButton button(String key, Runnable action) {
		return button(key, action, -1);
	}
	private static JButton button(String key, Runnable action, int number) {
		JButton button = new RoundedButton(20);
		button.setText((number < 0 ? "" : number + "  ") + tr(key));
		style(button);
		button.addActionListener(e -> action.run());
		return button;
	}
	private static void style(JButton button) {
		button.setFont(RetailPOSTheme.MANROPE_SEMIBOLD.deriveFont(14f));
		button.setBackground(RetailPOSColors.surface100());
		button.setForeground(RetailPOSColors.ink());
		button.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
		button.setHorizontalAlignment(JButton.LEADING);
	}

	private class RoundedSurface extends JPanel {
		private final int arc;
		private RoundedSurface(int arc, Color background) {
			this.arc = arc;
			setBackground(background);
			setOpaque(false);
		}
		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(getBackground());
			g.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 3, arc, arc);
			g.setColor(this == searchWell && search.isFocusOwner()
					? RetailPOSColors.borderStrong()
					: RetailPOSColors.border());
			g.setStroke(new BasicStroke(this == searchWell && search.isFocusOwner() ? 2f : 1f));
			g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, arc, arc);
			g.dispose();
			super.paintComponent(graphics);
		}
	}

	private static final class Badge extends JLabel {
		private final Color semantic;
		private Badge(String value, Color semantic) {
			super(value);
			this.semantic = semantic;
			setFont(RetailPOSTheme.MANROPE_BOLD.deriveFont(12f));
			setForeground(semantic);
			setBorder(BorderFactory.createEmptyBorder(4, 9, 4, 9));
		}
		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(new Color(semantic.getRed(), semantic.getGreen(), semantic.getBlue(), 32));
			g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
			g.dispose();
			super.paintComponent(graphics);
		}
	}

	private static final class RoundedButton extends JButton {
		private final int arc;
		private RoundedButton(int arc) {
			this.arc = arc;
			setContentAreaFilled(false);
			setOpaque(false);
			setFocusPainted(false);
		}
		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(getModel().isRollover() ? RetailPOSColors.surface200() : getBackground());
			g.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 3, arc, arc);
			g.dispose();
			super.paintComponent(graphics);
		}
		@Override
		protected void paintBorder(Graphics graphics) {
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(isFocusOwner() ? RetailPOSColors.borderStrong() : RetailPOSColors.border());
			g.setStroke(new BasicStroke(isFocusOwner() ? 2f : 1f));
			g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, arc, arc);
			g.dispose();
		}
	}

	/** The same 24px, 2px rounded-stroke icon family used in the wireframe. */
	private static final class LineIcon implements Icon {
		private final String name;
		private final Color color;
		private final int size;
		private LineIcon(String name, Color color, int size) {
			this.name = name;
			this.color = color;
			this.size = size;
		}
		@Override
		public int getIconWidth() {
			return size;
		}
		@Override
		public int getIconHeight() {
			return size;
		}
		@Override
		public void paintIcon(Component component, Graphics graphics, int x, int y) {
			Graphics2D g = (Graphics2D) graphics.create();
			g.translate(x, y);
			g.scale(size / 24.0, size / 24.0);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(color);
			g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			switch (name) {
				case "scan" -> {
					line(g, 3, 8, 3, 5, 6, 5);
					line(g, 18, 5, 21, 5, 21, 8);
					line(g, 3, 16, 3, 19, 6, 19);
					line(g, 18, 19, 21, 19, 21, 16);
					for (int i : new int[]{7, 10, 14, 17})
						g.drawLine(i, 9, i, 15);
				}
				case "new", "box" -> {
					line(g, 3, 7, 12, 3, 21, 7, 21, 17, 12, 21, 3, 17, 3, 7);
					line(g, 3, 7, 12, 11, 21, 7);
					g.drawLine(12, 11, 12, 21);
				}
				case "sale" -> {
					line(g, 3, 4, 12, 4, 21, 13, 21, 16, 16, 21, 13, 21, 3, 11, 3, 4);
					g.drawOval(6, 7, 2, 2);
				}
				case "receive" -> {
					g.drawRoundRect(3, 5, 18, 15, 2, 2);
					g.drawLine(3, 9, 21, 9);
					g.drawLine(12, 11, 12, 17);
					line(g, 9, 14, 12, 17, 15, 14);
				}
				case "correct" -> {
					g.drawArc(4, 4, 16, 16, 40, 160);
					g.drawArc(4, 4, 16, 16, 220, 160);
					line(g, 17, 4, 20, 6, 20, 10);
					line(g, 7, 20, 4, 18, 4, 14);
				}
				case "priceRules" -> {
					g.drawLine(4, 20, 4, 10);
					g.drawLine(10, 20, 10, 4);
					g.drawLine(16, 20, 16, 13);
					g.drawLine(2, 20, 22, 20);
				}
				case "organize" -> line(g, 3, 6, 10, 6, 12, 8, 21, 8, 21, 19, 3, 19, 3, 6);
				case "truck" -> {
					line(g, 2, 7, 13, 7, 13, 18, 2, 18, 2, 7);
					line(g, 13, 10, 17, 10, 21, 14, 21, 18, 13, 18);
					g.drawOval(5, 17, 3, 3);
					g.drawOval(16, 17, 3, 3);
				}
				case "person" -> {
					g.drawOval(9, 3, 6, 6);
					g.drawArc(4, 11, 16, 14, 0, 180);
				}
				case "next" -> line(g, 9, 5, 16, 12, 9, 19);
				case "down" -> line(g, 5, 9, 12, 16, 19, 9);
				default -> {
				}
			}
			g.dispose();
		}
		private static void line(Graphics2D g, int... coordinates) {
			Path2D path = new Path2D.Double();
			path.moveTo(coordinates[0], coordinates[1]);
			for (int i = 2; i < coordinates.length; i += 2)
				path.lineTo(coordinates[i], coordinates[i + 1]);
			g.draw(path);
		}
	}
}
