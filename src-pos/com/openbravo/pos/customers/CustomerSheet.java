package com.openbravo.pos.customers;

import com.openbravo.basic.BasicException;
import com.openbravo.pos.forms.AppView;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.DataLogicSales;
import com.openbravo.pos.forms.SupervisorAuthorization;
import com.openbravo.pos.payment.JPaymentSelect;
import com.openbravo.pos.payment.JPaymentSelectCustomer;
import com.openbravo.pos.payment.PaymentInfo;
import com.openbravo.pos.payment.PaymentInfoTicket;
import com.openbravo.pos.ticket.TicketInfo;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Window;
import java.util.Date;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class CustomerSheet extends JDialog {

	public static final int CLOSED = 0;
	public static final int CHANGE = 1;
	public static final int REMOVE = 2;

	private int result = CLOSED;
	private final AppView app;
	private final DataLogicSales dlSales;
	private CustomerInfoExt customer;
	private final JLabel debt = new JLabel();

	private CustomerSheet(Window parent, AppView app, CustomerInfoExt customer) {
		super(parent, ModalityType.APPLICATION_MODAL);
		this.app = app;
		this.dlSales = (DataLogicSales) app.getBean("com.openbravo.pos.forms.DataLogicSales");
		this.customer = customer;
		setTitle(AppLocal.getIntString("customer.title"));
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setLayout(new BorderLayout(10, 10));

		JPanel details = new JPanel(new GridLayout(0, 2, 8, 8));
		details.add(new JLabel(AppLocal.getIntString("customer.name")));
		details.add(new JLabel(customer.getName()));
		details.add(new JLabel(AppLocal.getIntString("customer.phone")));
		details.add(new JLabel(customer.getPhone() == null ? "" : customer.getPhone()));
		details.add(new JLabel(AppLocal.getIntString("customer.debt")));
		debt.setText(formatDebt(customer));
		details.add(debt);
		details.setBorder(javax.swing.BorderFactory.createEmptyBorder(14, 14, 4, 14));
		add(details, BorderLayout.CENTER);

		JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton pay = new JButton(AppLocal.getIntString("customer.paydebt"));
		JButton change = new JButton(AppLocal.getIntString("customer.change"));
		JButton remove = new JButton(AppLocal.getIntString("customer.remove"));
		JButton close = new JButton(AppLocal.getIntString("customer.close"));
		actions.add(pay);
		actions.add(change);
		actions.add(remove);
		actions.add(close);
		add(actions, BorderLayout.SOUTH);

		pay.setEnabled(customer.getCurdebt() != null && customer.getCurdebt().doubleValue() > 0.0);
		pay.addActionListener(e -> payDebt());
		change.addActionListener(e -> {
			result = CHANGE;
			dispose();
		});
		remove.addActionListener(e -> {
			result = REMOVE;
			dispose();
		});
		close.addActionListener(e -> dispose());
		pack();
		setSize(390, 230);
	}

	public static int showDialog(Component parent, AppView app, CustomerInfoExt customer) {
		Window window = getWindow(parent);
		CustomerSheet sheet = new CustomerSheet(window, app, customer);
		sheet.setLocationRelativeTo(parent);
		sheet.setVisible(true);
		return sheet.result;
	}

	private void payDebt() {
		if (!SupervisorAuthorization.authorize(this, app, AppLocal.getIntString("message.authorizeddebt"))) {
			return;
		}
		JPaymentSelect payment = JPaymentSelectCustomer.getDialog(this);
		payment.init(app);
		if (!payment.showDialog(customer.getCurdebt(), customer)) {
			return;
		}
		List<PaymentInfo> payments = payment.getSelectedPayments();
		double total = 0.0;
		for (PaymentInfo item : payments) {
			total += item.getTotal();
		}
		payments.add(new PaymentInfoTicket(-total, "debtpaid"));
		TicketInfo ticket = new TicketInfo();
		ticket.setTicketType(TicketInfo.RECEIPT_PAYMENT);
		ticket.setPayments(payments);
		ticket.setUserIfAbsent(app.getAppUserView().getUser().getTicketUserInfo());
		ticket.setActiveCash(app.getActiveCashIndex());
		ticket.setDate(new Date());
		ticket.setCustomer(customer);
		try {
			dlSales.saveTicket(ticket, app.getInventoryLocation());
			customer = dlSales.loadCustomerExt(customer.getId());
			debt.setText(formatDebt(customer));
		} catch (BasicException exception) {
			JOptionPane.showMessageDialog(this, AppLocal.getIntString("customer.payerror"),
					AppLocal.getIntString("customer.title"), JOptionPane.WARNING_MESSAGE);
		}
	}

	private static String formatDebt(CustomerInfoExt customer) {
		return customer.getCurdebt() == null ? "0.00" : customer.getCurdebt().toString();
	}

	private static Window getWindow(Component parent) {
		if (parent instanceof Frame || parent instanceof Dialog) {
			return (Window) parent;
		}
		return parent == null ? null : getWindow(parent.getParent());
	}
}
