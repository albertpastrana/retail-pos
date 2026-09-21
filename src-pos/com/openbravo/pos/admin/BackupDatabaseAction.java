package com.openbravo.pos.admin;

import java.io.File;
import java.io.IOException;

import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.AppConfig;
import com.openbravo.pos.forms.AppView;
import com.openbravo.pos.forms.BeanFactoryApp;
import com.openbravo.pos.forms.BeanFactoryException;
import com.openbravo.pos.forms.DatabaseBackup;
import com.openbravo.pos.forms.ConfigurationStore;
import com.openbravo.pos.forms.ProcessAction;

public class BackupDatabaseAction implements BeanFactoryApp, ProcessAction {

	private AppView app;

	public void init(AppView app) throws BeanFactoryException {
		this.app = app;
	}

	public Object getBean() {
		return this;
	}

	public MessageInf execute() throws BasicException {
		try {
			File backupFile = DatabaseBackup.backup(app.getProperties());
			if (app.getProperties() instanceof AppConfig) {
				ConfigurationStore.save((AppConfig) app.getProperties());
			}
			return new MessageInf(MessageInf.SGN_SUCCESS,
					AppLocal.getIntString("message.backupok", backupFile.getAbsolutePath()));
		} catch (BasicException | IOException e) {
			return new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.backupfailed", e.getMessage()),
					e);
		}
	}
}
