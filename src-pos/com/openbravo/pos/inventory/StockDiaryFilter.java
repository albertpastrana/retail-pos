package com.openbravo.pos.inventory;

import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.ComboBoxValModel;
import com.openbravo.data.loader.Datas;
import com.openbravo.data.loader.SerializerWrite;
import com.openbravo.data.loader.SerializerWriteBasic;
import com.openbravo.data.user.FilterEditorCreator;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.AppView;
import com.openbravo.pos.forms.DataLogicSales;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

final class StockDiaryFilter extends JPanel implements FilterEditorCreator {
	private final JTextField product = new JTextField();
	private final JComboBox reason = new JComboBox();
	private final JComboBox location = new JComboBox();
	private final JComboBox period = new JComboBox();
	private ComboBoxValModel reasonModel;
	private ComboBoxValModel locationModel;
	private final List<ActionListener> listeners = new java.util.ArrayList<ActionListener>();
	private final Timer filterTimer;

	StockDiaryFilter() {
		setLayout(new GridLayout(1, 4, 8, 0));
		add(field(AppLocal.getIntString("label.stockproduct"), product));
		add(field(AppLocal.getIntString("label.stockreason"), reason));
		add(field(AppLocal.getIntString("label.warehouse"), location));
		add(field(AppLocal.getIntString("label.stockperiod"), period));

		period.addItem(AppLocal.getIntString("stock.period.90"));
		period.addItem(AppLocal.getIntString("stock.period.30"));
		period.addItem(AppLocal.getIntString("stock.period.7"));
		period.addItem(AppLocal.getIntString("stock.period.all"));
		filterTimer = new Timer(300, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				fireChanged();
			}
		});
		filterTimer.setRepeats(false);

		DocumentListener changed = new DocumentListener() {
			public void insertUpdate(DocumentEvent e) {
				scheduleFilter();
			}
			public void removeUpdate(DocumentEvent e) {
				scheduleFilter();
			}
			public void changedUpdate(DocumentEvent e) {
				scheduleFilter();
			}
		};
		product.getDocument().addDocumentListener(changed);
		reason.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				fireChanged();
			}
		});
		location.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				fireChanged();
			}
		});
		period.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				fireChanged();
			}
		});
	}

	private void scheduleFilter() {
		filterTimer.restart();
	}

	private JPanel field(String label, java.awt.Component component) {
		JPanel panel = new JPanel(new BorderLayout(0, 2));
		panel.add(new JLabel(label), BorderLayout.NORTH);
		panel.add(component, BorderLayout.CENTER);
		return panel;
	}

	void init(AppView app) {
		DataLogicSales sales = app.getBean(DataLogicSales.class);
		List reasons = new java.util.ArrayList();
		reasons.add(null);
		reasons.add(MovementReason.IN_PURCHASE);
		reasons.add(MovementReason.IN_REFUND);
		reasons.add(MovementReason.IN_MOVEMENT);
		reasons.add(MovementReason.OUT_SALE);
		reasons.add(MovementReason.OUT_REFUND);
		reasons.add(MovementReason.OUT_BREAK);
		reasons.add(MovementReason.OUT_MOVEMENT);
		reasonModel = new ComboBoxValModel(reasons);
		reason.setModel(reasonModel);
		try {
			List locations = sales.getLocationsList().list();
			locations.add(0, null);
			locationModel = new ComboBoxValModel(locations);
			location.setModel(locationModel);
		} catch (BasicException e) {
			locationModel = new ComboBoxValModel();
			location.setModel(locationModel);
		}
	}

	void addActionListener(ActionListener listener) {
		listeners.add(listener);
	}

	private void fireChanged() {
		ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "APPLY");
		for (ActionListener listener : listeners) {
			listener.actionPerformed(event);
		}
	}

	public SerializerWrite getSerializerWrite() {
		return new SerializerWriteBasic(new Datas[]{Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING,
				Datas.OBJECT, Datas.OBJECT, Datas.OBJECT, Datas.OBJECT, Datas.TIMESTAMP});
	}

	public Object createValue() throws BasicException {
		String value = product.getText() == null ? "" : product.getText().trim();
		String pattern = "%" + value + "%";
		Object selectedReason = reasonModel == null ? null : reasonModel.getSelectedKey();
		Object selectedLocation = locationModel == null ? null : locationModel.getSelectedKey();
		int days = period.getSelectedIndex() == 2
				? 7
				: period.getSelectedIndex() == 1 ? 30 : period.getSelectedIndex() == 0 ? 90 : Integer.MAX_VALUE;
		Date from = days == Integer.MAX_VALUE
				? new Date(0L)
				: new Date(System.currentTimeMillis() - days * 24L * 60L * 60L * 1000L);
		return new Object[]{value, pattern, pattern, pattern, selectedReason, selectedReason, selectedLocation,
				selectedLocation, from};
	}
}
