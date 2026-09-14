package com.openbravo.pos.config;

import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.openbravo.data.user.DirtyManager;
import com.openbravo.pos.forms.AppConfig;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.ticket.LoyaltyStamps;

public class JPanelConfigLoyalty extends JPanel implements PanelConfig {

	private final DirtyManager dirty = new DirtyManager();
	private final JCheckBox enabled = new JCheckBox();
	private final JTextField name = new JTextField();

	public JPanelConfigLoyalty() {
		jPanel1 = new JPanel();
		jLabelName = new JLabel();

		jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(AppLocal.getIntString("Label.Loyalty")));
		enabled.setText(AppLocal.getIntString("label.loyalty.enabled"));
		jLabelName.setText(AppLocal.getIntString("label.loyalty.name"));

		javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
		jPanel1.setLayout(jPanel1Layout);
		jPanel1Layout.setHorizontalGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(jPanel1Layout.createSequentialGroup().addContainerGap()
						.addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addComponent(enabled)
								.addGroup(jPanel1Layout.createSequentialGroup()
										.addComponent(jLabelName, javax.swing.GroupLayout.PREFERRED_SIZE, 130,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(name, javax.swing.GroupLayout.PREFERRED_SIZE, 240,
												javax.swing.GroupLayout.PREFERRED_SIZE)))
						.addContainerGap(287, Short.MAX_VALUE)));
		jPanel1Layout.setVerticalGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(jPanel1Layout.createSequentialGroup()
						.addComponent(enabled, javax.swing.GroupLayout.PREFERRED_SIZE, 20,
								javax.swing.GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
						.addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
								.addComponent(jLabelName).addComponent(name, javax.swing.GroupLayout.PREFERRED_SIZE, 20,
										javax.swing.GroupLayout.PREFERRED_SIZE))
						.addContainerGap()));

		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
		setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup().addContainerGap().addComponent(jPanel1,
						javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addContainerGap()));
		layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup().addContainerGap()
						.addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE,
								javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
						.addContainerGap()));

		enabled.addActionListener(dirty);
		name.getDocument().addDocumentListener(dirty);
		enabled.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent evt) {
				name.setEnabled(enabled.isSelected());
			}
		});
	}

	public boolean hasChanged() {
		return dirty.isDirty();
	}

	public Component getConfigComponent() {
		return this;
	}

	public void loadProperties(AppConfig config) {
		enabled.setSelected(LoyaltyStamps.isEnabled(config.getProperty(LoyaltyStamps.ENABLED_KEY)));
		name.setText(LoyaltyStamps.name(config.getProperty(LoyaltyStamps.NAME_KEY)));
		name.setEnabled(enabled.isSelected());
		dirty.setDirty(false);
	}

	public void saveProperties(AppConfig config) {
		config.setProperty(LoyaltyStamps.ENABLED_KEY, Boolean.toString(enabled.isSelected()));
		config.setProperty(LoyaltyStamps.NAME_KEY, LoyaltyStamps.name(name.getText()));
		dirty.setDirty(false);
	}

	private final JPanel jPanel1;
	private final JLabel jLabelName;
}
