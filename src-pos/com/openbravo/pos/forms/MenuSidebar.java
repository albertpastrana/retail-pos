//    Openbravo POS is a point of sales application designed for touch screens.
//    Copyright (C) 2007-2009 Openbravo, S.L.
//
//    This file is part of Openbravo POS.

package com.openbravo.pos.forms;

import com.openbravo.pos.theme.RetailPOSColors;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

public class MenuSidebar extends JPanel implements Scrollable {

	private final List<MenuSidebarGroup> groups = new ArrayList<>();

	public MenuSidebar() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setOpaque(true);
		setBackground(RetailPOSColors.surface0());
		updateBorder(ComponentOrientation.LEFT_TO_RIGHT);
	}

	@Override
	public void applyComponentOrientation(ComponentOrientation orientation) {
		super.applyComponentOrientation(orientation);
		updateBorder(orientation);
	}

	private void updateBorder(ComponentOrientation orientation) {
		setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
	}

	public Component add(MenuSidebarGroup group) {
		groups.add(group);
		return super.add(group);
	}

	public void addToggleButton(JButton toggle, ActionListener listener) {
		toggle.addActionListener(listener);
		toggle.setAlignmentX(LEFT_ALIGNMENT);
		toggle.setHorizontalAlignment(SwingConstants.LEADING);
		toggle.setText(AppLocal.getIntString("Button.CollapseMenu"));
		toggle.setToolTipText(AppLocal.getIntString("Button.CollapseMenu"));
		toggle.setFont(toggle.getFont().deriveFont(12f));
		toggle.setIconTextGap(8);
		Dimension toggleSize = toggle.getPreferredSize();
		toggle.setPreferredSize(new Dimension(Math.max(56, toggleSize.width), 56));
		toggle.setMinimumSize(new Dimension(0, 56));
		toggle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
		toggle.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
		toggle.setOpaque(true);
		toggle.setBackground(RetailPOSColors.surface0());
		toggle.setForeground(RetailPOSColors.inkMuted());
		toggle.putClientProperty("FlatLaf.style",
				"background: " + RetailPOSColors.toHex(RetailPOSColors.surface0()) + "; hoverBackground: "
						+ RetailPOSColors.toHex(RetailPOSColors.surface200()) + "; pressedBackground: "
						+ RetailPOSColors.toHex(RetailPOSColors.surface200()));
		add(toggle, 0);
	}

	public void setSelectedTask(String taskName) {
		for (MenuSidebarGroup group : groups) {
			group.setSelectedTask(taskName);
		}
	}

	@Override
	public Dimension getPreferredScrollableViewportSize() {
		return getPreferredSize();
	}

	@Override
	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
		return 56;
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
