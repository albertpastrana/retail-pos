package com.openbravo.pos.admin;

import com.openbravo.format.Formats;
import com.openbravo.pos.forms.AppLocal;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Canonical permission keys, with the stored XML retained verbatim unless a
 * checkbox changes.
 */
final class RolePermissions {
	private static final String TEMPLATE = "/com/openbravo/pos/templates/Role.Administrator.xml";
	private static final List<String> CATALOGUE = loadCatalogue();

	private RolePermissions() {
	}

	static List<String> catalogue() {
		return CATALOGUE;
	}

	static String text(Object bytes) {
		return Formats.BYTEA.formatValue(bytes);
	}

	static Set<String> keys(String xml) throws Exception {
		Set<String> result = new LinkedHashSet<>();
		NodeList nodes = parse(xml).getElementsByTagName("class");
		for (int i = 0; i < nodes.getLength(); i++)
			result.add(((Element) nodes.item(i)).getAttribute("name"));
		return result;
	}

	static String update(String xml, Set<String> selected) throws Exception {
		Document doc = parse(xml);
		NodeList nodes = doc.getElementsByTagName("class");
		Set<String> present = new LinkedHashSet<>();
		for (int i = nodes.getLength() - 1; i >= 0; i--) {
			Element node = (Element) nodes.item(i);
			String key = node.getAttribute("name");
			if (CATALOGUE.contains(key) && !selected.contains(key))
				node.getParentNode().removeChild(node);
			else
				present.add(key);
		}
		for (String key : selected) {
			if (!present.contains(key)) {
				Element node = doc.createElement("class");
				node.setAttribute("name", key);
				doc.getDocumentElement().appendChild(node);
			}
		}
		TransformerFactory factory = TransformerFactory.newInstance();
		factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		StringWriter out = new StringWriter();
		var transformer = factory.newTransformer();
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.transform(new DOMSource(doc), new StreamResult(out));
		return out.toString();
	}

	static String area(String key) {
		if (key.startsWith("com.openbravo.pos.sales.")
				|| key.startsWith("com.openbravo.pos.panels.JPanel")
						&& (key.contains("Cash") || key.contains("Payments"))
				|| key.startsWith("sales.") || key.startsWith("payment.") || key.startsWith("refund.")
				|| key.startsWith("button."))
			return "sales";
		if (key.startsWith("com.openbravo.pos.reports.") || key.contains("MenuSalesManagement"))
			return "reports";
		if (key.startsWith("com.openbravo.pos.inventory.") || key.contains("CustomersPanel")
				|| key.contains("MenuStockManagement") || key.contains("MenuMaintenance")
				|| key.startsWith("Menu.Replenishment"))
			return "maintenance";
		return "system";
	}

	static String label(String key) {
		String menu = switch (key) {
			case "com.openbravo.pos.forms.JPanelWelcome" -> "Menu.Home";
			case "com.openbravo.pos.sales.JPanelTicketSales" -> "Menu.Ticket";
			case "com.openbravo.pos.sales.JPanelTicketEdits" -> "Menu.TicketEdit";
			case "com.openbravo.pos.panels.JPanelPayments" -> "Menu.Payments";
			case "com.openbravo.pos.panels.JPanelCloseMoney" -> "Menu.CloseTPV";
			case "com.openbravo.pos.panels.JPanelClosedCash" -> "Menu.Closing";
			case "com.openbravo.pos.customers.CustomersPanel" -> "Menu.Customers";
			case "com.openbravo.pos.inventory.TaxCustCategoriesPanel" -> "Menu.TaxCustCategories";
			case "com.openbravo.pos.forms.MenuStockManagement" -> "Menu.StockManagement";
			case "com.openbravo.pos.inventory.ProductsPanel" -> "Menu.Products";
			case "com.openbravo.pos.inventory.ProductsWarehousePanel" -> "Menu.ProductsWarehouse";
			case "com.openbravo.pos.inventory.PriceRulesPanel" -> "Menu.PriceRules";
			case "com.openbravo.pos.inventory.SaleMarkPanel" -> "Menu.SaleMark";
			case "com.openbravo.pos.inventory.CategoriesPanel" -> "Menu.Categories";
			case "com.openbravo.pos.inventory.TaxPanel" -> "Menu.Taxes";
			case "com.openbravo.pos.inventory.TaxCategoriesPanel" -> "Menu.TaxCategories";
			case "com.openbravo.pos.inventory.StockDiaryPanel" -> "Menu.StockDiary";
			case "com.openbravo.pos.inventory.StockReceivingPanel" -> "receiving.title";
			case "com.openbravo.pos.inventory.ReplenishmentPanel" -> "Menu.Replenishment";
			case "com.openbravo.pos.inventory.StockManagement" -> "Menu.StockMovement";
			case "com.openbravo.pos.forms.MenuSalesManagement" -> "Menu.SalesManagement";
			case "com.openbravo.pos.reports.JPanelSalesSummary" -> "Menu.SalesSummary";
			case "com.openbravo.pos.reports.JPanelProductSales" -> "Menu.ProductSalesSummary";
			case "com.openbravo.pos.reports.JPanelPaymentSales" -> "Menu.PaymentSalesSummary";
			case "com.openbravo.pos.reports.JPanelLowStock" -> "Menu.LowStockSummary";
			case "com.openbravo.pos.reports.JPanelTaxSummary" -> "Menu.TaxSummary";
			case "com.openbravo.pos.reports.JPanelCashClosing" -> "Menu.CashClosingSummary";
			case "com.openbravo.pos.reports.JPanelCustomerDebt" -> "Menu.CustomerDebtSummary";
			case "com.openbravo.pos.forms.MenuMaintenance" -> "Menu.Maintenance";
			case "com.openbravo.pos.admin.PeoplePanel" -> "Menu.Users";
			case "com.openbravo.pos.admin.RolesPanel" -> "Menu.Roles";
			case "com.openbravo.pos.admin.ResourcesPanel" -> "Menu.Resources";
			case "com.openbravo.pos.admin.BackupDatabaseAction" -> "Menu.DatabaseBackup";
			case "com.openbravo.pos.inventory.LocationsPanel" -> "Menu.Locations";
			case "com.openbravo.pos.mant.JPanelFloors" -> "Admin.Permission.Floors";
			case "com.openbravo.pos.mant.JPanelPlaces" -> "Admin.Permission.Tables";
			case "com.openbravo.possync.ProductsSyncCreate" -> "Menu.ERPProducts";
			case "com.openbravo.possync.OrdersSyncCreate" -> "Menu.ERPOrders";
			case "Menu.ChangePassword" -> "Menu.ChangePassword";
			case "Menu.Replenishment.Add" -> "Menu.Replenishment.Add";
			case "com.openbravo.pos.panels.JPanelPrinter" -> "Menu.Printer";
			case "com.openbravo.pos.config.JPanelConfiguration" -> "Menu.Configuration";
			default -> null;
		};
		if (menu != null)
			return AppLocal.getIntString(menu);
		if (key.startsWith("sales.") || key.startsWith("payment.") || key.startsWith("refund.")
				|| key.startsWith("button."))
			return AppLocal.getIntString("Admin.Permission." + key);
		String name = key.substring(key.lastIndexOf('.') + 1).replaceFirst("^JPanel", "").replaceFirst("^Panel", "");
		return name.replaceAll("([a-z])([A-Z])", "$1 $2").replace('_', ' ');
	}

	private static Document parse(String xml) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
		factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		factory.setXIncludeAware(false);
		factory.setExpandEntityReferences(false);
		Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
		if (!"permissions".equals(doc.getDocumentElement().getTagName()))
			throw new IllegalArgumentException("Expected permissions");
		return doc;
	}

	private static List<String> loadCatalogue() {
		try (InputStream in = RolePermissions.class.getResourceAsStream(TEMPLATE)) {
			if (in == null)
				throw new IllegalStateException("Missing permission catalogue " + TEMPLATE);
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			NodeList nodes = factory.newDocumentBuilder().parse(in).getElementsByTagName("class");
			List<String> result = new ArrayList<>();
			for (int i = 0; i < nodes.getLength(); i++)
				result.add(((Element) nodes.item(i)).getAttribute("name"));
			return List.copyOf(result);
		} catch (Exception ex) {
			throw new IllegalStateException("Cannot load permission catalogue", ex);
		}
	}
}
