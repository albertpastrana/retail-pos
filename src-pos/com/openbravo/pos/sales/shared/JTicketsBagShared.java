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

package com.openbravo.pos.sales.shared;

import com.openbravo.pos.ticket.TicketInfo;
import com.openbravo.pos.ticket.UserInfo;
import com.openbravo.pos.util.TillButtons;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.logging.Logger;
import javax.swing.*;

import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.pos.sales.*;
import com.openbravo.pos.forms.*;

public class JTicketsBagShared extends JTicketsBag {

	private static final String MESSAGE_NO_TICKET = "message.noticket";
	private static final Logger LOGGER = Logger.getLogger(JTicketsBagShared.class.getName());

	private String m_sCurrentTicket = null;
	private boolean currentTicketPersisted;
	private DataLogicReceipts dlReceipts = null;
	private ButtonGroup m_sellerGroup = new ButtonGroup();
	private Map<String, JToggleButton> m_sellerButtons = new HashMap<String, JToggleButton>();
	private Map<String, UserInfo> m_sellers = new HashMap<String, UserInfo>();
	private final String host;
	private final javax.swing.Timer ownershipTimer;

	/** Creates new form JTicketsBagShared */
	public JTicketsBagShared(AppView app, TicketsEditor panelticket) {

		super(app, panelticket);

		dlReceipts = (DataLogicReceipts) app.getBean("com.openbravo.pos.sales.DataLogicReceipts");
		host = app.getProperties().getHost();
		panelticket.setTicketChangeListener(this::saveActiveTicket);
		ownershipTimer = new javax.swing.Timer(2000, e -> refreshTicketOwnership());

		initComponents();
		initSellerButtons();
	}

	public void activate() {

		// precondicion es que no tenemos ticket activado ni ticket en el panel

		m_sCurrentTicket = null;
		createTicket(null);
		ownershipTimer.start();

		// Authorization
		m_jDelTicket.setEnabled(
				m_App.getAppUserView().getUser().hasPermission("com.openbravo.pos.sales.JPanelTicketEdits"));

		// postcondicion es que tenemos ticket activado aqui y ticket en el panel
	}

	public boolean deactivate() {

		// precondicion es que tenemos ticket activado aqui y ticket en el panel

		saveCurrentTicket();

		m_sCurrentTicket = null;
		m_panelticket.setActiveTicket(null, null);
		ownershipTimer.stop();

		return true;

		// postcondicion es que no tenemos ticket activado ni ticket en el panel
	}

	public void deleteTicket() {
		TicketInfo ticket = m_panelticket.getActiveTicket();
		if (currentTicketPersisted && m_sCurrentTicket != null && ticket != null && ticket.getLinesCount() > 0) {
			try {
				if (!dlReceipts.deleteSharedTicket(m_sCurrentTicket, host)) {
					showTicketTaken();
				}
			} catch (BasicException e) {
				new MessageInf(e).show(this);
				return;
			}
		}
		createTicket(null);
	}

	public boolean preparePayment() {
		try {
			if (dlReceipts.beginSharedTicketPayment(m_sCurrentTicket, host)) {
				return true;
			}
		} catch (BasicException e) {
			new MessageInf(e).show(this);
			return false;
		}
		showTicketTaken();
		return false;
	}

	public void cancelPayment() {
		try {
			dlReceipts.cancelSharedTicketPayment(m_sCurrentTicket, host);
		} catch (BasicException e) {
			new MessageInf(e).show(this);
		}
	}

	public void completePayment() throws BasicException {
		if (!dlReceipts.deleteSharedTicket(m_sCurrentTicket, host)) {
			throw new BasicException(AppLocal.getIntString(MESSAGE_NO_TICKET));
		}
		currentTicketPersisted = false;
	}

	protected JComponent getBagComponent() {
		return this;
	}

	protected JComponent getNullComponent() {
		return new JPanel();
	}

	private boolean saveCurrentTicket() {

		// save current ticket, if exists,
		TicketInfo ticket = m_panelticket.getActiveTicket();
		if (m_sCurrentTicket != null && ticket != null && ticket.getLinesCount() > 0) {
			try {
				if (!dlReceipts.parkSharedTicket(m_sCurrentTicket, ticket, host)) {
					LOGGER.warning("park rejected ticket=" + m_sCurrentTicket + " host=" + host);
					showTicketTaken();
					createTicket(null);
					return false;
				}
				currentTicketPersisted = true;
			} catch (BasicException e) {
				new MessageInf(e).show(this);
				return false;
			}
		}
		return true;
	}

	private void saveActiveTicket() {
		TicketInfo ticket = m_panelticket.getActiveTicket();
		if (m_sCurrentTicket == null || ticket == null) {
			return;
		}
		try {
			if (ticket.getLinesCount() == 0) {
				if (currentTicketPersisted && !dlReceipts.deleteSharedTicket(m_sCurrentTicket, host)) {
					showTicketTaken();
					createTicket(null);
				} else {
					currentTicketPersisted = false;
				}
			} else if (!dlReceipts.saveSharedTicket(m_sCurrentTicket, ticket, host)) {
				LOGGER.warning("save rejected ticket=" + m_sCurrentTicket + " host=" + host);
				showTicketTaken();
				createTicket(null);
			} else {
				currentTicketPersisted = true;
			}
		} catch (BasicException e) {
			new MessageInf(e).show(this);
		}
	}

	private void setActiveTicket(String id) throws BasicException {

		// BEGIN TRANSACTION
		TicketInfo ticket = dlReceipts.claimSharedTicket(id, host);
		if (ticket == null) {
			// Does not exists ???
			throw new BasicException(AppLocal.getIntString(MESSAGE_NO_TICKET));
		} else {
			m_sCurrentTicket = id;
			currentTicketPersisted = true;
			resolveSeller(ticket);
			m_panelticket.setActiveTicket(ticket, null);
			syncSellerSelection();
		}
		// END TRANSACTION
	}

	private void newTicket() {

		if (saveCurrentTicket()) {
			createTicket(null);
		}
	}

	private void createTicket(UserInfo seller) {
		TicketInfo ticket = new TicketInfo();
		if (seller != null) {
			ticket.setUser(seller);
		}
		m_sCurrentTicket = UUID.randomUUID().toString(); // m_fmtid.format(ticket.getId());
		currentTicketPersisted = false;
		m_panelticket.setActiveTicket(ticket, null);
		syncSellerSelection();
	}

	private void showTicketTaken() {
		new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString(MESSAGE_NO_TICKET)).show(this);
	}

	private void switchToSeller(AppUser user) {
		TicketInfo currentTicket = m_panelticket.getActiveTicket();
		if (currentTicket != null && currentTicket.getUser() != null
				&& user.getId().equals(currentTicket.getUser().getId())) {
			syncSellerSelection();
			return;
		}

		if (!saveCurrentTicket()) {
			syncSellerSelection();
			return;
		}

		try {
			String ticketId = dlReceipts.claimSharedTicketForSeller(user.getId(), host);
			if (ticketId == null) {
				createTicket(user.getUserInfo());
			} else {
				setActiveTicketFromClaim(ticketId, dlReceipts.getSharedTicket(ticketId));
			}
		} catch (BasicException e) {
			new MessageInf(e).show(this);
			createTicket(user.getUserInfo());
		}
	}

	private void setActiveTicketFromClaim(String id, TicketInfo ticket) {
		if (ticket == null) {
			LOGGER.warning("claimed ticket could not be read ticket=" + id + " host=" + host);
			try {
				dlReceipts.deleteSharedTicket(id, host);
			} catch (BasicException e) {
				LOGGER.warning("could not release unreadable ticket=" + id + " host=" + host);
			}
			createTicket(null);
			return;
		}
		m_sCurrentTicket = id;
		currentTicketPersisted = true;
		resolveSeller(ticket);
		m_panelticket.setActiveTicket(ticket, null);
		syncSellerSelection();
	}

	private void refreshTicketOwnership() {
		if (!currentTicketPersisted || m_sCurrentTicket == null) {
			return;
		}
		try {
			if (!dlReceipts.isSharedTicketOwned(m_sCurrentTicket, host)) {
				LOGGER.warning("ticket no longer owned ticket=" + m_sCurrentTicket + " host=" + host);
				currentTicketPersisted = false;
				createTicket(null);
			}
		} catch (BasicException e) {
			LOGGER.warning(
					"ownership check failed ticket=" + m_sCurrentTicket + " host=" + host + " error=" + e.getMessage());
		}
	}

	private void resolveSeller(TicketInfo ticket) {
		UserInfo seller = ticket.getUser();
		if (seller != null) {
			// The stored seller is a snapshot, so take the current name and discard
			// sellers that are no longer available.
			ticket.setUser(m_sellers.get(seller.getId()));
		}
	}

	private void initSellerButtons() {
		try {
			DataLogicSystem dlSystem = (DataLogicSystem) m_App.getBean("com.openbravo.pos.forms.DataLogicSystem");
			List people = dlSystem.listPeopleVisible();

			if (!people.isEmpty()) {
				jPanel1.add(Box.createHorizontalStrut(20));
			}

			int position = 0;
			for (Iterator i = people.iterator(); i.hasNext();) {
				final AppUser user = (AppUser) i.next();
				int shortcutKey = getSellerShortcut(position++);
				String shortcut = shortcutKey == 0 ? null : KeyEvent.getKeyText(shortcutKey);

				JToggleButton button = new JToggleButton(user.getIcon());
				TillButtons.labelUnderIcon(button, shortcut);
				button.setToolTipText(shortcut == null ? user.getName() : user.getName() + " (" + shortcut + ")");
				button.getAccessibleContext().setAccessibleName(user.getName());
				button.setFocusPainted(false);
				button.setFocusable(false);
				button.setRequestFocusEnabled(false);
				button.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						switchToSeller(user);
					}
				});

				if (shortcutKey != 0) {
					bindSellerShortcut(shortcutKey, user);
				}

				m_sellerGroup.add(button);
				m_sellerButtons.put(user.getId(), button);
				m_sellers.put(user.getId(), user.getUserInfo());
				jPanel1.add(button);
			}
		} catch (BasicException e) {
			new MessageInf(e).show(this);
		}
	}

	// Function keys do not reach the code field, which reads every printable
	// character as part of a barcode. F10 opens the window menu, so the keys stop
	// at
	// F9 and any further seller is mouse only.
	private static int getSellerShortcut(int position) {
		return position < 9 ? KeyEvent.VK_F1 + position : 0;
	}

	private void bindSellerShortcut(int shortcutKey, final AppUser user) {

		// The code field keeps the focus while selling, so the binding is on the
		// window.
		String actionKey = "seller." + user.getId();

		getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(shortcutKey, 0), actionKey);
		getActionMap().put(actionKey, new AbstractAction() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				switchToSeller(user);
			}
		});
	}

	private void syncSellerSelection() {
		m_sellerGroup.clearSelection();

		TicketInfo ticket = m_panelticket.getActiveTicket();
		if (ticket != null && ticket.getUser() != null) {
			JToggleButton button = m_sellerButtons.get(ticket.getUser().getId());
			if (button != null) {
				button.setSelected(true);
			}
		}
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	// <editor-fold defaultstate="collapsed" desc=" Generated Code
	// ">//GEN-BEGIN:initComponents
	private void initComponents() {
		jPanel1 = new javax.swing.JPanel();
		m_jNewTicket = new javax.swing.JButton();
		m_jDelTicket = new javax.swing.JButton();
		m_jListTickets = new javax.swing.JButton();

		setLayout(new java.awt.BorderLayout());

		m_jNewTicket.setIcon(TillButtons.icon("/com/openbravo/images/till/plus-circle.png"));
		m_jNewTicket.setToolTipText(AppLocal.getIntString("tooltiptext.newticket")); // NOI18N
		TillButtons.labelUnderIcon(m_jNewTicket, AppLocal.getIntString("buttonlabel.newticket")); // NOI18N
		m_jNewTicket.setFocusPainted(false);
		m_jNewTicket.setFocusable(false);
		m_jNewTicket.setRequestFocusEnabled(false);
		m_jNewTicket.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				m_jNewTicketActionPerformed(evt);
			}
		});

		jPanel1.add(m_jNewTicket);

		m_jDelTicket.setIcon(TillButtons.icon("/com/openbravo/images/till/trash.png"));
		m_jDelTicket.setToolTipText(AppLocal.getIntString("tooltiptext.deleteticket")); // NOI18N
		TillButtons.labelUnderIcon(m_jDelTicket, AppLocal.getIntString("buttonlabel.deleteticket")); // NOI18N
		m_jDelTicket.setFocusPainted(false);
		m_jDelTicket.setFocusable(false);
		m_jDelTicket.setRequestFocusEnabled(false);
		m_jDelTicket.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				m_jDelTicketActionPerformed(evt);
			}
		});

		jPanel1.add(m_jDelTicket);

		m_jListTickets.setIcon(TillButtons.icon("/com/openbravo/images/till/list-bullets.png"));
		m_jListTickets.setToolTipText(AppLocal.getIntString("tooltiptext.listtickets")); // NOI18N
		TillButtons.labelUnderIcon(m_jListTickets, AppLocal.getIntString("buttonlabel.listtickets")); // NOI18N
		m_jListTickets.setFocusPainted(false);
		m_jListTickets.setFocusable(false);
		m_jListTickets.setRequestFocusEnabled(false);
		m_jListTickets.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				m_jListTicketsActionPerformed(evt);
			}
		});

		jPanel1.add(m_jListTickets);

		add(jPanel1, java.awt.BorderLayout.WEST);

	}// </editor-fold>//GEN-END:initComponents

	private void m_jListTicketsActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_m_jListTicketsActionPerformed

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {

				try {
					List<SharedTicketInfo> l = dlReceipts.getSharedTicketList();

					JTicketsBagSharedList listDialog = JTicketsBagSharedList.newJDialog(JTicketsBagShared.this);
					String id = listDialog.showTicketsList(l);

					if (id != null) {
						if (saveCurrentTicket()) {
							setActiveTicket(id);
						}
					}
				} catch (BasicException e) {
					new MessageInf(e).show(JTicketsBagShared.this);
					newTicket();
				}
			}
		});

	}// GEN-LAST:event_m_jListTicketsActionPerformed

	private void m_jDelTicketActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_m_jDelTicketActionPerformed

		int res = JOptionPane.showConfirmDialog(this, AppLocal.getIntString("message.wannadelete"),
				AppLocal.getIntString("title.editor"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
		if (res == JOptionPane.YES_OPTION) {
			deleteTicket();
		}

	}// GEN-LAST:event_m_jDelTicketActionPerformed

	private void m_jNewTicketActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_m_jNewTicketActionPerformed

		newTicket();

	}// GEN-LAST:event_m_jNewTicketActionPerformed

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JPanel jPanel1;
	private javax.swing.JButton m_jDelTicket;
	private javax.swing.JButton m_jListTickets;
	private javax.swing.JButton m_jNewTicket;
	// End of variables declaration//GEN-END:variables

}
