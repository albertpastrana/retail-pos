package com.openbravo.pos.forms;

import com.openbravo.pos.theme.RetailPOSColors;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.AbstractBorder;

/**
 * A workflow action card whose visible content remains part of its hit area.
 */
public final class WorkflowCard extends JButton {
	private static final Color TEXT = new Color(36, 28, 20);
	private static final Color MUTED = new Color(107, 97, 84);
	private static final int CARD_RADIUS = 22;

	public WorkflowCard(Icon icon, String title, String hint, javax.swing.Action action) {
		setAction(action);
		setRolloverEnabled(true);
		setText(null);
		setIcon(null);
		getAccessibleContext().setAccessibleName(title);
		setFocusable(true);
		setRequestFocusEnabled(true);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		setHorizontalAlignment(SwingConstants.LEADING);
		setVerticalAlignment(SwingConstants.CENTER);
		setLayout(new BorderLayout(12, 0));
		setBorder(BorderFactory.createCompoundBorder(new RoundedLineBorder(RetailPOSColors.border(), CARD_RADIUS),
				BorderFactory.createEmptyBorder(16, 16, 16, 16)));
		setBackground(RetailPOSColors.surface100());
		setContentAreaFilled(false);
		setOpaque(false);
		setMargin(new Insets(0, 0, 0, 0));

		JLabel iconLabel = new JLabel(icon);
		add(iconLabel, BorderLayout.WEST);
		JPanel text = new JPanel();
		text.setOpaque(false);
		text.setLayout(new javax.swing.BoxLayout(text, javax.swing.BoxLayout.Y_AXIS));
		text.add(label(title, 16, Font.BOLD, TEXT));
		text.add(label(hint, 13, Font.PLAIN, MUTED));
		add(text, BorderLayout.CENTER);
		wireEvents(iconLabel);
		wireEvents(text);
	}

	public JButton asLink() {
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

	private void wireEvents(Component component) {
		component.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent event) {
				if (SwingUtilities.isLeftMouseButton(event)) {
					getModel().setArmed(true);
					getModel().setPressed(true);
				}
			}

			@Override
			public void mouseReleased(MouseEvent event) {
				if (!SwingUtilities.isLeftMouseButton(event) || !getModel().isPressed())
					return;
				java.awt.Point point = SwingUtilities.convertPoint(component, event.getPoint(), WorkflowCard.this);
				getModel().setArmed(contains(point));
				getModel().setPressed(false);
				getModel().setArmed(false);
			}

			@Override
			public void mouseEntered(MouseEvent event) {
				getModel().setRollover(true);
				repaint();
			}

			@Override
			public void mouseExited(MouseEvent event) {
				java.awt.Point point = SwingUtilities.convertPoint(component, event.getPoint(), WorkflowCard.this);
				if (!contains(point))
					getModel().setRollover(false);
				repaint();
			}

		});
		if (component instanceof java.awt.Container) {
			for (Component child : ((java.awt.Container) component).getComponents())
				wireEvents(child);
		}
	}

	private static JLabel label(String text, int size, int style, Color color) {
		JLabel label = new JLabel(text);
		label.setFont(label.getFont().deriveFont(style, (float) size));
		label.setForeground(color);
		return label;
	}

	@Override
	public Dimension getPreferredSize() {
		Dimension preferred = super.getPreferredSize();
		return new Dimension(preferred.width, Math.max(preferred.height, 84));
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		Graphics2D g2 = (Graphics2D) graphics.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setColor(getModel().isRollover() ? RetailPOSColors.surface200() : getBackground());
		g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, CARD_RADIUS, CARD_RADIUS);
		g2.clip(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), CARD_RADIUS, CARD_RADIUS));
		super.paintComponent(g2);
		g2.dispose();
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
			g2.setColor(component instanceof WorkflowCard && ((WorkflowCard) component).getModel().isRollover()
					? RetailPOSColors.borderStrong()
					: color);
			g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
			g2.dispose();
		}

		@Override
		public Insets getBorderInsets(Component component) {
			return new Insets(1, 1, 1, 1);
		}
	}
}
