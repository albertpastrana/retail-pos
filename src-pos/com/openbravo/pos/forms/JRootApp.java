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
import java.awt.geom.Path2D;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import javax.swing.*;
import javax.swing.border.AbstractBorder;

import com.openbravo.pos.printer.*;
import com.openbravo.pos.theme.RetailPOSColors;
import com.openbravo.pos.theme.RetailPOSTheme;

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
	private static final int CARD_ARC = 16;
	private JPanel loginSteps;
	private JPanel choiceStep;
	private int modeContentWidth;
	private JPanel administratorStep;
	private JPanel pickerColumn;
	private JPanel pickerHeader;
	private JPanel peopleGrid;
	private JButton backButton;
	private JLabel tillLabel;
	private final java.util.List<JButton> administratorButtons = new java.util.ArrayList<>();

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
	private KeyEventDispatcher cardDispatcher;

	private DeviceTicket m_TP;
	private TicketParser m_TTP;

	private Map<String, BeanFactory> m_aBeanFactories;

	private JPrincipalApp m_principalapp = null;

	/** Creates new form JRootApp */
	public JRootApp() {

		m_aBeanFactories = new HashMap<String, BeanFactory>();

		// Inicializo los componentes visuales
		initComponents();
		initHeader();
		cardDispatcher = event -> {
			if (!m_administrationLogin || !m_jPanelLogin.isShowing() || !SwingUtilities.isDescendingFrom(
					KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner(), m_jPanelLogin)) {
				return false;
			}
			if (event.getID() == KeyEvent.KEY_PRESSED && event.getKeyCode() == KeyEvent.VK_ESCAPE) {
				showChoiceStep();
				return true;
			}
			if (event.getID() == KeyEvent.KEY_PRESSED && event.getKeyCode() == KeyEvent.VK_ENTER
					&& inputtext.length() > 0) {
				processKey('\n');
				return true;
			}
			if (event.getID() == KeyEvent.KEY_TYPED && !event.isAltDown() && !event.isControlDown()
					&& !event.isMetaDown() && !Character.isISOControl(event.getKeyChar())
					&& event.getKeyChar() != ' ') {
				processKey(event.getKeyChar());
				return true;
			}
			return false;
		};
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(cardDispatcher);
	}

	private void initHeader() {

		Font base = m_jLblSubTitle.getFont();
		m_jLblSubTitle.setFont(base.deriveFont(Font.PLAIN, 14f));
		m_jLblClock.setFont(RetailPOSTheme.PLEX_MONO_SEMIBOLD.deriveFont(20f));
		m_jLblOperator.setFont(base.deriveFont(Font.PLAIN, 12f));
		m_jLblDemo.setFont(base.deriveFont(Font.BOLD, 16f));
		m_jLblDemo.setForeground(RetailPOSColors.warning());
		m_jLblDemo.setBackground(RetailPOSColors.surface100());
		m_jLblDemo.setHorizontalAlignment(SwingConstants.CENTER);
		m_jLblDemo.setBorder(
				BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(RetailPOSColors.warning(), 1),
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
		m_jLblClock.setText(new java.text.SimpleDateFormat("HH:mm").format(now));

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
		CatalogFileImporter.importAtStartup(session, m_props);

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
			KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(cardDispatcher);

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
		if (tillLabel != null) {
			updateCashStatus();
		}

		m_propsdb.setProperty("activecash", m_sActiveCashIndex);
		m_dlSystem.setResourceAsProperties(m_props.getHost() + "/properties", m_propsdb);
	}

	public AppProperties getProperties() {
		return m_props;
	}

	private BeanFactory getBeanFactory(Class<?> beanFactoryClass) throws BeanFactoryException {
		String beanFactoryName = beanFactoryClass.getName();
		BeanFactory beanFactory = m_aBeanFactories.get(beanFactoryName);
		if (beanFactory == null) {
			beanFactory = createBeanFactory(beanFactoryClass);
			m_aBeanFactories.put(beanFactoryName, beanFactory);
			if (beanFactory instanceof BeanFactoryApp) {
				((BeanFactoryApp) beanFactory).init(this);
			}
		}
		return beanFactory;
	}

	private BeanFactory createBeanFactory(Class<?> beanFactoryClass) throws BeanFactoryException {
		try {
			if (BeanFactory.class.isAssignableFrom(beanFactoryClass)) {
				return (BeanFactory) beanFactoryClass.getDeclaredConstructor().newInstance();
			}
			return new BeanFactoryObj(beanFactoryClass.getConstructor(AppView.class).newInstance(this));
		} catch (ReflectiveOperationException e) {
			throw new BeanFactoryException(e);
		}
	}

	public <T> T getBean(Class<T> beanClass) throws BeanFactoryException {
		if (beanClass == null) {
			throw new BeanFactoryException("Bean class cannot be null");
		}

		Object bean = getBeanFactory(beanClass).getBean();
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

			peopleGrid.removeAll();
			administratorButtons.clear();

			java.util.List people = m_dlSystem.listPeopleVisible();

			for (int i = 0; i < people.size(); i++) {

				AppUser user = (AppUser) people.get(i);
				if (administratorsOnly && !AppUser.ROLE_ADMINISTRATOR.equals(user.getRole())
						&& !AppUser.ROLE_MANAGER.equals(user.getRole())) {
					continue;
				}

				JButton btn = administratorButton(user);
				administratorButtons.add(btn);
				peopleGrid.add(btn);
			}
			reflowPeople();

		} catch (BasicException ee) {
			LOGGER.log(Level.WARNING, "event=login_users_load_failed", ee);
		}
	}

	JButton administratorButton(AppUser user) {
		JButton btn = new RoundedButton(false);
		btn.setAction(new AppUserAction(user));
		btn.applyComponentOrientation(getComponentOrientation());
		btn.setText("<html><b>" + escapeHtml(user.getName()) + "</b><br>" + AppLocal.getIntString(
				AppUser.ROLE_ADMINISTRATOR.equals(user.getRole()) ? "Label.AdministratorRole" : "Label.ManagerRole")
				+ "</html>");
		btn.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(17f));
		btn.setHorizontalAlignment(SwingConstants.LEADING);
		if (user.getIcon() == null) {
			btn.setIcon(new InitialsIcon(user.getName()));
		}
		btn.setIconTextGap(12);
		styleSecondary(btn, false);
		btn.setBorder(BorderFactory.createCompoundBorder(new RoundedBorder(RetailPOSColors.border(), 1, CARD_ARC),
				BorderFactory.createEmptyBorder(12, 16, 12, 16)));
		focusRing(btn);
		return btn;
	}

	private static String escapeHtml(String text) {
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private void reflowPeople() {
		// Leave breathing room on both sides of the picker. At a 1024px till this
		// yields a centred 720px grid, with readable cards in two columns.
		int width = Math.max(0, Math.min(720, m_jPanelLogin.getWidth() - 192));
		int columns = width >= 520 ? 2 : 1;
		if (!(peopleGrid.getLayout() instanceof GridLayout)
				|| ((GridLayout) peopleGrid.getLayout()).getColumns() != columns) {
			peopleGrid.setLayout(new GridLayout(0, columns, 12, 12));
		}
		peopleGrid.setPreferredSize(null);
		int height = peopleGrid.getPreferredSize().height;
		peopleGrid.setPreferredSize(new Dimension(width, height));
		peopleGrid.setMaximumSize(new Dimension(width, peopleGrid.getPreferredSize().height));
		peopleGrid.setMinimumSize(new Dimension(0, 0));
		pickerColumn.setPreferredSize(new Dimension(width, pickerHeader.getPreferredSize().height + 20 + height));
		pickerColumn.setMaximumSize(pickerColumn.getPreferredSize());
		pickerColumn.setMinimumSize(new Dimension(0, 0));
		administratorStep.invalidate();
		peopleGrid.revalidate();
		pickerColumn.revalidate();
		administratorStep.revalidate();
	}

	private void updateCashStatus() {
		if (m_props == null || m_dActiveCashDateStart == null) {
			return;
		}
		renderCashStatus(m_props.getHost(), m_dActiveCashDateEnd == null);
	}

	void renderCashStatus(String till, boolean open) {
		tillLabel.setText(AppLocal.getIntString("Label.Till") + " " + till + "  \u00b7  "
				+ AppLocal.getIntString(open ? "Label.CashOpen" : "Label.CashClosed"));
		m_jSalesMode.setText(salesModeText(open));
		m_jSalesMode.setToolTipText(AppLocal.getIntString(open ? "Label.SalesModeHint" : "Label.SalesNewSessionHint"));
		sizeModeButtons();
	}

	void sizeModeButtons() {
		// Give the two touch targets the same visual weight without stretching them
		// across a full-screen till. The content width remains the lower bound.
		int available = m_jPanelLogin.getWidth() - 64;
		int width = Math.max(modeContentWidth, Math.min(560, available * 54 / 100));
		m_jSalesMode.setPreferredSize(new Dimension(width, m_jSalesMode.getPreferredSize().height));
		m_jSalesMode.setMaximumSize(m_jSalesMode.getPreferredSize());
		m_jAdminMode.setPreferredSize(new Dimension(width, m_jAdminMode.getPreferredSize().height));
		m_jAdminMode.setMaximumSize(m_jAdminMode.getPreferredSize());
		choiceStep.revalidate();
	}

	private String salesModeText(boolean open) {
		return "<html><b>" + AppLocal.getIntString("Button.SalesMode") + "</b><br><font size='4'>"
				+ AppLocal.getIntString(open ? "Label.SalesModeHint" : "Label.SalesNewSessionHint") + "</font></html>";
	}

	private void showChoiceStep() {
		m_administrationLogin = false;
		inputtext = new StringBuffer();
		((CardLayout) loginSteps.getLayout()).show(loginSteps, "choice");
		updateCashStatus();
		SwingUtilities.invokeLater(() -> m_jSalesMode.requestFocusInWindow());
	}

	private void showAdministratorStep() {
		m_administrationLogin = true;
		inputtext = new StringBuffer();
		listPeople(true);
		((CardLayout) loginSteps.getLayout()).show(loginSteps, "administrators");
		SwingUtilities.invokeLater(() -> {
			if (!administratorButtons.isEmpty()) {
				administratorButtons.get(0).requestFocusInWindow();
			} else {
				backButton.requestFocusInWindow();
			}
		});
	}

	private void styleSecondary(JButton button, boolean danger) {
		button.setBackground(RetailPOSColors.surface100());
		button.setForeground(danger ? RetailPOSColors.dangerText() : RetailPOSColors.ink());
		button.setOpaque(false);
		button.setBorder(BorderFactory.createCompoundBorder(
				new RoundedBorder(danger ? RetailPOSColors.danger() : RetailPOSColors.border(), 1, CARD_ARC),
				BorderFactory.createEmptyBorder(12, 20, 12, 20)));
	}

	private void focusRing(JButton button) {
		javax.swing.border.Border normal = button.getBorder();
		button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2), normal));
		button.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent event) {
				button.setBorder(BorderFactory.createCompoundBorder(
						new RoundedBorder(RetailPOSColors.borderStrong(), 2, CARD_ARC + 4), normal));
			}
			@Override
			public void focusLost(FocusEvent event) {
				button.setBorder(
						BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2), normal));
			}
		});
	}

	private static class RoundedBorder extends AbstractBorder {
		private final Color color;
		private final int thickness;
		private final int arc;

		RoundedBorder(Color color, int thickness, int arc) {
			this.color = color;
			this.thickness = thickness;
			this.arc = arc;
		}

		@Override
		public Insets getBorderInsets(Component component) {
			return new Insets(thickness, thickness, thickness, thickness);
		}

		@Override
		public void paintBorder(Component component, Graphics graphics, int x, int y, int width, int height) {
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(color);
			g.setStroke(new BasicStroke(thickness));
			int inset = thickness / 2;
			g.drawRoundRect(x + inset, y + inset, width - thickness, height - thickness, arc, arc);
			g.dispose();
		}
	}

	private static class ModeIcon implements Icon {
		private final boolean sales;

		ModeIcon(boolean sales) {
			this.sales = sales;
		}

		@Override
		public int getIconWidth() {
			return 28;
		}

		@Override
		public int getIconHeight() {
			return 28;
		}

		@Override
		public void paintIcon(Component component, Graphics graphics, int x, int y) {
			Graphics2D g = (Graphics2D) graphics.create();
			g.translate(x + (sales ? 2 : 0), y + (sales ? 2 : 0));
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(sales ? RetailPOSColors.onBrand() : RetailPOSColors.ink());
			g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			if (sales) {
				// Same 24x24 basket silhouette as the wireframe SVG. Keep the curved
				// handle and the tapered body inside the icon's 28x28 paint bounds.
				Path2D.Double basket = new Path2D.Double();
				basket.moveTo(4, 7);
				basket.lineTo(20, 7);
				basket.lineTo(18.6, 17.2);
				basket.curveTo(18.45, 18.25, 17.65, 19, 16.6, 19);
				basket.lineTo(7.4, 19);
				basket.curveTo(6.35, 19, 5.55, 18.25, 5.4, 17.2);
				basket.closePath();
				g.draw(basket);
				Path2D.Double handle = new Path2D.Double();
				handle.moveTo(9, 7);
				handle.lineTo(9, 5.5);
				handle.curveTo(9, 3.85, 10.35, 2.5, 12, 2.5);
				handle.curveTo(13.65, 2.5, 15, 3.85, 15, 5.5);
				handle.lineTo(15, 7);
				g.draw(handle);
			} else {
				for (int row = 0; row < 3; row++) {
					int cy = 7 + row * 7;
					g.drawLine(3, cy, 25, cy);
					g.setColor(RetailPOSColors.surface100());
					g.fillOval(row == 1 ? 15 : 7, cy - 4, 8, 8);
					g.setColor(RetailPOSColors.ink());
					g.drawOval(row == 1 ? 15 : 7, cy - 4, 8, 8);
				}
			}
			g.dispose();
		}
	}

	private static class ExitIcon implements Icon {
		@Override
		public int getIconWidth() {
			return 20;
		}

		@Override
		public int getIconHeight() {
			return 20;
		}

		@Override
		public void paintIcon(Component component, Graphics graphics, int x, int y) {
			Graphics2D g = (Graphics2D) graphics.create();
			g.translate(x, y);
			g.scale(20.0 / 24, 20.0 / 24);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(RetailPOSColors.danger());
			g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			Path2D.Double door = new Path2D.Double();
			door.moveTo(10, 4);
			door.lineTo(6, 4);
			door.curveTo(4.9, 4, 4, 4.9, 4, 6);
			door.lineTo(4, 18);
			door.curveTo(4, 19.1, 4.9, 20, 6, 20);
			door.lineTo(10, 20);
			g.draw(door);
			Path2D.Double arrow = new Path2D.Double();
			arrow.moveTo(15, 8);
			arrow.lineTo(19, 12);
			arrow.lineTo(15, 16);
			arrow.moveTo(19, 12);
			arrow.lineTo(9, 12);
			g.draw(arrow);
			g.dispose();
		}
	}

	private static class BackIcon implements Icon {
		@Override
		public int getIconWidth() {
			return 20;
		}

		@Override
		public int getIconHeight() {
			return 20;
		}

		@Override
		public void paintIcon(Component component, Graphics graphics, int x, int y) {
			Graphics2D g = (Graphics2D) graphics.create();
			g.translate(x, y);
			g.scale(20.0 / 24, 20.0 / 24);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(RetailPOSColors.ink());
			g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			Path2D.Double arrow = new Path2D.Double();
			arrow.moveTo(15, 5);
			arrow.lineTo(8, 12);
			arrow.lineTo(15, 19);
			g.draw(arrow);
			g.dispose();
		}
	}

	private static class RoundedButton extends JButton {
		private final boolean primary;
		private boolean chevron;

		RoundedButton(boolean primary) {
			this.primary = primary;
			setOpaque(false);
			setContentAreaFilled(false);
			setRolloverEnabled(true);
		}

		void setChevron(boolean value) {
			chevron = value;
			repaint();
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(primary
					? (getModel().isRollover() || getModel().isPressed()
							? RetailPOSColors.brandStrong()
							: RetailPOSColors.brand())
					: (getModel().isRollover() ? RetailPOSColors.surface200() : RetailPOSColors.surface100()));
			g.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, CARD_ARC, CARD_ARC);
			g.dispose();
			super.paintComponent(graphics);
			if (chevron) {
				Graphics2D arrow = (Graphics2D) graphics.create();
				arrow.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				arrow.setColor(primary ? RetailPOSColors.onBrand() : RetailPOSColors.inkMuted());
				arrow.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				int center = getHeight() / 2;
				if (getComponentOrientation().isLeftToRight()) {
					int x = getWidth() - 27;
					arrow.drawLine(x, center - 6, x + 6, center);
					arrow.drawLine(x + 6, center, x, center + 6);
				} else {
					arrow.drawLine(27, center - 6, 21, center);
					arrow.drawLine(21, center, 27, center + 6);
				}
				arrow.dispose();
			}
		}
	}

	private static class InitialsIcon implements Icon {
		private final String initials;

		InitialsIcon(String name) {
			StringBuilder letters = new StringBuilder();
			for (String word : name.trim().split("\\s+")) {
				if (!word.isEmpty() && letters.length() < 2) {
					letters.append(word.charAt(0));
				}
			}
			initials = letters.toString().toUpperCase(Locale.getDefault());
		}

		@Override
		public int getIconWidth() {
			return 40;
		}

		@Override
		public int getIconHeight() {
			return 40;
		}

		@Override
		public void paintIcon(Component component, Graphics graphics, int x, int y) {
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(RetailPOSColors.surface200());
			g.fillOval(x, y, 40, 40);
			g.setColor(RetailPOSColors.inkMuted());
			g.setFont(RetailPOSTheme.MANROPE_BOLD.deriveFont(15f));
			FontMetrics metrics = g.getFontMetrics();
			g.drawString(initials, x + (40 - metrics.stringWidth(initials)) / 2,
					y + (40 + metrics.getAscent()) / 2 - 3);
			g.dispose();
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

		showView("login");
		showChoiceStep();

		// show welcome message
		printerStart();

		// keyboard listener activation
		inputtext = new StringBuffer();
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
		m_jSalesMode = new RoundedButton(true);
		m_jAdminMode = new RoundedButton(false);
		jPanel5 = new javax.swing.JPanel();
		jPanel2 = new javax.swing.JPanel();
		jPanel8 = new javax.swing.JPanel();
		m_jAbout = new JButton();
		m_jClose = new RoundedButton(false);

		setPreferredSize(new java.awt.Dimension(1024, 768));
		setLayout(new java.awt.BorderLayout());

		m_jPanelTitle.setBackground(RetailPOSColors.surface100());
		m_jPanelTitle.setBorder(javax.swing.BorderFactory.createCompoundBorder(
				javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, RetailPOSColors.border()),
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
						javax.swing.BorderFactory.createMatteBorder(0, 0, 0, 1, RetailPOSColors.border()),
						javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, HEADER_GAP))));
		m_jPanelBrand.add(m_jLblTitle, java.awt.BorderLayout.LINE_START);

		m_jLblSubTitle.setForeground(RetailPOSColors.inkMuted());
		m_jLblSubTitle.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, HEADER_GAP, 0, 0));
		m_jPanelBrand.add(m_jLblSubTitle, java.awt.BorderLayout.CENTER);

		m_jPanelTitle.add(m_jPanelBrand, java.awt.BorderLayout.LINE_START);

		m_jPanelStatus.setOpaque(false);
		m_jPanelStatus.setLayout(new javax.swing.BoxLayout(m_jPanelStatus, javax.swing.BoxLayout.Y_AXIS));

		m_jPanelStatus.add(javax.swing.Box.createVerticalGlue());

		m_jPanelStatus.add(m_jLblDemo);

		m_jLblClock.setForeground(RetailPOSColors.ink());
		m_jLblClock.setAlignmentX(java.awt.Component.RIGHT_ALIGNMENT);
		m_jPanelStatus.add(m_jLblClock);

		m_jLblOperator.setForeground(RetailPOSColors.inkMuted());
		m_jLblOperator.setAlignmentX(java.awt.Component.RIGHT_ALIGNMENT);
		m_jPanelStatus.add(m_jLblOperator);

		m_jPanelStatus.add(javax.swing.Box.createVerticalGlue());

		m_jPanelTitle.add(m_jPanelStatus, java.awt.BorderLayout.LINE_END);

		add(m_jPanelTitle, java.awt.BorderLayout.NORTH);

		m_jPanelContainer.setLayout(new java.awt.CardLayout());

		m_jPanelLogin.setLayout(new java.awt.BorderLayout());

		jPanel4.setLayout(new GridBagLayout());
		loginSteps = new JPanel(new CardLayout());
		loginSteps.setOpaque(false);
		choiceStep = new JPanel();
		choiceStep.setOpaque(false);
		choiceStep.setLayout(new BoxLayout(choiceStep, BoxLayout.Y_AXIS));
		choiceStep.setBorder(BorderFactory.createEmptyBorder(24, 8, 24, 8));
		tillLabel = new JLabel();
		tillLabel.setFont(RetailPOSTheme.MANROPE_BOLD.deriveFont(13f));
		tillLabel.setForeground(RetailPOSColors.inkMuted());
		tillLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		choiceStep.add(tillLabel);
		choiceStep.add(Box.createVerticalStrut(8));
		jLabel1.setText(AppLocal.getIntString("Label.ChooseMode"));
		jLabel1.setFont(RetailPOSTheme.MANROPE_BOLD.deriveFont(28f));
		jLabel1.setForeground(RetailPOSColors.ink());
		jLabel1.setAlignmentX(Component.CENTER_ALIGNMENT);
		choiceStep.add(jLabel1);
		choiceStep.add(Box.createVerticalStrut(24));

		// Size the shared mode column for the longer closed-session wording.
		m_jSalesMode.setText(salesModeText(false));
		m_jSalesMode.setIcon(new ModeIcon(true));
		((RoundedButton) m_jSalesMode).setChevron(true);
		m_jSalesMode.setToolTipText(AppLocal.getIntString("Label.SalesModeHint"));
		RetailPOSColors.primaryButton(m_jSalesMode);
		m_jSalesMode.setOpaque(false);
		m_jSalesMode.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(20f));
		m_jSalesMode.setHorizontalAlignment(SwingConstants.LEADING);
		m_jSalesMode.setIconTextGap(18);
		m_jSalesMode.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 52));
		m_jSalesMode.setOpaque(false);
		m_jSalesMode.setAlignmentX(0.5F);
		focusRing(m_jSalesMode);
		m_jSalesMode.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				openSalesView();
			}
		});
		m_jAdminMode.setText("<html><b>" + AppLocal.getIntString("Button.AdministrationMode") + "</b><br>"
				+ "<font size='4'>" + AppLocal.getIntString("Label.AdministrationModeHint") + "</font></html>");
		m_jAdminMode.setIcon(new ModeIcon(false));
		((RoundedButton) m_jAdminMode).setChevron(true);
		m_jAdminMode.setToolTipText(AppLocal.getIntString("Label.AdministrationModeHint"));
		styleSecondary(m_jAdminMode, false);
		m_jAdminMode.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(20f));
		m_jAdminMode.setHorizontalAlignment(SwingConstants.LEADING);
		m_jAdminMode.setIconTextGap(18);
		m_jAdminMode
				.setBorder(BorderFactory.createCompoundBorder(new RoundedBorder(RetailPOSColors.border(), 1, CARD_ARC),
						BorderFactory.createEmptyBorder(18, 24, 18, 52)));
		focusRing(m_jAdminMode);
		m_jAdminMode.setAlignmentX(0.5F);
		m_jAdminMode.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				showAdministratorStep();
			}
		});
		Dimension modeSize = m_jSalesMode.getPreferredSize();
		Dimension adminSize = m_jAdminMode.getPreferredSize();
		modeContentWidth = Math.max(modeSize.width, adminSize.width);
		sizeModeButtons();
		choiceStep.add(m_jSalesMode);
		choiceStep.add(Box.createVerticalStrut(12));
		choiceStep.add(m_jAdminMode);
		loginSteps.add(choiceStep, "choice");

		administratorStep = new JPanel();
		administratorStep.setLayout(new GridBagLayout());
		administratorStep.setOpaque(false);
		administratorStep.setBorder(BorderFactory.createEmptyBorder(24, 12, 24, 12));
		pickerColumn = new JPanel(new BorderLayout(0, 20));
		pickerColumn.setOpaque(false);
		pickerColumn.setAlignmentX(Component.CENTER_ALIGNMENT);
		pickerHeader = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
		pickerHeader.setOpaque(false);
		backButton = new RoundedButton(false);
		backButton.setText(AppLocal.getIntString("Button.BackToModes"));
		backButton.setIcon(new BackIcon());
		backButton.setIconTextGap(8);
		styleSecondary(backButton, false);
		focusRing(backButton);
		backButton.addActionListener(event -> showChoiceStep());
		pickerHeader.add(backButton);
		pickerHeader.add(Box.createHorizontalStrut(16));
		JPanel pickerCopy = new JPanel();
		pickerCopy.setOpaque(false);
		pickerCopy.setLayout(new BoxLayout(pickerCopy, BoxLayout.Y_AXIS));
		JLabel pickerTitle = new JLabel(AppLocal.getIntString("Label.ChooseAdministrator"));
		pickerTitle.setFont(RetailPOSTheme.MANROPE_BOLD.deriveFont(28f));
		pickerTitle.setForeground(RetailPOSColors.ink());
		pickerCopy.add(pickerTitle);
		JLabel pickerHint = new JLabel(AppLocal.getIntString("Label.AdministratorPickerHint"));
		pickerHint.setForeground(RetailPOSColors.inkMuted());
		pickerCopy.add(pickerHint);
		pickerHeader.add(pickerCopy);
		pickerColumn.add(pickerHeader, BorderLayout.NORTH);
		peopleGrid = new JPanel();
		peopleGrid.setOpaque(false);
		peopleGrid.setAlignmentX(Component.CENTER_ALIGNMENT);
		peopleGrid.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent event) {
				reflowPeople();
			}
		});
		pickerColumn.add(peopleGrid, BorderLayout.CENTER);
		GridBagConstraints pickerPosition = new GridBagConstraints();
		pickerPosition.weightx = 1;
		pickerPosition.weighty = 1;
		pickerPosition.anchor = GridBagConstraints.CENTER;
		administratorStep.add(pickerColumn, pickerPosition);
		loginSteps.add(administratorStep, "administrators");
		GridBagConstraints centered = new GridBagConstraints();
		centered.fill = GridBagConstraints.HORIZONTAL;
		centered.weightx = 1;
		centered.insets = new Insets(0, 32, 0, 32);
		jPanel4.add(loginSteps, centered);

		m_jPanelLogin.add(jPanel4, java.awt.BorderLayout.CENTER);
		m_jPanelLogin.setBackground(RetailPOSColors.surface0());
		jPanel4.setOpaque(false);

		jPanel5.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 24, 20, 24));
		jPanel5.setLayout(new java.awt.BorderLayout());

		jPanel2.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEADING, 0, 0));

		m_jAbout.setText(AppLocal.getIntString("Button.About")); // NOI18N
		m_jAbout.setForeground(RetailPOSColors.inkMuted());
		m_jAbout.setOpaque(false);
		m_jAbout.setContentAreaFilled(false);
		m_jAbout.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
		focusRing(m_jAbout);
		m_jAbout.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				m_jAboutActionPerformed(evt);
			}
		});
		jPanel2.add(m_jAbout);

		jPanel5.add(jPanel2, java.awt.BorderLayout.LINE_START);

		jPanel8.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.TRAILING, 0, 0));

		m_jClose.setText(AppLocal.getIntString("Button.CloseApplication"));
		styleSecondary(m_jClose, true);
		m_jClose.setIcon(new ExitIcon());
		m_jClose.setIconTextGap(8);
		focusRing(m_jClose);
		m_jClose.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				m_jCloseActionPerformed(evt);
			}
		});
		jPanel8.add(m_jClose);

		jPanel5.add(jPanel8, java.awt.BorderLayout.LINE_END);

		m_jPanelLogin.add(jPanel5, java.awt.BorderLayout.SOUTH);
		m_jPanelLogin.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent event) {
				sizeModeButtons();
				if (m_administrationLogin) {
					reflowPeople();
				}
			}
		});

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

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JLabel jLabel1;
	private javax.swing.JPanel jPanel2;
	private javax.swing.JPanel jPanel4;
	private javax.swing.JPanel jPanel5;
	private javax.swing.JPanel jPanel8;
	private javax.swing.JButton m_jAbout;
	private javax.swing.JButton m_jClose;
	private javax.swing.JLabel m_jLblClock;
	private javax.swing.JLabel m_jLblDemo;
	private javax.swing.JLabel m_jLblOperator;
	private javax.swing.JButton m_jAdminMode;
	private javax.swing.JButton m_jSalesMode;
	private javax.swing.JLabel m_jLblSubTitle;
	private javax.swing.JLabel m_jLblTitle;
	private javax.swing.JPanel m_jPanelBrand;
	private javax.swing.JPanel m_jPanelContainer;
	private javax.swing.JPanel m_jPanelLogin;
	private javax.swing.JPanel m_jPanelStatus;
	private javax.swing.JPanel m_jPanelTitle;
	// End of variables declaration//GEN-END:variables
}
