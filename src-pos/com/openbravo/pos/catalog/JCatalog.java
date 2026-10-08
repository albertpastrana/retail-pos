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

package com.openbravo.pos.catalog;

import com.openbravo.pos.ticket.CategoryInfo;
import com.openbravo.pos.ticket.ProductInfoExt;
import com.openbravo.pos.util.ThumbNailBuilder;

import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.awt.*;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.JMessageDialog;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.pos.forms.DataLogicSales;
import com.openbravo.pos.sales.TaxesLogic;
import com.openbravo.pos.ticket.TaxInfo;

/**
 *
 * @author adrianromero
 */
public class JCatalog extends JPanel implements ListSelectionListener, CatalogSelector {

	private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(JCatalog.class.getName());

	protected EventListenerList listeners = new EventListenerList();
	private DataLogicSales m_dlSales;
	private TaxesLogic taxeslogic;

	private boolean pricevisible;
	private boolean taxesincluded;

	// Set of Products panels
	private Map<String, ProductInfoExt> m_productsset = new HashMap<String, ProductInfoExt>();

	// Set of Categoriespanels
	private Set<String> m_categoriesset = new HashSet<String>();
	private final Map<String, CategoryInfo> categoriesById = new HashMap<String, CategoryInfo>();
	private final JPanel breadcrumbs = new JPanel(new FlowLayout(FlowLayout.LEADING, 4, 0));

	private ThumbNailBuilder tnbbutton;
	private ThumbNailBuilder tnbcat;

	private int buttonwidth;
	private int buttonheight;

	private CategoryInfo showingcategory = null;
	private boolean showingProductDetails;

	/** Creates new form JCatalog */
	public JCatalog(DataLogicSales dlSales) {
		this(dlSales, false, false, 64, 54);
	}

	public JCatalog(DataLogicSales dlSales, boolean pricevisible, boolean taxesincluded, int width, int height) {

		m_dlSales = dlSales;
		this.pricevisible = pricevisible;
		this.taxesincluded = taxesincluded;

		initComponents();

		m_jListCategories.addListSelectionListener(this);
		m_jscrollcat.getVerticalScrollBar().setPreferredSize(new Dimension(35, 35));

		// Older till configurations sized thumbnails, not readable touch tiles.
		buttonwidth = Math.max(144, width);
		buttonheight = Math.max(100, height);

		tnbcat = new ThumbNailBuilder(32, 32, "com/openbravo/images/folder_yellow.png");
		tnbbutton = new ThumbNailBuilder(40, 30);
	}

	public Component getComponent() {
		return this;
	}

	public void showCatalogPanel(String id) {

		if (id == null) {
			showRootCategoriesPanel();
		} else {
			showProductPanel(id);
		}
	}

	public void loadCatalog() throws BasicException {

		// delete all categories panel
		m_jProducts.removeAll();

		m_productsset.clear();
		m_categoriesset.clear();
		categoriesById.clear();

		showingcategory = null;
		showingProductDetails = false;

		// Load the taxes logic
		taxeslogic = new TaxesLogic(m_dlSales.getTaxList().list());

		// Load all categories.
		java.util.List<CategoryInfo> categories = m_dlSales.getRootCategories();
		for (CategoryInfo cat : categories) {
			categoriesById.put(cat.getID(), cat);
		}

		// Select the first category
		m_jListCategories.setCellRenderer(new SmallCategoryRenderer());
		m_jListCategories.setModel(new CategoriesListModel(categories)); // aCatList
		if (m_jListCategories.getModel().getSize() == 0) {
			m_jscrollcat.setVisible(false);
			jPanel2.setVisible(false);
		} else {
			m_jscrollcat.setVisible(true);
			jPanel2.setVisible(true);
			m_jListCategories.setSelectedIndex(0);
		}

		// Display catalog panel
		showRootCategoriesPanel();
	}

	public void setComponentEnabled(boolean value) {

		m_jListCategories.setEnabled(value);
		m_jscrollcat.setEnabled(value);
		m_jUp.setEnabled(value);
		m_jDown.setEnabled(value);
		m_lblIndicator.setEnabled(value);
		m_btnBack1.setEnabled(value);
		for (Component crumb : breadcrumbs.getComponents()) {
			crumb.setEnabled(value);
		}
		m_jProducts.setEnabled(value);
		synchronized (m_jProducts.getTreeLock()) {
			int compCount = m_jProducts.getComponentCount();
			for (int i = 0; i < compCount; i++) {
				m_jProducts.getComponent(i).setEnabled(value);
			}
		}

		this.setEnabled(value);
	}

	public void addActionListener(ActionListener l) {
		listeners.add(ActionListener.class, l);
	}

	public void removeActionListener(ActionListener l) {
		listeners.remove(ActionListener.class, l);
	}

	public void valueChanged(ListSelectionEvent evt) {

		if (!evt.getValueIsAdjusting()) {
			int i = m_jListCategories.getSelectedIndex();
			if (i >= 0) {
				// Lo hago visible...
				Rectangle oRect = m_jListCategories.getCellBounds(i, i);
				m_jListCategories.scrollRectToVisible(oRect);
			}
		}
	}

	protected void fireSelectedProduct(ProductInfoExt prod) {
		EventListener[] l = listeners.getListeners(ActionListener.class);
		ActionEvent e = null;
		for (int i = 0; i < l.length; i++) {
			if (e == null) {
				e = new ActionEvent(prod, ActionEvent.ACTION_PERFORMED, prod.getID());
			}
			((ActionListener) l[i]).actionPerformed(e);
		}
	}

	private void selectCategoryPanel(String catid) {

		try {
			// Load categories panel if not exists
			if (!m_categoriesset.contains(catid)) {

				JCatalogTab jcurrTab = new JCatalogTab();
				jcurrTab.applyComponentOrientation(getComponentOrientation());
				m_jProducts.add(jcurrTab, catid);
				m_categoriesset.add(catid);

				// Add subcategories
				java.util.List<CategoryInfo> categories = m_dlSales.getSubcategories(catid);
				for (CategoryInfo cat : categories) {
					categoriesById.put(cat.getID(), cat);
					addCategoryButton(jcurrTab, cat);
				}

				// Add products
				java.util.List<ProductInfoExt> products = m_dlSales.getProductCatalog(catid);
				for (ProductInfoExt prod : products) {
					addProductButton(jcurrTab, prod);
				}
			}

			// Show categories panel
			CardLayout cl = (CardLayout) (m_jProducts.getLayout());
			cl.show(m_jProducts, catid);
		} catch (BasicException e) {
			JMessageDialog.showMessage(this,
					new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.notactive"), e));
		}
	}

	/**
	 * Items without an image get a text button instead of the generic package
	 * thumbnail, which paints the name over the image and clips it.
	 */
	private void addProductButton(JCatalogTab tab, ProductInfoExt prod) {
		ImageIcon image = prod.getImage() == null ? null : new ImageIcon(tnbbutton.getThumbNail(prod.getImage()));
		tab.addButton(getProductTextLines(prod), image, buttonwidth, buttonheight, false, new SelectedAction(prod));
	}

	private void addCategoryButton(JCatalogTab tab, CategoryInfo cat) {

		ImageIcon image = cat.getImage() == null ? null : new ImageIcon(tnbbutton.getThumbNail(cat.getImage()));
		tab.addButton(new String[]{cat.getName()}, image, buttonwidth, buttonheight, true, new SelectedCategory(cat));
	}

	private String[] getProductTextLines(ProductInfoExt product) {

		if (pricevisible) {
			return new String[]{product.getName(), getProductPrice(product)};
		} else {
			return new String[]{product.getName()};
		}
	}

	private String getProductPrice(ProductInfoExt product) {

		if (taxesincluded) {
			TaxInfo tax = taxeslogic.getTaxInfo(product.getTaxCategoryID(), new Date());
			return product.printPriceSellTax(tax);
		} else {
			return product.printPriceSell();
		}
	}

	private void selectIndicatorPanel(String label) {

		m_lblIndicator.setText(label);
		m_jCategories.setVisible(false);
		m_jSubCategories.setVisible(true);
		m_btnBack1.setVisible(true);
	}

	private void selectIndicatorCategories() {
		m_jCategories.setVisible(true);
		m_jSubCategories.setVisible(false);
		m_btnBack1.setVisible(false);
	}

	private void showBreadcrumb(CategoryInfo category, String detail) {
		breadcrumbs.removeAll();
		java.util.List<CategoryInfo> path = new ArrayList<CategoryInfo>();
		Set<String> visited = new HashSet<String>();
		for (CategoryInfo current = category; current != null
				&& visited.add(current.getID()); current = categoriesById.get(current.getParentID())) {
			path.add(current);
		}
		Collections.reverse(path);
		for (int i = 0; i < path.size(); i++) {
			if (i > 0) {
				breadcrumbs.add(new JLabel("›"));
			}
			CategoryInfo item = path.get(i);
			if (i == path.size() - 1 && detail == null) {
				m_lblIndicator.setText(item.getName());
				breadcrumbs.add(m_lblIndicator);
			} else {
				JButton crumb = new JButton(item.getName());
				crumb.setMargin(new Insets(8, 10, 8, 10));
				crumb.setPreferredSize(new Dimension(crumb.getPreferredSize().width, 48));
				crumb.setFocusable(false); // Keep scanner input in the code field after touch navigation.
				crumb.addActionListener(e -> showCategory(item));
				breadcrumbs.add(crumb);
			}
		}
		if (detail != null) {
			if (!path.isEmpty()) {
				breadcrumbs.add(new JLabel("›"));
			}
			m_lblIndicator.setText(detail);
			breadcrumbs.add(m_lblIndicator);
		}
		// The root is already named in the category list. Reserve this row only
		// when it adds navigation context (a subcategory or a product detail).
		m_jSubCategories.setVisible(detail != null || category != null && category.getParentID() != null);
		breadcrumbs.revalidate();
		breadcrumbs.repaint();
	}

	private void showCategory(CategoryInfo category) {
		if (category.getParentID() == null) {
			m_jListCategories.setSelectedValue(category, true);
			showRootCategoriesPanel();
		} else {
			showSubcategoryPanel(category);
		}
	}

	private CategoryInfo productCategory(ProductInfoExt product) {
		String categoryId = product.getCategoryID();
		if (categoryId == null) {
			return null;
		}
		Set<String> visited = new HashSet<String>();
		try {
			for (String id = categoryId; id != null && visited.add(id) && !categoriesById.containsKey(id);) {
				CategoryInfo category = m_dlSales.getCatalogCategory(id);
				if (category == null) {
					break;
				}
				categoriesById.put(id, category);
				id = category.getParentID();
			}
		} catch (BasicException e) {
			LOGGER.log(java.util.logging.Level.WARNING,
					"event=catalog_category_path_load_failed category=" + categoryId, e);
		}
		return categoriesById.get(categoryId);
	}

	private void showRootCategoriesPanel() {

		selectIndicatorCategories();
		showingProductDetails = false;
		// Show selected root category
		CategoryInfo cat = (CategoryInfo) m_jListCategories.getSelectedValue();

		if (cat != null) {
			selectCategoryPanel(cat.getID());
		}
		showingcategory = null;
		showBreadcrumb(cat, null);
	}

	private void showSubcategoryPanel(CategoryInfo category) {
		selectIndicatorPanel(category.getName());
		selectCategoryPanel(category.getID());
		showingcategory = category;
		showingProductDetails = false;
		showBreadcrumb(category, null);
	}

	private void showProductPanel(String id) {

		ProductInfoExt product = m_productsset.get(id);

		if (product == null) {
			if (m_productsset.containsKey(id)) {
				// It is an empty panel
				if (showingcategory == null) {
					showRootCategoriesPanel();
				} else {
					showSubcategoryPanel(showingcategory);
				}
			} else {
				try {
					// Create products panel
					java.util.List<ProductInfoExt> products = m_dlSales.getProductComments(id);

					if (products.size() == 0) {
						// no hay productos por tanto lo anado a la de vacios y muestro el panel
						// principal.
						m_productsset.put(id, null);
						if (showingcategory == null) {
							showRootCategoriesPanel();
						} else {
							showSubcategoryPanel(showingcategory);
						}
					} else {

						// Load product panel
						product = m_dlSales.getProductInfo(id);
						m_productsset.put(id, product);

						JCatalogTab jcurrTab = new JCatalogTab();
						jcurrTab.applyComponentOrientation(getComponentOrientation());
						m_jProducts.add(jcurrTab, "PRODUCT." + id);

						// Add products
						for (ProductInfoExt prod : products) {
							addProductButton(jcurrTab, prod);
						}

						selectIndicatorPanel(product.getName());
						showingProductDetails = true;
						showBreadcrumb(productCategory(product), product.getName());

						CardLayout cl = (CardLayout) (m_jProducts.getLayout());
						cl.show(m_jProducts, "PRODUCT." + id);
					}
				} catch (BasicException eb) {
					LOGGER.log(java.util.logging.Level.WARNING, "event=catalog_category_load_failed category=" + id,
							eb);
					m_productsset.put(id, null);
					if (showingcategory == null) {
						showRootCategoriesPanel();
					} else {
						showSubcategoryPanel(showingcategory);
					}
				}
			}
		} else {
			// already exists
			selectIndicatorPanel(product.getName());
			showingProductDetails = true;
			showBreadcrumb(productCategory(product), product.getName());

			CardLayout cl = (CardLayout) (m_jProducts.getLayout());
			cl.show(m_jProducts, "PRODUCT." + id);
		}
	}

	private class SelectedAction implements ActionListener {
		private ProductInfoExt prod;

		public SelectedAction(ProductInfoExt prod) {
			this.prod = prod;
		}

		public void actionPerformed(ActionEvent e) {
			fireSelectedProduct(prod);
		}
	}

	private class SelectedCategory implements ActionListener {
		private CategoryInfo category;

		public SelectedCategory(CategoryInfo category) {
			this.category = category;
		}

		public void actionPerformed(ActionEvent e) {
			showSubcategoryPanel(category);
		}
	}

	private class CategoriesListModel extends AbstractListModel {
		private java.util.List m_aCategories;

		public CategoriesListModel(java.util.List aCategories) {
			m_aCategories = aCategories;
		}

		public int getSize() {
			return m_aCategories.size();
		}

		public Object getElementAt(int i) {
			return m_aCategories.get(i);
		}
	}

	private class SmallCategoryRenderer extends DefaultListCellRenderer {

		@Override
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
			super.getListCellRendererComponent(list, null, index, isSelected, cellHasFocus);
			CategoryInfo cat = (CategoryInfo) value;
			setText(cat.getName());
			setIcon(new ImageIcon(tnbcat.getThumbNail(cat.getImage())));
			return this;
		}
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	// <editor-fold defaultstate="collapsed" desc="Generated
	// Code">//GEN-BEGIN:initComponents
	private void initComponents() {

		m_jCategories = new javax.swing.JPanel();
		m_jRootCategories = new javax.swing.JPanel();
		m_jscrollcat = new javax.swing.JScrollPane();
		m_jListCategories = new javax.swing.JList();
		jPanel2 = new javax.swing.JPanel();
		jPanel3 = new javax.swing.JPanel();
		m_jUp = new javax.swing.JButton();
		m_jDown = new javax.swing.JButton();
		m_jSubCategories = new javax.swing.JPanel();
		jPanel4 = new javax.swing.JPanel();
		m_lblIndicator = new javax.swing.JLabel();
		jPanel1 = new javax.swing.JPanel();
		jPanel5 = new javax.swing.JPanel();
		m_btnBack1 = new javax.swing.JButton();
		m_jProducts = new javax.swing.JPanel();

		setLayout(new java.awt.BorderLayout());

		m_jCategories.setMaximumSize(new java.awt.Dimension(340, 600));
		m_jCategories.setPreferredSize(new java.awt.Dimension(340, 600));
		m_jCategories.setLayout(new java.awt.CardLayout());

		m_jRootCategories.setLayout(new java.awt.BorderLayout());

		m_jscrollcat.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		m_jscrollcat.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

		m_jListCategories.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
		m_jListCategories.setFocusable(false);
		m_jListCategories.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
			public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
				m_jListCategoriesValueChanged(evt);
			}
		});
		m_jscrollcat.setViewportView(m_jListCategories);

		m_jRootCategories.add(m_jscrollcat, java.awt.BorderLayout.CENTER);

		jPanel2.setLayout(new java.awt.BorderLayout());

		jPanel3.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 5, 0, 5));
		jPanel3.setLayout(new java.awt.GridLayout(0, 1, 0, 5));

		m_jUp.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/1uparrow22.png"))); // NOI18N
		m_jUp.setFocusPainted(false);
		m_jUp.setFocusable(false);
		m_jUp.setMargin(new java.awt.Insets(8, 14, 8, 14));
		m_jUp.setRequestFocusEnabled(false);
		m_jUp.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				m_jUpActionPerformed(evt);
			}
		});
		jPanel3.add(m_jUp);

		m_jDown.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/1downarrow22.png"))); // NOI18N
		m_jDown.setFocusPainted(false);
		m_jDown.setFocusable(false);
		m_jDown.setMargin(new java.awt.Insets(8, 14, 8, 14));
		m_jDown.setRequestFocusEnabled(false);
		m_jDown.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				m_jDownActionPerformed(evt);
			}
		});
		jPanel3.add(m_jDown);

		jPanel2.add(jPanel3, java.awt.BorderLayout.NORTH);

		m_jRootCategories.add(jPanel2, java.awt.BorderLayout.LINE_END);

		m_jCategories.add(m_jRootCategories, "rootcategories");

		m_jSubCategories.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 5, 4, 5));
		m_jSubCategories.setLayout(new java.awt.BorderLayout(12, 0));
		m_jSubCategories.setVisible(false);

		jPanel4.setLayout(new java.awt.BorderLayout());

		m_lblIndicator.setFont(m_lblIndicator.getFont().deriveFont(java.awt.Font.BOLD, 18f));
		javax.swing.JScrollPane breadcrumbScroll = new javax.swing.JScrollPane(breadcrumbs,
				javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
				javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		breadcrumbScroll.setBorder(null);
		jPanel4.add(breadcrumbScroll, java.awt.BorderLayout.CENTER);

		m_jSubCategories.add(jPanel4, java.awt.BorderLayout.CENTER);

		jPanel1.setLayout(new java.awt.BorderLayout());

		jPanel5.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 5, 0, 5));
		jPanel5.setLayout(new java.awt.GridLayout(0, 1, 0, 5));

		m_btnBack1.setText(AppLocal.getIntString("button.catalog.back"));
		m_btnBack1.setFocusPainted(false);
		m_btnBack1.setFocusable(false);
		m_btnBack1.setMargin(new java.awt.Insets(10, 14, 10, 14));
		m_btnBack1.setPreferredSize(new java.awt.Dimension(m_btnBack1.getPreferredSize().width, 48));
		m_btnBack1.setRequestFocusEnabled(false);
		m_btnBack1.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				m_btnBack1ActionPerformed(evt);
			}
		});
		jPanel5.add(m_btnBack1);

		jPanel1.add(jPanel5, java.awt.BorderLayout.NORTH);

		m_jSubCategories.add(jPanel1, java.awt.BorderLayout.LINE_START);

		add(m_jCategories, java.awt.BorderLayout.LINE_START);
		add(m_jSubCategories, java.awt.BorderLayout.NORTH);

		m_jProducts.setLayout(new java.awt.CardLayout());
		add(m_jProducts, java.awt.BorderLayout.CENTER);
	}// </editor-fold>//GEN-END:initComponents

	private void m_jDownActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_m_jDownActionPerformed

		int i = m_jListCategories.getSelectionModel().getMaxSelectionIndex();
		if (i < 0) {
			i = 0; // No hay ninguna seleccionada
		} else {
			i++;
			if (i >= m_jListCategories.getModel().getSize()) {
				i = m_jListCategories.getModel().getSize() - 1;
			}
		}

		if ((i >= 0) && (i < m_jListCategories.getModel().getSize())) {
			// Solo seleccionamos si podemos.
			m_jListCategories.getSelectionModel().setSelectionInterval(i, i);
		}

	}// GEN-LAST:event_m_jDownActionPerformed

	private void m_jUpActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_m_jUpActionPerformed

		int i = m_jListCategories.getSelectionModel().getMinSelectionIndex();
		if (i < 0) {
			i = m_jListCategories.getModel().getSize() - 1; // No hay ninguna seleccionada
		} else {
			i--;
			if (i < 0) {
				i = 0;
			}
		}

		if ((i >= 0) && (i < m_jListCategories.getModel().getSize())) {
			// Solo seleccionamos si podemos.
			m_jListCategories.getSelectionModel().setSelectionInterval(i, i);
		}

	}// GEN-LAST:event_m_jUpActionPerformed

	private void m_jListCategoriesValueChanged(javax.swing.event.ListSelectionEvent evt) {// GEN-FIRST:event_m_jListCategoriesValueChanged

		if (!evt.getValueIsAdjusting()) {
			CategoryInfo cat = (CategoryInfo) m_jListCategories.getSelectedValue();
			if (cat != null) {
				selectCategoryPanel(cat.getID());
				if (m_jCategories.isVisible()) {
					showBreadcrumb(cat, null);
				}
			}
		}

	}// GEN-LAST:event_m_jListCategoriesValueChanged

	private void m_btnBack1ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_m_btnBack1ActionPerformed

		if (showingProductDetails && showingcategory != null) {
			showSubcategoryPanel(showingcategory);
		} else if (!showingProductDetails && showingcategory != null
				&& categoriesById.containsKey(showingcategory.getParentID())) {
			showCategory(categoriesById.get(showingcategory.getParentID()));
		} else {
			showRootCategoriesPanel();
		}

	}// GEN-LAST:event_m_btnBack1ActionPerformed

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JPanel jPanel1;
	private javax.swing.JPanel jPanel2;
	private javax.swing.JPanel jPanel3;
	private javax.swing.JPanel jPanel4;
	private javax.swing.JPanel jPanel5;
	private javax.swing.JButton m_btnBack1;
	private javax.swing.JPanel m_jCategories;
	private javax.swing.JButton m_jDown;
	private javax.swing.JList m_jListCategories;
	private javax.swing.JPanel m_jProducts;
	private javax.swing.JPanel m_jRootCategories;
	private javax.swing.JPanel m_jSubCategories;
	private javax.swing.JButton m_jUp;
	private javax.swing.JScrollPane m_jscrollcat;
	private javax.swing.JLabel m_lblIndicator;
	// End of variables declaration//GEN-END:variables

}
