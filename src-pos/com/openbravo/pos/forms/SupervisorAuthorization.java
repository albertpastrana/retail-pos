package com.openbravo.pos.forms;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import com.openbravo.basic.BasicException;
import com.openbravo.beans.JPasswordDialog;

/** Authorizes one privileged operation without changing the active seller. */
public final class SupervisorAuthorization {

	private SupervisorAuthorization() {
	}

	public static boolean isSupervisor(AppUser user) {
		return user != null
				&& (AppUser.ROLE_ADMINISTRATOR.equals(user.getRole()) || AppUser.ROLE_MANAGER.equals(user.getRole()));
	}

	public static boolean authenticate(AppUser user, String password) {
		return isSupervisor(user) && (password == null ? user.authenticate() : user.authenticate(password));
	}

	public static boolean authorize(Component parent, AppView app, String operation) {
		AppUser current = app.getAppUserView().getUser();
		if (!current.isSellerSession()) {
			return true;
		}

		DataLogicSystem dlSystem = (DataLogicSystem) app.getBean("com.openbravo.pos.forms.DataLogicSystem");
		List supervisors = new ArrayList();
		try {
			for (Object value : dlSystem.listPeopleVisible()) {
				AppUser user = (AppUser) value;
				if (isSupervisor(user)) {
					supervisors.add(user);
				}
			}
		} catch (BasicException exception) {
			return false;
		}
		if (supervisors.isEmpty()) {
			return false;
		}

		AppUser supervisor = (AppUser) JOptionPane.showInputDialog(parent, operation,
				AppLocal.getIntString("title.editor"), JOptionPane.QUESTION_MESSAGE, null, supervisors.toArray(),
				supervisors.get(0));
		if (supervisor == null) {
			return false;
		}
		if (supervisor.authenticate()) {
			return true;
		}
		String password = JPasswordDialog.showEditPassword(parent, AppLocal.getIntString("Label.Password"),
				supervisor.getName(), supervisor.getIcon());
		return authenticate(supervisor, password);
	}

	public static boolean authorizeCard(DataLogicSystem dlSystem, String card) {
		try {
			return isSupervisor(dlSystem.findPeopleByCard(card));
		} catch (BasicException exception) {
			return false;
		}
	}
}
