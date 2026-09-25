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

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.event.*;
import java.lang.reflect.Constructor;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import javax.swing.*;

import com.openbravo.pos.printer.*;
import com.openbravo.pos.theme.RetailPOSColors;

import com.openbravo.beans.*;

import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.data.gui.JMessageDialog;
import com.openbravo.data.loader.Session;
import com.openbravo.pos.ticket.LoyaltyConfiguration;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author adrianromero
 */
public class JRootApp extends JPanel implements AppView {

	private static final Logger LOGGER = Logger.getLogger(JRootApp.class.getName());

	private static final int HEADER_HEIGHT = 64;
	private static final int HEADER_LOGO_HEIGHT = 40;
	private static final int HEADER_GAP = 18;
	// Retail POS design system tokens (com.openbravo.pos.theme.RetailPOSColors).
	private static final Color HEADER_BACKGROUND = RetailPOSColors.surface100();
	private static final Color HEADER_RULE = RetailPOSColors.border();
	private static final Color HEADER_TEXT = RetailPOSColors.ink();
	private static final Color HEADER_TEXT_MUTED = RetailPOSColors.inkMuted();
	private static final Color LOGIN_BACKGROUND = RetailPOSColors.surface0();
	private static final Color ADMIN_BUTTON = new Color(0x374151);

	// Two columns of staff buttons, four rows before the grid starts scrolling.
	private static final int LOGIN_BUTTON_WIDTH = 240;
	private static final int LOGIN_BUTTON_HEIGHT = 64;
	private static final int LOGIN_GRID_WIDTH = 2 * LOGIN_BUTTON_WIDTH + 30;
	private static final int LOGIN_GRID_HEIGHT = 4 * LOGIN_BUTTON_HEIGHT + 45;

	private AppProperties m_props;
	private Session session;
	private DataLogicSystem m_dlSystem;

	private Properties m_propsdb = null;
	private String m_sActiveCashIndex;
	private int m_iActiveCashSequence;
	private Date m_dActiveCashDateStart;
	private Date m_dActiveCashDateEnd;

	private String m_sInventoryLocation;

	private StringBuffer inputtext;
	private boolean m_administrationLogin;

	private DeviceTicket m_TP;
	private TicketParser m_TTP;

	private Map<String, BeanFactory> m_aBeanFactories;

	private JPrincipalApp m_principalapp = null;

	private static HashMap<String, String> m_oldclasses; // This is for backwards compatibility purposes

	static {
		initOldClasses();
	}

	/** Creates new form JRootApp */
	public JRootApp() {

		m_aBeanFactories = new HashMap<String, BeanFactory>();

		// Inicializo los componentes visuales
		initComponents();
		initHeader();
		jScrollPane1.getVerticalScrollBar().setPreferredSize(new Dimension(35, 35));
	}

	private void initHeader() {

		Font base = m_jLblSubTitle.getFont();
		m_jLblSubTitle.setFont(base.deriveFont(Font.PLAIN, 14f));
		m_jLblClock.setFont(base.deriveFont(Font.BOLD, 16f));
		m_jLblOperator.setFont(base.deriveFont(Font.PLAIN, 12f));
		m_jLblDemo.setFont(base.deriveFont(Font.BOLD, 16f));
		m_jLblDemo.setForeground(new Color(0x78350F));
		m_jLblDemo.setBackground(new Color(0xFDE68A));
		m_jLblDemo.setHorizontalAlignment(SwingConstants.CENTER);
		m_jLblDemo.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(0xF59E0B), 1),
				BorderFactory.createEmptyBorder(6, 14, 6, 14)));
		m_jLblDemo.setOpaque(true);
		m_jLblDemo.setText(AppLocal.getIntString("Label.DemoMode"));
		m_jLblDemo.setVisible(false);

		updateHeaderStatus();

		// HH:mm only, so a coarse tick is enough to keep it honest
		new Timer(10000, new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				updateHeaderStatus();
			}
		}).start();
	}

	private void updateHeaderStatus() {

		Date now = new Date();
		m_jLblClock.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(now));

		String sdate = DateFormat.getDateInstance(DateFormat.MEDIUM).format(now);
		m_jLblOperator
				.setText(m_principalapp == null ? sdate : m_principalapp.getUser().getName() + "  \u00B7  " + sdate);
		m_jLblDemo.setVisible(m_props != null && DemoMode.isActive(m_props));
	}

	private static Image scaleLogo(BufferedImage logo) {
		return logo.getHeight() == HEADER_LOGO_HEIGHT
				? logo
				: logo.getScaledInstance(-1, HEADER_LOGO_HEIGHT, Image.SCALE_SMOOTH);
	}

	public boolean initApp(AppProperties props) {

		m_props = props;
		LOGGER.info("event=pos_initialization_start host=" + props.getHost() + " " + LogSanitizer.configuration(props));
		// setPreferredSize(new java.awt.Dimension(800, 600));

		// support for different component orientation languages.
		applyComponentOrientation(ComponentOrientation.getOrientation(Locale.getDefault()));

		// Database start
		try {
			session = AppViewConnection.createSession(m_props);
		} catch (BasicException e) {
			LOGGER.log(Level.SEVERE, "event=pos_initialization_database_failed", e);
			JMessageDialog.showMessage(this, new MessageInf(MessageInf.SGN_DANGER, e.getMessage(), e));
			return false;
		}

		m_dlSystem = getBean(DataLogicSystem.class);

		// Cargamos las propiedades de base de datos
		m_propsdb = m_dlSystem.getResourceAsProperties(m_props.getHost() + "/properties");
		if (m_props instanceof AppConfig) {
			Properties localLoyalty = new Properties();
			LoyaltyConfiguration.apply(localLoyalty, m_dlSystem.getLoyaltySettings());
			((AppConfig) m_props).setProperty("loyalty.enabled", localLoyalty.getProperty("loyalty.enabled"));
			((AppConfig) m_props).setProperty("loyalty.name", localLoyalty.getProperty("loyalty.name"));
			((AppConfig) m_props).setProperty("loyalty.eligible_spend_per_stamp",
					localLoyalty.getProperty("loyalty.eligible_spend_per_stamp"));
			((AppConfig) m_props).setProperty("loyalty.redemption_value",
					localLoyalty.getProperty("loyalty.redemption_value"));
		}

		// creamos la caja activa si esta no existe
		try {
			String sActiveCashIndex = m_propsdb.getProperty("activecash");
			Object[] valcash = sActiveCashIndex == null ? null : m_dlSystem.findActiveCash(sActiveCashIndex);
			if (valcash == null || !m_props.getHost().equals(valcash[0])) {
				// no la encuentro o no es de mi host por tanto creo una...
				setActiveCash(UUID.randomUUID().toString(), m_dlSystem.getSequenceCash(m_props.getHost()) + 1,
						new Date(), null);

				// creamos la caja activa
				m_dlSystem.execInsertCash(new Object[]{getActiveCashIndex(), m_props.getHost(), getActiveCashSequence(),
						getActiveCashDateStart(), getActiveCashDateEnd()});
			} else {
				setActiveCash(sActiveCashIndex, (Integer) valcash[1], (Date) valcash[2], (Date) valcash[3]);
			}
		} catch (BasicException e) {
			LOGGER.log(Level.SEVERE, "event=pos_initialization_cash_failed", e);
			// Casco. Sin caja no hay pos
			MessageInf msg = new MessageInf(MessageInf.SGN_NOTICE, AppLocal.getIntString("message.cannotclosecash"), e);
			msg.show(this);
			session.close();
			return false;
		}

		// Leo la localizacion de la caja (Almacen).
		m_sInventoryLocation = m_propsdb.getProperty("location");
		if (m_sInventoryLocation == null) {
			m_sInventoryLocation = "0";
			m_propsdb.setProperty("location", m_sInventoryLocation);
			m_dlSystem.setResourceAsProperties(m_props.getHost() + "/properties", m_propsdb);
		}

		// Inicializo la impresora...
		m_TP = new DeviceTicket(this, m_props);

		// Inicializamos
		m_TTP = new TicketParser(getDeviceTicket(), m_dlSystem);
		printerStart();

		// Leemos los recursos basicos
		BufferedImage imgicon = m_dlSystem.getResourceAsImage("Window.Logo");
		m_jLblTitle.setIcon(imgicon == null ? null : new ImageIcon(scaleLogo(imgicon)));
		m_jLblTitle.setVisible(imgicon != null);
		m_jLblSubTitle.setText(m_dlSystem.getResourceAsText("Window.Title"));

		showLogin();

		DatabaseBackup.runDailyBackupIfDue(m_props);
		java.util.Timer backupTimer = new java.util.Timer("DailyDatabaseBackupScheduler", true);
		backupTimer.scheduleAtFixedRate(new java.util.TimerTask() {
			public void run() {
				DatabaseBackup.runDailyBackupIfDue(m_props);
			}
		}, 3600000L, 3600000L);

		LOGGER.info("event=pos_initialization_success printer=" + m_props.getProperty("machine.printer"));
		return true;
	}

	public void tryToClose() {

		if (closeAppView()) {

			// success. continue with the shut down

			// apago el visor
			m_TP.getDeviceDisplay().clearVisor();
			// me desconecto de la base de datos.
			session.close();

			// Download Root form
			SwingUtilities.getWindowAncestor(this).dispose();
		}
	}

	// Interfaz de aplicacion
	public DeviceTicket getDeviceTicket() {
		return m_TP;
	}

	public Session getSession() {
		return session;
	}

	public String getInventoryLocation() {
		return m_sInventoryLocation;
	}

	public String getActiveCashIndex() {
		return m_sActiveCashIndex;
	}

	public int getActiveCashSequence() {
		return m_iActiveCashSequence;
	}

	public Date getActiveCashDateStart() {
		return m_dActiveCashDateStart;
	}

	public Date getActiveCashDateEnd() {
		return m_dActiveCashDateEnd;
	}

	public void setActiveCash(String sIndex, int iSeq, Date dStart, Date dEnd) {
		m_sActiveCashIndex = sIndex;
		m_iActiveCashSequence = iSeq;
		m_dActiveCashDateStart = dStart;
		m_dActiveCashDateEnd = dEnd;

		m_propsdb.setProperty("activecash", m_sActiveCashIndex);
		m_dlSystem.setResourceAsProperties(m_props.getHost() + "/properties", m_propsdb);
	}

	public AppProperties getProperties() {
		return m_props;
	}

	public Object getBean(String beanfactory) throws BeanFactoryException {

		// For backwards compatibility
		beanfactory = mapNewClass(beanfactory);

		BeanFactory bf = m_aBeanFactories.get(beanfactory);
		if (bf == null) {

			// Class BeanFactory
			try {
				Class bfclass = Class.forName(beanfactory);

				if (BeanFactory.class.isAssignableFrom(bfclass)) {
					bf = (BeanFactory) bfclass.newInstance();
				} else {
					// the old construction for beans...
					Constructor constMyView = bfclass.getConstructor(new Class[]{AppView.class});
					Object bean = constMyView.newInstance(new Object[]{this});

					bf = new BeanFactoryObj(bean);
				}

			} catch (Exception e) {
				// ClassNotFoundException, InstantiationException, IllegalAccessException,
				// NoSuchMethodException, InvocationTargetException
				throw new BeanFactoryException(e);
			}

			// cache the factory
			m_aBeanFactories.put(beanfactory, bf);

			// Initialize if it is a BeanFactoryApp
			if (bf instanceof BeanFactoryApp) {
				((BeanFactoryApp) bf).init(this);
			}
		}
		return bf.getBean();
	}

	public <T> T getBean(Class<T> beanClass) throws BeanFactoryException {
		if (beanClass == null) {
			throw new BeanFactoryException("Bean class cannot be null");
		}

		Object bean = getBean(beanClass.getName());
		if (bean == null) {
			throw new BeanFactoryException("Bean " + beanClass.getName() + " resolved to null");
		}
		try {
			return beanClass.cast(bean);
		} catch (ClassCastException e) {
			BeanFactoryException exception = new BeanFactoryException(
					"Bean " + beanClass.getName() + " resolved to " + bean.getClass().getName());
			exception.initCause(e);
			throw exception;
		}
	}

	private static String mapNewClass(String classname) {
		String newclass = m_oldclasses.get(classname);
		return newclass == null ? classname : newclass;
	}

	private static void initOldClasses() {
		m_oldclasses = new HashMap<String, String>();

		// update bean names from 2.10 to 2.20
		m_oldclasses.put("com.openbravo.pos.panels.JPanelTax", "com.openbravo.pos.inventory.TaxPanel");

	}

	public void waitCursorBegin() {
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	}

	public void waitCursorEnd() {
		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}

	public AppUserView getAppUserView() {
		return m_principalapp;
	}

	private void printerStart() {

		String sresource = m_dlSystem.getResourceAsXML("Printer.Start");
		if (sresource == null) {
			m_TP.getDeviceDisplay().writeVisor(AppLocal.APP_NAME, AppLocal.APP_VERSION);
		} else {
			try {
				m_TTP.printTicket(sresource);
			} catch (TicketPrinterException eTP) {
				m_TP.getDeviceDisplay().writeVisor(AppLocal.APP_NAME, AppLocal.APP_VERSION);
			}
		}
	}

	private void listPeople(boolean administratorsOnly) {

		try {

			jScrollPane1.getViewport().setView(null);

			JFlowPanel jPeople = new JFlowPanel();
			jPeople.setOpaque(false);
			jPeople.applyComponentOrientation(getComponentOrientation());

			java.util.List people = m_dlSystem.listPeopleVisible();

			for (int i = 0; i < people.size(); i++) {

				AppUser user = (AppUser) people.get(i);
				if (administratorsOnly && !AppUser.ROLE_ADMINISTRATOR.equals(user.getRole())
						&& !AppUser.ROLE_MANAGER.equals(user.getRole())) {
					continue;
				}

				JButton btn = new JButton(new AppUserAction(user));
				btn.applyComponentOrientation(getComponentOrientation());
				btn.setFocusPainted(false);
				btn.setFocusable(false);
				btn.setRequestFocusEnabled(false);
				btn.setHorizontalAlignment(SwingConstants.LEADING);
				btn.setMaximumSize(new Dimension(LOGIN_BUTTON_WIDTH, LOGIN_BUTTON_HEIGHT));
				btn.setPreferredSize(new Dimension(LOGIN_BUTTON_WIDTH, LOGIN_BUTTON_HEIGHT));
				btn.setMinimumSize(new Dimension(LOGIN_BUTTON_WIDTH, LOGIN_BUTTON_HEIGHT));

				jPeople.add(btn);
			}
			jScrollPane1.getViewport().setView(jPeople);

		} catch (BasicException ee) {
			LOGGER.log(Level.WARNING, "event=login_users_load_failed", ee);
		}
	}

	// La accion del selector
	private class AppUserAction extends AbstractAction {

		private AppUser m_actionuser;

		public AppUserAction(AppUser user) {
			m_actionuser = user;
			putValue(Action.SMALL_ICON, m_actionuser.getIcon());
			putValue(Action.NAME, m_actionuser.getName());
		}

		public AppUser getUser() {
			return m_actionuser;
		}

		public void actionPerformed(ActionEvent evt) {
			try (LogContext.Scope ignored = LogContext.beginOperation()) {
				// String sPassword = m_actionuser.getPassword();
				if (m_actionuser.authenticate()) {
					LOGGER.info("event=login_success userId=" + m_actionuser.getId() + " role=" + m_actionuser.getRole()
							+ " authentication=implicit");
					// p'adentro directo, no tiene password
					openAdministrationView(m_actionuser);
				} else {
					// comprobemos la clave antes de entrar...
					String sPassword = JPasswordDialog.showEditPassword(JRootApp.this,
							AppLocal.getIntString("Label.Password"), m_actionuser.getName(), m_actionuser.getIcon());
					if (sPassword != null) {
						if (m_actionuser.authenticate(sPassword)) {
							LOGGER.info("event=login_success userId=" + m_actionuser.getId() + " role="
									+ m_actionuser.getRole() + " authentication=password");
							openAdministrationView(m_actionuser);
						} else {
							LOGGER.warning("event=login_failed userId=" + m_actionuser.getId() + " role="
									+ m_actionuser.getRole() + " reason=invalid_password");
							MessageInf msg = new MessageInf(MessageInf.SGN_WARNING,
									AppLocal.getIntString("message.BadPassword"));
							msg.show(JRootApp.this);
						}
					}
				}
			}
		}
	}
	private void showView(String view) {
		CardLayout cl = (CardLayout) (m_jPanelContainer.getLayout());
		cl.show(m_jPanelContainer, view);
	}

	private void openAppView(AppUser user, boolean administrationMode) {

		if (closeAppView()) {

			user.setAdministrationMode(administrationMode);
			m_principalapp = new JPrincipalApp(this, user);

			// The main panel
			m_jPanelContainer.add(m_principalapp, "_" + m_principalapp.getUser().getId());
			showView("_" + m_principalapp.getUser().getId());

			updateHeaderStatus();

			m_principalapp.activate();
			LOGGER.info("event=mode_open mode=" + (administrationMode ? "administration" : "sales") + " userId="
					+ user.getId() + " role=" + user.getRole());
		}
	}

	private void openAdministrationView(AppUser user) {
		openAppView(user, true);
	}

	void openSalesView() {
		AppUser till = new AppUser("till", AppLocal.getIntString("Button.SalesMode"), null, null, AppUser.ROLE_SELLER,
				null);
		till.setSellerSession(true);
		openAppView(till, false);
	}

	public boolean closeAppView() {

		if (m_principalapp == null) {
			return true;
		} else if (!m_principalapp.deactivate()) {
			return false;
		} else {
			LOGGER.info("event=mode_close userId=" + m_principalapp.getUser().getId() + " role="
					+ m_principalapp.getUser().getRole());
			// remove the card
			m_jPanelContainer.remove(m_principalapp);
			m_principalapp = null;

			updateHeaderStatus();

			showLogin();

			return true;
		}
	}

	private void showLogin() {

		m_administrationLogin = false;
		jLabel1.setText(AppLocal.getIntString("Label.ChooseMode"));
		m_jSalesMode.setVisible(true);
		m_jAdminMode.setVisible(true);
		m_jLogonName.setVisible(false);
		jScrollPane1.getViewport().setView(null);
		showView("login");

		// show welcome message
		printerStart();

		// keyboard listener activation
		inputtext = new StringBuffer();
		m_txtKeys.setText(null);
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				m_txtKeys.requestFocus();
			}
		});
	}

	private void processKey(char c) {

		if (c == '\n') {

			if (!m_administrationLogin) {
				inputtext = new StringBuffer();
				return;
			}

			AppUser user = null;
			try {
				user = m_dlSystem.findPeopleByCard(inputtext.toString());
			} catch (BasicException e) {
				LOGGER.log(Level.WARNING, "event=login_card_lookup_failed", e);
			}

			if (user == null) {
				LOGGER.warning("event=login_failed authentication=card reason=unknown_card");
				// user not found
				MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.nocard"));
				msg.show(this);
			} else {
				if (AppUser.ROLE_ADMINISTRATOR.equals(user.getRole()) || AppUser.ROLE_MANAGER.equals(user.getRole())) {
					LOGGER.info("event=login_success userId=" + user.getId() + " role=" + user.getRole()
							+ " authentication=card");
					openAdministrationView(user);
				} else {
					LOGGER.warning("event=login_failed userId=" + user.getId() + " role=" + user.getRole()
							+ " authentication=card reason=admin_only");
					MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.adminonly"));
					msg.show(this);
				}
			}

			inputtext = new StringBuffer();
		} else {
			inputtext.append(c);
		}
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the FormEditor.
	 */
	// <editor-fold defaultstate="collapsed" desc="Generated
	// Code">//GEN-BEGIN:initComponents
	private void initComponents() {

		m_jPanelTitle = new javax.swing.JPanel();
		m_jPanelBrand = new javax.swing.JPanel();
		m_jLblTitle = new javax.swing.JLabel();
		m_jLblSubTitle = new javax.swing.JLabel();
		m_jPanelStatus = new javax.swing.JPanel();
		m_jLblClock = new javax.swing.JLabel();
		m_jLblOperator = new javax.swing.JLabel();
		m_jLblDemo = new javax.swing.JLabel();
		m_jPanelContainer = new javax.swing.JPanel();
		m_jPanelLogin = new javax.swing.JPanel();
		jPanel4 = new javax.swing.JPanel();
		jLabel1 = new javax.swing.JLabel();
		m_jSalesMode = new javax.swing.JButton();
		m_jAdminMode = new javax.swing.JButton();
		jPanel5 = new javax.swing.JPanel();
		m_jLogonName = new javax.swing.JPanel();
		jScrollPane1 = new javax.swing.JScrollPane();
		jPanel2 = new javax.swing.JPanel();
		jPanel8 = new javax.swing.JPanel();
		m_jAbout = new javax.swing.JButton();
		m_jClose = new javax.swing.JButton();
		jPanel1 = new javax.swing.JPanel();
		m_txtKeys = new javax.swing.JTextField();

		setPreferredSize(new java.awt.Dimension(1024, 768));
		setLayout(new java.awt.BorderLayout());

		m_jPanelTitle.setBackground(HEADER_BACKGROUND);
		m_jPanelTitle.setBorder(javax.swing.BorderFactory.createCompoundBorder(
				javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, HEADER_RULE),
				javax.swing.BorderFactory.createEmptyBorder(0, HEADER_GAP, 0, HEADER_GAP)));
		m_jPanelTitle.setPreferredSize(new java.awt.Dimension(0, HEADER_HEIGHT));
		m_jPanelTitle.setLayout(new java.awt.BorderLayout());

		m_jPanelBrand.setOpaque(false);
		m_jPanelBrand.setLayout(new java.awt.BorderLayout());

		// The logo carries the rule that separates it from the tagline, so hiding
		// the logo when the resource is empty hides the rule with it.
		m_jLblTitle.setBorder(javax.swing.BorderFactory.createCompoundBorder(
				javax.swing.BorderFactory.createEmptyBorder(12, 0, 12, 0),
				javax.swing.BorderFactory.createCompoundBorder(
						javax.swing.BorderFactory.createMatteBorder(0, 0, 0, 1, HEADER_RULE),
						javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, HEADER_GAP))));
		m_jPanelBrand.add(m_jLblTitle, java.awt.BorderLayout.LINE_START);

		m_jLblSubTitle.setForeground(HEADER_TEXT_MUTED);
		m_jLblSubTitle.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, HEADER_GAP, 0, 0));
		m_jPanelBrand.add(m_jLblSubTitle, java.awt.BorderLayout.CENTER);

		m_jPanelTitle.add(m_jPanelBrand, java.awt.BorderLayout.LINE_START);

		m_jPanelStatus.setOpaque(false);
		m_jPanelStatus.setLayout(new javax.swing.BoxLayout(m_jPanelStatus, javax.swing.BoxLayout.Y_AXIS));

		m_jPanelStatus.add(javax.swing.Box.createVerticalGlue());

		m_jPanelStatus.add(m_jLblDemo);

		m_jLblClock.setForeground(HEADER_TEXT);
		m_jLblClock.setAlignmentX(java.awt.Component.RIGHT_ALIGNMENT);
		m_jPanelStatus.add(m_jLblClock);

		m_jLblOperator.setForeground(HEADER_TEXT_MUTED);
		m_jLblOperator.setAlignmentX(java.awt.Component.RIGHT_ALIGNMENT);
		m_jPanelStatus.add(m_jLblOperator);

		m_jPanelStatus.add(javax.swing.Box.createVerticalGlue());

		m_jPanelTitle.add(m_jPanelStatus, java.awt.BorderLayout.LINE_END);

		add(m_jPanelTitle, java.awt.BorderLayout.NORTH);

		m_jPanelContainer.setLayout(new java.awt.CardLayout());

		m_jPanelLogin.setLayout(new java.awt.BorderLayout());

		jPanel4.setBorder(javax.swing.BorderFactory.createEmptyBorder(48, 24, 24, 24));
		jPanel4.setLayout(new javax.swing.BoxLayout(jPanel4, javax.swing.BoxLayout.Y_AXIS));

		jLabel1.setText(AppLocal.getIntString("Label.WhoWorks")); // NOI18N
		jLabel1.setFont(jLabel1.getFont().deriveFont(java.awt.Font.BOLD, 20f));
		jLabel1.setForeground(HEADER_TEXT);
		jLabel1.setAlignmentX(0.5F);
		jPanel4.add(jLabel1);

		jPanel4.add(javax.swing.Box.createVerticalStrut(20));

		m_jSalesMode.setText("<html><b>" + AppLocal.getIntString("Button.SalesMode") + "</b><br>" + "<font size='3'>"
				+ AppLocal.getIntString("Label.SalesModeHint") + "</font></html>");
		m_jSalesMode.setIcon(new ImageIcon(getClass().getResource("/com/openbravo/images/menu-sales.png")));
		m_jSalesMode.setToolTipText(AppLocal.getIntString("Label.SalesModeHint"));
		RetailPOSColors.primaryButton(m_jSalesMode);
		m_jSalesMode.setFont(m_jSalesMode.getFont().deriveFont(java.awt.Font.BOLD, 20f));
		m_jSalesMode.setHorizontalAlignment(SwingConstants.LEADING);
		m_jSalesMode.setIconTextGap(18);
		m_jSalesMode.setFocusPainted(false);
		m_jSalesMode.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));
		m_jSalesMode.setOpaque(true);
		m_jSalesMode.setAlignmentX(0.5F);
		m_jSalesMode.setMaximumSize(new Dimension(460, 82));
		m_jSalesMode.setPreferredSize(new Dimension(460, 82));
		m_jSalesMode.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				openSalesView();
			}
		});
		m_jAdminMode.setText("<html><b>" + AppLocal.getIntString("Button.AdministrationMode") + "</b><br>"
				+ "<font size='3'>" + AppLocal.getIntString("Label.AdministrationModeHint") + "</font></html>");
		m_jAdminMode.setIcon(new ImageIcon(getClass().getResource("/com/openbravo/images/menu-maintenance.png")));
		m_jAdminMode.setToolTipText(AppLocal.getIntString("Label.AdministrationModeHint"));
		m_jAdminMode.setBackground(ADMIN_BUTTON);
		m_jAdminMode.setForeground(Color.WHITE);
		m_jAdminMode.setFont(m_jAdminMode.getFont().deriveFont(java.awt.Font.BOLD, 20f));
		m_jAdminMode.setHorizontalAlignment(SwingConstants.LEADING);
		m_jAdminMode.setIconTextGap(18);
		m_jAdminMode.setFocusPainted(false);
		m_jAdminMode.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));
		m_jAdminMode.setOpaque(true);
		m_jAdminMode.setAlignmentX(0.5F);
		m_jAdminMode.setMaximumSize(new Dimension(460, 82));
		m_jAdminMode.setPreferredSize(new Dimension(460, 82));
		m_jAdminMode.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				m_administrationLogin = true;
				jLabel1.setText(AppLocal.getIntString("Label.ChooseAdministrator"));
				m_jSalesMode.setVisible(true);
				m_jAdminMode.setVisible(true);
				m_jLogonName.setVisible(true);
				listPeople(true);
				jPanel4.revalidate();
				jPanel4.repaint();
			}
		});
		jPanel4.add(m_jSalesMode);
		jPanel4.add(javax.swing.Box.createVerticalStrut(8));
		jPanel4.add(m_jAdminMode);
		jPanel4.add(javax.swing.Box.createVerticalStrut(20));

		m_jLogonName.setLayout(new java.awt.BorderLayout());
		m_jLogonName.setOpaque(false);
		m_jLogonName.setAlignmentX(0.5F);
		m_jLogonName.setMaximumSize(new java.awt.Dimension(LOGIN_GRID_WIDTH, LOGIN_GRID_HEIGHT));
		m_jLogonName.setVisible(false);

		jScrollPane1.setBorder(null);
		jScrollPane1.setOpaque(false);
		jScrollPane1.getViewport().setOpaque(false);
		jScrollPane1.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		jScrollPane1.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		jScrollPane1.setPreferredSize(new java.awt.Dimension(LOGIN_GRID_WIDTH, LOGIN_GRID_HEIGHT));
		m_jLogonName.add(jScrollPane1, java.awt.BorderLayout.CENTER);

		jPanel4.add(m_jLogonName);

		m_jPanelLogin.add(jPanel4, java.awt.BorderLayout.CENTER);
		m_jPanelLogin.setBackground(LOGIN_BACKGROUND);
		jPanel4.setOpaque(false);

		jPanel5.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 24, 20, 24));
		jPanel5.setLayout(new java.awt.BorderLayout());

		// Card swipes are read by a zero-sized field that always holds the focus.
		jPanel1.setLayout(null);
		jPanel1.setPreferredSize(new java.awt.Dimension(0, 0));

		m_txtKeys.setPreferredSize(new java.awt.Dimension(0, 0));
		m_txtKeys.addKeyListener(new java.awt.event.KeyAdapter() {
			public void keyTyped(java.awt.event.KeyEvent evt) {
				m_txtKeysKeyTyped(evt);
			}
		});
		jPanel1.add(m_txtKeys);
		m_txtKeys.setBounds(0, 0, 0, 0);

		jPanel2.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEADING, 0, 0));
		jPanel2.add(jPanel1);

		m_jAbout.setText(AppLocal.getIntString("Button.About")); // NOI18N
		m_jAbout.setForeground(HEADER_TEXT_MUTED);
		m_jAbout.setFocusPainted(false);
		m_jAbout.setFocusable(false);
		m_jAbout.setRequestFocusEnabled(false);
		m_jAbout.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				m_jAboutActionPerformed(evt);
			}
		});
		jPanel2.add(m_jAbout);

		jPanel5.add(jPanel2, java.awt.BorderLayout.LINE_START);

		jPanel8.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.TRAILING, 0, 0));

		m_jClose.setText(AppLocal.getIntString("Button.Close")); // NOI18N
		m_jClose.setForeground(HEADER_TEXT_MUTED);
		m_jClose.setFocusPainted(false);
		m_jClose.setFocusable(false);
		m_jClose.setRequestFocusEnabled(false);
		m_jClose.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				m_jCloseActionPerformed(evt);
			}
		});
		jPanel8.add(m_jClose);

		jPanel5.add(jPanel8, java.awt.BorderLayout.LINE_END);

		m_jPanelLogin.add(jPanel5, java.awt.BorderLayout.SOUTH);

		m_jPanelContainer.add(m_jPanelLogin, "login");

		add(m_jPanelContainer, java.awt.BorderLayout.CENTER);

	}// </editor-fold>//GEN-END:initComponents

	private void m_jCloseActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_m_jCloseActionPerformed

		tryToClose();

	}// GEN-LAST:event_m_jCloseActionPerformed

	private void m_jAboutActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_m_jAboutActionPerformed

		showAbout();

	}// GEN-LAST:event_m_jAboutActionPerformed

	private void showAbout() {

		JLabel about = new JLabel("<html><body style='width: 420px'>" + "<b>" + AppLocal.APP_NAME + "</b> "
				+ AppLocal.APP_VERSION + "<br><br>"
				+ "Retail POS is a point of sale application designed for touch screens.<br>"
				+ "A fork of Openbravo POS.<br>" + "Copyright \u00A9 2007-2009 Openbravo, S.L.<br><br>"
				+ "Retail POS is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.<br><br>"
				+ "Retail POS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.<br><br>"
				+ "You should have received a copy of the GNU General Public License along with Retail POS.  If not, see http://www.gnu.org/licenses/.</body></html>");
		about.setIcon(new ImageIcon(getClass().getResource("/com/openbravo/images/logo.png")));
		about.setVerticalTextPosition(SwingConstants.BOTTOM);
		about.setHorizontalTextPosition(SwingConstants.CENTER);
		about.applyComponentOrientation(getComponentOrientation());

		JOptionPane.showMessageDialog(this, about, AppLocal.getIntString("Button.About"), JOptionPane.PLAIN_MESSAGE);
	}

	private void m_txtKeysKeyTyped(java.awt.event.KeyEvent evt) {// GEN-FIRST:event_m_txtKeysKeyTyped

		m_txtKeys.setText("0");

		processKey(evt.getKeyChar());

	}// GEN-LAST:event_m_txtKeysKeyTyped

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JLabel jLabel1;
	private javax.swing.JPanel jPanel1;
	private javax.swing.JPanel jPanel2;
	private javax.swing.JPanel jPanel4;
	private javax.swing.JPanel jPanel5;
	private javax.swing.JPanel jPanel8;
	private javax.swing.JScrollPane jScrollPane1;
	private javax.swing.JButton m_jAbout;
	private javax.swing.JButton m_jClose;
	private javax.swing.JLabel m_jLblClock;
	private javax.swing.JLabel m_jLblDemo;
	private javax.swing.JLabel m_jLblOperator;
	private javax.swing.JButton m_jAdminMode;
	private javax.swing.JButton m_jSalesMode;
	private javax.swing.JLabel m_jLblSubTitle;
	private javax.swing.JLabel m_jLblTitle;
	private javax.swing.JPanel m_jLogonName;
	private javax.swing.JPanel m_jPanelBrand;
	private javax.swing.JPanel m_jPanelContainer;
	private javax.swing.JPanel m_jPanelLogin;
	private javax.swing.JPanel m_jPanelStatus;
	private javax.swing.JPanel m_jPanelTitle;
	private javax.swing.JTextField m_txtKeys;
	// End of variables declaration//GEN-END:variables
}
