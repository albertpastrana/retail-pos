package com.openbravo.pos.admin;

import java.awt.Component;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.pos.forms.AppConfig;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.AppView;
import com.openbravo.pos.forms.BeanFactoryApp;
import com.openbravo.pos.forms.BeanFactoryException;
import com.openbravo.pos.forms.DemoMode;
import com.openbravo.pos.forms.ProcessAction;

public class DemoModeAction implements BeanFactoryApp, ProcessAction {
	private AppView app;

	public void init(AppView app) throws BeanFactoryException {
		this.app = app;
	}

	public Object getBean() {
		return this;
	}

	public MessageInf execute() throws BasicException {
		if (!(app.getProperties() instanceof AppConfig)) {
			return new MessageInf(MessageInf.SGN_WARNING, "Demo mode requires a local application configuration");
		}
		AppConfig config = (AppConfig) app.getProperties();
		if (DemoMode.isActive(config)) {
			DemoMode.exit(config);
			restartApplication(config);
			return new MessageInf(MessageInf.SGN_SUCCESS, AppLocal.getIntString("message.demoexit"));
		}

		boolean hasDemo = config.getProperty(DemoMode.URL_KEY) != null;
		Object[] options = hasDemo
				? new Object[]{AppLocal.getIntString("Button.DemoContinue"), AppLocal.getIntString("Button.DemoCopy"),
						AppLocal.getIntString("Button.DemoEmpty"), AppLocal.getIntString("Button.Cancel")}
				: new Object[]{AppLocal.getIntString("Button.DemoCopy"), AppLocal.getIntString("Button.DemoEmpty"),
						AppLocal.getIntString("Button.Cancel")};
		int choice = JOptionPane.showOptionDialog((Component) app, AppLocal.getIntString("message.demoselect"),
				AppLocal.getIntString("Menu.DemoMode"), JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null,
				options, options[0]);
		if (choice < 0 || choice == options.length - 1) {
			return null;
		}
		if (hasDemo && choice == 0) {
			DemoMode.prepare(config, false, false);
		} else {
			int sourceChoice = hasDemo ? choice - 1 : choice;
			DemoMode.prepare(config, sourceChoice == 0, true);
		}
		restartApplication(config);
		return new MessageInf(MessageInf.SGN_SUCCESS, AppLocal.getIntString("message.demorestart"));
	}

	private void restartApplication(AppConfig config) throws BasicException {
		Window window = SwingUtilities.getWindowAncestor((Component) app);
		if (window != null) {
			window.dispose();
		}
		String java = new File(new File(System.getProperty("java.home"), "bin"),
				System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java").getAbsolutePath();
		List<String> command = new ArrayList<String>();
		command.add(java);
		addSystemProperty(command, "dirname.path");
		addSystemProperty(command, "java.library.path");
		addSystemProperty(command, "java.util.logging.config.file");
		command.add("-cp");
		command.add(System.getProperty("java.class.path"));
		command.add("com.openbravo.pos.forms.ApplicationRestart");
		command.add(config.getConfigFile().getAbsolutePath());
		try {
			new ProcessBuilder(command).inheritIO().start();
		} catch (IOException e) {
			throw new BasicException("Cannot restart the application", e);
		}
		System.exit(0);
	}

	private static void addSystemProperty(List<String> command, String key) {
		String value = System.getProperty(key);
		if (value != null && !value.isEmpty()) {
			command.add("-D" + key + "=" + value);
		}
	}
}
