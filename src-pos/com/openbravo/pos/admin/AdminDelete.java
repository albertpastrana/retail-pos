package com.openbravo.pos.admin;

import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.data.user.BrowsableEditableData;
import com.openbravo.data.user.DirtyManager;
import com.openbravo.pos.forms.AppLocal;
import java.awt.Component;
import javax.swing.JOptionPane;

final class AdminDelete {
	private AdminDelete() {
	}

	static void confirm(Component parent, BrowsableEditableData data, DirtyManager dirty, String name) {
		if (data == null || data.getState() != BrowsableEditableData.ST_UPDATE)
			return;
		if (dirty.isDirty()) {
			JOptionPane.showMessageDialog(parent, AppLocal.getIntString("Admin.SaveBeforeDelete"));
			return;
		}
		if (JOptionPane.showConfirmDialog(parent, String.format(AppLocal.getIntString("Admin.ConfirmDelete"), name),
				AppLocal.getIntString("Admin.Delete"), JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION)
			return;
		try {
			data.actionDelete();
			data.saveData();
		} catch (BasicException ex) {
			new MessageInf(ex).show(parent);
		}
	}
}
