//    Openbravo POS is a point of sales application designed for touch screens.
//    Copyright (C) 2007-2009 Openbravo, S.L.
//    http://www.openbravo.com/product/pos
//
//    This file is part of Openbravo POS.
//
//    Openbravo POS is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    Openbravo POS is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with Openbravo POS.  If not, see <http://www.gnu.org/licenses/>.

package com.openbravo.pos.catalog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import com.openbravo.beans.JFlowPanel;
import com.openbravo.pos.util.StringUtils;

/**
 *
 * @author adrianromero
 */
public class JCatalogTab extends javax.swing.JPanel {

	private JFlowPanel flowpanel;

	/** Creates new form JCategoryProducts */
	public JCatalogTab() {
		initComponents();

		flowpanel = new JFlowPanel();
		JScrollPane scroll = new JScrollPane(flowpanel);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scroll.getVerticalScrollBar().setPreferredSize(new Dimension(35, 35));

		add(scroll, BorderLayout.CENTER);
	}

	public void setEnabled(boolean value) {
		flowpanel.setEnabled(value);
		super.setEnabled(value);
	}

	/**
	 * A readable product or subcategory tile, with the image separate from its
	 * caption.
	 */
	public void addButton(String[] lines, Icon image, int width, int height, boolean category, ActionListener al) {
		JButton btn = createButton(al);
		btn.setFont(btn.getFont().deriveFont(Math.min(13f, btn.getFont().getSize2D())));
		if (category) {
			btn.setFont(btn.getFont().deriveFont(Font.BOLD));
			btn.setBackground(getCategoryBackground());
		}
		btn.setIcon(image);
		btn.setToolTipText(String.join(" · ", lines));
		btn.getAccessibleContext().setAccessibleName(String.join(" · ", lines));
		btn.setIconTextGap(4);
		btn.setMargin(new Insets(6, 8, 6, 8));
		Insets ins = btn.getInsets();
		Dimension size = new Dimension(width, height);
		btn.setPreferredSize(size);
		btn.setMinimumSize(size);
		btn.setMaximumSize(size);
		btn.setText(fitText(btn, lines, Math.max(48, width - ins.left - ins.right - 8),
				height - ins.top - ins.bottom - (image == null ? 0 : image.getIconHeight() + 4)));
		flowpanel.add(btn);
	}

	/** Button background with a hint of the look and feel accent colour. */
	private Color getCategoryBackground() {

		Color base = UIManager.getColor("Button.background");
		if (base == null) {
			base = getBackground();
		}
		Color accent = UIManager.getColor("Component.accentColor");
		if (accent == null) {
			accent = UIManager.getColor("List.selectionBackground");
		}
		if (accent == null) {
			return base;
		}

		double mix = 0.14;
		return new Color((int) (base.getRed() * (1 - mix) + accent.getRed() * mix),
				(int) (base.getGreen() * (1 - mix) + accent.getGreen() * mix),
				(int) (base.getBlue() * (1 - mix) + accent.getBlue() * mix));
	}

	/**
	 * Wraps the lines to the given width and returns them as HTML. Shrinks the font
	 * by up to two points when the wrapped text is taller than the button.
	 */
	private String fitText(JButton btn, String[] lines, int width, int height) {

		Font basefont = btn.getFont();
		List<String> wrapped = null;
		for (int shrink = 0; shrink <= 2; shrink++) {
			Font font = basefont.deriveFont(basefont.getSize2D() - shrink);
			btn.setFont(font);
			FontMetrics metrics = btn.getFontMetrics(font);
			wrapped = wrapLines(lines, metrics, width);
			if (wrapped.size() * metrics.getHeight() <= height) {
				break;
			}
		}
		FontMetrics metrics = btn.getFontMetrics(btn.getFont());
		int maxLines = Math.max(1, height / metrics.getHeight());
		if (wrapped.size() > maxLines) {
			// Keep a separate price line visible even when the product name is long.
			String last = lines.length > 1 ? lines[lines.length - 1] : wrapped.get(maxLines - 1);
			wrapped = new ArrayList<String>(wrapped.subList(0, maxLines - (lines.length > 1 ? 1 : 0)));
			if (lines.length > 1) {
				wrapped.add(last);
			} else {
				wrapped.set(maxLines - 1, shorten(last, metrics, width));
			}
			if (lines.length > 1 && maxLines > 1) {
				wrapped.set(maxLines - 2, shorten(wrapped.get(maxLines - 2) + "…", metrics, width));
			}
		}

		StringBuilder text = new StringBuilder("<html><center>");
		for (int i = 0; i < wrapped.size(); i++) {
			if (i > 0) {
				text.append("<br>");
			}
			text.append(StringUtils.encodeXML(wrapped.get(i)));
		}
		return text.append("</center></html>").toString();
	}

	private String shorten(String text, FontMetrics metrics, int width) {
		String result = text.endsWith("…") ? text : text + "…";
		while (result.length() > 1 && metrics.stringWidth(result) > width) {
			result = result.substring(0, result.length() - 2) + "…";
		}
		return result;
	}

	private List<String> wrapLines(String[] lines, FontMetrics metrics, int width) {

		List<String> wrapped = new ArrayList<String>();
		for (String line : lines) {
			StringBuilder current = new StringBuilder();
			for (String word : line.split(" ")) {
				while (metrics.stringWidth(word) > width && word.length() > 1) {
					int end = 1;
					while (end < word.length() && metrics.stringWidth(word.substring(0, end + 1)) <= width) {
						end++;
					}
					if (current.length() > 0) {
						wrapped.add(current.toString());
						current.setLength(0);
					}
					wrapped.add(word.substring(0, end));
					word = word.substring(end);
				}
				if (current.length() == 0) {
					current.append(word);
				} else if (metrics.stringWidth(current + " " + word) <= width) {
					current.append(" ").append(word);
				} else {
					wrapped.add(current.toString());
					current.setLength(0);
					current.append(word);
				}
			}
			if (current.length() > 0) {
				wrapped.add(current.toString());
			}
		}
		return wrapped;
	}

	private JButton createButton(ActionListener al) {
		JButton btn = new JButton();
		btn.applyComponentOrientation(getComponentOrientation());
		btn.setFocusPainted(false);
		btn.setFocusable(false);
		btn.setRequestFocusEnabled(false);
		btn.setHorizontalTextPosition(SwingConstants.CENTER);
		btn.setVerticalTextPosition(SwingConstants.BOTTOM);
		btn.setMargin(new Insets(2, 2, 2, 2));
		btn.addActionListener(al);
		return btn;
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	// <editor-fold defaultstate="collapsed" desc="Generated
	// Code">//GEN-BEGIN:initComponents
	private void initComponents() {

		setLayout(new java.awt.BorderLayout());
	}// </editor-fold>//GEN-END:initComponents

	// Variables declaration - do not modify//GEN-BEGIN:variables
	// End of variables declaration//GEN-END:variables

}
