//    Openbravo POS is a point of sales application designed for touch screens.
//    Copyright (C) 2008-2009 Openbravo, S.L.
//
//    This file is part of Openbravo POS.

package com.openbravo.pos.inventory;

import com.openbravo.basic.BasicException;
import com.openbravo.pos.forms.AppView;
import com.openbravo.pos.forms.BeanFactoryApp;
import com.openbravo.pos.forms.BeanFactoryException;
import com.openbravo.pos.forms.JPanelView;
import java.awt.BorderLayout;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.JPanel;

/**
 * Single entry point for maintaining product attributes and attribute sets. The
 * existing table panels remain responsible for their CRUD behaviour while this
 * panel keeps the related workflows together.
 */
public class AttributeManagementPanel extends JPanel implements JPanelView, BeanFactoryApp {

	private final JTabbedPane tabs = new JTabbedPane();
	private final AttributesPanel attributes = new AttributesPanel();
	private final AttributeValuesPanel values = new AttributeValuesPanel();
	private final AttributeSetsPanel sets = new AttributeSetsPanel();
	private final AttributeUsePanel usage = new AttributeUsePanel();

	public AttributeManagementPanel() {
		setLayout(new BorderLayout());
		add(tabs, BorderLayout.CENTER);
	}

	public void init(AppView app) throws BeanFactoryException {
		attributes.init(app);
		values.init(app);
		sets.init(app);
		usage.init(app);

		tabs.addTab(com.openbravo.pos.forms.AppLocal.getIntString("Menu.Attributes"), attributes.getComponent());
		tabs.addTab(com.openbravo.pos.forms.AppLocal.getIntString("Menu.AttributeValues"), values.getComponent());
		tabs.addTab(com.openbravo.pos.forms.AppLocal.getIntString("Menu.AttributeSets"), sets.getComponent());
		tabs.addTab(com.openbravo.pos.forms.AppLocal.getIntString("Menu.AttributeUse"), usage.getComponent());
		tabs.applyComponentOrientation(getComponentOrientation());
	}

	public Object getBean() {
		return this;
	}

	public JComponent getComponent() {
		return this;
	}

	public String getTitle() {
		return com.openbravo.pos.forms.AppLocal.getIntString("Menu.AttributeManagement");
	}

	public void activate() throws BasicException {
		attributes.activate();
		values.activate();
		sets.activate();
		usage.activate();
	}

	public boolean deactivate() {
		return attributes.deactivate() && values.deactivate() && sets.deactivate() && usage.deactivate();
	}
}
