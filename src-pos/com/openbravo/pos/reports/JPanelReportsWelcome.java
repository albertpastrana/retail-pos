package com.openbravo.pos.reports;

import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.format.Formats;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.AppView;
import com.openbravo.pos.forms.BeanFactoryApp;
import com.openbravo.pos.forms.BeanFactoryException;
import com.openbravo.pos.forms.JPanelView;
import com.openbravo.pos.forms.MenuDefinition;
import com.openbravo.pos.forms.MenuElement;
import com.openbravo.pos.forms.MenuItemDefinition;
import com.openbravo.pos.theme.RetailPOSColors;
import com.openbravo.pos.theme.RetailPOSTheme;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.List;
import javax.swing.Action;
import javax.swing.border.AbstractBorder;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JToggleButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;

/**
 * Landing screen answering the first sales questions before a report is opened.
 */
public final class JPanelReportsWelcome extends JPanel implements JPanelView, BeanFactoryApp {

	private final MenuDefinition reportsMenu;
	private final SalesSummaryRepository repository = new SalesSummaryRepository();
	private final SalesTrendRepository trends = new SalesTrendRepository();
	private final WelcomeSalesRepository whatSoldRepository = new WelcomeSalesRepository();
	private final JButton[] periodButtons = new JButton[ReportsWelcomePeriod.values().length];
	private final JLabel[] selectedTags = new JLabel[ReportsWelcomePeriod.values().length];
	private final JToggleButton[] segments = new JToggleButton[ReportsWelcomePeriod.values().length];
	private final JLabel[] cardValues = new JLabel[ReportsWelcomePeriod.values().length];
	private final JLabel[] cardDeltas = new JLabel[ReportsWelcomePeriod.values().length];
	private final JLabel[] cardRanges = new JLabel[ReportsWelcomePeriod.values().length];
	private final JLabel[] cardBaselines = new JLabel[ReportsWelcomePeriod.values().length];
	private final JLabel[] cardBaselineValues = new JLabel[ReportsWelcomePeriod.values().length];
	private JButton previous;
	private JButton next;
	private JButton back;
	private JLabel rollingNavigation;
	private JLabel periodTitle;
	private JLabel periodStatus;
	private JLabel periodComparison;
	private JPanel detailHeading;
	private JPanel detailTitleBlock;
	private JPanel detailNavigation;
	private boolean navigationWrapped = true;
	private JLabel totalRange;
	private JLabel totalValue;
	private JLabel totalDelta;
	private JLabel totalBaseline;
	private JLabel totalBaselineValue;
	private JLabel seriesCurrent;
	private JLabel seriesPrevious;
	private JPanel chartHeading;
	private JPanel chartLegend;
	private boolean legendWrapped = true;
	private JLabel today;
	private JLabel updated;
	private JLabel receipts;
	private JLabel receiptsDelta;
	private JLabel average;
	private JLabel averageDelta;
	private JLabel refunds;
	private JLabel refundCount;
	private JLabel chartTitle;
	private JLabel chartComparisonNote;
	private JLabel chartTargetNote;
	private JPanel chartPartialLegend;
	private JLabel chartPartialLabel;
	private SalesTrendChart chart;
	private JPanel whatSold;
	private WelcomeSalesRepository.WelcomeSalesData whatSoldData;
	private int expandedCategory = -1;
	private JLabel loyaltyCount;
	private JLabel loyaltyValue;
	private JLabel loyaltyDelta;
	private SalesSummaryComparison[] loaded;
	private Date asOf = new Date();
	private Date focusAsOf = asOf;
	private int focusGeneration;
	private JScrollPane scroll;
	private AppView app;
	private ReportsWelcomePeriod selected = ReportsWelcomePeriod.MONTH;

	public JPanelReportsWelcome(AppView app, MenuDefinition reportsMenu) {
		this.reportsMenu = reportsMenu;
		initComponents();
		this.app = app;
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
	public javax.swing.JComponent getComponent() {
		return this;
	}

	@Override
	public String getTitle() {
		return null;
	}

	@Override
	public void activate() throws BasicException {
		Date currentDate = new Date();
		asOf = lastClosedDay(currentDate);
		focusAsOf = asOf;
		updateDates();
		updatePeriodNavigation();
		javax.swing.SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(0));
		loadData();
	}

	@Override
	public boolean deactivate() {
		return true;
	}

	private void initComponents() {
		setLayout(new BorderLayout(0, 12));
		setBackground(RetailPOSColors.surface0());
		setBorder(BorderFactory.createEmptyBorder(16, 28, 28, 28));

		JPanel header = new JPanel();
		header.setLayout(new BoxLayout(header, BoxLayout.X_AXIS));
		header.setOpaque(false);
		JPanel heading = new JPanel();
		heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
		heading.setOpaque(false);
		JLabel eyebrow = new JLabel(AppLocal.getIntString("reports.welcome.title").toUpperCase(Locale.getDefault()));
		eyebrow.setFont(RetailPOSTheme.MANROPE_BOLD.deriveFont(12f));
		eyebrow.setForeground(RetailPOSColors.inkMuted());
		today = new JLabel();
		today.setFont(RetailPOSTheme.MANROPE_BOLD.deriveFont(26f));
		today.setForeground(RetailPOSColors.ink());
		heading.add(eyebrow);
		heading.add(Box.createVerticalStrut(4));
		heading.add(today);
		JPanel context = new JPanel();
		context.setLayout(new BoxLayout(context, BoxLayout.Y_AXIS));
		context.setOpaque(false);
		updated = new JLabel();
		updated.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(12f));
		updated.setForeground(RetailPOSColors.inkMuted());
		updated.setAlignmentX(RIGHT_ALIGNMENT);
		context.add(updated);
		JButton detailed = new RoundedButton(12);
		detailed.setText(AppLocal.getIntString("reports.welcome.details") + "  ↓");
		detailed.setFont(RetailPOSTheme.MANROPE_SEMIBOLD.deriveFont(14f));
		detailed.setPreferredSize(new Dimension(210, 48));
		detailed.setMaximumSize(detailed.getPreferredSize());
		styleButton(detailed, 12);
		detailed.addActionListener(e -> scrollToReports());
		header.add(heading);
		header.add(Box.createHorizontalGlue());
		header.add(context);
		header.add(Box.createHorizontalStrut(16));
		header.add(detailed);
		updateDates();
		add(header, BorderLayout.NORTH);

		JPanel body = new ScrollBody();
		body.setOpaque(false);
		body.setLayout(new GridBagLayout());
		GridBagConstraints section = new GridBagConstraints();
		section.gridx = 0;
		section.weightx = 1;
		section.fill = GridBagConstraints.HORIZONTAL;
		section.anchor = GridBagConstraints.NORTHWEST;
		section.insets = new Insets(0, 0, 12, 0);
		body.add(createCardsSection(), section);
		section.gridy = 1;
		section.insets = new Insets(0, 0, 12, 0);
		body.add(createDetail(), section);
		section.gridy = 2;
		section.insets = new Insets(0, 0, 0, 0);
		body.add(createReports(), section);
		section.gridy = 3;
		section.weighty = 1;
		section.fill = GridBagConstraints.BOTH;
		JPanel filler = new JPanel();
		filler.setOpaque(false);
		body.add(filler, section);
		scroll = new JScrollPane(body);
		scroll.setBorder(null);
		scroll.getViewport().setOpaque(false);
		scroll.setOpaque(false);
		add(scroll, BorderLayout.CENTER);
	}

	private JPanel createCardsSection() {
		JPanel section = new JPanel(new BorderLayout(0, 8));
		section.setOpaque(false);
		JPanel heading = new JPanel();
		heading.setOpaque(false);
		heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
		JLabel title = new JLabel(AppLocal.getIntString("reports.welcome.upToToday"));
		title.setFont(RetailPOSTheme.MANROPE_BOLD.deriveFont(18f));
		JLabel caption = new JLabel(AppLocal.getIntString("reports.welcome.cardsCaption"));
		caption.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(12f));
		caption.setForeground(RetailPOSColors.inkMuted());
		caption.setAlignmentX(Component.LEFT_ALIGNMENT);
		heading.add(title);
		heading.add(caption);
		section.add(heading, BorderLayout.NORTH);
		section.add(createCards(), BorderLayout.CENTER);
		return section;
	}

	private JPanel createCards() {
		JPanel cards = new JPanel(new GridLayout(1, 4, 12, 0));
		cards.setOpaque(false);
		int index = 0;
		for (ReportsWelcomePeriod period : ReportsWelcomePeriod.values()) {
			final int cardIndex = index;
			JButton button = new RoundedButton(16);
			button.setLayout(new BorderLayout());
			// Four cards and their gaps must fit the 1000px viewport without shrinking the
			// detail chart.
			button.setPreferredSize(new Dimension(220, 184));
			styleButton(button, 16);
			button.addActionListener(e -> selectPeriod(ReportsWelcomePeriod.values()[cardIndex]));
			periodButtons[index] = button;
			cardValues[index] = new JLabel("-");
			cardDeltas[index] = new DeltaBadge();
			cardRanges[index] = new JLabel(period.rangeLabel(asOf));
			cardBaselines[index] = new JLabel(period.baselineLabel());
			cardBaselineValues[index] = new JLabel("-");
			cardValues[index].setFont(RetailPOSTheme.PLEX_MONO_SEMIBOLD.deriveFont(25f));
			cardDeltas[index].setFont(RetailPOSTheme.PLEX_MONO_SEMIBOLD.deriveFont(13f));
			cardRanges[index].setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(10f));
			cardRanges[index].setForeground(RetailPOSColors.inkMuted());
			cardBaselines[index].setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(10f));
			cardBaselines[index].setForeground(RetailPOSColors.inkMuted());
			cardBaselineValues[index].setFont(RetailPOSTheme.PLEX_MONO_REGULAR.deriveFont(13f));
			cardBaselineValues[index].setForeground(RetailPOSColors.inkMuted());
			JPanel content = new JPanel();
			content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
			content.setOpaque(false);
			content.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));
			JLabel label = new JLabel(period.getLabel().toUpperCase(Locale.getDefault()));
			label.setFont(RetailPOSTheme.MANROPE_BOLD.deriveFont(12f));
			label.setForeground(RetailPOSColors.inkMuted());
			JPanel cardHeader = new JPanel(new BorderLayout());
			cardHeader.setOpaque(false);
			cardHeader.setBorder(BorderFactory.createEmptyBorder(10, 8, 0, 8));
			cardHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
			cardHeader.add(label, BorderLayout.CENTER);
			selectedTags[index] = new ChipLabel(AppLocal.getIntString("reports.welcome.selectedTag"));
			selectedTags[index].setFont(RetailPOSTheme.MANROPE_BOLD.deriveFont(11f));
			cardHeader.add(selectedTags[index], BorderLayout.EAST);
			content.add(cardRanges[index]);
			content.add(Box.createVerticalStrut(7));
			content.add(cardValues[index]);
			content.add(Box.createVerticalStrut(6));
			content.add(cardDeltas[index]);
			content.add(Box.createVerticalStrut(5));
			content.add(cardBaselines[index]);
			content.add(cardBaselineValues[index]);
			button.add(cardHeader, BorderLayout.NORTH);
			button.add(content, BorderLayout.CENTER);
			cards.add(button);
			index++;
		}
		return cards;
	}

	private JPanel createDetail() {
		JPanel panel = new RoundedPanel(16, RetailPOSColors.surface100());
		panel.setLayout(new BorderLayout(0, 12));
		panel.setBorder(BorderFactory.createCompoundBorder(new RoundedLineBorder(RetailPOSColors.border(), 16),
				BorderFactory.createEmptyBorder(12, 16, 12, 16)));
		JPanel top = new JPanel(new BorderLayout(0, 12));
		top.setOpaque(false);
		JPanel track = new RoundedPanel(12, RetailPOSColors.surface200());
		track.setLayout(new GridLayout(1, 0));
		track.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		ButtonGroup group = new ButtonGroup();
		for (ReportsWelcomePeriod period : ReportsWelcomePeriod.values()) {
			JToggleButton segment = new SegmentButton(
					AppLocal.getIntString("reports.welcome.segment." + period.name().toLowerCase(Locale.ROOT)));
			segment.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(14f));
			segment.setPreferredSize(new Dimension(0, 44));
			segment.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
			segment.addActionListener(e -> selectPeriod(period));
			segments[period.ordinal()] = segment;
			group.add(segment);
			track.add(segment);
		}
		top.add(track, BorderLayout.NORTH);
		detailHeading = new JPanel(new GridBagLayout()) {
			@Override
			public void doLayout() {
				updateHeaderLayout();
				super.doLayout();
			}
		};
		detailHeading.setOpaque(false);
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.weightx = 1;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		detailTitleBlock = new JPanel();
		detailTitleBlock.setOpaque(false);
		detailTitleBlock.setLayout(new BoxLayout(detailTitleBlock, BoxLayout.Y_AXIS));
		JPanel titleRow = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEADING, 8, 0));
		titleRow.setOpaque(false);
		titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
		periodTitle = new JLabel();
		periodTitle.setFont(RetailPOSTheme.MANROPE_BOLD.deriveFont(20f));
		periodStatus = new JLabel();
		periodStatus.setFont(RetailPOSTheme.MANROPE_BOLD.deriveFont(11f));
		periodStatus.setOpaque(true);
		periodStatus.setBackground(RetailPOSColors.surface200());
		periodStatus.setForeground(RetailPOSColors.ink());
		periodStatus.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
		titleRow.add(periodTitle);
		titleRow.add(periodStatus);
		periodComparison = new JLabel();
		periodComparison.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(12f));
		periodComparison.setForeground(RetailPOSColors.inkMuted());
		periodComparison.setAlignmentX(Component.LEFT_ALIGNMENT);
		detailTitleBlock.add(titleRow);
		detailTitleBlock.add(periodComparison);
		detailHeading.add(detailTitleBlock, c);
		detailNavigation = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.TRAILING, 6, 0));
		detailNavigation.setOpaque(false);
		back = periodButton();
		previous = periodButton();
		next = periodButton();
		back.addActionListener(e -> {
			focusAsOf = asOf;
			loadFocus();
			updatePeriodNavigation();
		});
		previous.addActionListener(e -> changePeriod(selected, -1));
		next.addActionListener(e -> changePeriod(selected, 1));
		rollingNavigation = new JLabel(AppLocal.getIntString("reports.welcome.rollingNavigation"));
		rollingNavigation.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(12f));
		detailNavigation.add(back);
		detailNavigation.add(previous);
		detailNavigation.add(next);
		detailNavigation.add(rollingNavigation);
		c.gridx = 0;
		c.gridy = 1;
		c.weightx = 1;
		c.anchor = GridBagConstraints.EAST;
		detailHeading.add(detailNavigation, c);
		top.add(detailHeading, BorderLayout.CENTER);
		JPanel figures = new JPanel(new GridBagLayout());
		figures.setOpaque(false);
		JPanel totalCell = detailCell("reports.welcome.total", false);
		totalCell.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
		totalRange = detailHint(totalCell);
		totalRange.setFont(RetailPOSTheme.PLEX_MONO_REGULAR.deriveFont(13f));
		totalValue = detailNumber(totalCell);
		totalValue.setFont(RetailPOSTheme.PLEX_MONO_SEMIBOLD.deriveFont(40f));
		totalDelta = new DeltaBadge();
		totalDelta.setFont(RetailPOSTheme.PLEX_MONO_SEMIBOLD.deriveFont(13f));
		totalCell.add(totalDelta);
		totalBaseline = detailHint(totalCell);
		totalBaseline.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(12f));
		totalBaselineValue = detailHint(totalCell);
		totalBaselineValue.setFont(RetailPOSTheme.PLEX_MONO_REGULAR.deriveFont(13f));
		addFigureCell(figures, totalCell, 0, 2);
		JPanel receiptsCell = detailCell("label.reportreceipts", false);
		receipts = detailNumber(receiptsCell);
		receiptsDelta = detailHint(receiptsCell);
		addFigureCell(figures, receiptsCell, 2, 1);
		JPanel averageCell = detailCell("reports.welcome.average", true);
		average = detailNumber(averageCell);
		averageDelta = detailHint(averageCell);
		addFigureCell(figures, averageCell, 3, 1);
		JPanel loyaltyCell = detailCell("reports.welcome.loyalty", true);
		JPanel loyaltyAmount = new JPanel();
		loyaltyAmount.setOpaque(false);
		loyaltyAmount.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEADING, 4, 0));
		loyaltyCount = new JLabel("-");
		loyaltyCount.setFont(RetailPOSTheme.PLEX_MONO_SEMIBOLD.deriveFont(24f));
		loyaltyValue = new JLabel(" ");
		loyaltyValue.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(11f));
		loyaltyValue.setForeground(RetailPOSColors.inkMuted());
		loyaltyAmount.add(loyaltyCount);
		loyaltyAmount.add(loyaltyValue);
		loyaltyAmount.setAlignmentX(LEFT_ALIGNMENT);
		loyaltyAmount.setMaximumSize(new Dimension(Integer.MAX_VALUE, loyaltyAmount.getPreferredSize().height));
		loyaltyCell.add(loyaltyAmount);
		loyaltyDelta = detailHint(loyaltyCell);
		addFigureCell(figures, loyaltyCell, 4, 1);
		JPanel refundsCell = detailCell("label.reportrefunds", true);
		refunds = detailNumber(refundsCell);
		refundCount = detailHint(refundsCell);
		addFigureCell(figures, refundsCell, 5, 1);
		JPanel aboveChart = new JPanel();
		aboveChart.setOpaque(false);
		aboveChart.setLayout(new BoxLayout(aboveChart, BoxLayout.Y_AXIS));
		top.setAlignmentX(Component.LEFT_ALIGNMENT);
		figures.setAlignmentX(Component.LEFT_ALIGNMENT);
		aboveChart.add(top);
		aboveChart.add(Box.createVerticalStrut(8));
		aboveChart.add(figures);
		panel.add(aboveChart, BorderLayout.NORTH);
		whatSold = createWhatSold();
		JPanel overview = createOverview();
		panel.add(overview, BorderLayout.CENTER);
		selectPeriod(selected);
		return panel;
	}

	private void addFigureCell(JPanel figures, JPanel cell, int column, int span) {
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = column;
		c.gridy = 0;
		c.gridwidth = span;
		c.weightx = span;
		c.fill = GridBagConstraints.BOTH;
		figures.add(cell, c);
	}

	private JPanel detailCell(String key, boolean divider) {
		JPanel cell = new JPanel();
		cell.setOpaque(false);
		cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
		cell.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0, divider ? 1 : 0, 0, 0, RetailPOSColors.border()),
				BorderFactory.createEmptyBorder(16, 16, 16, 8)));
		JLabel caption = new JLabel(AppLocal.getIntString(key).toUpperCase(Locale.getDefault()));
		caption.setFont(RetailPOSTheme.MANROPE_BOLD.deriveFont(12f));
		caption.setForeground(RetailPOSColors.inkMuted());
		cell.add(caption);
		cell.add(Box.createVerticalStrut(4));
		return cell;
	}

	private JLabel detailNumber(JPanel cell) {
		JLabel value = new JLabel("-");
		value.setFont(RetailPOSTheme.PLEX_MONO_SEMIBOLD.deriveFont(24f));
		cell.add(value);
		return value;
	}

	private JLabel detailHint(JPanel cell) {
		JLabel hint = new JLabel(" ");
		hint.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f));
		hint.setForeground(RetailPOSColors.inkMuted());
		cell.add(hint);
		return hint;
	}

	private JPanel createReports() {
		JPanel panel = new JPanel(new BorderLayout(0, 12));
		panel.setOpaque(false);
		JLabel heading = new JLabel(AppLocal.getIntString("reports.welcome.details"));
		heading.setFont(RetailPOSTheme.MANROPE_SEMIBOLD.deriveFont(18f));
		panel.add(heading, BorderLayout.NORTH);
		JPanel entries = new JPanel(new GridLayout(0, 3, 8, 8));
		entries.setOpaque(false);
		for (int i = 0; i < reportsMenu.countMenuElements(); i++) {
			MenuElement element = reportsMenu.getMenuElement(i);
			if (element instanceof MenuItemDefinition) {
				JButton button = new RoundedButton(12);
				button.setAction(((MenuItemDefinition) element).getAction());
				button.setPreferredSize(new Dimension(260, 64));
				button.setMinimumSize(new Dimension(220, 64));
				button.setHorizontalAlignment(SwingConstants.LEADING);
				styleButton(button, 12);
				button.setBorder(BorderFactory.createCompoundBorder(new RoundedLineBorder(RetailPOSColors.border(), 12),
						BorderFactory.createEmptyBorder(8, 16, 8, 16)));
				button.setIconTextGap(10);
				button.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(16f));
				entries.add(button);
			}
		}
		panel.add(entries, BorderLayout.CENTER);
		return panel;
	}

	private JPanel createWhatSold() {
		JPanel panel = new JPanel(new BorderLayout(0, 10));
		panel.setOpaque(false);
		JPanel heading = new JPanel();
		heading.setOpaque(false);
		heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
		JPanel title = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 0));
		title.setOpaque(false);
		title.setAlignmentX(Component.LEFT_ALIGNMENT);
		JLabel label = new JLabel();
		label.setName("whatSoldTitle");
		label.setFont(RetailPOSTheme.MANROPE_BOLD.deriveFont(20f));
		JLabel chip = new JLabel(AppLocal.getIntString("reports.welcome.followPeriod"));
		chip.setFont(RetailPOSTheme.MANROPE_BOLD.deriveFont(11f));
		chip.setForeground(RetailPOSColors.ink());
		chip.setOpaque(true);
		chip.setBackground(RetailPOSColors.surface200());
		chip.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
		title.add(label);
		title.add(chip);
		heading.add(title);
		JLabel caption = new JLabel();
		caption.setName("whatSoldCaption");
		caption.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(12f));
		caption.setForeground(RetailPOSColors.inkMuted());
		caption.setAlignmentX(Component.LEFT_ALIGNMENT);
		heading.add(caption);
		label.setText(AppLocal.getIntString("reports.welcome.whatSold") + " · "
				+ ReportsWelcomeLabels.title(selected, focusAsOf));
		caption.setText(
				selected.rangeLabel(focusAsOf) + " · " + AppLocal.getIntString("reports.welcome.whatSoldBaseline"));
		panel.add(heading, BorderLayout.NORTH);
		WhatSoldGrid cards = new WhatSoldGrid();
		cards.setOpaque(false);
		cards.setName("whatSoldCards");
		panel.setName("whatSold");
		cards.addCard(new RoundedPanel(16, RetailPOSColors.surface100()), 0, 0);
		cards.addCard(new RoundedPanel(16, RetailPOSColors.surface100()), 1, 0);
		cards.addCard(new RoundedPanel(16, RetailPOSColors.surface100()), 0, 1);
		cards.addCard(Box.createGlue(), 1, 1);
		panel.add(cards, BorderLayout.CENTER);
		return panel;
	}

	private void showWhatSold() {
		if (whatSold == null || whatSoldData == null) {
			return;
		}
		JPanel heading = (JPanel) whatSold.getComponent(0);
		JPanel title = (JPanel) heading.getComponent(0);
		((JLabel) title.getComponent(0)).setText(AppLocal.getIntString("reports.welcome.whatSold") + " · "
				+ ReportsWelcomeLabels.title(selected, focusAsOf));
		((JLabel) heading.getComponent(1)).setText(selected.rangeLabel(focusAsOf) + " · "
				+ (ReportsWelcomeLabels.current(selected, focusAsOf, asOf)
						? AppLocal.getIntString("reports.welcome.whatSoldBaseline")
						: selected.baselineLabel()));
		WhatSoldGrid cards = (WhatSoldGrid) whatSold.getComponent(1);
		cards.removeAll();
		cards.addCard(productCard(), 0, 0);
		cards.addCard(categoryCard(), 1, 0);
		cards.addCard(paymentCard(), 0, 1);
		cards.addCard(Box.createGlue(), 1, 1);
		cards.revalidate();
		cards.repaint();
		whatSold.revalidate();
	}

	private JPanel productCard() {
		JPanel card = blockPanel(AppLocal.getIntString("reports.welcome.topProducts"), "Menu.ProductSalesSummary",
				"reports.welcome.viewProducts");
		JPanel rows = (JPanel) card.getClientProperty("rows");
		int amountWidth = widestNumberWidth(whatSoldData.getProducts().stream()
				.map(insight -> Formats.CURRENCY.formatValue(insight.getRow().getAmount()))
				.collect(java.util.stream.Collectors.toList()), 70);
		List<String> productDeltas = whatSoldData.getProducts().stream()
				.map(insight -> insight.getPrevious() == null
						? AppLocal.getIntString("reports.welcome.noPreviousShort")
						: delta(percent(insight.getRow().getAmount(), insight.getPrevious())))
				.collect(java.util.stream.Collectors.toList());
		int deltaWidth = widestNumberWidth(productDeltas, 70);
		int rank = 1;
		for (WelcomeSalesRepository.ProductInsight insight : whatSoldData.getProducts()) {
			ProductSalesRow row = insight.getRow();
			SharedRow line = new SharedRow();
			JLabel number = mono(Integer.toString(rank++));
			number.setForeground(RetailPOSColors.inkMuted());
			line.addFixed(number, 28);
			JPanel names = new JPanel();
			names.setOpaque(false);
			names.setLayout(new BoxLayout(names, BoxLayout.Y_AXIS));
			EllipsisLabel name = new EllipsisLabel(row.getProductName());
			name.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(14f));
			name.setForeground(RetailPOSColors.ink());
			name.setToolTipText(row.getProductName());
			JLabel detail = label(insight.getCategoryPath() + " · " + formatUnits(row.getUnits()), 11,
					RetailPOSColors.inkMuted());
			names.add(name);
			names.add(detail);
			JLabel value = mono(Formats.CURRENCY.formatValue(row.getAmount()));
			JLabel delta = deltaLabel(
					insight.getPrevious() == null ? null : percent(row.getAmount(), insight.getPrevious()), true);
			line.addFlexible(names);
			line.addFixed(rightLabel(value, amountWidth), amountWidth);
			line.addFixed(rightLabel(delta, deltaWidth), deltaWidth);
			rows.add(line);
		}
		finishRows(rows);
		return card;
	}

	private JPanel categoryCard() {
		JPanel card = blockPanel(AppLocal.getIntString("reports.welcome.categoryShare"), "Menu.ProductSalesSummary",
				"reports.welcome.viewProductsByCategory");
		JPanel rows = (JPanel) card.getClientProperty("rows");
		double total = 0;
		for (WelcomeSalesRepository.CategoryInsight row : whatSoldData.getCategories())
			total += row.getAmount();
		final double categoryTotal = total;
		List<WelcomeSalesRepository.CategoryInsight> visible = whatSoldData.getCategories().subList(0,
				Math.min(5, whatSoldData.getCategories().size()));
		if (whatSoldData.getCategories().size() > 5) {
			double other = 0;
			double previousOther = 0;
			for (WelcomeSalesRepository.CategoryInsight row : whatSoldData.getCategories().subList(5,
					whatSoldData.getCategories().size())) {
				other += row.getAmount();
				previousOther += row.getPrevious();
			}
			visible = new java.util.ArrayList<>(visible);
			visible.add(new WelcomeSalesRepository.CategoryInsight(
					AppLocal.getIntString("reports.welcome.otherCategories", whatSoldData.getCategories().size() - 5),
					other, previousOther, java.util.Collections.emptyList(),
					whatSoldData.getCategories().subList(5, whatSoldData.getCategories().size()).stream()
							.allMatch(WelcomeSalesRepository.CategoryInsight::hasComparablePeriods)));
		}
		boolean showPoints = whatSoldData.getCategories().stream()
				.anyMatch(WelcomeSalesRepository.CategoryInsight::hasComparablePeriods);
		int nameWidth = categoryNameWidth(visible);
		int shareWidth = widestNumberWidth(
				visible.stream().map(row -> formatShare(share(row.getAmount(), categoryTotal)))
						.collect(java.util.stream.Collectors.toList()),
				55);
		int amountWidth = widestNumberWidth(visible.stream().map(row -> Formats.CURRENCY.formatValue(row.getAmount()))
				.collect(java.util.stream.Collectors.toList()), 75);
		int pointsWidth = widestNumberWidth(visible.stream()
				.map(row -> Double.isNaN(points(row, categoryTotal, previousCategoryTotal()))
						? AppLocal.getIntString("reports.welcome.noPreviousShort")
						: pointsLabel(points(row, categoryTotal, previousCategoryTotal())).getText())
				.collect(java.util.stream.Collectors.toList()), 70);
		for (int i = 0; i < visible.size(); i++) {
			WelcomeSalesRepository.CategoryInsight row = visible.get(i);
			final int index = i;
			JButton button = new JButton();
			button.setLayout(new BorderLayout(8, 0));
			button.setHorizontalAlignment(SwingConstants.LEADING);
			button.setOpaque(false);
			button.setContentAreaFilled(false);
			button.setMinimumSize(new Dimension(0, 48));
			button.setPreferredSize(new Dimension(0, 48));
			button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
			button.setBorder(index == expandedCategory
					? BorderFactory.createCompoundBorder(new RoundedLineBorder(RetailPOSColors.borderStrong(), 8, true),
							BorderFactory.createEmptyBorder())
					: (i + 1 < visible.size()
							? BorderFactory.createCompoundBorder(
									BorderFactory.createMatteBorder(0, 0, 1, 0, RetailPOSColors.border()),
									BorderFactory.createEmptyBorder())
							: BorderFactory.createEmptyBorder()));
			addCategoryGrid(button, row, total, previousCategoryTotal(), nameWidth, index == expandedCategory,
					showPoints, shareWidth, amountWidth, pointsWidth);
			if (i < 5) {
				button.addActionListener(e -> {
					expandedCategory = expandedCategory == index ? -1 : index;
					showWhatSold();
				});
			}
			rows.add(button);
			if (i < 5 && index == expandedCategory) {
				for (WelcomeSalesRepository.CategoryInsight child : row.getChildren()) {
					JButton childLabel = new JButton();
					childLabel.setOpaque(false);
					childLabel.setContentAreaFilled(false);
					childLabel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, RetailPOSColors.border()));
					childLabel.setPreferredSize(new Dimension(0, 48));
					childLabel.setMinimumSize(new Dimension(0, 48));
					addCategoryGrid(childLabel, child, row.getAmount(), row.getPrevious(), nameWidth, false, showPoints,
							shareWidth, amountWidth, pointsWidth);
					rows.add(childLabel);
				}
			}
		}
		rows.add(label(
				AppLocal.getIntString(
						showPoints ? "reports.welcome.categoryHint" : "reports.welcome.categoryNoPrevious"),
				11, RetailPOSColors.inkMuted()));
		finishRows(rows);
		return card;
	}

	private JPanel paymentCard() {
		JPanel card = blockPanel(AppLocal.getIntString("reports.welcome.payments"), "Menu.PaymentSalesSummary",
				"reports.welcome.viewPayments");
		JPanel rows = (JPanel) card.getClientProperty("rows");
		double total = 0;
		for (WelcomeSalesRepository.PaymentInsight row : whatSoldData.getPayments())
			total += row.getAmount();
		final double paymentTotal = total;
		javax.swing.JComponent stack = new PaymentStackBar(whatSoldData.getPayments());
		rows.add(stack);
		List<WelcomeSalesRepository.PaymentInsight> visiblePayments = whatSoldData.getPayments().stream()
				.filter(row -> row.getAmount() != 0 || row.getPrevious() != 0)
				.collect(java.util.stream.Collectors.toList());
		int shareWidth = widestNumberWidth(
				visiblePayments.stream().map(row -> formatShare(share(row.getAmount(), paymentTotal)))
						.collect(java.util.stream.Collectors.toList()),
				55);
		int amountWidth = widestNumberWidth(
				visiblePayments.stream().map(row -> Formats.CURRENCY.formatValue(row.getAmount()))
						.collect(java.util.stream.Collectors.toList()),
				75);
		int pointsWidth = widestNumberWidth(visiblePayments.stream().map(row -> {
			Double points = row.getPrevious() == 0 || paymentTotal == 0 || previousTotal() == 0
					? null
					: (row.getAmount() / paymentTotal - row.getPrevious() / previousTotal()) * 100;
			return pointsLabel(points).getText();
		}).collect(java.util.stream.Collectors.toList()), 70);
		for (WelcomeSalesRepository.PaymentInsight row : visiblePayments) {
			SharedRow line = new SharedRow();
			JPanel swatch = new JPanel();
			swatch.setPreferredSize(new Dimension(14, 14));
			swatch.setBackground(paymentColor(row.getCategory()));
			line.addFixed(swatch, 14);
			line.addFlexible(label(paymentLabel(row.getCategory()), 13, RetailPOSColors.ink()));
			line.addFixed(rightMono(formatShare(share(row.getAmount(), total)), shareWidth), shareWidth);
			line.addFixed(rightMono(Formats.CURRENCY.formatValue(row.getAmount()), amountWidth), amountWidth);
			Double points = row.getPrevious() == 0 || paymentTotal == 0 || previousTotal() == 0
					? null
					: (row.getAmount() / paymentTotal - row.getPrevious() / previousTotal()) * 100;
			line.addFixed(rightLabel(pointsLabel(points), pointsWidth), pointsWidth);
			rows.add(line);
		}
		finishRows(rows);
		return card;
	}

	private int categoryNameWidth(List<WelcomeSalesRepository.CategoryInsight> rows) {
		java.awt.Font font = RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f);
		int width = 0;
		for (WelcomeSalesRepository.CategoryInsight row : rows) {
			String affordance = row.getChildren().isEmpty() ? "" : " ›";
			width = Math.max(width, getFontMetrics(font).stringWidth(row.getName() + affordance));
		}
		return width + 12;
	}

	private void addCategoryGrid(JButton rowButton, WelcomeSalesRepository.CategoryInsight row, double total,
			double previousTotal, int nameWidth, boolean expanded, boolean showPoints, int shareWidth, int amountWidth,
			int pointsWidth) {
		SharedRow rowLayout = new SharedRow();
		String affordance = row.getChildren().isEmpty() ? "" : expanded ? " ⌄" : " ›";
		JLabel name = label(row.getName() + affordance, 13, RetailPOSColors.ink());
		name.setPreferredSize(new Dimension(nameWidth, 30));
		name.setMaximumSize(new Dimension(nameWidth, 30));
		javax.swing.JComponent bar = new ShareBar(share(row.getAmount(), total));
		bar.setPreferredSize(new Dimension(0, 8));
		JLabel amount = mono(Formats.CURRENCY.formatValue(row.getAmount()));
		amount.setForeground(RetailPOSColors.inkMuted());
		rowLayout.addFixed(name, nameWidth);
		rowLayout.addFlexible(bar);
		rowLayout.addFixed(rightMono(formatShare(share(row.getAmount(), total)), shareWidth), shareWidth);
		rowLayout.addFixed(rightLabel(amount, amountWidth), amountWidth);
		if (showPoints) {
			rowLayout.addFixed(rightLabel(pointsLabel(points(row, total, previousTotal)), pointsWidth), pointsWidth);
		}
		rowButton.setLayout(new BorderLayout());
		rowButton.add(rowLayout, BorderLayout.CENTER);
	}

	private JLabel rightMono(String text, int width) {
		JLabel result = mono(text);
		return rightLabel(result, width);
	}

	private int widestNumberWidth(List<String> values, int minimum) {
		int width = minimum;
		for (String value : values)
			width = Math.max(width,
					getFontMetrics(RetailPOSTheme.PLEX_MONO_REGULAR.deriveFont(12f)).stringWidth(value) + 8);
		return width;
	}

	private JLabel rightLabel(JLabel label, int width) {
		label.setHorizontalAlignment(SwingConstants.RIGHT);
		label.setPreferredSize(new Dimension(width, label.getPreferredSize().height));
		label.setMinimumSize(new Dimension(width, label.getMinimumSize().height));
		label.setMaximumSize(new Dimension(width, Integer.MAX_VALUE));
		return label;
	}

	private void finishRows(JPanel rows) {
		rows.setMaximumSize(new Dimension(Integer.MAX_VALUE, rows.getPreferredSize().height));
		List<SharedRow> sharedRows = new java.util.ArrayList<>();
		for (Component component : rows.getComponents()) {
			if (component instanceof SharedRow)
				sharedRows.add((SharedRow) component);
		}
		for (int i = 0; i < sharedRows.size(); i++) {
			sharedRows.get(i)
					.setBorder(i + 1 < sharedRows.size()
							? BorderFactory.createMatteBorder(0, 0, 1, 0, RetailPOSColors.border())
							: BorderFactory.createEmptyBorder());
		}
	}

	private JPanel blockPanel(String title, String reportKey, String reportLabelKey) {
		JPanel card = new RoundedPanel(16, RetailPOSColors.surface100());
		card.setLayout(new BorderLayout(0, 12));
		card.setBorder(BorderFactory.createCompoundBorder(new RoundedLineBorder(RetailPOSColors.border(), 16),
				BorderFactory.createEmptyBorder(20, 20, 20, 20)));
		JPanel content = new JPanel();
		content.setOpaque(false);
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		JLabel heading = label(title, 17, RetailPOSColors.ink());
		heading.setFont(RetailPOSTheme.MANROPE_BOLD.deriveFont(17f));
		heading.setAlignmentX(Component.LEFT_ALIGNMENT);
		content.add(heading);
		javax.swing.JComponent titleGap = (javax.swing.JComponent) Box.createVerticalStrut(12);
		titleGap.setAlignmentX(Component.LEFT_ALIGNMENT);
		content.add(titleGap);
		JPanel rows = new JPanel();
		rows.setOpaque(false);
		rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
		rows.setAlignmentX(Component.LEFT_ALIGNMENT);
		content.add(rows);
		javax.swing.JComponent glue = (javax.swing.JComponent) Box.createVerticalGlue();
		glue.setAlignmentX(Component.LEFT_ALIGNMENT);
		content.add(glue);
		card.putClientProperty("rows", rows);
		card.add(content, BorderLayout.CENTER);
		JButton link = reportLink(reportKey, reportLabelKey);
		if (link != null)
			card.add(link, BorderLayout.SOUTH);
		return card;
	}

	private JButton reportLink(String key, String labelKey) {
		String target = AppLocal.getIntString(key);
		for (int i = 0; i < reportsMenu.countMenuElements(); i++) {
			MenuElement element = reportsMenu.getMenuElement(i);
			if (element instanceof MenuItemDefinition) {
				Action action = ((MenuItemDefinition) element).getAction();
				if (target.equals(action.getValue(Action.NAME))) {
					JButton button = new JButton(action);
					button.setText(AppLocal.getIntString(labelKey));
					button.setHorizontalAlignment(SwingConstants.LEADING);
					button.setFont(RetailPOSTheme.MANROPE_SEMIBOLD.deriveFont(12f));
					return button;
				}
			}
		}
		return null;
	}

	private JLabel label(String text, float size, java.awt.Color color) {
		JLabel result = new JLabel(text);
		result.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(size));
		result.setForeground(color);
		return result;
	}

	private JLabel mono(String text) {
		JLabel result = label(text, 12, RetailPOSColors.ink());
		result.setFont(RetailPOSTheme.PLEX_MONO_REGULAR.deriveFont(12f));
		return result;
	}

	private JLabel deltaLabel(Double value, boolean shortNoData) {
		JLabel result = label(value == null || value.isNaN()
				? AppLocal.getIntString(shortNoData ? "reports.welcome.noPreviousShort" : "reports.welcome.noPrevious")
				: delta(value), 11, RetailPOSColors.inkMuted());
		if (value != null && !value.isNaN())
			setDeltaColor(result, value);
		return result;
	}

	private JLabel pointsLabel(Double value) {
		JLabel result = label(value == null || value.isNaN()
				? AppLocal.getIntString("reports.welcome.noPreviousShort")
				: String.format(Locale.getDefault(), "%s%+.1f pt", value > 0 ? "↑ " : value < 0 ? "↓ " : "→ ", value),
				11, RetailPOSColors.inkMuted());
		if (value != null && !value.isNaN())
			setDeltaColor(result, value);
		return result;
	}

	private double percent(double current, double previous) {
		return previous == 0 ? 0 : (current - previous) * 100 / previous;
	}
	private double share(double value, double total) {
		return total == 0 ? 0 : value / total;
	}
	private double points(WelcomeSalesRepository.CategoryInsight row, double total, double oldTotal) {
		return !row.hasComparablePeriods() || total == 0 || oldTotal == 0
				? Double.NaN
				: share(row.getAmount(), total) * 100 - share(row.getPrevious(), oldTotal) * 100;
	}
	private double previousCategoryTotal() {
		double total = 0;
		for (WelcomeSalesRepository.CategoryInsight row : whatSoldData.getCategories())
			total += row.getPrevious();
		return total;
	}
	private double previousTotal() {
		double total = 0;
		for (WelcomeSalesRepository.PaymentInsight row : whatSoldData.getPayments())
			total += row.getPrevious();
		return total;
	}
	private String paymentLabel(String key) {
		return AppLocal.getIntString("reports.welcome.payment." + key);
	}

	private java.awt.Color paymentColor(String key) {
		return PaymentSalesRepository.CARD.equals(key)
				? RetailPOSColors.brand()
				: PaymentSalesRepository.CASH.equals(key)
						? RetailPOSColors.brandSubtle()
						: RetailPOSColors.surface200();
	}

	static String formatUnits(double units) {
		NumberFormat format = NumberFormat.getNumberInstance(Locale.getDefault());
		format.setGroupingUsed(false);
		format.setMaximumFractionDigits(2);
		format.setMinimumFractionDigits(Math.rint(units) == units ? 0 : 1);
		return format.format(units) + " u.";
	}

	static String formatShare(double fraction) {
		NumberFormat format = NumberFormat.getNumberInstance(Locale.getDefault());
		format.setGroupingUsed(false);
		format.setMinimumFractionDigits(1);
		format.setMaximumFractionDigits(1);
		return format.format(fraction * 100) + " %";
	}

	private JPanel createOverview() {
		JPanel overview = new JPanel(new BorderLayout());
		overview.setOpaque(false);
		overview.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
		chartTitle = new JLabel();
		chartTitle.setFont(RetailPOSTheme.MANROPE_BOLD.deriveFont(16f));
		chart = new SalesTrendChart();
		seriesCurrent = new JLabel();
		seriesPrevious = new JLabel();
		chartLegend = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.TRAILING, 8, 0));
		chartLegend.setOpaque(false);
		for (JLabel series : new JLabel[]{seriesCurrent, seriesPrevious}) {
			series.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f));
			series.setForeground(RetailPOSColors.inkMuted());
		}
		chartLegend.add(new LegendMarker(true));
		chartLegend.add(seriesCurrent);
		chartLegend.add(new LegendMarker(false));
		chartLegend.add(seriesPrevious);
		chartHeading = new JPanel(new GridBagLayout()) {
			@Override
			public void doLayout() {
				updateChartHeaderLayout();
				super.doLayout();
			}
		};
		chartHeading.setOpaque(false);
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		chartHeading.add(chartTitle, c);
		c.gridy = 1;
		c.anchor = GridBagConstraints.EAST;
		chartHeading.add(chartLegend, c);
		chartComparisonNote = new JLabel(AppLocal.getIntString("reports.welcome.elapsedNote"));
		chartComparisonNote.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f));
		chartComparisonNote.setForeground(RetailPOSColors.inkMuted());
		chartTargetNote = new JLabel(AppLocal.getIntString("reports.welcome.futureNote"));
		chartTargetNote.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f));
		chartTargetNote.setForeground(RetailPOSColors.inkMuted());
		JPanel caption = new JPanel();
		caption.setOpaque(false);
		caption.setLayout(new BoxLayout(caption, BoxLayout.Y_AXIS));
		chartPartialLegend = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEADING, 8, 0));
		chartPartialLegend.setOpaque(false);
		chartPartialLegend.setAlignmentX(Component.LEFT_ALIGNMENT);
		chartPartialLegend.add(new SplitLegendMarker());
		chartPartialLabel = new JLabel();
		chartPartialLabel.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f));
		chartPartialLabel.setForeground(RetailPOSColors.inkMuted());
		chartPartialLegend.add(chartPartialLabel);
		chartPartialLegend.setVisible(false);
		chartComparisonNote.setAlignmentX(Component.LEFT_ALIGNMENT);
		chartTargetNote.setAlignmentX(Component.LEFT_ALIGNMENT);
		caption.add(chartPartialLegend);
		caption.add(chartComparisonNote);
		caption.add(chartTargetNote);
		overview.add(chartHeading, BorderLayout.NORTH);
		JPanel chartContent = new JPanel();
		chartContent.setOpaque(false);
		chartContent.setLayout(new BoxLayout(chartContent, BoxLayout.Y_AXIS));
		chartContent.add(chart);
		chartContent.add(caption);
		chartContent.add(Box.createVerticalStrut(20));
		chartContent.add(whatSold);
		overview.add(chartContent, BorderLayout.CENTER);
		JPanel footerSpacer = new JPanel();
		footerSpacer.setOpaque(false);
		footerSpacer.setPreferredSize(new Dimension(0, 0));
		overview.add(footerSpacer, BorderLayout.SOUTH);
		return overview;
	}

	private void selectPeriod(ReportsWelcomePeriod period) {
		selected = period;
		focusAsOf = asOf;
		for (int i = 0; i < periodButtons.length; i++) {
			boolean active = ReportsWelcomePeriod.values()[i] == period;
			periodButtons[i].setBackground(RetailPOSColors.surface100());
			periodButtons[i].setBorder(BorderFactory.createCompoundBorder(
					new RoundedLineBorder(active ? RetailPOSColors.borderStrong() : RetailPOSColors.border(), 16,
							active),
					BorderFactory.createEmptyBorder(1, 1, 1, 1)));
			selectedTags[i].setVisible(active);
		}
		if (periodTitle != null) {
			for (int i = 0; i < segments.length; i++) {
				segments[i].setSelected(i == period.ordinal());
				segments[i]
						.setFont((i == period.ordinal() ? RetailPOSTheme.MANROPE_BOLD : RetailPOSTheme.MANROPE_MEDIUM)
								.deriveFont(14f));
				segments[i].setBorder(i == period.ordinal()
						? new RoundedLineBorder(RetailPOSColors.borderStrong(), 12, true)
						: BorderFactory.createEmptyBorder(2, 2, 2, 2));
			}
			updatePeriodNavigation();
		}
		if (loaded != null) {
			loadFocus();
		}
	}

	private void loadData() {
		final Date snapshot = asOf;
		loaded = null;
		focusGeneration++;
		for (JLabel label : cardValues) {
			label.setText("...");
		}
		for (JLabel label : cardBaselineValues) {
			label.setText("-");
		}
		new SwingWorker<SalesSummaryComparison[], Void>() {
			@Override
			protected SalesSummaryComparison[] doInBackground() throws Exception {
				SalesSummaryComparison[] values = new SalesSummaryComparison[ReportsWelcomePeriod.values().length];
				for (int i = 0; i < values.length; i++) {
					ReportsWelcomePeriod period = ReportsWelcomePeriod.values()[i];
					values[i] = repository.loadComparison(app.getSession().getConnection(), period.current(snapshot),
							period.previous(snapshot));
				}
				return values;
			}

			@Override
			protected void done() {
				try {
					SalesSummaryComparison[] values = get();
					if (snapshot != asOf) {
						return;
					}
					loaded = values;
					for (int i = 0; i < values.length; i++) {
						showCard(i, values[i]);
					}
					showDetail(values[selected.ordinal()]);
					loadFocus();
				} catch (Exception e) {
					new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotloadreport"), e)
							.show(JPanelReportsWelcome.this);
				}
			}
		}.execute();
	}

	private JButton periodButton() {
		JButton button = new JButton();
		button.setFont(RetailPOSTheme.MANROPE_SEMIBOLD.deriveFont(13f));
		button.setPreferredSize(new Dimension(120, 48));
		styleButton(button, 10);
		return button;
	}

	private void changePeriod(ReportsWelcomePeriod period, int amount) {
		if (period == ReportsWelcomePeriod.ROLLING_YEAR) {
			return;
		}
		Calendar targetStart = Calendar.getInstance();
		targetStart.setTime(period.current(focusAsOf).getStartInclusive());
		switch (period) {
			case WEEK :
				targetStart.add(Calendar.DAY_OF_MONTH, amount * 7);
				break;
			case MONTH :
				targetStart.add(Calendar.MONTH, amount);
				break;
			case YEAR :
				targetStart.add(Calendar.YEAR, amount);
				break;
			default :
				throw new IllegalStateException("Unknown dashboard period");
		}
		Date currentStart = period.current(asOf).getStartInclusive();
		if (targetStart.getTime().after(currentStart)) {
			return;
		}
		if (targetStart.getTime().equals(currentStart)) {
			focusAsOf = asOf;
		} else {
			Calendar targetEnd = (Calendar) targetStart.clone();
			if (period == ReportsWelcomePeriod.WEEK) {
				targetEnd.add(Calendar.DAY_OF_MONTH, 7);
			} else {
				targetEnd.add(period == ReportsWelcomePeriod.MONTH ? Calendar.MONTH : Calendar.YEAR, 1);
			}
			targetEnd.add(Calendar.DAY_OF_MONTH, -1);
			focusAsOf = targetEnd.getTime();
		}
		loadFocus();
		updatePeriodNavigation();
	}

	private void updatePeriodNavigation() {
		boolean rolling = selected == ReportsWelcomePeriod.ROLLING_YEAR;
		boolean current = ReportsWelcomeLabels.current(selected, focusAsOf, asOf);
		previous.setVisible(!rolling);
		next.setVisible(!rolling);
		back.setVisible(!rolling && !current);
		rollingNavigation.setVisible(rolling);
		if (!rolling) {
			setNavigationLabel(previous, ReportsWelcomeLabels.navigation(selected, focusAsOf, asOf, -1));
			setNavigationLabel(next, ReportsWelcomeLabels.navigation(selected, focusAsOf, asOf, 1));
			setNavigationLabel(back, ReportsWelcomeLabels.navigation(selected, focusAsOf, asOf, 0));
			next.setEnabled(!current);
		}
		periodTitle.setText(ReportsWelcomeLabels.title(selected, focusAsOf));
		periodStatus.setText(ReportsWelcomeLabels.status(selected, focusAsOf, asOf));
		periodComparison.setText(ReportsWelcomeLabels.comparison(selected, focusAsOf, asOf));
		if (whatSold != null) {
			JPanel heading = (JPanel) whatSold.getComponent(0);
			JPanel title = (JPanel) heading.getComponent(0);
			((JLabel) title.getComponent(0)).setText(AppLocal.getIntString("reports.welcome.whatSold") + " · "
					+ ReportsWelcomeLabels.title(selected, focusAsOf));
			((JLabel) heading.getComponent(1)).setText(
					selected.rangeLabel(focusAsOf) + " · " + AppLocal.getIntString("reports.welcome.whatSoldBaseline"));
		}
		updateChartLabels();
		detailHeading.revalidate();
	}

	private void updateChartLabels() {
		ReportsWelcomePeriod period = selected;
		Date snapshot = focusAsOf;
		String focusName = ReportsWelcomeLabels.chartPeriod(period, snapshot);
		Calendar baselineEnd = Calendar.getInstance();
		baselineEnd.setTime(period.previous(snapshot).getEndExclusive());
		baselineEnd.add(Calendar.DAY_OF_MONTH, -1);
		String previousName = ReportsWelcomeLabels.chartPeriod(period, baselineEnd.getTime());
		chartTitle.setText(AppLocal.getIntString("reports.welcome.chartTitle",
				AppLocal.getIntString(period == ReportsWelcomePeriod.WEEK || period == ReportsWelcomePeriod.MONTH
						? "reports.welcome.chartDays"
						: "reports.welcome.chartMonths"),
				focusName));
		seriesCurrent.setText(ReportsWelcomeLabels.seriesCurrent(period, snapshot, asOf));
		seriesPrevious.setText(ReportsWelcomeLabels.seriesPrevious(period, previousName, snapshot));
		chartHeading.revalidate();
		chartComparisonNote.setVisible(ReportsWelcomeLabels.current(period, snapshot, asOf));
		chartTargetNote.setVisible(
				ReportsWelcomeLabels.current(period, snapshot, asOf) && period != ReportsWelcomePeriod.ROLLING_YEAR);
	}

	private void updateHeaderLayout() {
		if (detailHeading.getWidth() == 0) {
			return;
		}
		boolean wrap = detailTitleBlock.getPreferredSize().width + detailNavigation.getPreferredSize().width
				+ 8 > detailHeading.getWidth();
		if (wrap == navigationWrapped) {
			return;
		}
		navigationWrapped = wrap;
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = wrap ? 0 : 1;
		c.gridy = wrap ? 1 : 0;
		c.weightx = wrap ? 1 : 0;
		c.anchor = GridBagConstraints.EAST;
		c.fill = GridBagConstraints.HORIZONTAL;
		((GridBagLayout) detailHeading.getLayout()).setConstraints(detailNavigation, c);
		detailHeading.revalidate();
	}

	private void updateChartHeaderLayout() {
		if (chartHeading.getWidth() == 0) {
			return;
		}
		boolean wrap = chartTitle.getPreferredSize().width + chartLegend.getPreferredSize().width + 8 > chartHeading
				.getWidth();
		if (wrap == legendWrapped) {
			return;
		}
		legendWrapped = wrap;
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = wrap ? 0 : 1;
		c.gridy = wrap ? 1 : 0;
		c.weightx = wrap ? 1 : 0;
		c.anchor = GridBagConstraints.EAST;
		c.fill = GridBagConstraints.HORIZONTAL;
		((GridBagLayout) chartHeading.getLayout()).setConstraints(chartLegend, c);
		chartHeading.revalidate();
	}

	private void setNavigationLabel(JButton button, String label) {
		button.setText(label);
		button.setPreferredSize(
				new Dimension(Math.max(120, button.getFontMetrics(button.getFont()).stringWidth(label) + 24), 48));
	}

	private void showCard(int index, SalesSummaryComparison comparison) {
		cardValues[index].setText(Formats.CURRENCY.formatValue(comparison.getCurrent().getNetSales()));
		if (comparison.hasPreviousSales()) {
			double percent = comparison.netDeltaPercent();
			cardDeltas[index].setText(delta(percent));
			((DeltaBadge) cardDeltas[index]).setTrend(percent);
			cardBaselineValues[index].setText(Formats.CURRENCY.formatValue(comparison.getPrevious().getNetSales()));
		} else {
			cardDeltas[index].setText(AppLocal.getIntString("reports.welcome.noPrevious"));
			((DeltaBadge) cardDeltas[index]).setTrend(null);
			cardBaselineValues[index].setText(" ");
		}
	}

	private void showDetail(SalesSummaryComparison value) {
		SalesSummary current = value.getCurrent();
		totalRange.setText(selected.rangeLabel(focusAsOf));
		totalRange.setToolTipText(selected.rangeLabel(focusAsOf));
		totalValue.setText(Formats.CURRENCY.formatValue(current.getNetSales()));
		totalDelta.setText(value.hasPreviousSales()
				? delta(value.netDeltaPercent())
				: AppLocal.getIntString("reports.welcome.noPrevious"));
		((DeltaBadge) totalDelta).setTrend(value.hasPreviousSales() ? value.netDeltaPercent() : null);
		totalBaseline.setText(value.hasPreviousSales() ? selected.baselineLabel() : " ");
		totalBaselineValue.setText(
				value.hasPreviousSales() ? Formats.CURRENCY.formatValue(value.getPrevious().getNetSales()) : " ");
		receipts.setText(Integer.toString(current.getReceiptCount()));
		receiptsDelta.setText(value.getPrevious().getReceiptCount() == 0
				? AppLocal.getIntString("reports.welcome.noPrevious")
				: delta((current.getReceiptCount() - value.getPrevious().getReceiptCount()) * 100.0
						/ value.getPrevious().getReceiptCount()));
		setDeltaColor(receiptsDelta, current.getReceiptCount() - value.getPrevious().getReceiptCount());
		average.setText(Formats.CURRENCY.formatValue(current.getAverageReceipt()));
		averageDelta.setText(value.getPrevious().getAverageReceipt() == 0
				? AppLocal.getIntString("reports.welcome.noPrevious")
				: delta((current.getAverageReceipt() - value.getPrevious().getAverageReceipt()) * 100.0
						/ value.getPrevious().getAverageReceipt()));
		setDeltaColor(averageDelta, current.getAverageReceipt() - value.getPrevious().getAverageReceipt());
		refunds.setText(Formats.CURRENCY.formatValue(Math.abs(current.getRefunds())));
		refundCount.setText(AppLocal.getIntString(
				current.getRefundCount() == 1 ? "reports.welcome.refundCountOne" : "reports.welcome.refundCount",
				current.getRefundCount()));
		loyaltyCount.setText(Integer.toString(value.getLoyaltyCurrent().getCount()));
		loyaltyValue.setText(Formats.CURRENCY.formatValue(value.getLoyaltyCurrent().getValue()) + " "
				+ AppLocal.getIntString("reports.welcome.loyaltyValue"));
		loyaltyDelta.setText(AppLocal.getIntString("reports.welcome.loyaltyLastYear") + ": "
				+ value.getLoyaltyPrevious().getCount());
	}

	private void loadFocus() {
		final int generation = ++focusGeneration;
		final ReportsWelcomePeriod period = selected;
		final Date snapshot = focusAsOf;
		updatePeriodNavigation();
		chart.setTrend(null);
		chartPartialLegend.setVisible(false);
		for (JLabel label : new JLabel[]{totalValue, totalDelta, totalBaseline, totalBaselineValue, receipts,
				receiptsDelta, average, averageDelta, loyaltyCount, loyaltyValue, loyaltyDelta, refunds, refundCount}) {
			label.setText("…");
		}
		new SwingWorker<FocusData, Void>() {
			@Override
			protected FocusData doInBackground() throws Exception {
				return new FocusData(
						repository.loadComparison(app.getSession().getConnection(), period.current(snapshot),
								period.previous(snapshot)),
						trends.load(app.getSession().getConnection(), period, snapshot), whatSoldRepository.load(
								app.getSession().getConnection(), period.current(snapshot), period.previous(snapshot)));
			}

			@Override
			protected void done() {
				if (generation != focusGeneration) {
					return;
				}
				try {
					FocusData focus = get();
					showDetail(focus.comparison);
					showTrend(focus.trend, snapshot);
					whatSoldData = focus.whatSold;
					showWhatSold();
				} catch (Exception e) {
					new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotloadreport"), e)
							.show(JPanelReportsWelcome.this);
				}
			}
		}.execute();
	}

	private void showTrend(SalesTrend trend, Date snapshot) {
		chart.setTrend(trend);
		chartPartialLegend.setVisible(trend.partialMonthIndex >= 0);
		if (trend.partialMonthIndex >= 0) {
			chartPartialLabel.setText(ReportsWelcomeLabels.partialMonthLegend(snapshot));
		}
	}

	private static final class FocusData {
		private final SalesSummaryComparison comparison;
		private final SalesTrend trend;
		private final WelcomeSalesRepository.WelcomeSalesData whatSold;

		private FocusData(SalesSummaryComparison comparison, SalesTrend trend,
				WelcomeSalesRepository.WelcomeSalesData whatSold) {
			this.comparison = comparison;
			this.trend = trend;
			this.whatSold = whatSold;
		}
	}

	private String delta(double percent) {
		return String.format(Locale.getDefault(), "%s%+.1f %%", percent > 0 ? "↑ " : percent < 0 ? "↓ " : "→ ",
				percent);
	}

	private void setDeltaColor(JLabel label, double change) {
		label.setForeground(change > 0
				? RetailPOSColors.success()
				: change < 0 ? RetailPOSColors.danger() : RetailPOSColors.inkMuted());
	}

	private void updateDates() {
		if (today == null) {
			return;
		}
		Locale locale = Locale.getDefault();
		String pattern = "es".equals(locale.getLanguage()) ? "EEEE d 'de' MMMM" : "EEEE d MMMM";
		String date = new SimpleDateFormat(pattern, locale).format(asOf);
		today.setText(date.substring(0, 1).toUpperCase(locale) + date.substring(1));
		updated.setText(AppLocal.getIntString("reports.welcome.updated",
				DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(asOf)));
		for (int i = 0; i < cardRanges.length; i++) {
			if (cardRanges[i] != null) {
				cardRanges[i].setText(ReportsWelcomePeriod.values()[i].rangeLabel(asOf));
			}
		}
	}

	static Date lastClosedDay(Date currentDate) {
		Calendar closed = Calendar.getInstance();
		closed.setTime(currentDate);
		closed.add(Calendar.DAY_OF_MONTH, -1);
		closed.set(Calendar.HOUR_OF_DAY, 0);
		closed.set(Calendar.MINUTE, 0);
		closed.set(Calendar.SECOND, 0);
		closed.set(Calendar.MILLISECOND, 0);
		return closed.getTime();
	}

	private void styleButton(JButton button, int radius) {
		button.setOpaque(false);
		button.setBackground(RetailPOSColors.surface100());
		button.setForeground(RetailPOSColors.ink());
		button.setBorder(BorderFactory.createCompoundBorder(new RoundedLineBorder(RetailPOSColors.border(), radius),
				BorderFactory.createEmptyBorder(1, 1, 1, 1)));
	}

	private void scrollToReports() {
		if (scroll != null) {
			scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum());
		}
	}

	/**
	 * Track the viewport width so every grid row receives the same available space.
	 */
	private static final class ScrollBody extends JPanel implements Scrollable {
		@Override
		public Dimension getPreferredScrollableViewportSize() {
			return getPreferredSize();
		}

		@Override
		public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
			return 24;
		}

		@Override
		public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
			return visibleRect.height;
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

	private static final class ResponsiveCards extends JPanel {
		private ResponsiveCards() {
			setOpaque(false);
			setLayout(new GridLayout(0, 2, 12, 12));
		}

		@Override
		public void doLayout() {
			int columns = getWidth() < 700 ? 1 : 2;
			GridLayout layout = (GridLayout) getLayout();
			if (layout.getColumns() != columns) {
				layout.setColumns(columns);
			}
			super.doLayout();
		}
	}

	private static final class WhatSoldGrid extends JPanel {
		private WhatSoldGrid() {
			setLayout(new GridBagLayout());
		}

		private void addCard(Component component, int column, int row) {
			GridBagConstraints constraints = new GridBagConstraints();
			constraints.gridx = column;
			constraints.gridy = row;
			constraints.weightx = 1;
			constraints.weighty = 0;
			constraints.fill = GridBagConstraints.HORIZONTAL;
			constraints.anchor = GridBagConstraints.NORTHWEST;
			constraints.insets = new Insets(0, column == 0 ? 0 : 6, 12, column == 0 ? 6 : 0);
			add(component, constraints);
		}
	}

	private static final class SharedRow extends JPanel {
		private int column;

		private SharedRow() {
			setOpaque(false);
			setLayout(new GridBagLayout());
			setPreferredSize(new Dimension(300, 48));
			setMinimumSize(new Dimension(0, 48));
			setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
		}

		private void addFixed(Component component, int width) {
			GridBagConstraints constraints = constraints();
			constraints.weightx = 0;
			constraints.fill = GridBagConstraints.NONE;
			constraints.insets = new Insets(0, column == 0 ? 0 : 8, 0, 0);
			component.setPreferredSize(new Dimension(width, component.getPreferredSize().height));
			component.setMinimumSize(new Dimension(width, component.getMinimumSize().height));
			component.setMaximumSize(new Dimension(width, Integer.MAX_VALUE));
			add(component, constraints);
			column++;
		}

		private void addFlexible(Component component) {
			GridBagConstraints constraints = constraints();
			constraints.weightx = 1;
			constraints.fill = GridBagConstraints.HORIZONTAL;
			constraints.insets = new Insets(0, column == 0 ? 0 : 8, 0, 0);
			add(component, constraints);
			column++;
		}

		private GridBagConstraints constraints() {
			GridBagConstraints constraints = new GridBagConstraints();
			constraints.gridx = column;
			constraints.gridy = 0;
			constraints.anchor = GridBagConstraints.CENTER;
			return constraints;
		}
	}

	private static final class ShareBar extends javax.swing.JComponent {
		private final double share;

		private ShareBar(double share) {
			this.share = share;
			setOpaque(false);
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g2 = (Graphics2D) graphics.create();
			g2.setColor(RetailPOSColors.surface200());
			g2.fillRect(0, 0, getWidth(), getHeight());
			g2.setColor(RetailPOSColors.brand());
			g2.fillRect(0, 0, (int) Math.round(getWidth() * share), getHeight());
			g2.setColor(RetailPOSColors.border());
			g2.drawRect(0, 0, Math.max(0, getWidth() - 1), Math.max(0, getHeight() - 1));
			g2.dispose();
		}
	}

	private static final class EllipsisLabel extends JLabel {
		private EllipsisLabel(String text) {
			super(text == null ? "" : text);
		}

		@Override
		public Dimension getPreferredSize() {
			Dimension preferred = super.getPreferredSize();
			return new Dimension(Math.min(preferred.width, 240), preferred.height);
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			FontMetrics metrics = graphics.getFontMetrics(getFont());
			int available = getWidth() - getInsets().left - getInsets().right;
			if (metrics.stringWidth(getText()) <= available) {
				super.paintComponent(graphics);
				return;
			}
			String suffix = "...";
			int end = getText().length();
			while (end > 0 && metrics.stringWidth(getText().substring(0, end) + suffix) > available)
				end--;
			Graphics2D g2 = (Graphics2D) graphics.create();
			g2.setColor(getForeground());
			g2.drawString(getText().substring(0, end) + suffix, getInsets().left,
					(getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
			g2.dispose();
		}
	}

	private final class PaymentStackBar extends javax.swing.JComponent {
		private final List<WelcomeSalesRepository.PaymentInsight> rows;

		private PaymentStackBar(List<WelcomeSalesRepository.PaymentInsight> payments) {
			rows = new java.util.ArrayList<>();
			for (WelcomeSalesRepository.PaymentInsight payment : payments) {
				if (payment.getAmount() > 0)
					rows.add(payment);
			}
			setToolTipText(AppLocal.getIntString("reports.welcome.payments"));
			setPreferredSize(new Dimension(100, 12));
			setMinimumSize(new Dimension(0, 12));
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g2 = (Graphics2D) graphics.create();
			g2.setColor(RetailPOSColors.surface200());
			g2.fillRect(0, 0, getWidth(), getHeight());
			int x = 0;
			int[] widths = segmentWidths(getWidth());
			for (int i = 0; i < rows.size(); i++) {
				WelcomeSalesRepository.PaymentInsight row = rows.get(i);
				int end = x + widths[i];
				g2.setColor(PaymentSalesRepository.CARD.equals(row.getCategory())
						? RetailPOSColors.brand()
						: PaymentSalesRepository.CASH.equals(row.getCategory())
								? RetailPOSColors.brandSubtle()
								: RetailPOSColors.surface200());
				g2.fillRect(x, 0, Math.max(0, end - x), getHeight());
				x = end;
			}
			g2.setColor(RetailPOSColors.border());
			g2.drawRect(0, 0, Math.max(0, getWidth() - 1), Math.max(0, getHeight() - 1));
			g2.dispose();
		}

		int[] segmentWidths(int width) {
			int[] result = new int[rows.size()];
			double total = 0;
			for (WelcomeSalesRepository.PaymentInsight row : rows)
				total += row.getAmount();
			int used = 0;
			for (int i = 0; i < rows.size(); i++) {
				result[i] = i == rows.size() - 1
						? Math.max(0, width - used)
						: (int) Math.round(width * rows.get(i).getAmount() / total);
				used += result[i];
			}
			return result;
		}
	}

	private static final class RoundedPanel extends JPanel {
		private final int radius;

		private RoundedPanel(int radius, java.awt.Color background) {
			this.radius = radius;
			setBackground(background);
			setOpaque(false);
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g2 = (Graphics2D) graphics.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(getBackground());
			g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
			g2.clip(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), radius, radius));
			super.paintComponent(g2);
			g2.dispose();
		}
	}

	private static final class DeltaBadge extends JLabel {
		private java.awt.Color trend;

		private void setTrend(Double value) {
			trend = value == null
					? null
					: value > 0
							? RetailPOSColors.success()
							: value < 0 ? RetailPOSColors.danger() : RetailPOSColors.inkMuted();
			setForeground(trend == null ? RetailPOSColors.inkMuted() : trend);
			setBorder(trend == null ? null : BorderFactory.createEmptyBorder(2, 7, 2, 7));
			repaint();
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			if (trend != null) {
				Graphics2D g2 = (Graphics2D) graphics.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(new java.awt.Color(trend.getRed(), trend.getGreen(), trend.getBlue(), 32));
				g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
				g2.dispose();
			}
			super.paintComponent(graphics);
		}
	}

	private static final class ChipLabel extends JLabel {
		private ChipLabel(String text) {
			super(text);
			setForeground(RetailPOSColors.surface100());
			setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g2 = (Graphics2D) graphics.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(RetailPOSColors.ink());
			g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
			g2.dispose();
			super.paintComponent(graphics);
		}
	}

	private static final class RoundedButton extends JButton {
		private final int radius;

		private RoundedButton(int radius) {
			this.radius = radius;
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g2 = (Graphics2D) graphics.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(getBackground());
			g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
			g2.clip(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), radius, radius));
			super.paintComponent(g2);
			g2.dispose();
		}
	}

	private static final class SegmentButton extends JToggleButton {
		private SegmentButton(String text) {
			super(text);
			setOpaque(false);
			setContentAreaFilled(false);
			setForeground(RetailPOSColors.ink());
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			if (isSelected()) {
				Graphics2D g2 = (Graphics2D) graphics.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(RetailPOSColors.surface100());
				g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
				g2.dispose();
			}
			super.paintComponent(graphics);
		}
	}

	private static final class LegendMarker extends javax.swing.JComponent {
		private final boolean filled;

		private LegendMarker(boolean filled) {
			this.filled = filled;
			setPreferredSize(new Dimension(12, 16));
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g2 = (Graphics2D) graphics.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(filled ? RetailPOSColors.brand() : RetailPOSColors.borderStrong());
			if (filled) {
				g2.fillRoundRect(1, 3, 10, 10, 3, 3);
			} else {
				g2.drawRoundRect(1, 3, 10, 10, 3, 3);
			}
			g2.dispose();
		}
	}

	private static final class SplitLegendMarker extends javax.swing.JComponent {
		private SplitLegendMarker() {
			setPreferredSize(new Dimension(16, 18));
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g2 = (Graphics2D) graphics.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(RetailPOSColors.borderStrong());
			g2.setStroke(new java.awt.BasicStroke(1f));
			g2.drawRect(2, 9, 12, 7);
			g2.setStroke(new java.awt.BasicStroke(1f, java.awt.BasicStroke.CAP_BUTT, java.awt.BasicStroke.JOIN_MITER,
					1f, new float[]{3f, 3f}, 0f));
			g2.drawRect(2, 2, 12, 7);
			g2.dispose();
		}
	}

	private static final class RoundedLineBorder extends AbstractBorder {
		private final java.awt.Color color;
		private final int radius;
		private final boolean selected;

		private RoundedLineBorder(java.awt.Color color, int radius) {
			this(color, radius, false);
		}

		private RoundedLineBorder(java.awt.Color color, int radius, boolean selected) {
			this.color = color;
			this.radius = radius;
			this.selected = selected;
		}

		@Override
		public void paintBorder(Component component, Graphics graphics, int x, int y, int width, int height) {
			Graphics2D g2 = (Graphics2D) graphics.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(color);
			g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
			if (selected) {
				g2.setStroke(new java.awt.BasicStroke(2f));
				g2.drawRoundRect(x + 1, y + 1, width - 3, height - 3, radius, radius);
			}
			g2.dispose();
		}

		@Override
		public Insets getBorderInsets(Component component) {
			return new Insets(1, 1, 1, 1);
		}
	}
}
