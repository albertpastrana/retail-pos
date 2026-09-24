//    Openbravo POS is a point of sales application designed for touch screens.
//    Copyright (C) 2008-2009 Openbravo, S.L.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/**
 *
 * @author adrianromero
 */
public class AppConfig implements AppProperties {

	private Properties m_propsconfig;

	AppConfig() {
		m_propsconfig = new Properties();
	}

	public String getProperty(String sKey) {
		return m_propsconfig.getProperty(sKey);
	}

	public String getHost() {
		return getProperty("machine.hostname");
	}

	public void setProperty(String sKey, String sValue) {
		if (sValue == null) {
			m_propsconfig.remove(sKey);
		} else {
			m_propsconfig.setProperty(sKey, sValue);
		}
	}

	private String getLocalHostName() {
		try {
			return java.net.InetAddress.getLocalHost().getHostName();
		} catch (java.net.UnknownHostException eUH) {
			return "localhost";
		}
	}

	void load(InputStream input) {
		loadDefault();
		try (InputStream in = input) {
			if (in != null) {
				m_propsconfig.load(in);
			}
		} catch (IOException e) {
			loadDefault();
		}

		applyDemoConfiguration();

	}

	private void applyDemoConfiguration() {
		if (!"true".equalsIgnoreCase(m_propsconfig.getProperty("demo.active"))) {
			return;
		}

		String demoUrl = m_propsconfig.getProperty("demo.db.URL");
		if (demoUrl != null && !demoUrl.trim().isEmpty()) {
			if (demoUrl.startsWith("jdbc:derby:") && demoUrl.indexOf(';') < 0) {
				demoUrl += ";create=true";
			}
			m_propsconfig.setProperty("db.URL", demoUrl);
		}
		String demoUser = m_propsconfig.getProperty("demo.db.user");
		if (demoUser != null) {
			m_propsconfig.setProperty("db.user", demoUser);
		}
		String demoPassword = m_propsconfig.getProperty("demo.db.password");
		if (demoPassword != null) {
			m_propsconfig.setProperty("db.password", demoPassword);
		}
		DemoMode.removeIncompleteDerbyDatabase(m_propsconfig);
	}

	void store(OutputStream out) throws IOException {
		m_propsconfig.store(out, AppLocal.APP_NAME + ". Configuration file.");
	}

	private void loadDefault() {

		m_propsconfig = new Properties();

		m_propsconfig.setProperty("db.driver", "com.mysql.cj.jdbc.Driver");
		m_propsconfig.setProperty("db.URL", "jdbc:mysql://mysqldb.corp.cbits.co.uk:3306/sales");
		m_propsconfig.setProperty("db.user", "sales");
		m_propsconfig.setProperty("db.password", "");

		// m_propsconfig.setProperty("db.driver", "org.postgresql.Driver");
		// m_propsconfig.setProperty("db.URL",
		// "jdbc:postgresql://localhost:5432/database");
		// m_propsconfig.setProperty("db.user", "user");
		// m_propsconfig.setProperty("db.password", "password");

		m_propsconfig.setProperty("machine.hostname", getLocalHostName());

		m_propsconfig.setProperty("user.language", "ca");
		m_propsconfig.setProperty("user.country", "ES");
		m_propsconfig.setProperty("user.variant", "");

		m_propsconfig.setProperty("swing.defaultlaf",
				System.getProperty("swing.defaultlaf", "javax.swing.plaf.metal.MetalLookAndFeel"));

		m_propsconfig.setProperty("machine.printer", "screen");
		m_propsconfig.setProperty("machine.printer.2", "Not defined");
		m_propsconfig.setProperty("machine.printer.3", "Not defined");
		m_propsconfig.setProperty("machine.display", "screen");
		m_propsconfig.setProperty("machine.screenmode", "window"); // fullscreen / window
		m_propsconfig.setProperty("machine.ticketsbag", "standard");
		m_propsconfig.setProperty("machine.scanner", "Not defined");

		m_propsconfig.setProperty("payment.commerceid", "");
		m_propsconfig.setProperty("payment.commercepassword", "password");

		m_propsconfig.setProperty("machine.printername", "(Default)");

		// Receipt printer paper set to 72mmx200mm
		m_propsconfig.setProperty("paper.receipt.x", "10");
		m_propsconfig.setProperty("paper.receipt.y", "287");
		m_propsconfig.setProperty("paper.receipt.width", "190");
		m_propsconfig.setProperty("paper.receipt.height", "546");
		m_propsconfig.setProperty("paper.receipt.mediasizename", "A4");

		// Normal printer paper for A4
		m_propsconfig.setProperty("paper.standard.x", "72");
		m_propsconfig.setProperty("paper.standard.y", "72");
		m_propsconfig.setProperty("paper.standard.width", "451");
		m_propsconfig.setProperty("paper.standard.height", "698");
		m_propsconfig.setProperty("paper.standard.mediasizename", "A4");

		m_propsconfig.setProperty("machine.uniqueinstance", "false");

		m_propsconfig.setProperty("backup.daily", "false");
		m_propsconfig.setProperty("backup.dir", "");

		m_propsconfig.setProperty("update.check", "true");
		m_propsconfig.setProperty("update.dir", "");
		m_propsconfig.setProperty("update.url",
				"https://api.github.com/repos/albertpastrana/retail-pos/releases/latest");
	}
}
