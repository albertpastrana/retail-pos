package com.openbravo.pos.reports;

import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.ComboBoxValModel;
import com.openbravo.data.loader.Datas;
import com.openbravo.data.loader.QBFCompareEnum;
import com.openbravo.data.loader.SentenceList;
import com.openbravo.data.loader.SerializerWrite;
import com.openbravo.data.loader.SerializerWriteBasic;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.AppView;
import com.openbravo.pos.forms.DataLogicSales;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

/**
 * Compact product filters for reports. It preserves ProductFilter's serialized
 * parameter order so existing report queries do not need to change.
 */
public class ProductFilterReport extends javax.swing.JPanel implements ReportEditorCreator {

	private SentenceList categories;
	private ComboBoxValModel categoryModel;
	private SentenceList brands;
	private ComboBoxValModel brandModel;

	private final JTextField barcode = new JTextField(14);
	private final JTextField name = new JTextField(18);
	private final JComboBox category = new JComboBox();
	private final JComboBox brand = new JComboBox();

	public ProductFilterReport() {
		setLayout(new GridBagLayout());

		addField(AppLocal.getIntString("label.prodname"), name, 0, 0);
		addField(AppLocal.getIntString("label.prodcategory"), category, 2, 0);
		addField(AppLocal.getIntString("label.prodbrand"), brand, 0, 1);
		addField(AppLocal.getIntString("label.prodbarcode"), barcode, 2, 1);
	}

	private void addField(String label, Component field, int x, int y) {
		GridBagConstraints labelConstraints = new GridBagConstraints();
		labelConstraints.gridx = x;
		labelConstraints.gridy = y;
		labelConstraints.anchor = GridBagConstraints.LINE_END;
		labelConstraints.insets = new Insets(4, 8, 4, 4);
		add(new JLabel(label), labelConstraints);

		GridBagConstraints fieldConstraints = new GridBagConstraints();
		fieldConstraints.gridx = x + 1;
		fieldConstraints.gridy = y;
		fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
		fieldConstraints.weightx = 1.0;
		fieldConstraints.insets = new Insets(4, 4, 4, 8);
		add(field, fieldConstraints);
	}

	public void init(AppView app) {
		DataLogicSales sales = (DataLogicSales) app.getBean("com.openbravo.pos.forms.DataLogicSales");
		categories = sales.getCategoriesList();
		brands = sales.getBrandsList();
		categoryModel = new ComboBoxValModel();
		brandModel = new ComboBoxValModel();
	}

	public void activate() throws BasicException {
		List categoryValues = categories.list();
		categoryValues.add(0, null);
		categoryModel = new ComboBoxValModel(categoryValues);
		category.setModel(categoryModel);

		List brandValues = brands.list();
		brandValues.add(0, null);
		brandModel = new ComboBoxValModel(brandValues);
		brand.setModel(brandModel);

		barcode.setText(null);
		name.setText(null);
	}

	public SerializerWrite getSerializerWrite() {
		return new SerializerWriteBasic(
				new Datas[]{Datas.OBJECT, Datas.STRING, Datas.OBJECT, Datas.DOUBLE, Datas.OBJECT, Datas.DOUBLE,
						Datas.OBJECT, Datas.STRING, Datas.OBJECT, Datas.STRING, Datas.OBJECT, Datas.STRING});
	}

	public Component getComponent() {
		return this;
	}

	public Object createValue() throws BasicException {
		String barcodeValue = barcode.getText();
		if (barcodeValue != null && !barcodeValue.equals("")) {
			return new Object[]{QBFCompareEnum.COMP_NONE, null, QBFCompareEnum.COMP_NONE, null,
					QBFCompareEnum.COMP_NONE, null, QBFCompareEnum.COMP_NONE, null, QBFCompareEnum.COMP_BLOOKUP,
					barcodeValue, QBFCompareEnum.COMP_NONE, null};
		}

		String nameValue = name.getText();
		return new Object[]{
				nameValue == null || nameValue.equals("") ? QBFCompareEnum.COMP_NONE : QBFCompareEnum.COMP_CONTAINS,
				nameValue, QBFCompareEnum.COMP_NONE, null, QBFCompareEnum.COMP_NONE, null,
				categoryModel.getSelectedKey() == null ? QBFCompareEnum.COMP_NONE : QBFCompareEnum.COMP_EQUALS,
				categoryModel.getSelectedKey(), QBFCompareEnum.COMP_NONE, null,
				brandModel.getSelectedItem() == null ? QBFCompareEnum.COMP_NONE : QBFCompareEnum.COMP_EQUALS,
				brandModel.getSelectedItem()};
	}
}
