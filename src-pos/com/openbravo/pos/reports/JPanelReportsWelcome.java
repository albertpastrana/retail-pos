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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import javax.swing.border.AbstractBorder;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Action;
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
	private final ReportAttentionRepository attentions = new ReportAttentionRepository();
	private final JButton[] periodButtons = new JButton[ReportsWelcomePeriod.values().length];
	private final JLabel[] cardValues = new JLabel[ReportsWelcomePeriod.values().length];
	private final JLabel[] cardDeltas = new JLabel[ReportsWelcomePeriod.values().length];
	private final JLabel[] cardRanges = new JLabel[ReportsWelcomePeriod.values().length];
	private final JLabel[] cardBaselines = new JLabel[ReportsWelcomePeriod.values().length];
	private final JLabel[] cardBaselineValues = new JLabel[ReportsWelcomePeriod.values().length];
	private final JButton[] periodPrevious = new JButton[ReportsWelcomePeriod.values().length];
	private final JButton[] periodNext = new JButton[ReportsWelcomePeriod.values().length];
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
	private SalesTrendChart chart;
	private JPanel notices;
	private JLabel loyaltyCount;
	private JLabel loyaltyValue;
	private JLabel loyaltyDelta;
	private SalesSummaryComparison[] loaded;
	private Date asOf = new Date();
	private Date focusAsOf = asOf;
	private int focusGeneration;
	private JScrollPane scroll;
	private AppView app;
	private ReportsWelcomePeriod selected = ReportsWelcomePeriod.WEEK;

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
		JLabel explanation = new JLabel(AppLocal.getIntString("reports.welcome.explanation"));
		explanation.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(12f));
		explanation.setForeground(RetailPOSColors.inkMuted());
		explanation.setAlignmentX(RIGHT_ALIGNMENT);
		updated = new JLabel();
		updated.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(12f));
		updated.setForeground(RetailPOSColors.inkMuted());
		updated.setAlignmentX(RIGHT_ALIGNMENT);
		context.add(explanation);
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
		body.add(createCards(), section);
		section.gridy = 1;
		section.insets = new Insets(0, 0, 12, 0);
		body.add(createDetail(), section);
		section.gridy = 2;
		section.insets = new Insets(0, 0, 24, 0);
		body.add(createOverview(), section);
		section.gridy = 3;
		section.insets = new Insets(0, 0, 0, 0);
		body.add(createReports(), section);
		section.gridy = 4;
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

	private JPanel createCards() {
		JPanel cards = new JPanel(new GridLayout(1, 4, 12, 0));
		cards.setOpaque(false);
		int index = 0;
		for (ReportsWelcomePeriod period : ReportsWelcomePeriod.values()) {
			final int cardIndex = index;
			JButton button = new RoundedButton(16);
			button.setLayout(new BorderLayout());
			button.setPreferredSize(new Dimension(232, 184));
			button.setMinimumSize(new Dimension(200, 184));
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
			cardHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
			cardHeader.add(label, BorderLayout.CENTER);
			if (period != ReportsWelcomePeriod.ROLLING_YEAR) {
				JPanel navigation = new JPanel(new GridLayout(1, 2, 4, 0));
				navigation.setOpaque(false);
				JButton previous = periodButton("<", "reports.welcome.previousPeriod");
				JButton next = periodButton(">", "reports.welcome.nextPeriod");
				periodPrevious[index] = previous;
				periodNext[index] = next;
				previous.addActionListener(e -> changePeriod(period, -1));
				next.addActionListener(e -> changePeriod(period, 1));
				navigation.add(previous);
				navigation.add(next);
				cardHeader.add(navigation, BorderLayout.EAST);
			}
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
		selectPeriod(selected);
		return cards;
	}

	private JPanel createDetail() {
		JPanel panel = new RoundedPanel(16, RetailPOSColors.surface100());
		panel.setLayout(new GridLayout(1, 4));
		panel.setBorder(BorderFactory.createCompoundBorder(new RoundedLineBorder(RetailPOSColors.border(), 16),
				BorderFactory.createEmptyBorder(12, 16, 12, 16)));
		panel.setPreferredSize(new Dimension(0, 104));
		JPanel receiptsCell = detailCell("label.reportreceipts", false);
		receipts = detailNumber(receiptsCell);
		receiptsDelta = detailHint(receiptsCell);
		panel.add(receiptsCell);
		JPanel averageCell = detailCell("reports.welcome.average", true);
		average = detailNumber(averageCell);
		averageDelta = detailHint(averageCell);
		panel.add(averageCell);
		JPanel loyaltyCell = detailCell("reports.welcome.loyalty", true);
		JPanel loyaltyAmount = new JPanel();
		loyaltyAmount.setOpaque(false);
		loyaltyAmount.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEADING, 12, 0));
		loyaltyCount = new JLabel("-");
		loyaltyCount.setFont(RetailPOSTheme.PLEX_MONO_SEMIBOLD.deriveFont(20f));
		loyaltyValue = new JLabel(" ");
		loyaltyValue.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f));
		loyaltyValue.setForeground(RetailPOSColors.inkMuted());
		loyaltyAmount.add(loyaltyCount);
		loyaltyAmount.add(loyaltyValue);
		loyaltyAmount.setAlignmentX(LEFT_ALIGNMENT);
		loyaltyAmount.setMaximumSize(new Dimension(Integer.MAX_VALUE, loyaltyAmount.getPreferredSize().height));
		loyaltyCell.add(loyaltyAmount);
		loyaltyDelta = detailHint(loyaltyCell);
		panel.add(loyaltyCell);
		JPanel refundsCell = detailCell("label.reportrefunds", true);
		refunds = detailNumber(refundsCell);
		refundCount = detailHint(refundsCell);
		panel.add(refundsCell);
		return panel;
	}

	private JPanel detailCell(String key, boolean divider) {
		JPanel cell = new JPanel();
		cell.setOpaque(false);
		cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
		cell.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0, divider ? 1 : 0, 0, 0, RetailPOSColors.border()),
				BorderFactory.createEmptyBorder(0, divider ? 16 : 4, 0, 8)));
		JLabel caption = new JLabel(AppLocal.getIntString(key).toUpperCase(Locale.getDefault()));
		caption.setFont(RetailPOSTheme.MANROPE_BOLD.deriveFont(12f));
		caption.setForeground(RetailPOSColors.inkMuted());
		cell.add(caption);
		cell.add(Box.createVerticalStrut(4));
		return cell;
	}

	private JLabel detailNumber(JPanel cell) {
		JLabel value = new JLabel("-");
		value.setFont(RetailPOSTheme.PLEX_MONO_SEMIBOLD.deriveFont(20f));
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

	private JPanel createOverview() {
		JPanel overview = new JPanel(new GridBagLayout());
		overview.setOpaque(false);
		GridBagConstraints left = new GridBagConstraints();
		left.gridx = 0;
		left.weightx = 2;
		left.fill = GridBagConstraints.BOTH;
		left.insets = new Insets(0, 0, 0, 12);
		JPanel chartCard = new RoundedPanel(16, RetailPOSColors.surface100());
		chartCard.setLayout(new BorderLayout(0, 8));
		chartCard.setBorder(BorderFactory.createCompoundBorder(new RoundedLineBorder(RetailPOSColors.border(), 16),
				BorderFactory.createEmptyBorder(16, 18, 16, 18)));
		chartTitle = new JLabel();
		chartTitle.setFont(RetailPOSTheme.MANROPE_BOLD.deriveFont(16f));
		chart = new SalesTrendChart();
		JLabel legend = new JLabel(AppLocal.getIntString("reports.welcome.chartLegend"));
		legend.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f));
		legend.setForeground(RetailPOSColors.inkMuted());
		chartComparisonNote = new JLabel(AppLocal.getIntString("reports.welcome.elapsedNote"));
		chartComparisonNote.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f));
		chartComparisonNote.setForeground(RetailPOSColors.inkMuted());
		chartTargetNote = new JLabel(AppLocal.getIntString("reports.welcome.futureNote"));
		chartTargetNote.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f));
		chartTargetNote.setForeground(RetailPOSColors.inkMuted());
		JPanel caption = new JPanel();
		caption.setOpaque(false);
		caption.setLayout(new BoxLayout(caption, BoxLayout.Y_AXIS));
		caption.add(legend);
		caption.add(chartComparisonNote);
		caption.add(chartTargetNote);
		chartCard.add(chartTitle, BorderLayout.NORTH);
		chartCard.add(chart, BorderLayout.CENTER);
		chartCard.add(caption, BorderLayout.SOUTH);
		overview.add(chartCard, left);
		GridBagConstraints right = new GridBagConstraints();
		right.gridx = 1;
		right.weightx = 1;
		right.fill = GridBagConstraints.BOTH;
		JPanel noticeCard = new RoundedPanel(16, RetailPOSColors.surface100());
		noticeCard.setLayout(new BorderLayout(0, 12));
		noticeCard.setBorder(BorderFactory.createCompoundBorder(new RoundedLineBorder(RetailPOSColors.border(), 16),
				BorderFactory.createEmptyBorder(16, 18, 16, 18)));
		JLabel heading = new JLabel(AppLocal.getIntString("reports.welcome.attention"));
		heading.setFont(RetailPOSTheme.MANROPE_BOLD.deriveFont(16f));
		notices = new JPanel();
		notices.setOpaque(false);
		notices.setLayout(new BoxLayout(notices, BoxLayout.Y_AXIS));
		noticeCard.add(heading, BorderLayout.NORTH);
		noticeCard.add(notices, BorderLayout.CENTER);
		overview.add(noticeCard, right);
		return overview;
	}

	private void selectPeriod(ReportsWelcomePeriod period) {
		selected = period;
		focusAsOf = asOf;
		for (int i = 0; i < periodButtons.length; i++) {
			boolean active = ReportsWelcomePeriod.values()[i] == period;
			periodButtons[i].setBackground(active ? RetailPOSColors.brandSubtle() : RetailPOSColors.surface100());
			periodButtons[i].setBorder(BorderFactory.createCompoundBorder(
					new RoundedLineBorder(active ? RetailPOSColors.brand() : RetailPOSColors.border(), 16, active),
					BorderFactory.createEmptyBorder(1, 1, 1, 1)));
			if (periodPrevious[i] != null) {
				periodPrevious[i].setVisible(active);
				periodNext[i].setVisible(active);
			}
		}
		updatePeriodNavigation();
		if (loaded != null) {
			showDetail(loaded[selected.ordinal()]);
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

	private JButton periodButton(String text, String tooltipKey) {
		JButton button = new JButton(text);
		button.setToolTipText(AppLocal.getIntString(tooltipKey));
		button.getAccessibleContext().setAccessibleName(AppLocal.getIntString(tooltipKey));
		button.setFont(RetailPOSTheme.MANROPE_BOLD.deriveFont(22f));
		button.setPreferredSize(new Dimension(32, 32));
		button.setMinimumSize(button.getPreferredSize());
		button.setMaximumSize(button.getPreferredSize());
		styleButton(button, 10);
		button.setBorder(BorderFactory.createEmptyBorder());
		button.setContentAreaFilled(false);
		button.setForeground(RetailPOSColors.inkMuted());
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
		for (int i = 0; i < periodNext.length; i++) {
			if (periodNext[i] == null) {
				continue;
			}
			boolean active = ReportsWelcomePeriod.values()[i] == selected;
			periodPrevious[i].setVisible(active);
			periodNext[i].setVisible(active);
			if (active) {
				periodNext[i].setEnabled(!selected.current(focusAsOf).getStartInclusive()
						.equals(selected.current(asOf).getStartInclusive()));
			}
		}
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
		refundCount.setText(AppLocal.getIntString("reports.welcome.refundCount", current.getRefundCount()));
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
		chartTitle.setText(
				AppLocal.getIntString(period == ReportsWelcomePeriod.WEEK || period == ReportsWelcomePeriod.MONTH
						? "reports.welcome.chartDays"
						: "reports.welcome.chartMonths"));
		chartTargetNote.setVisible(period != ReportsWelcomePeriod.ROLLING_YEAR);
		chart.setTrend(null);
		notices.removeAll();
		JLabel loading = new JLabel(AppLocal.getIntString("reports.welcome.loading"));
		loading.setForeground(RetailPOSColors.inkMuted());
		notices.add(loading);
		notices.revalidate();
		new SwingWorker<FocusData, Void>() {
			@Override
			protected FocusData doInBackground() throws Exception {
				return new FocusData(
						repository.loadComparison(app.getSession().getConnection(), period.current(snapshot),
								period.previous(snapshot)),
						trends.load(app.getSession().getConnection(), period, snapshot),
						attentions.load(app.getSession().getConnection(), period.current(snapshot), snapshot));
			}

			@Override
			protected void done() {
				if (generation != focusGeneration) {
					return;
				}
				try {
					FocusData focus = get();
					showDetail(focus.comparison);
					chart.setTrend(focus.trend);
					showNotices(focus.notices);
				} catch (Exception e) {
					new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotloadreport"), e)
							.show(JPanelReportsWelcome.this);
				}
			}
		}.execute();
	}

	private void showNotices(java.util.List<ReportAttention> items) {
		notices.removeAll();
		if (items.isEmpty()) {
			JLabel empty = new JLabel(AppLocal.getIntString("reports.welcome.noAttention"));
			empty.setForeground(RetailPOSColors.inkMuted());
			notices.add(empty);
		}
		for (ReportAttention notice : items) {
			Action action = app.getAppUserView().getTaskAction(notice.task);
			if (action == null) {
				continue;
			}
			JButton button = new JButton(action);
			button.setText("›  " + notice.label);
			button.setToolTipText(notice.label);
			button.setIcon(null);
			button.setFont(RetailPOSTheme.MANROPE_SEMIBOLD.deriveFont(13f));
			button.setForeground(notice.color);
			button.setHorizontalAlignment(SwingConstants.LEADING);
			button.setContentAreaFilled(false);
			button.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));
			button.setMinimumSize(new Dimension(0, 48));
			button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
			notices.add(button);
		}
		if (notices.getComponentCount() == 0) {
			JLabel empty = new JLabel(AppLocal.getIntString("reports.welcome.noAttention"));
			empty.setForeground(RetailPOSColors.inkMuted());
			notices.add(empty);
		}
		notices.revalidate();
		notices.repaint();
	}

	private static final class FocusData {
		private final SalesSummaryComparison comparison;
		private final SalesTrend trend;
		private final java.util.List<ReportAttention> notices;

		private FocusData(SalesSummaryComparison comparison, SalesTrend trend,
				java.util.List<ReportAttention> notices) {
			this.comparison = comparison;
			this.trend = trend;
			this.notices = notices;
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
		String pattern = "en".equals(locale.getLanguage()) ? "EEEE d MMMM" : "EEEE d 'de' MMMM";
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
		button.putClientProperty("FlatLaf.style", "arc: " + radius + ";");
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
				g2.setStroke(new java.awt.BasicStroke(3f));
				g2.drawLine(x + 1, y + 12, x + 1, y + height - 13);
			}
			g2.dispose();
		}

		@Override
		public Insets getBorderInsets(Component component) {
			return new Insets(1, 1, 1, 1);
		}
	}
}
