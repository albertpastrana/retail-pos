package com.openbravo.pos.inventory;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import com.openbravo.data.gui.JConfirmationDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import com.openbravo.basic.BasicException;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.AppView;
import com.openbravo.pos.forms.BeanFactoryApp;
import com.openbravo.pos.forms.BeanFactoryException;
import com.openbravo.pos.forms.JPanelView;

public class PriceRulesPanel extends JPanel implements JPanelView, BeanFactoryApp {

	private PriceRuleService service;
	private final RulesModel model = new RulesModel();
	private final JTable table = new JTable(model);
	private final JButton save = new JButton();
	private final JButton addBrand = new JButton();
	private final JComboBox<String> taxRegime = new JComboBox<String>();
	private final JLabel explanation = new JLabel();

	public PriceRulesPanel() {
		setLayout(new BorderLayout(8, 8));
		JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 6));
		addBrand.setText(AppLocal.getIntString("button.pricerule.addbrand"));
		save.setText(AppLocal.getIntString("Button.Save"));
		addBrand.addActionListener(this::addBrand);
		save.addActionListener(this::saveRules);
		toolbar.add(addBrand);
		toolbar.add(save);
		toolbar.add(new JLabel(AppLocal.getIntString("label.pricerule.taxregime") + ":"));
		taxRegime.addItem(AppLocal.getIntString("label.pricerule.taxregime.equivalence"));
		taxRegime.addItem(AppLocal.getIntString("label.pricerule.taxregime.normal"));
		toolbar.add(taxRegime);
		add(toolbar, BorderLayout.NORTH);

		table.setRowHeight(28);
		table.setFillsViewportHeight(true);
		table.getColumnModel().getColumn(1).setCellRenderer(new PercentageRenderer());
		JComboBox<String> rounding = new JComboBox<String>(new String[]{roundingLabel(PriceRule.ROUND_CHARM),
				roundingLabel(PriceRule.ROUND_NONE), roundingLabel(PriceRule.ROUND_95)});
		table.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor(rounding));
		add(new JScrollPane(table), BorderLayout.CENTER);

		explanation.setText("<html>" + AppLocal.getIntString("message.pricerules.help") + "</html>");
		add(explanation, BorderLayout.SOUTH);
	}

	@Override
	public void init(AppView app) throws BeanFactoryException {
		this.service = new PriceRuleService(app.getSession());
	}

	@Override
	public Object getBean() {
		return this;
	}

	@Override
	public String getTitle() {
		return AppLocal.getIntString("Menu.PriceRules");
	}

	@Override
	public void activate() throws BasicException {
		loadRules();
	}

	@Override
	public boolean deactivate() {
		return true;
	}

	@Override
	public JComponent getComponent() {
		return this;
	}

	private void loadRules() throws BasicException {
		try {
			model.setRules(service.findAll());
			taxRegime.setSelectedIndex(service.getTaxRegime() == TaxRegime.NORMAL ? 1 : 0);
		} catch (SQLException e) {
			throw new BasicException(AppLocal.getIntString("message.pricerules.loaderror"), e);
		}
	}

	private void addBrand(ActionEvent event) {
		try {
			List<String> choices = service.findBrands();
			choices.removeAll(model.getBrands());
			if (choices.isEmpty()) {
				JOptionPane.showMessageDialog(this, AppLocal.getIntString("message.pricerules.nobrands"));
				return;
			}
			Object selected = JOptionPane.showInputDialog(this, AppLocal.getIntString("message.pricerules.choosebrand"),
					AppLocal.getIntString("button.pricerule.addbrand"), JOptionPane.PLAIN_MESSAGE, null,
					choices.toArray(), choices.get(0));
			if (selected != null) {
				RuleRow defaults = model.getDefaultRow();
				model.addRule(new PriceRule(UUID.randomUUID().toString(), selected.toString(), defaults.markupPercent,
						defaults.rounding));
			}
		} catch (SQLException e) {
			showError(e);
		}
	}

	private void saveRules(ActionEvent event) {
		if (table.isEditing()) {
			table.getCellEditor().stopCellEditing();
		}
		try {
			for (RuleRow row : model.rows) {
				if (row.markupPercent < 0.0 || row.markupPercent >= 1000.0) {
					throw new IllegalArgumentException(AppLocal.getIntString("message.pricerules.invalidpercent"));
				}
			}
			Map<String, PriceRule> before = model.originalRules();
			service.saveTaxRegime(selectedTaxRegime());
			for (RuleRow row : model.rows) {
				service.save(row.toRule());
			}
			for (RuleRow row : model.rows) {
				PriceRule oldRule = before.get(row.id);
				if (oldRule != null && ruleChanged(oldRule, row.toRule())) {
					offerBulkUpdate(oldRule, row.toRule());
				}
			}
			loadRules();
		} catch (Exception e) {
			showError(e);
		}
	}

	private TaxRegime selectedTaxRegime() {
		return taxRegime.getSelectedIndex() == 1 ? TaxRegime.NORMAL : TaxRegime.EQUIVALENCE_SURCHARGE;
	}

	private void offerBulkUpdate(PriceRule oldRule, PriceRule newRule) throws SQLException {
		int count = service.countProductsUsingOldRule(newRule.getBrand(), oldRule);
		if (count == 0) {
			return;
		}
		String scope = newRule.getBrand() == null
				? AppLocal.getIntString("label.pricerule.default")
				: newRule.getBrand();
		String message = AppLocal.getIntString("message.pricerules.bulk", scope, Integer.valueOf(count));
		int answer = JConfirmationDialog.show(this, message, AppLocal.getIntString("title.pricerules.bulk"),
				AppLocal.getIntString("confirm.cancel"), AppLocal.getIntString("confirm.update"), false);
		if (answer == JOptionPane.YES_OPTION) {
			int changed = service.repriceProductsUsingOldRule(newRule.getBrand(), oldRule, newRule);
			JOptionPane.showMessageDialog(this,
					AppLocal.getIntString("message.pricerules.bulkdone", Integer.valueOf(changed)));
		}
	}

	private static boolean ruleChanged(PriceRule first, PriceRule second) {
		return Math.abs(first.getMarkupPercent() - second.getMarkupPercent()) > 0.0001
				|| !first.getRounding().equals(second.getRounding());
	}

	private void showError(Exception error) {
		Window window = SwingUtilities.getWindowAncestor(this);
		JOptionPane.showMessageDialog(window, error.getMessage(), AppLocal.getIntString("title.editor"),
				JOptionPane.ERROR_MESSAGE);
	}

	private static final class RulesModel extends AbstractTableModel {
		private final String[] columns = {AppLocal.getIntString("label.pricerule.brand"),
				AppLocal.getIntString("label.pricerule.markup"), AppLocal.getIntString("label.pricerule.margin"),
				AppLocal.getIntString("label.pricerule.rounding")};
		private final List<RuleRow> rows = new ArrayList<RuleRow>();

		void setRules(List<PriceRule> rules) {
			rows.clear();
			for (PriceRule rule : rules) {
				rows.add(new RuleRow(rule));
			}
			fireTableDataChanged();
		}

		void addRule(PriceRule rule) {
			rows.add(new RuleRow(rule));
			fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
		}

		List<String> getBrands() {
			List<String> brands = new ArrayList<String>();
			for (RuleRow row : rows) {
				if (row.brand != null) {
					brands.add(row.brand);
				}
			}
			return brands;
		}

		RuleRow getDefaultRow() {
			return rows.get(0);
		}

		Map<String, PriceRule> originalRules() {
			Map<String, PriceRule> result = new HashMap<String, PriceRule>();
			for (RuleRow row : rows) {
				if (row.original != null) {
					result.put(row.id, row.original);
				}
			}
			return result;
		}

		@Override
		public int getRowCount() {
			return rows.size();
		}

		@Override
		public int getColumnCount() {
			return columns.length;
		}

		@Override
		public String getColumnName(int column) {
			return columns[column];
		}

		@Override
		public Class<?> getColumnClass(int column) {
			return column == 1 || column == 2 ? Double.class : String.class;
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			return column == 1 || column == 3;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			RuleRow row = rows.get(rowIndex);
			switch (columnIndex) {
				case 0 :
					return row.brand == null ? AppLocal.getIntString("label.pricerule.default") : row.brand;
				case 1 :
					return Double.valueOf(row.markupPercent);
				case 2 :
					return Double.valueOf(100.0 * row.markupPercent / (100.0 + row.markupPercent));
				case 3 :
					return roundingLabel(row.rounding);
				default :
					return null;
			}
		}

		@Override
		public void setValueAt(Object value, int rowIndex, int columnIndex) {
			RuleRow row = rows.get(rowIndex);
			if (columnIndex == 1) {
				String text = String.valueOf(value).replace(',', '.').replace("%", "").trim();
				row.markupPercent = Double.parseDouble(text);
				fireTableRowsUpdated(rowIndex, rowIndex);
			} else if (columnIndex == 3) {
				row.rounding = roundingCode(String.valueOf(value));
			}
		}
	}

	private static String roundingLabel(String code) {
		if (PriceRule.ROUND_NONE.equals(code)) {
			return AppLocal.getIntString("label.pricerule.rounding.none");
		}
		if (PriceRule.ROUND_95.equals(code)) {
			return AppLocal.getIntString("label.pricerule.rounding.95");
		}
		return AppLocal.getIntString("label.pricerule.rounding.charm");
	}

	private static String roundingCode(String label) {
		if (roundingLabel(PriceRule.ROUND_NONE).equals(label)) {
			return PriceRule.ROUND_NONE;
		}
		if (roundingLabel(PriceRule.ROUND_95).equals(label)) {
			return PriceRule.ROUND_95;
		}
		return PriceRule.ROUND_CHARM;
	}

	private static final class RuleRow {
		private final String id;
		private final String brand;
		private double markupPercent;
		private String rounding;
		private final PriceRule original;

		RuleRow(PriceRule rule) {
			id = rule.getId();
			brand = rule.getBrand();
			markupPercent = rule.getMarkupPercent();
			rounding = rule.getRounding();
			original = rule;
		}

		PriceRule toRule() {
			return new PriceRule(id, brand, markupPercent, rounding);
		}
	}

	private static final class PercentageRenderer extends DefaultTableCellRenderer {
		private final DecimalFormat format = new DecimalFormat("0.0' %'");

		@Override
		protected void setValue(Object value) {
			setHorizontalAlignment(RIGHT);
			super.setValue(value instanceof Number ? format.format(((Number) value).doubleValue()) : value);
		}
	}
}
