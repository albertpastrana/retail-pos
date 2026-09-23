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

import com.openbravo.data.loader.LocalRes;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import javax.swing.Icon;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import com.openbravo.pos.ticket.UserInfo;
import com.openbravo.pos.util.Hashcypher;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author adrianromero
 */
public class AppUser {

	public static final String ROLE_ADMINISTRATOR = "0";
	public static final String ROLE_MANAGER = "1";
	public static final String ROLE_SELLER = "2";

	private static Logger logger = Logger.getLogger("com.openbravo.pos.forms.AppUser");

	private static SAXParser m_sp = null;
	private static HashMap<String, String> m_oldclasses; // This is for backwards compatibility purposes

	private String m_sId;
	private String m_sName;
	private String m_sCard;
	private String m_sPassword;
	private String m_sRole;
	private Icon m_Icon;
	private UserInfo m_selectedTicketUser;

	private Set<String> m_apermissions;
	private boolean m_administrationMode;
	private boolean m_sellerSession;

	static {
		initOldClasses();
	}

	/** Creates a new instance of AppUser */
	public AppUser(String id, String name, String password, String card, String role, Icon icon) {
		m_sId = id;
		m_sName = name;
		m_sPassword = password;
		m_sCard = card;
		m_sRole = role;
		m_Icon = icon;
		m_apermissions = null;
		m_administrationMode = false;
		m_sellerSession = false;
	}

	public Icon getIcon() {
		return m_Icon;
	}

	public void setIcon(Icon icon) {
		m_Icon = icon;
	}

	public String getId() {
		return m_sId;
	}

	public String getName() {
		return m_sName;
	}

	@Override
	public String toString() {
		return m_sName;
	}

	public void setPassword(String sValue) {
		m_sPassword = sValue;
	}

	public String getPassword() {
		return m_sPassword;
	}

	public String getRole() {
		return m_sRole;
	}

	public void setAdministrationMode(boolean value) {
		m_administrationMode = value;
	}

	public void setSellerSession(boolean value) {
		m_sellerSession = value;
	}

	public boolean isSellerSession() {
		return m_sellerSession;
	}

	public String getCard() {
		return m_sCard;
	}

	public boolean authenticate() {
		return m_sPassword == null || m_sPassword.equals("") || m_sPassword.startsWith("empty:");
	}
	public boolean authenticate(String sPwd) {
		return Hashcypher.authenticate(sPwd, m_sPassword);
	}

	public void fillPermissions(DataLogicSystem dlSystem) {

		// inicializamos los permisos
		m_apermissions = new HashSet<String>();
		// Y lo que todos tienen permisos
		m_apermissions.add("com.openbravo.pos.forms.JPanelMenu");
		m_apermissions.add("Menu.Exit");
		// Replenishment is an operational action available from the till menu.
		if (ROLE_SELLER.equals(m_sRole)) {
			// Keep the normal till workflow available for databases with an older
			// Seller role definition. These are also present in Role.Seller.xml.
			m_apermissions.add("com.openbravo.pos.sales.JPanelTicketEdits");
			m_apermissions.add("sales.EditTicket");
			m_apermissions.add("com.openbravo.pos.inventory.ReplenishmentPanel");
			m_apermissions.add("Menu.Replenishment.Add");
		}
		if (ROLE_ADMINISTRATOR.equals(m_sRole)) {
			m_apermissions.add("com.openbravo.pos.admin.DemoModeAction");
		}

		String sRolePermisions = dlSystem.findRolePermissions(m_sRole);

		if (sRolePermisions != null) {
			try {
				if (m_sp == null) {
					SAXParserFactory spf = SAXParserFactory.newInstance();
					m_sp = spf.newSAXParser();
				}
				m_sp.parse(new InputSource(new StringReader(sRolePermisions)), new ConfigurationHandler());

			} catch (ParserConfigurationException ePC) {
				logger.log(Level.WARNING, LocalRes.getIntString("exception.parserconfig"), ePC);
			} catch (SAXException eSAX) {
				logger.log(Level.WARNING, LocalRes.getIntString("exception.xmlfile"), eSAX);
			} catch (IOException eIO) {
				logger.log(Level.WARNING, LocalRes.getIntString("exception.iofile"), eIO);
			}
		}

		// Keep the integrated attribute workflow available to roles created before
		// the four attribute maintenance permissions were consolidated.
		if (m_apermissions.contains("com.openbravo.pos.inventory.AttributesPanel")
				|| m_apermissions.contains("com.openbravo.pos.inventory.AttributeValuesPanel")
				|| m_apermissions.contains("com.openbravo.pos.inventory.AttributeSetsPanel")
				|| m_apermissions.contains("com.openbravo.pos.inventory.AttributeUsePanel")) {
			m_apermissions.add("com.openbravo.pos.inventory.AttributeManagementPanel");
		}

	}

	public boolean hasPermission(String classname) {
		if (m_apermissions == null || !m_apermissions.contains(classname)) {
			return false;
		}
		if (m_administrationMode) {
			return !classname.startsWith("sales.") && !classname.startsWith("payment.")
					&& !classname.startsWith("refund.")
					&& !classname.equals("com.openbravo.pos.sales.JPanelTicketSales")
					&& !classname.equals("com.openbravo.pos.sales.JPanelTicketEdits")
					&& !classname.equals("com.openbravo.pos.panels.JPanelPayments")
					&& !classname.equals("com.openbravo.pos.panels.JPanelCloseMoney")
					&& !classname.equals("com.openbravo.pos.panels.JPanelClosedCash");
		}
		return true;
	}

	public UserInfo getUserInfo() {
		return new UserInfo(m_sId, m_sName);
	}

	public UserInfo getTicketUserInfo() {
		return m_sellerSession ? null : getUserInfo();
	}

	public UserInfo getSelectedTicketUser() {
		return m_selectedTicketUser;
	}

	public void setSelectedTicketUser(UserInfo user) {
		m_selectedTicketUser = user;
	}

	private static String mapNewClass(String classname) {
		String newclass = m_oldclasses.get(classname);
		return newclass == null ? classname : newclass;
	}

	private static void initOldClasses() {
		m_oldclasses = new HashMap<String, String>();

		// update permissions from 0.0.24 to 2.20
		m_oldclasses.put("net.adrianromero.tpv.panelsales.JPanelTicketSales",
				"com.openbravo.pos.sales.JPanelTicketSales");
		m_oldclasses.put("net.adrianromero.tpv.panelsales.JPanelTicketEdits",
				"com.openbravo.pos.sales.JPanelTicketEdits");
		m_oldclasses.put("net.adrianromero.tpv.panels.JPanelPayments", "com.openbravo.pos.panels.JPanelPayments");
		m_oldclasses.put("net.adrianromero.tpv.panels.JPanelCloseMoney", "com.openbravo.pos.panels.JPanelCloseMoney");

		// m_oldclasses.put("payment.cash", "");
		// m_oldclasses.put("payment.cheque", "");
		// m_oldclasses.put("payment.paper", "");
		// m_oldclasses.put("payment.tichet", "");
		// m_oldclasses.put("payment.magcard", "");
		// m_oldclasses.put("payment.free", "");
		// m_oldclasses.put("refund.cash", "");
		// m_oldclasses.put("refund.cheque", "");
		// m_oldclasses.put("refund.paper", "");
		// m_oldclasses.put("refund.magcard", "");

		m_oldclasses.put("Menu.StockManagement", "com.openbravo.pos.forms.MenuStockManagement");
		m_oldclasses.put("net.adrianromero.tpv.inventory.ProductsPanel", "com.openbravo.pos.inventory.ProductsPanel");
		m_oldclasses.put("net.adrianromero.tpv.inventory.CategoriesPanel",
				"com.openbravo.pos.inventory.CategoriesPanel");
		m_oldclasses.put("net.adrianromero.tpv.panels.JPanelTax", "com.openbravo.pos.inventory.TaxPanel");
		m_oldclasses.put("net.adrianromero.tpv.inventory.StockDiaryPanel",
				"com.openbravo.pos.inventory.StockDiaryPanel");

		m_oldclasses.put("Menu.SalesManagement", "com.openbravo.pos.forms.MenuSalesManagement");

		m_oldclasses.put("Menu.Maintenance", "com.openbravo.pos.forms.MenuMaintenance");
		m_oldclasses.put("net.adrianromero.tpv.admin.PeoplePanel", "com.openbravo.pos.admin.PeoplePanel");
		m_oldclasses.put("net.adrianromero.tpv.admin.RolesPanel", "com.openbravo.pos.admin.RolesPanel");
		m_oldclasses.put("net.adrianromero.tpv.admin.ResourcesPanel", "com.openbravo.pos.admin.ResourcesPanel");

		m_oldclasses.put("Menu.ChangePassword", "Menu.ChangePassword");
		m_oldclasses.put("net.adrianromero.tpv.panels.JPanelPrinter", "com.openbravo.pos.panels.JPanelPrinter");
		m_oldclasses.put("net.adrianromero.tpv.config.JPanelConfiguration",
				"com.openbravo.pos.config.JPanelConfiguration");

		// m_oldclasses.put("button.print", "");
		// m_oldclasses.put("button.opendrawer", "");

		// update permissions from 2.10 to 2.20
		m_oldclasses.put("com.openbravo.pos.panels.JPanelTax", "com.openbravo.pos.inventory.TaxPanel");

	}

	private class ConfigurationHandler extends DefaultHandler {
		@Override
		public void startDocument() throws SAXException {
		}
		@Override
		public void endDocument() throws SAXException {
		}
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)
				throws SAXException {
			if ("class".equals(qName)) {
				m_apermissions.add(mapNewClass(attributes.getValue("name")));
			}
		}
		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
		}
		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
		}
	}

}
