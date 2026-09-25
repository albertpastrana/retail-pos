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

package com.openbravo.pos.forms;

import com.openbravo.pos.util.HiDpiIcon;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;

/**
 *
 * @author adrianromero
 */
public class MenuExecAction extends AbstractAction {

	private AppView m_App;
	private Class<? extends ProcessAction> m_actionClass;

	/** Creates a new instance of MenuExecAction */
	public MenuExecAction(AppView app, String icon, String keytext, Class<? extends ProcessAction> actionClass) {
		putValue(Action.SMALL_ICON, new HiDpiIcon(JPrincipalApp.class.getResource(icon)));
		putValue(Action.NAME, AppLocal.getIntString(keytext));
		putValue(AppUserView.ACTION_TASKNAME, actionClass.getName());
		m_App = app;
		m_actionClass = actionClass;
	}

	public void actionPerformed(ActionEvent evt) {
		m_App.getAppUserView().executeTask(m_actionClass);
	}
}
