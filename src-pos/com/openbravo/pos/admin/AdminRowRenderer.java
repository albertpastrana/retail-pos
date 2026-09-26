package com.openbravo.pos.admin;

import java.awt.Component;
import java.util.function.Function;
import javax.swing.Icon;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

final class AdminRowRenderer extends DefaultListCellRenderer {
	private final Function<Object, String> detail;
	private final Function<Object, Icon> icon;

	AdminRowRenderer(Function<Object, String> detail) {
		this(detail, row -> null);
	}

	AdminRowRenderer(Function<Object, String> detail, Function<Object, Icon> icon) {
		this.detail = detail;
		this.icon = icon;
	}

	@Override
	public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected,
			boolean focused) {
		super.getListCellRendererComponent(list, value, index, selected, focused);
		Object[] row = (Object[]) value;
		setText("<html><b>" + escape(String.valueOf(row[1])) + "</b><br>" + escape(detail.apply(value)) + "</html>");
		setIcon(icon.apply(value));
		setIconTextGap(10);
		setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 10, 8, 10));
		return this;
	}

	private static String escape(String text) {
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
