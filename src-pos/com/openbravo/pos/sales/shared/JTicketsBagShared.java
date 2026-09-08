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
import java.awt.event.KeyEvent;
import java.util.*;
import javax.swing.*;

import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.pos.sales.*;
import com.openbravo.pos.forms.*;

public class JTicketsBagShared extends JTicketsBag {

	private String m_sCurrentTicket = null;
	private DataLogicReceipts dlReceipts = null;
	private ButtonGroup m_sellerGroup = new ButtonGroup();
	private Map<String, JToggleButton> m_sellerButtons = new HashMap<String, JToggleButton>();
	private Map<String, UserInfo> m_sellers = new HashMap<String, UserInfo>();

	/** Creates new form JTicketsBagShared */
	public JTicketsBagShared(AppView app, TicketsEditor panelticket) {

		super(app, panelticket);

		dlReceipts = (DataLogicReceipts) app.getBean("com.openbravo.pos.sales.DataLogicReceipts");

		initComponents();
		initSellerButtons();
	}

	public void activate() {

		// precondicion es que no tenemos ticket activado ni ticket en el panel

		m_sCurrentTicket = null;
		selectValidTicket();

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

		return true;

		// postcondicion es que no tenemos ticket activado ni ticket en el panel
	}

	public void deleteTicket() {
		m_sCurrentTicket = null;
		selectValidTicket();
	}

	protected JComponent getBagComponent() {
		return this;
	}

	protected JComponent getNullComponent() {
		return new JPanel();
	}

	private boolean saveCurrentTicket() {

		// save current ticket, if exists,
		if (m_sCurrentTicket != null) {
			try {
				dlReceipts.insertSharedTicket(m_sCurrentTicket, m_panelticket.getActiveTicket());
			} catch (BasicException e) {
				new MessageInf(e).show(this);
				return false;
			}
		}
		return true;
	}

	private void setActiveTicket(String id) throws BasicException {

		// BEGIN TRANSACTION
		TicketInfo ticket = dlReceipts.getSharedTicket(id);
		if (ticket == null) {
			// Does not exists ???
			throw new BasicException(AppLocal.getIntString("message.noticket"));
		} else {
			dlReceipts.deleteSharedTicket(id);
			m_sCurrentTicket = id;
			resolveSeller(ticket);
			m_panelticket.setActiveTicket(ticket, null);
			syncSellerSelection();
		}
		// END TRANSACTION
	}

	private void selectValidTicket() {

		try {
			List<SharedTicketInfo> l = dlReceipts.getSharedTicketList();
			if (l.size() == 0) {
				newTicket();
			} else {
				setActiveTicket(l.get(0).getId());
			}
		} catch (BasicException e) {
			new MessageInf(e).show(this);
			newTicket();
		}
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
		m_panelticket.setActiveTicket(ticket, null);
		syncSellerSelection();
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
			String ticketId = findTicketForSeller(user.getId());
			if (ticketId == null) {
				createTicket(user.getUserInfo());
			} else {
				setActiveTicket(ticketId);
			}
		} catch (BasicException e) {
			new MessageInf(e).show(this);
			createTicket(user.getUserInfo());
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

	private String findTicketForSeller(String sellerId) throws BasicException {
		List<SharedTicketInfo> tickets = dlReceipts.getSharedTicketList();
		for (SharedTicketInfo ticketInfo : tickets) {
			TicketInfo ticket = dlReceipts.getSharedTicket(ticketInfo.getId());
			if (ticket != null && ticket.getUser() != null && sellerId.equals(ticket.getUser().getId())) {
				return ticketInfo.getId();
			}
		}
		return null;
	}

	private String getInitials(String name) {
		String[] nameParts = name.trim().split("\\s+");
		String initials = nameParts[0].substring(0, 1);
		if (nameParts.length > 1) {
			initials += nameParts[nameParts.length - 1].substring(0, 1);
		}
		return initials.toUpperCase(Locale.ROOT);
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

				JToggleButton button = new JToggleButton(
						shortcut == null ? getInitials(user.getName()) : getInitials(user.getName()) + " " + shortcut);
				button.setToolTipText(shortcut == null ? user.getName() : user.getName() + " (" + shortcut + ")");
				button.getAccessibleContext().setAccessibleName(user.getName());
				button.setFocusPainted(false);
				button.setFocusable(false);
				button.setMargin(new java.awt.Insets(8, 14, 8, 14));
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

		m_jNewTicket.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/editnew.png")));
		m_jNewTicket.setToolTipText(AppLocal.getIntString("tooltiptext.newticket")); // NOI18N
		m_jNewTicket.setFocusPainted(false);
		m_jNewTicket.setFocusable(false);
		m_jNewTicket.setMargin(new java.awt.Insets(8, 14, 8, 14));
		m_jNewTicket.setRequestFocusEnabled(false);
		m_jNewTicket.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				m_jNewTicketActionPerformed(evt);
			}
		});

		jPanel1.add(m_jNewTicket);

		m_jDelTicket.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/editdelete.png")));
		m_jDelTicket.setToolTipText(AppLocal.getIntString("tooltiptext.deleteticket")); // NOI18N
		m_jDelTicket.setFocusPainted(false);
		m_jDelTicket.setFocusable(false);
		m_jDelTicket.setMargin(new java.awt.Insets(8, 14, 8, 14));
		m_jDelTicket.setRequestFocusEnabled(false);
		m_jDelTicket.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				m_jDelTicketActionPerformed(evt);
			}
		});

		jPanel1.add(m_jDelTicket);

		m_jListTickets
				.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/unsortedList.png")));
		m_jListTickets.setToolTipText(AppLocal.getIntString("tooltiptext.listtickets")); // NOI18N
		m_jListTickets.setFocusPainted(false);
		m_jListTickets.setFocusable(false);
		m_jListTickets.setMargin(new java.awt.Insets(8, 14, 8, 14));
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
