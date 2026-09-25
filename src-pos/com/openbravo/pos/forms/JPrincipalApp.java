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

import com.openbravo.basic.BasicException;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import com.openbravo.beans.RoundedBorder;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.data.gui.JMessageDialog;
import com.openbravo.pos.util.Hashcypher;
import com.openbravo.pos.util.HiDpiIcon;
import com.openbravo.pos.theme.RetailPOSColors;

/**
 *
 * @author adrianromero
 */
public class JPrincipalApp extends javax.swing.JPanel implements AppUserView {

	private static Logger logger = Logger.getLogger("com.openbravo.pos.forms.JPrincipalApp");
	private static final String MENU_STATE_KEY = "ui.menu.state";
	private static final int MENU_RAIL_WIDTH = 72;

	private JRootApp m_appview;
	private AppUser m_appuser;

	private DataLogicSystem m_dlSystem;

	private JPanelView m_jLastView;
	private Action m_actionfirst;

	private Map<String, JPanelView> m_aPreparedViews; // Prepared views
	private Map<String, JPanelView> m_aCreatedViews;

	private Icon menu_open;
	private Icon menu_close;
	private JPanel m_jMenuRail;
	private JButton m_jRailToggle;
	private ScriptMenu m_scriptMenu;
	private Component m_jMenuFull;
	private boolean m_menuRail;
	private Map<String, Action> m_menuActions;
	private java.util.List<Action> m_menuRailActions;

	/** Creates new form JPrincipalApp */
	public JPrincipalApp(JRootApp appview, AppUser appuser) {

		m_appview = appview;
		m_appuser = appuser;

		m_dlSystem = m_appview.getBean(DataLogicSystem.class);

		// Cargamos los permisos del usuario
		m_appuser.fillPermissions(m_dlSystem);

		m_actionfirst = null;
		m_jLastView = null;
		m_aPreparedViews = new HashMap<String, JPanelView>();
		m_aCreatedViews = new HashMap<String, JPanelView>();
		m_menuActions = new LinkedHashMap<String, Action>();
		m_menuRailActions = new ArrayList<Action>();

		initComponents();

		applyComponentOrientation(appview.getComponentOrientation());

		if (jButton1.getComponentOrientation().isLeftToRight()) {
			menu_open = new HiDpiIcon(getClass().getResource("/com/openbravo/images/menu-right.png"), 1);
			menu_close = new HiDpiIcon(getClass().getResource("/com/openbravo/images/menu-left.png"), 1);
		} else {
			menu_open = new HiDpiIcon(getClass().getResource("/com/openbravo/images/menu-left.png"), 1);
			menu_close = new HiDpiIcon(getClass().getResource("/com/openbravo/images/menu-right.png"), 1);
		}
		assignMenuButtonIcon();

		// m_jPanelTitle.setUI(new GradientUI());
		m_jPanelTitle.setBorder(RoundedBorder.createGradientBorder());
		m_jPanelTitle.setVisible(false);

		// Anado el panel nulo
		m_jPanelContainer.add(new JPanel(), "<NULL>");
		showView("<NULL>");

		setMenuViews(buildMenu());
	}

	private Component buildMenu() {
		ScriptMenu menu = new ScriptMenu();
		m_scriptMenu = menu;
		ScriptGroup group = menu.addGroup("Menu.Home");
		group.addPanel("/com/openbravo/images/menu-catalog.png", "Menu.Home", "com.openbravo.pos.forms.JPanelWelcome",
				JPanelWelcome.class);

		group = menu.addGroup("Menu.Main");
		group.addPanel("/com/openbravo/images/menu-sales.png", "Menu.Ticket",
				"com.openbravo.pos.sales.JPanelTicketSales", com.openbravo.pos.sales.JPanelTicketSales.class);
		group.addPanel("/com/openbravo/images/menu-edit-sales.png", "Menu.TicketEdit",
				"com.openbravo.pos.sales.JPanelTicketEdits", com.openbravo.pos.sales.JPanelTicketEdits.class);
		group.addPanel("/com/openbravo/images/menu-package-plus.png", "Menu.Replenishment",
				"com.openbravo.pos.inventory.ReplenishmentPanel", com.openbravo.pos.inventory.ReplenishmentPanel.class);
		group.addPanel("/com/openbravo/images/menu-customers-payment.png", "Menu.CustomersPayment",
				"com.openbravo.pos.customers.CustomersPanel", com.openbravo.pos.customers.CustomersPanel.class);
		group.addPanel("/com/openbravo/images/menu-payments.png", "Menu.Payments",
				"com.openbravo.pos.panels.JPanelPayments", com.openbravo.pos.panels.JPanelPayments.class);
		group.addPanel("/com/openbravo/images/menu-close-cash.png", "Menu.CloseTPV",
				"com.openbravo.pos.panels.JPanelCloseMoney", com.openbravo.pos.panels.JPanelCloseMoney.class);
		group.addPanel("/com/openbravo/images/menu-cash-closed.png", "Menu.Closing",
				"com.openbravo.pos.panels.JPanelClosedCash", com.openbravo.pos.panels.JPanelClosedCash.class);

		group = menu.addGroup("Menu.Backoffice");
		ScriptSubmenu submenu = group.addSubmenu("/com/openbravo/images/menu-stock.png", "Menu.StockManagement",
				"com.openbravo.pos.forms.MenuStockManagement");
		submenu.addTitle("Menu.StockManagement.Edit");
		submenu.addPanel("/com/openbravo/images/menu-products.png", "Menu.Products",
				"com.openbravo.pos.inventory.ProductsPanel", com.openbravo.pos.inventory.ProductsPanel.class);
		submenu.addPanel("/com/openbravo/images/menu-products.png", "Menu.PriceRules",
				"com.openbravo.pos.inventory.PriceRulesPanel", com.openbravo.pos.inventory.PriceRulesPanel.class);
		submenu.addPanel("/com/openbravo/images/menu-products.png", "Menu.SaleMark",
				"com.openbravo.pos.inventory.SaleMarkPanel", com.openbravo.pos.inventory.SaleMarkPanel.class);
		submenu.addPanel("/com/openbravo/images/menu-categories.png", "Menu.Categories",
				"com.openbravo.pos.inventory.CategoriesPanel", com.openbravo.pos.inventory.CategoriesPanel.class);
		submenu.addPanel("/com/openbravo/images/menu-taxes.png", "Menu.Taxes", "com.openbravo.pos.inventory.TaxPanel",
				com.openbravo.pos.inventory.TaxPanel.class);
		submenu.addPanel("/com/openbravo/images/menu-stock-diary.png", "Menu.StockDiary",
				"com.openbravo.pos.inventory.StockDiaryPanel", com.openbravo.pos.inventory.StockDiaryPanel.class);

		submenu = group.addSubmenu("/com/openbravo/images/menu-sales-reports.png", "Menu.SalesManagement",
				"com.openbravo.pos.forms.MenuSalesManagement");
		submenu.addTitle("Menu.SalesManagement.Reports");
		submenu.addPanel("/com/openbravo/images/menu-sales-reports.png", "Menu.SalesSummary",
				"com.openbravo.pos.reports.JPanelSalesSummary", com.openbravo.pos.reports.JPanelSalesSummary.class);
		submenu.addPanel("/com/openbravo/images/menu-product-sales.png", "Menu.ProductSalesSummary",
				"com.openbravo.pos.reports.JPanelProductSales", com.openbravo.pos.reports.JPanelProductSales.class);
		submenu.addPanel("/com/openbravo/images/menu-sales-reports.png", "Menu.PaymentSalesSummary",
				"com.openbravo.pos.reports.JPanelPaymentSales", com.openbravo.pos.reports.JPanelPaymentSales.class);
		submenu.addPanel("/com/openbravo/images/menu-inventory-current.png", "Menu.LowStockSummary",
				"com.openbravo.pos.reports.JPanelLowStock", com.openbravo.pos.reports.JPanelLowStock.class);
		submenu.addPanel("/com/openbravo/images/menu-taxes-report.png", "Menu.TaxSummary",
				"com.openbravo.pos.reports.JPanelTaxSummary", com.openbravo.pos.reports.JPanelTaxSummary.class);
		submenu.addPanel("/com/openbravo/images/menu-cash-closed.png", "Menu.CashClosingSummary",
				"com.openbravo.pos.reports.JPanelCashClosing", com.openbravo.pos.reports.JPanelCashClosing.class);
		submenu.addPanel("/com/openbravo/images/menu-customers-report.png", "Menu.CustomerDebtSummary",
				"com.openbravo.pos.reports.JPanelCustomerDebt", com.openbravo.pos.reports.JPanelCustomerDebt.class);

		submenu = group.addSubmenu("/com/openbravo/images/menu-maintenance.png", "Menu.Maintenance",
				"com.openbravo.pos.forms.MenuMaintenance");
		submenu.addTitle("Menu.Maintenance.POS");
		submenu.addPanel("/com/openbravo/images/menu-users.png", "Menu.Users", "com.openbravo.pos.admin.PeoplePanel",
				com.openbravo.pos.admin.PeoplePanel.class);
		submenu.addPanel("/com/openbravo/images/menu-roles.png", "Menu.Roles", "com.openbravo.pos.admin.RolesPanel",
				com.openbravo.pos.admin.RolesPanel.class);
		submenu.addPanel("/com/openbravo/images/menu-resources.png", "Menu.Resources",
				"com.openbravo.pos.admin.ResourcesPanel", com.openbravo.pos.admin.ResourcesPanel.class);
		submenu.addExecution("/com/openbravo/images/ark2.png", "Menu.DatabaseBackup",
				com.openbravo.pos.admin.BackupDatabaseAction.class);
		submenu.addExecution("/com/openbravo/images/ark2.png", "Menu.DemoMode",
				com.openbravo.pos.admin.DemoModeAction.class);

		group = menu.addGroup("Menu.System");
		group.addChangePasswordAction();
		group.addPanel("/com/openbravo/images/menu-configuration.png", "Menu.Configuration",
				"com.openbravo.pos.config.JPanelConfiguration", com.openbravo.pos.config.JPanelConfiguration.class);
		group.addExitAction();

		m_jMenuRail = menu.getRailMenu();
		menu.addToggleButton(jButton1);
		return menu.getTaskPane();
	}

	private Component getScriptMenu(String menutext) {
		return buildMenu();
		/*
		 * // Older databases may still contain a Menu.Root resource without this //
		 * frequently used till action. Keep the menu available while they migrate.
		 * String editSales =
		 * "group.addPanel(\"/com/openbravo/images/menu-edit-sales.png\", \"Menu.TicketEdit\", \"com.openbravo.pos.sales.JPanelTicketEdits\");"
		 * ; if (!menutext.contains(editSales)) { String sales =
		 * "group.addPanel(\"/com/openbravo/images/menu-sales.png\", \"Menu.Ticket\", \"com.openbravo.pos.sales.JPanelTicketSales\");"
		 * ; menutext = menutext.replace(sales, sales + " " + editSales); } String
		 * replenishment =
		 * "group.addPanel(\"/com/openbravo/images/menu-package-plus.png\", \"Menu.Replenishment\", \"com.openbravo.pos.inventory.ReplenishmentPanel\");"
		 * ; menutext = menutext.replace(
		 * "submenu.addPanel(\"/com/openbravo/images/menu-stock.png\", \"Menu.Replenishment\", \"com.openbravo.pos.inventory.ReplenishmentPanel\");"
		 * , ""); menutext = menutext.replace(replenishment, ""); menutext =
		 * menutext.replace(
		 * "group.addPanel(\"/com/openbravo/images/menu-stock.png\", \"Menu.Replenishment\", \"com.openbravo.pos.inventory.ReplenishmentPanel\");"
		 * , ""); menutext = menutext.replace(editSales, editSales + " " +
		 * replenishment); String demoAction =
		 * "submenu.addExecution(\"/com/openbravo/images/ark2.png\", \"Menu.DemoMode\", \"com.openbravo.pos.admin.DemoModeAction\");"
		 * ; if (!menutext.contains("com.openbravo.pos.admin.DemoModeAction")) {
		 * menutext = menutext.replace(
		 * "submenu.addExecution(\"/com/openbravo/images/ark2.png\", \"Menu.DatabaseBackup\", \"com.openbravo.pos.admin.BackupDatabaseAction\");"
		 * ,
		 * "submenu.addExecution(\"/com.openbravo/images/ark2.png\", \"Menu.DatabaseBackup\", \"com.openbravo.pos.admin.BackupDatabaseAction\"); "
		 * + demoAction); }
		 *
		 */
	}

	private void setMenuViews(Component fullMenu) {
		m_jMenuFull = fullMenu;
		m_jPanelLeft.setBackground(RetailPOSColors.surface0());
		m_jPanelLeft.setBorder(
				javax.swing.BorderFactory.createMatteBorder(0, getComponentOrientation().isLeftToRight() ? 0 : 1, 0,
						getComponentOrientation().isLeftToRight() ? 1 : 0, RetailPOSColors.border()));
		m_jPanelLeft.getViewport().setBackground(RetailPOSColors.surface0());
		m_jPanelLeft.setViewportView(fullMenu);
		setMenuRail(isRailPreference(), false);
	}

	private boolean isRailPreference() {
		String state = m_appview.getProperties().getProperty(MENU_STATE_KEY);
		return state == null ? getBounds().width <= 800 : "rail".equals(state);
	}

	private void assignMenuButtonIcon() {
		jButton1.setIcon(m_menuRail ? menu_open : menu_close);
		if (m_jRailToggle != null) {
			m_jRailToggle.setIcon(m_menuRail ? menu_open : menu_close);
		}
	}

	public class ScriptMenu {
		private MenuSidebar taskPane;

		private ScriptMenu() {
			taskPane = new MenuSidebar();
			taskPane.applyComponentOrientation(getComponentOrientation());
		}

		public ScriptGroup addGroup(String key) {

			ScriptGroup group = new ScriptGroup(key);
			taskPane.add(group.getTaskGroup());
			return group;
		}

		// public JTaskPane getTaskPane() {
		public MenuSidebar getTaskPane() {
			return taskPane;
		}

		private JPanel getRailMenu() {
			JPanel rail = new JPanel();
			rail.setLayout(new BoxLayout(rail, BoxLayout.Y_AXIS));
			rail.setOpaque(true);
			rail.setBackground(RetailPOSColors.surface0());
			rail.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));
			rail.applyComponentOrientation(getComponentOrientation());
			m_jRailToggle = new JButton();
			m_jRailToggle.setIcon(menu_close);
			m_jRailToggle.setPreferredSize(new Dimension(56, 56));
			m_jRailToggle.setMinimumSize(new Dimension(56, 56));
			m_jRailToggle.setMaximumSize(new Dimension(56, 56));
			m_jRailToggle.setAlignmentX(Component.CENTER_ALIGNMENT);
			m_jRailToggle.setFocusPainted(false);
			m_jRailToggle.setFocusable(false);
			m_jRailToggle.setRequestFocusEnabled(false);
			m_jRailToggle.setOpaque(true);
			m_jRailToggle.setBackground(RetailPOSColors.surface0());
			m_jRailToggle.putClientProperty("FlatLaf.style",
					"background: " + RetailPOSColors.toHex(RetailPOSColors.surface0()) + "; hoverBackground: "
							+ RetailPOSColors.toHex(RetailPOSColors.surface200()) + "; pressedBackground: "
							+ RetailPOSColors.toHex(RetailPOSColors.surface200()));
			m_jRailToggle.addActionListener(e -> setMenuRail(false, true));
			rail.add(m_jRailToggle);
			rail.add(Box.createVerticalStrut(4));
			for (Action action : m_menuRailActions) {
				JButton button = new JButton(action);
				button.setText(null);
				button.setIcon((Icon) action.getValue(Action.SMALL_ICON));
				button.setToolTipText((String) action.getValue(Action.NAME));
				button.setAlignmentX(Component.CENTER_ALIGNMENT);
				button.setFocusPainted(false);
				button.setFocusable(false);
				button.setRequestFocusEnabled(false);
				button.putClientProperty("menu.task", action.getValue(AppUserView.ACTION_TASKNAME));
				button.setOpaque(true);
				button.setBackground(RetailPOSColors.surface0());
				button.setForeground(RetailPOSColors.ink());
				button.putClientProperty("FlatLaf.style",
						"hoverBackground: " + RetailPOSColors.toHex(RetailPOSColors.surface200()));
				button.setPreferredSize(new Dimension(56, 56));
				button.setMinimumSize(new Dimension(56, 56));
				button.setMaximumSize(new Dimension(56, 56));
				rail.add(button);
				rail.add(Box.createVerticalStrut(4));
			}
			return rail;
		}

		private void addToggleButton(JButton toggle) {
			taskPane.addToggleButton(toggle, e -> setMenuRail(true, true));
		}

		private void setSelectedTask(String taskName) {
			taskPane.setSelectedTask(taskName);
			for (Component component : m_jMenuRail.getComponents()) {
				if (component instanceof JButton) {
					JButton button = (JButton) component;
					boolean selected = taskName != null && taskName.equals(button.getClientProperty("menu.task"));
					button.setBackground(selected ? RetailPOSColors.brandSubtle() : RetailPOSColors.surface0());
					button.putClientProperty("FlatLaf.style",
							"background: "
									+ RetailPOSColors.toHex(
											selected ? RetailPOSColors.brandSubtle() : RetailPOSColors.surface0())
									+ "; hoverBackground: " + RetailPOSColors.toHex(RetailPOSColors.surface200()));
				}
			}
		}
	}

	public class ScriptGroup {
		private MenuSidebarGroup taskGroup;

		private ScriptGroup(String key) {
			taskGroup = new MenuSidebarGroup();
			taskGroup.applyComponentOrientation(getComponentOrientation());
			taskGroup.setFocusable(false);
			taskGroup.setRequestFocusEnabled(false);
			taskGroup.setTitle(AppLocal.getIntString(key));
			taskGroup.setVisible(false); // Only groups with sons are visible.
		}

		public void addPanel(String icon, String key, String classname, Class<? extends JPanelView> viewClass) {
			addAction(new MenuPanelAction(m_appview, icon, key, classname, viewClass));
		}

		public void addExecution(String icon, String key, Class<? extends ProcessAction> actionClass) {
			addAction(new MenuExecAction(m_appview, icon, key, actionClass));
		}

		public ScriptSubmenu addSubmenu(String icon, String key, String classname) {
			ScriptSubmenu submenu = new ScriptSubmenu(key);
			m_aPreparedViews.put(classname, new JPanelMenu(submenu.getMenuDefinition()));
			addAction(new MenuPanelAction(m_appview, icon, key, classname, null));
			return submenu;
		}

		public void addChangePasswordAction() {
			addAction(
					new ChangePasswordAction("/com/openbravo/images/menu-change-password.png", "Menu.ChangePassword"));
		}

		public void addExitAction() {
			addAction(new ExitAction("/com/openbravo/images/menu-exit.png", "Menu.Exit"));
		}

		private void addAction(Action act) {

			if (m_appuser.hasPermission((String) act.getValue(AppUserView.ACTION_TASKNAME))) {
				// add the action
				Component c = taskGroup.add(act);
				m_actionfirst = m_actionfirst == null ? act : m_actionfirst;
				// Keep the same actions available in the compact icon rail.
				m_menuActions.put((String) act.getValue(AppUserView.ACTION_TASKNAME), act);
				m_menuRailActions.add(act);
				c.applyComponentOrientation(getComponentOrientation());
				c.setFocusable(false);
				// c.setRequestFocusEnabled(false);

				taskGroup.setVisible(true);

			}
		}

		public MenuSidebarGroup getTaskGroup() {
			return taskGroup;
		}
	}

	public class ScriptSubmenu {
		private MenuDefinition menudef;

		private ScriptSubmenu(String key) {
			menudef = new MenuDefinition(key);
		}

		public void addTitle(String key) {
			menudef.addMenuTitle(key);
		}

		public void addPanel(String icon, String key, String classname, Class<? extends JPanelView> viewClass) {
			addAction(new MenuPanelAction(m_appview, icon, key, classname, viewClass));
		}

		public void addExecution(String icon, String key, Class<? extends ProcessAction> actionClass) {
			addAction(new MenuExecAction(m_appview, icon, key, actionClass));
		}

		public ScriptSubmenu addSubmenu(String icon, String key, String classname) {
			ScriptSubmenu submenu = new ScriptSubmenu(key);
			m_aPreparedViews.put(classname, new JPanelMenu(submenu.getMenuDefinition()));
			addAction(new MenuPanelAction(m_appview, icon, key, classname, null));
			return submenu;
		}

		public void addChangePasswordAction() {
			addAction(
					new ChangePasswordAction("/com/openbravo/images/menu-change-password.png", "Menu.ChangePassword"));
		}

		public void addExitAction() {
			addAction(new ExitAction("/com/openbravo/images/menu-exit.png", "Menu.Exit"));
		}

		private void addAction(Action action) {
			menudef.addMenuItem(action);
			m_menuActions.put((String) action.getValue(AppUserView.ACTION_TASKNAME), action);
		}

		public MenuDefinition getMenuDefinition() {
			return menudef;
		}
	}

	private void setMenuRail(boolean value, boolean persist) {
		m_menuRail = value;
		m_jPanelLeft.setVisible(true);
		m_jPanelLeft.setPreferredSize(value ? new Dimension(MENU_RAIL_WIDTH, 0) : null);
		m_jPanelLeft.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		m_jPanelLeft.setViewportView(value ? m_jMenuRail : m_jMenuFull);
		assignMenuButtonIcon();
		if (persist && m_appview.getProperties() instanceof AppConfig) {
			AppConfig config = (AppConfig) m_appview.getProperties();
			config.setProperty(MENU_STATE_KEY, value ? "rail" : "full");
			try {
				ConfigurationStore.save(config);
			} catch (IOException e) {
				logger.log(Level.WARNING, "Cannot save the menu state", e);
			}
		}
		revalidate();
	}

	public void activate() {
		// arranco la primera opcion
		if (m_actionfirst != null) {
			m_actionfirst.actionPerformed(null);
			m_actionfirst = null;
		}
	}

	public boolean deactivate() {
		if (m_jLastView == null) {
			return true;
		} else if (m_jLastView.deactivate()) {
			m_jLastView = null;
			showView("<NULL>");
			return true;
		} else {
			return false;
		}

	}

	private class ExitAction extends AbstractAction {

		public ExitAction(String icon, String keytext) {
			putValue(Action.SMALL_ICON, new HiDpiIcon(JPrincipalApp.class.getResource(icon)));
			putValue(Action.NAME, AppLocal.getIntString(keytext));
			putValue(AppUserView.ACTION_TASKNAME, keytext);
		}

		public void actionPerformed(ActionEvent evt) {
			m_appview.closeAppView();
		}
	}

	// La accion de cambio de password..
	private class ChangePasswordAction extends AbstractAction {
		public ChangePasswordAction(String icon, String keytext) {
			putValue(Action.SMALL_ICON, new HiDpiIcon(JPrincipalApp.class.getResource(icon)));
			putValue(Action.NAME, AppLocal.getIntString(keytext));
			putValue(AppUserView.ACTION_TASKNAME, keytext);

		}

		public void actionPerformed(ActionEvent evt) {

			String sNewPassword = Hashcypher.changePassword(JPrincipalApp.this, m_appuser.getPassword());
			if (sNewPassword != null) {
				try {

					m_dlSystem.execChangePassword(new Object[]{sNewPassword, m_appuser.getId()});
					m_appuser.setPassword(sNewPassword);
				} catch (BasicException e) {
					JMessageDialog.showMessage(JPrincipalApp.this, new MessageInf(MessageInf.SGN_WARNING,
							AppLocal.getIntString("message.cannotchangepassword")));
				}
			}
		}
	}

	private void showView(String sView) {
		CardLayout cl = (CardLayout) (m_jPanelContainer.getLayout());
		cl.show(m_jPanelContainer, sView);
	}

	public AppUser getUser() {
		return m_appuser;
	}

	@Override
	public Action getTaskAction(String sTaskClass) {
		return m_appuser.hasPermission(sTaskClass) ? m_menuActions.get(sTaskClass) : null;
	}

	public void showTask(String sTaskClass, Class<? extends JPanelView> viewClass) {

		logger.info("event=task_open_start task=" + sTaskClass + " userId=" + m_appuser.getId() + " role="
				+ m_appuser.getRole());
		m_appview.waitCursorBegin();

		if (m_appuser.hasPermission(sTaskClass)) {

			JPanelView m_jMyView = (JPanelView) m_aCreatedViews.get(sTaskClass);

			// cierro la antigua
			if (m_jLastView == null || (m_jMyView != m_jLastView && m_jLastView.deactivate())) {

				// Construct the new view
				if (m_jMyView == null) {

					// Is the view prepared
					m_jMyView = m_aPreparedViews.get(sTaskClass);
					if (m_jMyView == null) {
						// The view is not prepared. Try to get as a Bean...
						try {
							m_jMyView = m_appview.getBean(viewClass);
						} catch (BeanFactoryException e) {
							m_jMyView = new JPanelNull(m_appview, e);
						}
					}

					m_jMyView.getComponent().applyComponentOrientation(getComponentOrientation());
					m_jPanelContainer.add(m_jMyView.getComponent(), sTaskClass);
					m_aCreatedViews.put(sTaskClass, m_jMyView);
				}

				// ejecuto la tarea
				try {
					m_jMyView.activate();
				} catch (BasicException e) {
					logger.log(Level.WARNING, "event=task_activate_failed task=" + sTaskClass, e);
					JMessageDialog.showMessage(this,
							new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.notactive"), e));
				}

				// se tiene que mostrar el panel
				m_jLastView = m_jMyView;

				showView(sTaskClass);
				// Y ahora que he cerrado la antigua me abro yo
				String sTitle = m_jMyView.getTitle();
				m_jPanelTitle.setVisible(sTitle != null);
				m_jTitle.setText(sTitle);
				m_scriptMenu.setSelectedTask(sTaskClass);
				logger.info("event=task_open_success task=" + sTaskClass + " userId=" + m_appuser.getId());
			}
		} else {
			logger.warning("event=task_open_denied task=" + sTaskClass + " userId=" + m_appuser.getId() + " role="
					+ m_appuser.getRole());
			// No hay permisos para ejecutar la accion...
			JMessageDialog.showMessage(this,
					new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.notpermissions")));
		}
		m_appview.waitCursorEnd();
	}

	public void executeTask(Class<? extends ProcessAction> actionClass) {
		executeProcess(actionClass.getName(), () -> m_appview.getBean(actionClass));
	}

	private void executeProcess(String taskName, ProcessSupplier supplier) {

		logger.info("event=task_execute_start task=" + taskName + " userId=" + m_appuser.getId() + " role="
				+ m_appuser.getRole());
		m_appview.waitCursorBegin();

		if (m_appuser.hasPermission(taskName)) {
			try {
				ProcessAction myProcess = supplier.get();

				// execute the proces
				try {
					MessageInf m = myProcess.execute();
					if (m != null) {
						// si devuelve un mensaje, lo muestro
						JMessageDialog.showMessage(JPrincipalApp.this, m);
					}
				} catch (BasicException eb) {
					logger.log(Level.WARNING, "event=task_execute_failed task=" + taskName, eb);
					// Si se produce un error lo muestro.
					JMessageDialog.showMessage(JPrincipalApp.this, new MessageInf(eb));
				}
			} catch (BeanFactoryException e) {
				logger.log(Level.WARNING, "event=task_execute_load_failed task=" + taskName, e);
				JMessageDialog.showMessage(JPrincipalApp.this,
						new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("Label.LoadError"), e));
			}
		} else {
			logger.warning("event=task_execute_denied task=" + taskName + " userId=" + m_appuser.getId() + " role="
					+ m_appuser.getRole());
			// No hay permisos para ejecutar la accion...
			JMessageDialog.showMessage(JPrincipalApp.this,
					new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.notpermissions")));
		}
		m_appview.waitCursorEnd();
	}

	@FunctionalInterface
	private interface ProcessSupplier {
		ProcessAction get() throws BeanFactoryException;
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	// <editor-fold defaultstate="collapsed" desc="Generated
	// Code">//GEN-BEGIN:initComponents
	private void initComponents() {

		jPanel1 = new javax.swing.JPanel();
		jButton1 = new javax.swing.JButton();
		m_jPanelLeft = new javax.swing.JScrollPane();
		m_jPanelRight = new javax.swing.JPanel();
		m_jPanelTitle = new javax.swing.JPanel();
		m_jTitle = new javax.swing.JLabel();
		m_jPanelContainer = new javax.swing.JPanel();

		setLayout(new java.awt.BorderLayout());

		jPanel1.setLayout(new java.awt.BorderLayout());

		jButton1.setFocusPainted(false);
		jButton1.setFocusable(false);
		jButton1.setMargin(new java.awt.Insets(14, 2, 14, 2));
		jButton1.setRequestFocusEnabled(false);
		jPanel1.add(m_jPanelLeft, java.awt.BorderLayout.CENTER);

		add(jPanel1, java.awt.BorderLayout.LINE_START);

		m_jPanelRight.setLayout(new java.awt.BorderLayout());
		m_jPanelRight.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 16, 0, 0));

		m_jPanelTitle.setLayout(new java.awt.BorderLayout());

		m_jTitle.setFont(new java.awt.Font("SansSerif", 1, 18)); // NOI18N
		m_jTitle.setBorder(javax.swing.BorderFactory.createCompoundBorder(
				javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, java.awt.Color.darkGray),
				javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10)));
		m_jPanelTitle.add(m_jTitle, java.awt.BorderLayout.NORTH);

		m_jPanelRight.add(m_jPanelTitle, java.awt.BorderLayout.NORTH);

		m_jPanelContainer.setLayout(new java.awt.CardLayout());
		m_jPanelRight.add(m_jPanelContainer, java.awt.BorderLayout.CENTER);

		add(m_jPanelRight, java.awt.BorderLayout.CENTER);
	}// </editor-fold>//GEN-END:initComponents

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JButton jButton1;
	private javax.swing.JPanel jPanel1;
	private javax.swing.JPanel m_jPanelContainer;
	private javax.swing.JScrollPane m_jPanelLeft;
	private javax.swing.JPanel m_jPanelRight;
	private javax.swing.JPanel m_jPanelTitle;
	private javax.swing.JLabel m_jTitle;
	// End of variables declaration//GEN-END:variables

}
