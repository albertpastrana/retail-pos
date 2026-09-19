package com.openbravo.pos.reports;

import com.openbravo.beans.JCalendarDialog;
import com.openbravo.format.Formats;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.theme.RetailPOSColors;
import com.openbravo.pos.theme.RetailPOSTheme;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Calendar;
import java.util.Date;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** Shared date-only period selector for management reports. */
final class ReportPeriodSelector extends JPanel {

	private Date startDate;
	private Date endDate;
	private JTextField startField;
	private JTextField endField;

	ReportPeriodSelector(String titleKey) {
		setLayout(new BorderLayout(0, 12));
		setBackground(RetailPOSColors.surface100());
		setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(RetailPOSColors.border()),
				BorderFactory.createEmptyBorder(16, 16, 16, 16)));
		JLabel heading = new JLabel(AppLocal.getIntString(titleKey));
		heading.setFont(RetailPOSTheme.MANROPE_BOLD.deriveFont(20f));
		heading.setForeground(RetailPOSColors.ink());
		add(heading, BorderLayout.NORTH);
		JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
		controls.setOpaque(false);
		controls.add(new JLabel(AppLocal.getIntString("label.reportstart")));
		controls.add(createDatePicker(true));
		controls.add(new JLabel(AppLocal.getIntString("label.reportend")));
		controls.add(createDatePicker(false));
		add(controls, BorderLayout.CENTER);
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
	}

	void addActionButton(JButton button) {
		JPanel controls = (JPanel) getComponent(1);
		controls.add(button);
		controls.revalidate();
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
			}
		});

		JPanel picker = new JPanel(new BorderLayout(4, 0));
		picker.setOpaque(false);
		picker.add(field, BorderLayout.CENTER);
		picker.add(calendar, BorderLayout.LINE_END);
		return picker;
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
}
