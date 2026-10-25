package com.openbravo.pos.reports;

import com.openbravo.beans.JCalendarDialog;
import com.openbravo.format.Formats;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.theme.RetailPOSColors;
import com.openbravo.pos.theme.RetailPOSTheme;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Calendar;
import java.util.Date;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ButtonGroup;
import javax.swing.AbstractAction;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;

/** Shared date-only period selector for management reports. */
final class ReportPeriodSelector extends JPanel {

	private Date startDate;
	private Date endDate;
	private JTextField startField;
	private JTextField endField;
	private final Runnable onChange;
	private JPanel customDates;
	private JPanel weekNavigation;
	private JLabel validation;
	private JLabel range;
	private JToggleButton[] presets;
	private SearchHeader searchHeader;
	private int selectedPreset = 3;

	ReportPeriodSelector() {
		this(null);
	}

	/**
	 * Preset mode is opt-in; existing report screens retain their date pickers and
	 * Load buttons.
	 */
	ReportPeriodSelector(Runnable onChange) {
		this.onChange = onChange;
		setLayout(new BorderLayout(0, 12));
		setBackground(RetailPOSColors.surface100());
		setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(RetailPOSColors.border()),
				BorderFactory.createEmptyBorder(16, 16, 16, 16)));
		customDates = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
		customDates.setOpaque(false);
		customDates.add(new JLabel(AppLocal.getIntString("label.reportstart")));
		customDates.add(createDatePicker(true));
		customDates.add(new JLabel(AppLocal.getIntString("label.reportend")));
		customDates.add(createDatePicker(false));
		if (onChange == null) {
			add(customDates, BorderLayout.CENTER);
		} else {
			JPanel top = new WrappingPresets();
			top.setOpaque(false);
			String[] names = {"today", "yesterday", "week", "month", "previousMonth", "custom"};
			ButtonGroup buttons = new ButtonGroup();
			presets = new JToggleButton[names.length];
			for (int i = 0; i < names.length; i++) {
				final int index = i;
				JToggleButton button = new PresetButton(AppLocal.getIntString("reports.product.period." + names[i]));
				button.setFont(RetailPOSTheme.MANROPE_SEMIBOLD.deriveFont(14f));
				Dimension preferred = button.getPreferredSize();
				button.setPreferredSize(new Dimension(preferred.width, 44));
				activateOnEnter(button);
				button.addActionListener(e -> choose(index));
				buttons.add(button);
				top.add(button);
				presets[i] = button;
			}
			JPanel detail = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
			detail.setOpaque(false);
			weekNavigation = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
			weekNavigation.setOpaque(false);
			for (int delta : new int[]{-7, 7}) {
				JButton button = new JButton(
						AppLocal.getIntString(delta < 0 ? "reports.product.previousWeek" : "reports.product.nextWeek"));
				button.setPreferredSize(new Dimension(150, 48));
				button.addActionListener(e -> {
					startDate = shift(startDate, Calendar.DAY_OF_MONTH, delta);
					endDate = shift(endDate, Calendar.DAY_OF_MONTH, delta);
					showRange();
					onChange.run();
				});
				weekNavigation.add(button);
			}
			validation = new JLabel(AppLocal.getIntString("message.reportperiod"));
			validation.setForeground(RetailPOSColors.warning());
			validation.setVisible(false);
			range = new JLabel();
			range.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f));
			range.setForeground(RetailPOSColors.inkMuted());
			detail.add(range);
			detail.add(customDates);
			detail.add(weekNavigation);
			detail.add(validation);
			searchHeader = new SearchHeader(top);
			add(searchHeader, BorderLayout.NORTH);
			add(detail, BorderLayout.CENTER);
			addComponentListener(new java.awt.event.ComponentAdapter() {
				@Override
				public void componentResized(java.awt.event.ComponentEvent e) {
					searchHeader.revalidate();
				}
			});
		}
		reset();
	}

	void reset() {
		Calendar today = Calendar.getInstance();
		endDate = startOfDay(today.getTime());
		Calendar yesterday = (Calendar) today.clone();
		yesterday.add(Calendar.DAY_OF_MONTH, -1);
		startDate = startOfDay(yesterday.getTime());
		startField.setText(Formats.DATE.formatValue(startDate));
		endField.setText(Formats.DATE.formatValue(endDate));
		if (onChange != null) {
			choose(3, false);
		}
	}

	private void choose(int preset) {
		choose(preset, true);
	}

	private void choose(int preset, boolean notify) {
		selectedPreset = preset;
		presets[preset].setSelected(true);
		customDates.setVisible(preset == 5);
		weekNavigation.setVisible(preset == 2);
		validation.setVisible(false);
		if (preset != 5) {
			Calendar today = Calendar.getInstance();
			startDate = startOfDay(today.getTime());
			endDate = startDate;
			switch (preset) {
				case 1 :
					startDate = shift(startDate, Calendar.DAY_OF_MONTH, -1);
					endDate = startDate;
					break;
				case 2 :
					startDate = shift(startDate, Calendar.DAY_OF_MONTH, -(today.get(Calendar.DAY_OF_WEEK) + 5) % 7);
					break;
				case 3 :
					today.set(Calendar.DAY_OF_MONTH, 1);
					startDate = startOfDay(today.getTime());
					break;
				case 4 :
					today.set(Calendar.DAY_OF_MONTH, 1);
					endDate = shift(startOfDay(today.getTime()), Calendar.DAY_OF_MONTH, -1);
					today.add(Calendar.MONTH, -1);
					startDate = startOfDay(today.getTime());
					break;
				default :
					break;
			}
		} else {
			startField.setText(Formats.DATE.formatValue(startDate));
			endField.setText(Formats.DATE.formatValue(endDate));
		}
		revalidate();
		showRange();
		if (notify && !startDate.after(endDate)) {
			onChange.run();
		}
	}

	private void showRange() {
		if (range != null) {
			range.setText(Formats.DATE.formatValue(startDate) + " – " + Formats.DATE.formatValue(endDate));
		}
	}

	private static Date shift(Date date, int field, int amount) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(field, amount);
		return calendar.getTime();
	}

	void addActionButton(JButton button) {
		JPanel controls = (JPanel) getComponent(0);
		controls.add(button);
		controls.revalidate();
	}

	void addSearchField(JComponent field) {
		searchHeader.addSearchField(field);
	}

	SalesSummaryParameters getParameters() {
		return new SalesSummaryParameters(startOfDay(startDate), nextDay(startOfDay(endDate)));
	}

	private JPanel createDatePicker(final boolean isStart) {
		final JTextField field = new JTextField(10);
		field.setEditable(false);
		field.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(15f));
		field.setPreferredSize(new Dimension(112, 48));
		field.setText(Formats.DATE.formatValue(isStart ? startDate : endDate));
		if (isStart) {
			startField = field;
		} else {
			endField = field;
		}
		if (onChange != null) {
			field.addActionListener(e -> confirmCustomRange());
		}

		JButton calendar = new JButton(new ImageIcon(getClass().getResource("/com/openbravo/images/date.png")));
		calendar.setToolTipText(AppLocal.getIntString("button.selectdate"));
		calendar.setPreferredSize(new Dimension(48, 48));
		calendar.setFocusPainted(false);
		calendar.setOpaque(true);
		calendar.setBackground(RetailPOSColors.surface200());
		calendar.setForeground(RetailPOSColors.ink());
		calendar.setBorder(BorderFactory.createLineBorder(RetailPOSColors.border()));
		calendar.addActionListener(e -> {
			Date selected = JCalendarDialog.showCalendar(ReportPeriodSelector.this, isStart ? startDate : endDate);
			if (selected != null) {
				selected = startOfDay(selected);
				if (isStart) {
					startDate = selected;
					startField.setText(Formats.DATE.formatValue(startDate));
				} else {
					endDate = selected;
					endField.setText(Formats.DATE.formatValue(endDate));
				}
				if (onChange != null && selectedPreset == 5) {
					showRange();
					confirmCustomRange();
				}
			}
		});

		JPanel picker = new JPanel(new BorderLayout(4, 0));
		picker.setOpaque(false);
		picker.add(field, BorderLayout.CENTER);
		picker.add(calendar, BorderLayout.LINE_END);
		return picker;
	}

	private void confirmCustomRange() {
		if (selectedPreset != 5) {
			return;
		}
		validation.setVisible(startDate.after(endDate));
		if (!startDate.after(endDate)) {
			onChange.run();
		}
	}

	private static Date startOfDay(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTime();
	}

	private static Date nextDay(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.DAY_OF_MONTH, 1);
		return calendar.getTime();
	}

	static void activateOnEnter(JToggleButton button) {
		button.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ENTER"), "reportSelect");
		button.getActionMap().put("reportSelect", new AbstractAction() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e) {
				if (button.isEnabled()) {
					button.doClick();
				}
			}
		});
	}

	private final class SearchHeader extends JPanel {
		private final JPanel presetsPanel;
		private JComponent searchField;
		private boolean wrapped = true;

		SearchHeader(JPanel presetsPanel) {
			super(new BorderLayout(16, 8));
			setOpaque(false);
			this.presetsPanel = presetsPanel;
			add(presetsPanel, BorderLayout.NORTH);
		}

		void addSearchField(JComponent field) {
			JPanel slot = new JPanel(new java.awt.GridBagLayout());
			slot.setOpaque(false);
			slot.setMinimumSize(field.getMinimumSize());
			slot.setPreferredSize(field.getPreferredSize());
			java.awt.GridBagConstraints constraints = new java.awt.GridBagConstraints();
			constraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
			constraints.weightx = 1;
			slot.add(field, constraints);
			searchField = slot;
			add(slot, BorderLayout.SOUTH);
			revalidate();
		}

		private boolean shouldWrap(int width) {
			return searchField != null && width > 0
					&& width < presetsPanel.getPreferredSize().width + searchField.getMinimumSize().width + 16;
		}

		@Override
		public Dimension getPreferredSize() {
			Dimension preferred = super.getPreferredSize();
			if (searchField == null) {
				return preferred;
			}
			int width = getWidth();
			java.awt.Container controls = ReportPeriodSelector.this.getParent();
			java.awt.Container report = controls == null ? null : controls.getParent();
			if (report != null && report.getWidth() > 0) {
				width = report.getWidth() - report.getInsets().left - report.getInsets().right
						- ReportPeriodSelector.this.getInsets().left - ReportPeriodSelector.this.getInsets().right;
			} else if (ReportPeriodSelector.this.getWidth() > 0) {
				width = ReportPeriodSelector.this.getWidth() - ReportPeriodSelector.this.getInsets().left
						- ReportPeriodSelector.this.getInsets().right;
			}
			int height = shouldWrap(width)
					? presetsPanel.getPreferredSize().height + searchField.getPreferredSize().height + 8
					: Math.max(presetsPanel.getPreferredSize().height, searchField.getPreferredSize().height);
			return new Dimension(preferred.width, height);
		}

		@Override
		public void doLayout() {
			boolean wrap = shouldWrap(getWidth());
			if (searchField != null && wrap != wrapped) {
				wrapped = wrap;
				remove(presetsPanel);
				remove(searchField);
				add(presetsPanel, wrap ? BorderLayout.NORTH : BorderLayout.WEST);
				add(searchField, wrap ? BorderLayout.SOUTH : BorderLayout.CENTER);
				revalidate();
			}
			super.doLayout();
		}
	}

	/** FlowLayout needs a width-aware preferred height when presets wrap. */
	private static final class WrappingPresets extends JPanel {
		private static final int GAP = 8;

		WrappingPresets() {
			super(new FlowLayout(FlowLayout.LEFT, GAP, GAP));
			setOpaque(false);
		}

		@Override
		public Dimension getPreferredSize() {
			Dimension preferred = super.getPreferredSize();
			int width = getParent() == null
					? 0
					: getParent().getWidth() - getParent().getInsets().left - getParent().getInsets().right;
			if (width <= 0) {
				return preferred;
			}
			int rowWidth = 0;
			int rows = 1;
			for (java.awt.Component child : getComponents()) {
				int next = child.getPreferredSize().width + GAP;
				if (rowWidth > 0 && rowWidth + next > width) {
					rows++;
					rowWidth = 0;
				}
				rowWidth += next;
			}
			return new Dimension(preferred.width, rows * (44 + GAP) + GAP);
		}
	}

	static final class PresetButton extends JToggleButton {
		PresetButton(String text) {
			super(text);
			setOpaque(false);
			setContentAreaFilled(false);
			setFocusPainted(true);
			setForeground(RetailPOSColors.ink());
			setBorder(BorderFactory.createEmptyBorder(2, 12, 2, 12));
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(RetailPOSColors.surface200());
			g.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
			if (isSelected()) {
				g.setColor(RetailPOSColors.borderStrong());
				g.setStroke(new java.awt.BasicStroke(2f));
				g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 10, 10);
			}
			if (hasFocus()) {
				// An inset ink ring remains distinguishable from the selected outline.
				g.setColor(RetailPOSColors.ink());
				g.setStroke(new java.awt.BasicStroke(1.5f));
				g.drawRoundRect(4, 4, getWidth() - 9, getHeight() - 9, 8, 8);
			}
			g.dispose();
			super.paintComponent(graphics);
		}
	}
}
