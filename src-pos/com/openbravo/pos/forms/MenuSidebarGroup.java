//    Openbravo POS is a point of sales application designed for touch screens.
//    Copyright (C) 2007-2009 Openbravo, S.L.
//
//    This file is part of Openbravo POS.

package com.openbravo.pos.forms;

import com.openbravo.pos.theme.RetailPOSColors;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class MenuSidebarGroup extends JPanel {

	private final JLabel titleLabel;
	private final List<JButton> actionButtons = new ArrayList<>();

	public MenuSidebarGroup() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setAlignmentX(LEFT_ALIGNMENT);
		setOpaque(false);
		titleLabel = new JLabel();
		titleLabel.setAlignmentX(LEFT_ALIGNMENT);
		titleLabel.setFont(titleLabel.getFont().deriveFont(13f));
		titleLabel.setForeground(RetailPOSColors.inkMuted());
		titleLabel.setBorder(BorderFactory.createEmptyBorder(16, 12, 4, 12));
		titleLabel.setFocusable(false);
		add(titleLabel);
	}

	public void setTitle(String title) {
		titleLabel.setText(title.toUpperCase(Locale.getDefault()));
	}

	public Component add(javax.swing.Action action) {
		JButton button = new JButton(action);
		button.setHorizontalAlignment(SwingConstants.LEADING);
		button.setIconTextGap(8);
		button.setFont(button.getFont().deriveFont(17f));
		Dimension buttonSize = button.getPreferredSize();
		button.setPreferredSize(new Dimension(Math.max(48, buttonSize.width), 48));
		button.setMinimumSize(new Dimension(0, 48));
		button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
		button.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
		button.setFocusPainted(false);
		button.setFocusable(false);
		button.setRequestFocusEnabled(false);
		button.setOpaque(true);
		styleButton(button, false);
		actionButtons.add(button);
		super.add(button);
		return button;
	}

	public void setSelectedTask(String taskName) {
		for (JButton button : actionButtons) {
			boolean selected = taskName != null
					&& taskName.equals(button.getAction().getValue(AppUserView.ACTION_TASKNAME));
			styleButton(button, selected);
		}
	}

	private void styleButton(JButton button, boolean selected) {
		button.setBackground(selected ? RetailPOSColors.brandSubtle() : RetailPOSColors.surface0());
		button.setForeground(RetailPOSColors.ink());
		button.putClientProperty("FlatLaf.style",
				"background: "
						+ RetailPOSColors.toHex(selected ? RetailPOSColors.brandSubtle() : RetailPOSColors.surface0())
						+ "; foreground: " + RetailPOSColors.toHex(RetailPOSColors.ink()) + "; hoverBackground: "
						+ RetailPOSColors.toHex(RetailPOSColors.surface200()) + "; pressedBackground: "
						+ RetailPOSColors.toHex(RetailPOSColors.surface200()));
	}
}
