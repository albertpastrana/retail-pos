package com.openbravo.pos.util;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.openbravo.pos.theme.RetailPOSColors;
import java.awt.Component;
import java.awt.Graphics;
import java.net.URL;
import javax.swing.Icon;

/** Shared runtime loading and sizing convention for design-system SVG icons. */
public final class PosIcons {

	private static final String ROOT = "/com/openbravo/images/icons/";

	private PosIcons() {
	}

	public static Icon sidebarOpen(boolean leftToRight) {
		return icon("panel-left-open.svg", leftToRight);
	}

	public static Icon sidebarClose(boolean leftToRight) {
		return icon("panel-left-close.svg", leftToRight);
	}

	public static FlatSVGIcon printer() {
		return icon("printer.svg", 24);
	}

	public static FlatSVGIcon cardPayment() {
		return icon("credit-card.svg", 24);
	}

	public static FlatSVGIcon discountTag() {
		return icon("tag.svg", 24);
	}

	public static FlatSVGIcon search() {
		return icon("search.svg", 24);
	}

	public static FlatSVGIcon barcodeScan() {
		return icon("barcode-scan.svg", 24);
	}

	public static FlatSVGIcon cashDrawer() {
		return icon("cash-drawer.svg", 24);
	}

	private static FlatSVGIcon icon(String name, int size) {
		URL resource = PosIcons.class.getResource(ROOT + name);
		if (resource == null) {
			throw new IllegalStateException("Missing critical SVG icon resource: " + ROOT + name);
		}
		FlatSVGIcon icon = new FlatSVGIcon(resource).derive(size, size);
		if (!icon.hasFound()) {
			throw new IllegalStateException("Unsupported SVG icon resource: " + resource);
		}
		return icon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> RetailPOSColors.ink()));
	}

	private static Icon icon(String name, boolean leftToRight) {
		FlatSVGIcon icon = icon(name, 24);
		return leftToRight ? icon : new MirroredIcon(icon);
	}

	private static final class MirroredIcon implements Icon {
		private final Icon delegate;

		private MirroredIcon(Icon delegate) {
			this.delegate = delegate;
		}

		@Override
		public void paintIcon(Component component, Graphics graphics, int x, int y) {
			Graphics mirrored = graphics.create(x, y, getIconWidth(), getIconHeight());
			mirrored.translate(getIconWidth(), 0);
			mirrored.setClip(0, 0, getIconWidth(), getIconHeight());
			((java.awt.Graphics2D) mirrored).scale(-1, 1);
			delegate.paintIcon(component, mirrored, 0, 0);
			mirrored.dispose();
		}

		@Override
		public int getIconWidth() {
			return delegate.getIconWidth();
		}

		@Override
		public int getIconHeight() {
			return delegate.getIconHeight();
		}
	}
}
