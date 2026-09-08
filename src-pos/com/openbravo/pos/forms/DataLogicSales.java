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

package com.openbravo.pos.forms;

import com.openbravo.pos.ticket.CategoryInfo;
import com.openbravo.pos.ticket.ProductInfoExt;
import com.openbravo.pos.ticket.TaxInfo;
import com.openbravo.pos.ticket.TicketInfo;
import com.openbravo.pos.ticket.TicketLineInfo;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import com.openbravo.data.loader.*;
import com.openbravo.format.Formats;
import com.openbravo.basic.BasicException;
import com.openbravo.data.model.Field;
import com.openbravo.data.model.Row;
import com.openbravo.pos.customers.CustomerInfoExt;
import com.openbravo.pos.inventory.AttributeSetInfo;
import com.openbravo.pos.inventory.TaxCustCategoryInfo;
import com.openbravo.pos.inventory.LocationInfo;
import com.openbravo.pos.inventory.MovementReason;
import com.openbravo.pos.inventory.TaxCategoryInfo;
import com.openbravo.pos.payment.GiftVoucherInfo;
import com.openbravo.pos.payment.PaymentInfo;
import com.openbravo.pos.payment.PaymentInfoTicket;
import com.openbravo.pos.ticket.FindTicketsInfo;
import com.openbravo.pos.ticket.TicketTaxInfo;
import com.openbravo.pos.util.RoundUtils;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 *
 * @author adrianromero
 */
public class DataLogicSales extends BeanFactoryDataSingle {

	private static final Logger LOGGER = Logger.getLogger("com.openbravo.pos.forms.DataLogicSales");

	protected Session s;

	protected Datas[] auxiliarDatas;
	protected Datas[] stockdiaryDatas;
	// protected Datas[] productcatDatas;
	protected Datas[] paymenttabledatas;
	protected Datas[] stockdatas;

	protected Row productsRow;

	/** Creates a new instance of SentenceContainerGeneric */
	public DataLogicSales() {
		stockdiaryDatas = new Datas[] { Datas.STRING, Datas.TIMESTAMP, Datas.INT, Datas.STRING, Datas.STRING,
				Datas.STRING, Datas.DOUBLE, Datas.DOUBLE };
		paymenttabledatas = new Datas[] { Datas.STRING, Datas.STRING, Datas.TIMESTAMP, Datas.STRING, Datas.STRING,
				Datas.DOUBLE };
		stockdatas = new Datas[] { Datas.STRING, Datas.STRING, Datas.STRING, Datas.DOUBLE, Datas.DOUBLE, Datas.DOUBLE };
		auxiliarDatas = new Datas[] { Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING,
				Datas.STRING };

		productsRow = new Row(new Field("ID", Datas.STRING, Formats.STRING),
				new Field(AppLocal.getIntString("label.prodref"), Datas.STRING, Formats.STRING, true, true, true),
				new Field(AppLocal.getIntString("label.prodbarcode"), Datas.STRING, Formats.STRING, false, true, true),
				new Field(AppLocal.getIntString("label.prodname"), Datas.STRING, Formats.STRING, true, true, true),
				new Field("ISCOM", Datas.BOOLEAN, Formats.BOOLEAN),
				new Field("ISSCALE", Datas.BOOLEAN, Formats.BOOLEAN),
				new Field(AppLocal.getIntString("label.prodpricebuy"), Datas.DOUBLE, Formats.CURRENCY, false, true,
						true),
				new Field(AppLocal.getIntString("label.prodpricesell"), Datas.DOUBLE, Formats.CURRENCY, false, true,
						true),
				new Field(AppLocal.getIntString("label.prodcategory"), Datas.STRING, Formats.STRING, false, false,
						true),
				new Field(AppLocal.getIntString("label.taxcategory"), Datas.STRING, Formats.STRING, false, false, true),
				new Field(AppLocal.getIntString("label.attributeset"), Datas.STRING, Formats.STRING, false, false,
						true),
				new Field("IMAGE", Datas.IMAGE, Formats.NULL), new Field("STOCKCOST", Datas.DOUBLE, Formats.CURRENCY),
				new Field("STOCKVOLUME", Datas.DOUBLE, Formats.DOUBLE),
				new Field("ISCATALOG", Datas.BOOLEAN, Formats.BOOLEAN), new Field("CATORDER", Datas.INT, Formats.INT),
				new Field("PROPERTIES", Datas.BYTES, Formats.NULL));
	}

	public void init(Session s) {
		this.s = s;
	}

	private Session getSession() {
		return s;
	}

	public final Row getProductsRow() {
		return productsRow;
	}

	// Utilidades de productos
	public final ProductInfoExt getProductInfo(String id) throws BasicException {
		return (ProductInfoExt) new PreparedSentence(s,
				"SELECT ID, REFERENCE, CODE, NAME, ISCOM, ISSCALE, PRICEBUY, PRICESELL, TAXCAT, CATEGORY, ATTRIBUTESET_ID, IMAGE, ATTRIBUTES "
						+ "FROM PRODUCTS WHERE ID = ?",
				SerializerWriteString.INSTANCE, ProductInfoExt.getSerializerRead()).find(id);
	}

	public final GiftVoucherInfo findGiftVoucher(String code) throws BasicException {
		Object[] voucher = (Object[]) new PreparedSentence(s,
				"SELECT CODE, INITIALVALUE, BALANCE FROM GIFTVOUCHERS WHERE CODE = ?",
				SerializerWriteString.INSTANCE,
				new SerializerReadBasic(new Datas[] { Datas.STRING, Datas.DOUBLE, Datas.DOUBLE }))
						.find(code.trim().toUpperCase());
		return voucher == null ? null
				: new GiftVoucherInfo((String) voucher[0], ((Double) voucher[1]).doubleValue(),
						((Double) voucher[2]).doubleValue());
	}

	public final ProductInfoExt getProductInfoByCode(String sCode) throws BasicException {
		// A code scanned as EAN-13 carries up to two leading zeros that the stored
		// code does not, so a barcode lookup has to try the padded forms too.
		return (ProductInfoExt) new PreparedSentence(s,
				"SELECT ID, REFERENCE, CODE, NAME, ISCOM, ISSCALE, PRICEBUY, PRICESELL, TAXCAT, CATEGORY, ATTRIBUTESET_ID, IMAGE, ATTRIBUTES "
						+ "FROM PRODUCTS WHERE CODE IN (?, ?, ?) "
						+ "OR EXISTS (SELECT 1 FROM BARCODE_TABLE WHERE BARCODE_TABLE.PID = PRODUCTS.ID AND BARCODE_TABLE.CODE IN (?, ?, ?))",
				new SerializerWriteBasic(new Datas[] { Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING,
						Datas.STRING, Datas.STRING }),
				ProductInfoExt.getSerializerRead()).find(sCode, "0" + sCode, "00" + sCode, sCode, "0" + sCode,
						"00" + sCode);
	}

	public final ProductInfoExt getCatalogProductByCode(String code, String productsPath, String categoriesPath)
			throws BasicException {
		if (productsPath == null || categoriesPath == null) {
			return null;
		}

		try {
			String[] row = findProduct(code, productsPath);
			if (row == null) {
				LOGGER.info("Barcode " + code + " not found in import catalog " + productsPath);
				return null;
			}

			ProductInfoExt product = new ProductInfoExt();
			product.setID(row[0]);
			product.setReference(row[1]);
			product.setCode(row[2]);
			product.setName(row[3]);
			product.setCategoryID(row[4]);
			product.setPriceBuy(Double.parseDouble(row[5]));
			product.setPriceSell(Double.parseDouble(row[6]));
			product.setTaxCategoryID("001");

			String[] category = readCategories(categoriesPath).get(row[4]);
			product.setProperty("catalog.category.name", category == null ? row[4] : category[1]);
			product.setProperty("catalog.brand", row.length > 7 ? row[7] : null);
			return product;
		} catch (Exception e) {
			throw new BasicException("Cannot read barcode " + code + " from import catalog", e);
		}
	}

	public final ProductInfoExt importProductByCode(String code, String productsPath, String categoriesPath)
			throws BasicException {
		if (productsPath == null || categoriesPath == null) {
			return null;
		}

		try {
			String[] product = findProduct(code, productsPath);
			if (product == null) {
				return null;
			}
			LOGGER.info("Importing barcode " + code + " as " + product[1] + " from " + productsPath);

			Map<String, String[]> categories = readCategories(categoriesPath);
			Connection connection = s.getConnection();
			boolean oldAutoCommit = connection.getAutoCommit();
			connection.setAutoCommit(false);
			try {
				ensureCategory(connection, product[4], categories);

				PreparedStatement insert = connection
						.prepareStatement("INSERT INTO PRODUCTS (ID, REFERENCE, CODE, CODETYPE, NAME, "
								+ "PRICEBUY, PRICESELL, CATEGORY, TAXCAT, ATTRIBUTESET_ID, "
								+ "STOCKCOST, STOCKVOLUME, IMAGE, ISCOM, ISSCALE, ATTRIBUTES, BRAND) "
								+ "VALUES (?, ?, ?, 'EAN13', ?, ?, ?, ?, '001', NULL, "
								+ "NULL, NULL, NULL, 0, 0, NULL, ?)");
				insert.setString(1, product[0]);
				insert.setString(2, product[1]);
				insert.setString(3, product[2]);
				insert.setString(4, product[3]);
				insert.setDouble(5, Double.parseDouble(product[5]));
				insert.setDouble(6, Double.parseDouble(product[6]));
				insert.setString(7, product[4]);
				insert.setString(8, product.length > 7 ? product[7] : null);
				insert.executeUpdate();
				insert.close();

				insert = connection.prepareStatement("INSERT INTO PRODUCTS_CAT (PRODUCT, CATORDER) VALUES (?, NULL)");
				insert.setString(1, product[0]);
				insert.executeUpdate();
				insert.close();
				connection.commit();
			} catch (Exception e) {
				connection.rollback();
				throw e;
			} finally {
				connection.setAutoCommit(oldAutoCommit);
			}
			return getProductInfoByCode(code);
		} catch (Exception e) {
			throw new BasicException("Cannot import barcode " + code, e);
		}
	}

	private String[] findProduct(String code, String productsPath) throws IOException {
		BufferedReader reader = utf8Reader(productsPath);
		try {
			String line;
			while ((line = reader.readLine()) != null) {
				String[] product = line.split("\t", -1);
				if (product.length >= 7 && (product[2].equals(code) || product[2].equals("0" + code)
						|| product[2].equals("00" + code))) {
					return product;
				}
			}
			return null;
		} finally {
			reader.close();
		}
	}

	private Map<String, String[]> readCategories(String categoriesPath) throws IOException {
		Map<String, String[]> categories = new HashMap<String, String[]>();
		BufferedReader reader = utf8Reader(categoriesPath);
		try {
			String line;
			while ((line = reader.readLine()) != null) {
				String[] category = line.split("\t", -1);
				if (category.length >= 2) {
					categories.put(category[0], category);
				}
			}
		} finally {
			reader.close();
		}
		return categories;
	}

	private BufferedReader utf8Reader(String path) throws IOException {
		return new BufferedReader(new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8));
	}

	private void ensureCategory(Connection connection, String categoryId, Map<String, String[]> categories)
			throws Exception {
		PreparedStatement find = connection.prepareStatement("SELECT ID FROM CATEGORIES WHERE ID = ?");
		find.setString(1, categoryId);
		ResultSet result = find.executeQuery();
		boolean exists = result.next();
		result.close();
		find.close();
		if (exists) {
			return;
		}

		String[] category = categories.get(categoryId);
		if (category == null) {
			throw new IOException("Category not found in catalog: " + categoryId);
		}
		String parentId = category.length > 2 && category[2].length() > 0 ? category[2] : null;
		if (parentId != null) {
			ensureCategory(connection, parentId, categories);
		}

		PreparedStatement insert = connection
				.prepareStatement("INSERT INTO CATEGORIES (ID, NAME, PARENTID, IMAGE) VALUES (?, ?, ?, NULL)");
		insert.setString(1, category[0]);
		insert.setString(2, category[1]);
		if (parentId == null) {
			insert.setNull(3, java.sql.Types.VARCHAR);
		} else {
			insert.setString(3, parentId);
		}
		insert.executeUpdate();
		insert.close();
	}

	public final ProductInfoExt getProductInfoByReference(String sReference) throws BasicException {
		return (ProductInfoExt) new PreparedSentence(s,
				"SELECT ID, REFERENCE, CODE, NAME, ISCOM, ISSCALE, PRICEBUY, PRICESELL, TAXCAT, CATEGORY, ATTRIBUTESET_ID, IMAGE, ATTRIBUTES "
						+ "FROM PRODUCTS WHERE REFERENCE = ?",
				SerializerWriteString.INSTANCE, ProductInfoExt.getSerializerRead()).find(sReference);
	}

	// Catalogo de productos
	public final List<CategoryInfo> getRootCategories() throws BasicException {
		return new PreparedSentence(s, "SELECT ID, NAME, IMAGE FROM CATEGORIES WHERE PARENTID IS NULL ORDER BY NAME",
				null, CategoryInfo.getSerializerRead()).list();
	}

	public final List<CategoryInfo> getSubcategories(String category) throws BasicException {
		return new PreparedSentence(s, "SELECT ID, NAME, IMAGE FROM CATEGORIES WHERE PARENTID = ? ORDER BY NAME",
				SerializerWriteString.INSTANCE, CategoryInfo.getSerializerRead()).list(category);
	}

	public List<ProductInfoExt> getProductCatalog(String category) throws BasicException {
		return new PreparedSentence(s,
				"SELECT P.ID, P.REFERENCE, P.CODE, P.NAME, P.ISCOM, P.ISSCALE, P.PRICEBUY, P.PRICESELL, P.TAXCAT, P.CATEGORY, P.ATTRIBUTESET_ID, P.IMAGE, P.ATTRIBUTES "
						+ "FROM PRODUCTS P, PRODUCTS_CAT O WHERE P.ID = O.PRODUCT AND P.CATEGORY = ? "
						+ "ORDER BY O.CATORDER, P.NAME",
				SerializerWriteString.INSTANCE, ProductInfoExt.getSerializerRead()).list(category);
	}

	public List<ProductInfoExt> getProductComments(String id) throws BasicException {
		return new PreparedSentence(s,
				"SELECT P.ID, P.REFERENCE, P.CODE, P.NAME, P.ISCOM, P.ISSCALE, P.PRICEBUY, P.PRICESELL, P.TAXCAT, P.CATEGORY, P.ATTRIBUTESET_ID, P.IMAGE, P.ATTRIBUTES "
						+ "FROM PRODUCTS P, PRODUCTS_CAT O, PRODUCTS_COM M WHERE P.ID = O.PRODUCT AND P.ID = M.PRODUCT2 AND M.PRODUCT = ? "
						+ "AND P.ISCOM = " + s.DB.TRUE() + " " + "ORDER BY O.CATORDER, P.NAME",
				SerializerWriteString.INSTANCE, ProductInfoExt.getSerializerRead()).list(id);
	}

	// Products list
	public final SentenceList getProductList() {
		return productListSentence("?(QBF_FILTER)");
	}

	// Products list
	public SentenceList getProductListNormal() {
		return productListSentence("P.ISCOM = " + s.DB.FALSE() + " AND ?(QBF_FILTER)");
	}

	// Auxiliar list for a filter
	public SentenceList getProductListAuxiliar() {
		return productListSentence("P.ISCOM = " + s.DB.TRUE() + " AND ?(QBF_FILTER)");
	}

	private SentenceList productListSentence(String where) {
		return new StaticSentence(s, new QBFBuilder(
				"SELECT P.ID, P.REFERENCE, P.CODE, P.NAME, P.ISCOM, P.ISSCALE, P.PRICEBUY, P.PRICESELL, P.TAXCAT, P.CATEGORY, P.ATTRIBUTESET_ID, P.IMAGE, P.ATTRIBUTES "
						+ "FROM PRODUCTS P LEFT JOIN CATEGORIES CAT ON P.CATEGORY = CAT.ID WHERE " + where
						+ " ORDER BY P.REFERENCE",
				new String[] { "P.NAME", "CAT.NAME", "P.BRAND", "P.CODE" }),
				new SerializerWriteBasic(new Datas[] { Datas.OBJECT, Datas.STRING, Datas.OBJECT, Datas.STRING,
						Datas.OBJECT, Datas.STRING, Datas.OBJECT, Datas.STRING }),
				ProductInfoExt.getSerializerRead());
	}

	// Tickets and Receipt list
	public SentenceList getTicketsList() {
		return new StaticSentence(s,
				new QBFBuilder("SELECT T.TICKETID, T.TICKETTYPE, R.DATENEW, P.NAME, C.NAME, SUM(PM.TOTAL) "
						+ "FROM RECEIPTS R JOIN TICKETS T ON R.ID = T.ID LEFT OUTER JOIN PAYMENTS PM ON R.ID = PM.RECEIPT LEFT OUTER JOIN CUSTOMERS C ON C.ID = T.CUSTOMER LEFT OUTER JOIN PEOPLE P ON T.PERSON = P.ID "
						+ "WHERE ?(QBF_FILTER) GROUP BY T.ID, T.TICKETID, T.TICKETTYPE, R.DATENEW, P.NAME, C.NAME ORDER BY R.DATENEW DESC, T.TICKETID",
						new String[] { "T.TICKETID", "T.TICKETTYPE", "PM.TOTAL", "R.DATENEW", "R.DATENEW", "P.NAME",
								"C.NAME" }),
				new SerializerWriteBasic(new Datas[] { Datas.OBJECT, Datas.INT, Datas.OBJECT, Datas.INT, Datas.OBJECT,
						Datas.DOUBLE, Datas.OBJECT, Datas.TIMESTAMP, Datas.OBJECT, Datas.TIMESTAMP, Datas.OBJECT,
						Datas.STRING, Datas.OBJECT, Datas.STRING }),
				new SerializerReadClass(FindTicketsInfo.class));
	}

	public List getRecentTickets(int tickettype, int max) throws BasicException {
		Object[] afilter = new Object[] { QBFCompareEnum.COMP_NONE, null, QBFCompareEnum.COMP_EQUALS,
				Integer.valueOf(tickettype), QBFCompareEnum.COMP_NONE, null, QBFCompareEnum.COMP_NONE, null,
				QBFCompareEnum.COMP_NONE, null, QBFCompareEnum.COMP_NONE, null, QBFCompareEnum.COMP_NONE, null };
		return getTicketsList().listPage(afilter, 0, max);
	}

	// User list
	public final SentenceList getUserList() {
		return new StaticSentence(s, "SELECT ID, NAME FROM PEOPLE ORDER BY NAME", null, new SerializerRead() {
			public Object readValues(DataRead dr) throws BasicException {
				return new TaxCategoryInfo(dr.getString(1), dr.getString(2));
			}
		});
	}

	// Listados para combo
	public final SentenceList getTaxList() {
		return new StaticSentence(s,
				"SELECT ID, NAME, CATEGORY, VALIDFROM, CUSTCATEGORY, PARENTID, RATE, RATECASCADE, RATEORDER FROM TAXES ORDER BY NAME",
				null, new SerializerRead() {
					public Object readValues(DataRead dr) throws BasicException {
						return new TaxInfo(dr.getString(1), dr.getString(2), dr.getString(3), dr.getTimestamp(4),
								dr.getString(5), dr.getString(6), dr.getDouble(7).doubleValue(),
								dr.getBoolean(8).booleanValue(), dr.getInt(9));
					}
				});
	}

	public final SentenceList getCategoriesList() {
		return new StaticSentence(s, "SELECT ID, NAME, IMAGE FROM CATEGORIES ORDER BY NAME", null,
				CategoryInfo.getSerializerRead());
	}

	public final SentenceList getBrandsList() {
		return new StaticSentence(s, "SELECT DISTINCT BRAND FROM PRODUCTS WHERE BRAND IS NOT NULL ORDER BY BRAND", null,
				SerializerReadString.INSTANCE);
	}

	public final SentenceList getTaxCustCategoriesList() {
		return new StaticSentence(s, "SELECT ID, NAME FROM TAXCUSTCATEGORIES ORDER BY NAME", null,
				new SerializerRead() {
					public Object readValues(DataRead dr) throws BasicException {
						return new TaxCustCategoryInfo(dr.getString(1), dr.getString(2));
					}
				});
	}

	public final SentenceList getTaxCategoriesList() {
		return new StaticSentence(s, "SELECT ID, NAME FROM TAXCATEGORIES ORDER BY NAME", null, new SerializerRead() {
			public Object readValues(DataRead dr) throws BasicException {
				return new TaxCategoryInfo(dr.getString(1), dr.getString(2));
			}
		});
	}

	public final SentenceList getAttributeSetList() {
		return new StaticSentence(s, "SELECT ID, NAME FROM ATTRIBUTESET ORDER BY NAME", null, new SerializerRead() {
			public Object readValues(DataRead dr) throws BasicException {
				return new AttributeSetInfo(dr.getString(1), dr.getString(2));
			}
		});
	}

	public final SentenceList getLocationsList() {
		return new StaticSentence(s, "SELECT ID, NAME, ADDRESS FROM LOCATIONS ORDER BY NAME", null,
				new SerializerReadClass(LocationInfo.class));
	}

	public CustomerInfoExt findCustomerExt(String card) throws BasicException {
		return (CustomerInfoExt) new PreparedSentence(s,
				"SELECT ID, TAXID, SEARCHKEY, NAME, CARD, TAXCATEGORY, NOTES, MAXDEBT, VISIBLE, CURDATE, CURDEBT"
						+ ", FIRSTNAME, LASTNAME, EMAIL, PHONE, PHONE2, FAX"
						+ ", ADDRESS, ADDRESS2, POSTAL, CITY, REGION, COUNTRY"
						+ " FROM CUSTOMERS WHERE CARD = ? AND VISIBLE = " + s.DB.TRUE(),
				SerializerWriteString.INSTANCE, new CustomerExtRead()).find(card);
	}

	public CustomerInfoExt loadCustomerExt(String id) throws BasicException {
		return (CustomerInfoExt) new PreparedSentence(s,
				"SELECT ID, TAXID, SEARCHKEY, NAME, CARD, TAXCATEGORY, NOTES, MAXDEBT, VISIBLE, CURDATE, CURDEBT"
						+ ", FIRSTNAME, LASTNAME, EMAIL, PHONE, PHONE2, FAX"
						+ ", ADDRESS, ADDRESS2, POSTAL, CITY, REGION, COUNTRY" + " FROM CUSTOMERS WHERE ID = ?",
				SerializerWriteString.INSTANCE, new CustomerExtRead()).find(id);
	}

	public final boolean isCashActive(String id) throws BasicException {

		return new PreparedSentence(s, "SELECT MONEY FROM CLOSEDCASH WHERE DATEEND IS NULL AND MONEY = ?",
				SerializerWriteString.INSTANCE, SerializerReadString.INSTANCE).find(id) != null;
	}

	public final TicketInfo loadTicket(final int tickettype, final int ticketid) throws BasicException {
		TicketInfo ticket = (TicketInfo) new PreparedSentence(s,
				"SELECT T.ID, T.TICKETTYPE, T.TICKETID, R.DATENEW, R.MONEY, R.ATTRIBUTES, P.ID, P.NAME, T.CUSTOMER FROM RECEIPTS R JOIN TICKETS T ON R.ID = T.ID LEFT OUTER JOIN PEOPLE P ON T.PERSON = P.ID WHERE T.TICKETTYPE = ? AND T.TICKETID = ?",
				SerializerWriteParams.INSTANCE, new SerializerReadClass(TicketInfo.class)).find(new DataParams() {
					public void writeValues() throws BasicException {
						setInt(1, tickettype);
						setInt(2, ticketid);
					}
				});
		if (ticket != null) {

			String customerid = ticket.getCustomerId();
			ticket.setCustomer(customerid == null ? null : loadCustomerExt(customerid));

			ticket.setLines(new PreparedSentence(s,
					"SELECT L.TICKET, L.LINE, L.PRODUCT, L.ATTRIBUTESETINSTANCE_ID, L.UNITS, L.PRICE, T.ID, T.NAME, T.CATEGORY, T.VALIDFROM, T.CUSTCATEGORY, T.PARENTID, T.RATE, T.RATECASCADE, T.RATEORDER, L.ATTRIBUTES "
							+ "FROM TICKETLINES L, TAXES T WHERE L.TAXID = T.ID AND L.TICKET = ? ORDER BY L.LINE",
					SerializerWriteString.INSTANCE, new SerializerReadClass(TicketLineInfo.class))
							.list(ticket.getId()));
			ticket.setPayments(new PreparedSentence(s,
					"SELECT PM.PAYMENT, PM.TOTAL, PM.TRANSID, V.BALANCE "
							+ "FROM PAYMENTS PM LEFT JOIN GIFTVOUCHERS V ON PM.TRANSID = V.CODE WHERE PM.RECEIPT = ?",
					SerializerWriteString.INSTANCE, new SerializerReadClass(PaymentInfoTicket.class))
							.list(ticket.getId()));
		}
		return ticket;
	}

	public final void saveTicket(final TicketInfo ticket, final String location) throws BasicException {

		Transaction t = new Transaction(s) {
			public Object transact() throws BasicException {

				// Set Receipt Id
				if (ticket.getTicketId() == 0) {
					switch (ticket.getTicketType()) {
					case TicketInfo.RECEIPT_NORMAL:
						ticket.setTicketId(getNextTicketIndex().intValue());
						break;
					case TicketInfo.RECEIPT_REFUND:
						ticket.setTicketId(getNextTicketRefundIndex().intValue());
						break;
					case TicketInfo.RECEIPT_PAYMENT:
						ticket.setTicketId(getNextTicketPaymentIndex().intValue());
						break;
					default:
						throw new BasicException();
					}
				}

				// new receipt
				new PreparedSentence(s, "INSERT INTO RECEIPTS (ID, MONEY, DATENEW, ATTRIBUTES) VALUES (?, ?, ?, ?)",
						SerializerWriteParams.INSTANCE).exec(new DataParams() {
							public void writeValues() throws BasicException {
								setString(1, ticket.getId());
								setString(2, ticket.getActiveCash());
								setTimestamp(3, ticket.getDate());
								try {
									ByteArrayOutputStream o = new ByteArrayOutputStream();
									ticket.getProperties().storeToXML(o, AppLocal.APP_NAME, "UTF-8");
									setBytes(4, o.toByteArray());
								} catch (IOException e) {
									setBytes(4, null);
								}
							}
						});

				// new ticket
				new PreparedSentence(s,
						"INSERT INTO TICKETS (ID, TICKETTYPE, TICKETID, PERSON, CUSTOMER) VALUES (?, ?, ?, ?, ?)",
						SerializerWriteParams.INSTANCE).exec(new DataParams() {
							public void writeValues() throws BasicException {
								setString(1, ticket.getId());
								setInt(2, ticket.getTicketType());
								setInt(3, ticket.getTicketId());
								setString(4, ticket.getUser().getId());
								setString(5, ticket.getCustomerId());
							}
						});

				SentenceExec ticketlineinsert = new PreparedSentence(s,
						"INSERT INTO TICKETLINES (TICKET, LINE, PRODUCT, ATTRIBUTESETINSTANCE_ID, UNITS, PRICE, TAXID, ATTRIBUTES) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
						SerializerWriteBuilder.INSTANCE);

				for (TicketLineInfo l : ticket.getLines()) {
					if (l.isGiftVoucher() && l.getMultiply() < 0.0) {
						throw new BasicException(AppLocal.getIntString("message.vouchernorefund"));
					}
					if (l.isGiftVoucher() && l.getMultiply() > 0.0) {
						int voucherCount = (int) Math.round(l.getMultiply());
						if (voucherCount < 1 || Math.abs(l.getMultiply() - voucherCount) > 0.000001) {
							throw new BasicException(AppLocal.getIntString("message.voucherwholeunits"));
						}
						if (l.getPriceTax() <= 0.0) {
							throw new BasicException(AppLocal.getIntString("message.voucherpositivevalue"));
						}

						StringBuilder codes = new StringBuilder();
						for (int voucherIndex = 0; voucherIndex < voucherCount; voucherIndex++) {
							String code = createGiftVoucherCode();
							if (codes.length() > 0) {
								codes.append(", ");
							}
							codes.append(code);
							new PreparedSentence(s,
									"INSERT INTO GIFTVOUCHERS (ID, CODE, INITIALVALUE, BALANCE, ISSUEDRECEIPT, ISSUEDLINE, ISSUEDDATE) VALUES (?, ?, ?, ?, ?, ?, ?)",
									new SerializerWriteBasic(new Datas[] { Datas.STRING, Datas.STRING, Datas.DOUBLE,
											Datas.DOUBLE, Datas.STRING, Datas.INT, Datas.TIMESTAMP }))
													.exec(UUID.randomUUID().toString(), code, new Double(l.getPriceTax()),
															new Double(l.getPriceTax()), ticket.getId(),
															new Integer(l.getTicketLine()), ticket.getDate());
						}
						l.setProperty("giftvoucher.codes", codes.toString());
					}
					ticketlineinsert.exec(l);
					if (l.getProductID() != null) {
						// update the stock
						getStockDiaryInsert().exec(new Object[] { UUID.randomUUID().toString(), ticket.getDate(),
								l.getMultiply() < 0.0 ? MovementReason.IN_REFUND.getKey()
										: MovementReason.OUT_SALE.getKey(),
								location, l.getProductID(), l.getProductAttSetInstId(), new Double(-l.getMultiply()),
								new Double(l.getPrice()) });
					}
				}

				SentenceExec paymentinsert = new PreparedSentence(s,
						"INSERT INTO PAYMENTS (ID, RECEIPT, PAYMENT, TOTAL, TRANSID, RETURNMSG) VALUES (?, ?, ?, ?, ?, ?)",
						SerializerWriteParams.INSTANCE);
				for (final PaymentInfo p : ticket.getPayments()) {
					if ("paperin".equals(p.getName())) {
						if (p.getTransactionID() == null) {
							throw new BasicException(AppLocal.getIntString("message.vouchercoderequired"));
						}
						int redeemed = new PreparedSentence(s,
								"UPDATE GIFTVOUCHERS SET BALANCE = BALANCE - ? WHERE CODE = ? AND BALANCE >= ?",
								new SerializerWriteBasic(new Datas[] { Datas.DOUBLE, Datas.STRING, Datas.DOUBLE }))
										.exec(new Double(p.getTotal()), p.getTransactionID(), new Double(p.getTotal()));
						if (redeemed != 1) {
							throw new BasicException(AppLocal.getIntString("message.voucherbalancechanged"));
						}
					}
					paymentinsert.exec(new DataParams() {
						public void writeValues() throws BasicException {
							setString(1, UUID.randomUUID().toString());
							setString(2, ticket.getId());
							setString(3, p.getName());
							setDouble(4, p.getTotal());
							setString(5, p.getTransactionID());
							setBytes(6, (byte[]) Formats.BYTEA.parseValue(ticket.getReturnMessage()));
						}
					});

					if ("debt".equals(p.getName()) || "debtpaid".equals(p.getName())) {

						// udate customer fields...
						ticket.getCustomer().updateCurDebt(p.getTotal(), ticket.getDate());

						// save customer fields...
						getDebtUpdate().exec(new DataParams() {
							public void writeValues() throws BasicException {
								setDouble(1, ticket.getCustomer().getCurdebt());
								setTimestamp(2, ticket.getCustomer().getCurdate());
								setString(3, ticket.getCustomer().getId());
							}
						});
					}
				}

				SentenceExec taxlinesinsert = new PreparedSentence(s,
						"INSERT INTO TAXLINES (ID, RECEIPT, TAXID, BASE, AMOUNT)  VALUES (?, ?, ?, ?, ?)",
						SerializerWriteParams.INSTANCE);
				if (ticket.getTaxes() != null) {
					for (final TicketTaxInfo tickettax : ticket.getTaxes()) {
						taxlinesinsert.exec(new DataParams() {
							public void writeValues() throws BasicException {
								setString(1, UUID.randomUUID().toString());
								setString(2, ticket.getId());
								setString(3, tickettax.getTaxInfo().getId());
								setDouble(4, tickettax.getSubTotal());
								setDouble(5, tickettax.getTax());
							}
						});
					}
				}

				return null;
			}
		};
		try {
			t.execute();
		} catch (BasicException e) {
			for (TicketLineInfo line : ticket.getLines()) {
				line.removeProperty("giftvoucher.codes");
			}
			throw e;
		}
	}

	private String createGiftVoucherCode() {
		String raw = UUID.randomUUID().toString().replace("-", "").toUpperCase();
		return raw.substring(0, 4) + "-" + raw.substring(4, 8) + "-" + raw.substring(8, 12);
	}

	public final void deleteTicket(final TicketInfo ticket, final String location) throws BasicException {

		Transaction t = new Transaction(s) {
			public Object transact() throws BasicException {

				Integer usedIssuedVouchers = (Integer) new PreparedSentence(s,
						"SELECT COUNT(*) FROM GIFTVOUCHERS WHERE ISSUEDRECEIPT = ? AND BALANCE <> INITIALVALUE",
						SerializerWriteString.INSTANCE, SerializerReadInteger.INSTANCE).find(ticket.getId());
				if (usedIssuedVouchers != null && usedIssuedVouchers.intValue() > 0) {
					throw new BasicException(AppLocal.getIntString("message.voucherissuedused"));
				}

				// update the inventory
				Date d = new Date();
				for (int i = 0; i < ticket.getLinesCount(); i++) {
					if (ticket.getLine(i).getProductID() != null) {
						// Hay que actualizar el stock si el hay producto
						getStockDiaryInsert().exec(new Object[] { UUID.randomUUID().toString(), d,
								ticket.getLine(i).getMultiply() >= 0.0 ? MovementReason.IN_REFUND.getKey()
										: MovementReason.OUT_SALE.getKey(),
								location, ticket.getLine(i).getProductID(), ticket.getLine(i).getProductAttSetInstId(),
								new Double(ticket.getLine(i).getMultiply()),
								new Double(ticket.getLine(i).getPrice()) });
					}
				}

				// update customer debts
				for (PaymentInfo p : ticket.getPayments()) {
					if ("debt".equals(p.getName()) || "debtpaid".equals(p.getName())) {

						// udate customer fields...
						ticket.getCustomer().updateCurDebt(-p.getTotal(), ticket.getDate());

						// save customer fields...
						getDebtUpdate().exec(new DataParams() {
							public void writeValues() throws BasicException {
								setDouble(1, ticket.getCustomer().getCurdebt());
								setTimestamp(2, ticket.getCustomer().getCurdate());
								setString(3, ticket.getCustomer().getId());
							}
						});
					}
				}

				for (PaymentInfo p : ticket.getPayments()) {
					if ("paperin".equals(p.getName()) && p.getTransactionID() != null) {
						new PreparedSentence(s, "UPDATE GIFTVOUCHERS SET BALANCE = BALANCE + ? WHERE CODE = ?",
								new SerializerWriteBasic(new Datas[] { Datas.DOUBLE, Datas.STRING }))
										.exec(new Double(RoundUtils.round(p.getTotal())), p.getTransactionID());
					}
				}
				new StaticSentence(s, "DELETE FROM GIFTVOUCHERS WHERE ISSUEDRECEIPT = ?",
						SerializerWriteString.INSTANCE).exec(ticket.getId());

				// and delete the receipt
				new StaticSentence(s, "DELETE FROM TAXLINES WHERE RECEIPT = ?", SerializerWriteString.INSTANCE)
						.exec(ticket.getId());
				new StaticSentence(s, "DELETE FROM PAYMENTS WHERE RECEIPT = ?", SerializerWriteString.INSTANCE)
						.exec(ticket.getId());
				new StaticSentence(s, "DELETE FROM TICKETLINES WHERE TICKET = ?", SerializerWriteString.INSTANCE)
						.exec(ticket.getId());
				new StaticSentence(s, "DELETE FROM TICKETS WHERE ID = ?", SerializerWriteString.INSTANCE)
						.exec(ticket.getId());
				new StaticSentence(s, "DELETE FROM RECEIPTS WHERE ID = ?", SerializerWriteString.INSTANCE)
						.exec(ticket.getId());
				return null;
			}
		};
		t.execute();
	}

	public final Integer getNextTicketIndex() throws BasicException {
		return (Integer) s.DB.getSequenceSentence(s, "TICKETSNUM").find();
	}

	public final Integer getNextTicketRefundIndex() throws BasicException {
		return (Integer) s.DB.getSequenceSentence(s, "TICKETSNUM_REFUND").find();
	}

	public final Integer getNextTicketPaymentIndex() throws BasicException {
		return (Integer) s.DB.getSequenceSentence(s, "TICKETSNUM_PAYMENT").find();
	}

	public final SentenceFind getProductImage() {
		return new PreparedSentence(s, "SELECT IMAGE FROM PRODUCTS WHERE ID = ?", SerializerWriteString.INSTANCE,
				new SerializerRead() {
					@Override
					public Object readValues(DataRead dr) throws BasicException {
						return ImageUtils.readImage(dr.getBytes(1));
					}
				});
	}

	public final SentenceList getProductCatQBF() {
		return new StaticSentence(s, new QBFBuilder(
				"SELECT PRODUCTS.ID, PRODUCTS.REFERENCE, PRODUCTS.CODE, PRODUCTS.NAME, PRODUCTS.ISCOM, PRODUCTS.ISSCALE, PRODUCTS.PRICEBUY, PRODUCTS.PRICESELL, PRODUCTS.CATEGORY, PRODUCTS.TAXCAT, PRODUCTS.ATTRIBUTESET_ID, "
						+ s.DB.CHAR_NULL()
						+ ", PRODUCTS.STOCKCOST, PRODUCTS.STOCKVOLUME, CASE WHEN C.PRODUCT IS NULL THEN " + s.DB.FALSE()
						+ " ELSE " + s.DB.TRUE() + " END, C.CATORDER, PRODUCTS.ATTRIBUTES "
						+ "FROM PRODUCTS, PRODUCTS_CAT C " + "WHERE ?(QBF_FILTER) AND PRODUCTS.ID = C.PRODUCT "
						+ "ORDER BY PRODUCTS.REFERENCE",
				new String[] { "PRODUCTS.NAME", "PRODUCTS.PRICEBUY", "PRODUCTS.PRICESELL", "PRODUCTS.CATEGORY",
						"PRODUCTS.CODE", "PRODUCTS.BRAND" }),
				new SerializerWriteBasic(new Datas[] { Datas.OBJECT, Datas.STRING, Datas.OBJECT, Datas.DOUBLE,
						Datas.OBJECT, Datas.DOUBLE, Datas.OBJECT, Datas.STRING, Datas.OBJECT, Datas.STRING,
						Datas.OBJECT, Datas.STRING }),
				productsRow.getSerializerRead());
	}

	public final SentenceExec getProductCatInsert() {
		return new SentenceExecTransaction(s) {
			public int execInTransaction(Object params) throws BasicException {
				Object[] values = (Object[]) params;
				int i = new PreparedSentence(s,
						"INSERT INTO PRODUCTS (ID, REFERENCE, CODE, NAME, ISCOM, ISSCALE, PRICEBUY, PRICESELL, CATEGORY, TAXCAT, ATTRIBUTESET_ID, IMAGE, STOCKCOST, STOCKVOLUME, ATTRIBUTES) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
						new SerializerWriteBasicExt(productsRow.getDatas(),
								new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 16 })).exec(params);
				if (i > 0) {
					applyStockLevel(values);
				}
				if (i > 0 && ((Boolean) values[14]).booleanValue()) {
					return new PreparedSentence(s, "INSERT INTO PRODUCTS_CAT (PRODUCT, CATORDER) VALUES (?, ?)",
							new SerializerWriteBasicExt(productsRow.getDatas(), new int[] { 0, 15 })).exec(params);
				} else {
					return i;
				}
			}
		};
	}

	public final SentenceExec getProductCatUpdate() {
		return new SentenceExecTransaction(s) {
			public int execInTransaction(Object params) throws BasicException {
				Object[] values = (Object[]) params;
				int i = new PreparedSentence(s,
						"UPDATE PRODUCTS SET ID = ?, REFERENCE = ?, CODE = ?, NAME = ?, ISCOM = ?, ISSCALE = ?, PRICEBUY = ?, PRICESELL = ?, CATEGORY = ?, TAXCAT = ?, ATTRIBUTESET_ID = ?, IMAGE = ?, STOCKCOST = ?, STOCKVOLUME = ?, ATTRIBUTES = ? WHERE ID = ?",
						new SerializerWriteBasicExt(productsRow.getDatas(),
								new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 16, 0 })).exec(params);
				if (i > 0) {
					applyStockLevel(values);
					if (((Boolean) values[14]).booleanValue()) {
						if (new PreparedSentence(s, "UPDATE PRODUCTS_CAT SET CATORDER = ? WHERE PRODUCT = ?",
								new SerializerWriteBasicExt(productsRow.getDatas(), new int[] { 15, 0 }))
										.exec(params) == 0) {
							new PreparedSentence(s, "INSERT INTO PRODUCTS_CAT (PRODUCT, CATORDER) VALUES (?, ?)",
									new SerializerWriteBasicExt(productsRow.getDatas(), new int[] { 0, 15 }))
											.exec(params);
						}
					} else {
						new PreparedSentence(s, "DELETE FROM PRODUCTS_CAT WHERE PRODUCT = ?",
								new SerializerWriteBasicExt(productsRow.getDatas(), new int[] { 0 })).exec(params);
					}
				}
				return i;
			}
		};
	}

	public final SentenceExec getProductCatDelete() {
		return new SentenceExecTransaction(s) {
			public int execInTransaction(Object params) throws BasicException {
				new PreparedSentence(s, "DELETE FROM PRODUCTS_CAT WHERE PRODUCT = ?",
						new SerializerWriteBasicExt(productsRow.getDatas(), new int[] { 0 })).exec(params);
				new PreparedSentence(s, "DELETE FROM BARCODE_TABLE WHERE PID = ?",
						new SerializerWriteBasicExt(productsRow.getDatas(), new int[] { 0 })).exec(params);
				int t = new PreparedSentence(s, "DELETE FROM PRODUCTS WHERE ID = ?",
						new SerializerWriteBasicExt(productsRow.getDatas(), new int[] { 0 })).exec(params);
				return t;
			}
		};
	}

	public final SentenceExec getDebtUpdate() {

		return new PreparedSentence(s, "UPDATE CUSTOMERS SET CURDEBT = ?, CURDATE = ? WHERE ID = ?",
				SerializerWriteParams.INSTANCE);
	}

	// The products editor appends the stock the user typed, its location, and how
	// much of that stock comes from units added rather than a correction
	private void applyStockLevel(Object[] values) throws BasicException {

		Double units = (Double) values[17];
		String location = (String) values[18];
		String product = (String) values[0];
		Double added = (Double) values[19];
		Double pricebuy = (Double) values[6];

		if (units != null) {
			double diff = units.doubleValue() - (added == null ? 0.0 : added.doubleValue())
					- findProductStock(location, product, null);
			if (diff != 0.0) {
				getStockDiaryInsert().exec(new Object[] { UUID.randomUUID().toString(), new Date(),
						diff > 0.0 ? MovementReason.IN_MOVEMENT.getKey() : MovementReason.OUT_MOVEMENT.getKey(),
						location, product, null, new Double(diff), pricebuy == null ? new Double(0.0) : pricebuy });
			}
		}

		if (added != null) {
			addProductStock(location, product, added.doubleValue(), pricebuy);
		}
	}

	public final void addProductStock(String location, String product, double units, Double pricebuy)
			throws BasicException {
		if (units <= 0.0) {
			throw new BasicException("Units to add must be greater than zero.");
		}

		getStockDiaryInsert()
				.exec(new Object[] { UUID.randomUUID().toString(), new Date(), MovementReason.IN_PURCHASE.getKey(),
						location, product, null, new Double(units), pricebuy == null ? new Double(0.0) : pricebuy });
	}

	public final SentenceExec getStockDiaryInsert() {
		return new SentenceExecTransaction(s) {
			public int execInTransaction(Object params) throws BasicException {
				int updateresult = ((Object[]) params)[5] == null // si ATTRIBUTESETINSTANCE_ID is null
						? new PreparedSentence(s,
								"UPDATE STOCKCURRENT SET UNITS = (UNITS + ?) WHERE LOCATION = ? AND PRODUCT = ? AND ATTRIBUTESETINSTANCE_ID IS NULL",
								new SerializerWriteBasicExt(stockdiaryDatas, new int[] { 6, 3, 4 })).exec(params)
						: new PreparedSentence(s,
								"UPDATE STOCKCURRENT SET UNITS = (UNITS + ?) WHERE LOCATION = ? AND PRODUCT = ? AND ATTRIBUTESETINSTANCE_ID = ?",
								new SerializerWriteBasicExt(stockdiaryDatas, new int[] { 6, 3, 4, 5 })).exec(params);

				if (updateresult == 0) {
					new PreparedSentence(s,
							"INSERT INTO STOCKCURRENT (LOCATION, PRODUCT, ATTRIBUTESETINSTANCE_ID, UNITS) VALUES (?, ?, ?, ?)",
							new SerializerWriteBasicExt(stockdiaryDatas, new int[] { 3, 4, 5, 6 })).exec(params);
				}
				return new PreparedSentence(s,
						"INSERT INTO STOCKDIARY (ID, DATENEW, REASON, LOCATION, PRODUCT, ATTRIBUTESETINSTANCE_ID, UNITS, PRICE) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
						new SerializerWriteBasicExt(stockdiaryDatas, new int[] { 0, 1, 2, 3, 4, 5, 6, 7 }))
								.exec(params);
			}
		};
	}

	public final SentenceExec getStockDiaryDelete() {
		return new SentenceExecTransaction(s) {
			public int execInTransaction(Object params) throws BasicException {
				int updateresult = ((Object[]) params)[5] == null // if ATTRIBUTESETINSTANCE_ID is null
						? new PreparedSentence(s,
								"UPDATE STOCKCURRENT SET UNITS = (UNITS - ?) WHERE LOCATION = ? AND PRODUCT = ? AND ATTRIBUTESETINSTANCE_ID IS NULL",
								new SerializerWriteBasicExt(stockdiaryDatas, new int[] { 6, 3, 4 })).exec(params)
						: new PreparedSentence(s,
								"UPDATE STOCKCURRENT SET UNITS = (UNITS - ?) WHERE LOCATION = ? AND PRODUCT = ? AND ATTRIBUTESETINSTANCE_ID = ?",
								new SerializerWriteBasicExt(stockdiaryDatas, new int[] { 6, 3, 4, 5 })).exec(params);

				if (updateresult == 0) {
					new PreparedSentence(s,
							"INSERT INTO STOCKCURRENT (LOCATION, PRODUCT, ATTRIBUTESETINSTANCE_ID, UNITS) VALUES (?, ?, ?, -(?))",
							new SerializerWriteBasicExt(stockdiaryDatas, new int[] { 3, 4, 5, 6 })).exec(params);
				}
				return new PreparedSentence(s, "DELETE FROM STOCKDIARY WHERE ID = ?",
						new SerializerWriteBasicExt(stockdiaryDatas, new int[] { 0 })).exec(params);
			}
		};
	}

	public final SentenceExec getPaymentMovementInsert() {
		return new SentenceExecTransaction(s) {
			public int execInTransaction(Object params) throws BasicException {
				new PreparedSentence(s, "INSERT INTO RECEIPTS (ID, MONEY, DATENEW) VALUES (?, ?, ?)",
						new SerializerWriteBasicExt(paymenttabledatas, new int[] { 0, 1, 2 })).exec(params);
				return new PreparedSentence(s, "INSERT INTO PAYMENTS (ID, RECEIPT, PAYMENT, TOTAL) VALUES (?, ?, ?, ?)",
						new SerializerWriteBasicExt(paymenttabledatas, new int[] { 3, 0, 4, 5 })).exec(params);
			}
		};
	}

	public final SentenceExec getPaymentMovementDelete() {
		return new SentenceExecTransaction(s) {
			public int execInTransaction(Object params) throws BasicException {
				new PreparedSentence(s, "DELETE FROM PAYMENTS WHERE ID = ?",
						new SerializerWriteBasicExt(paymenttabledatas, new int[] { 3 })).exec(params);
				return new PreparedSentence(s, "DELETE FROM RECEIPTS WHERE ID = ?",
						new SerializerWriteBasicExt(paymenttabledatas, new int[] { 0 })).exec(params);
			}
		};
	}

	public final double findProductStock(String warehouse, String id, String attsetinstid) throws BasicException {

		PreparedSentence p = attsetinstid == null ? new PreparedSentence(s,
				"SELECT UNITS FROM STOCKCURRENT WHERE LOCATION = ? AND PRODUCT = ? AND ATTRIBUTESETINSTANCE_ID IS NULL",
				new SerializerWriteBasic(Datas.STRING, Datas.STRING), SerializerReadDouble.INSTANCE)
				: new PreparedSentence(s,
						"SELECT UNITS FROM STOCKCURRENT WHERE LOCATION = ? AND PRODUCT = ? AND ATTRIBUTESETINSTANCE_ID = ?",
						new SerializerWriteBasic(Datas.STRING, Datas.STRING, Datas.STRING),
						SerializerReadDouble.INSTANCE);

		Double d = (Double) p.find(warehouse, id, attsetinstid);
		return d == null ? 0.0 : d.doubleValue();
	}

	public final SentenceExec getCatalogCategoryAdd() {
		return new StaticSentence(s, "INSERT INTO PRODUCTS_CAT(PRODUCT, CATORDER) SELECT ID, " + s.DB.INTEGER_NULL()
				+ " FROM PRODUCTS WHERE CATEGORY = ?", SerializerWriteString.INSTANCE);
	}

	public final SentenceExec getCatalogCategoryDel() {
		return new StaticSentence(s,
				"DELETE FROM PRODUCTS_CAT WHERE PRODUCT = ANY (SELECT ID FROM PRODUCTS WHERE CATEGORY = ?)",
				SerializerWriteString.INSTANCE);
	}

	public final TableDefinition getTableCategories() {
		return new TableDefinition(s, "CATEGORIES", new String[] { "ID", "NAME", "PARENTID", "IMAGE" },
				new String[] { "ID", AppLocal.getIntString("Label.Name"), "", AppLocal.getIntString("label.image") },
				new Datas[] { Datas.STRING, Datas.STRING, Datas.STRING, Datas.IMAGE },
				new Formats[] { Formats.STRING, Formats.STRING, Formats.STRING, Formats.NULL }, new int[] { 0 });
	}

	public final TableDefinition getTableTaxes() {
		return new TableDefinition(s, "TAXES",
				new String[] { "ID", "NAME", "CATEGORY", "VALIDFROM", "CUSTCATEGORY", "PARENTID", "RATE", "RATECASCADE",
						"RATEORDER" },
				new String[] { "ID", AppLocal.getIntString("Label.Name"), AppLocal.getIntString("label.taxcategory"),
						AppLocal.getIntString("Label.ValidFrom"), AppLocal.getIntString("label.custtaxcategory"),
						AppLocal.getIntString("label.taxparent"), AppLocal.getIntString("label.dutyrate"),
						AppLocal.getIntString("label.cascade"), AppLocal.getIntString("label.order") },
				new Datas[] { Datas.STRING, Datas.STRING, Datas.STRING, Datas.TIMESTAMP, Datas.STRING, Datas.STRING,
						Datas.DOUBLE, Datas.BOOLEAN, Datas.INT },
				new Formats[] { Formats.STRING, Formats.STRING, Formats.STRING, Formats.TIMESTAMP, Formats.STRING,
						Formats.STRING, Formats.PERCENT, Formats.BOOLEAN, Formats.INT },
				new int[] { 0 });
	}

	public final TableDefinition getTableTaxCustCategories() {
		return new TableDefinition(s, "TAXCUSTCATEGORIES", new String[] { "ID", "NAME" },
				new String[] { "ID", AppLocal.getIntString("Label.Name") }, new Datas[] { Datas.STRING, Datas.STRING },
				new Formats[] { Formats.STRING, Formats.STRING }, new int[] { 0 });
	}

	public final TableDefinition getTableTaxCategories() {
		return new TableDefinition(s, "TAXCATEGORIES", new String[] { "ID", "NAME" },
				new String[] { "ID", AppLocal.getIntString("Label.Name") }, new Datas[] { Datas.STRING, Datas.STRING },
				new Formats[] { Formats.STRING, Formats.STRING }, new int[] { 0 });
	}

	public final TableDefinition getTableLocations() {
		return new TableDefinition(s, "LOCATIONS", new String[] { "ID", "NAME", "ADDRESS" },
				new String[] { "ID", AppLocal.getIntString("label.locationname"),
						AppLocal.getIntString("label.locationaddress") },
				new Datas[] { Datas.STRING, Datas.STRING, Datas.STRING },
				new Formats[] { Formats.STRING, Formats.STRING, Formats.STRING }, new int[] { 0 });
	}

	protected static class CustomerExtRead implements SerializerRead {
		public Object readValues(DataRead dr) throws BasicException {
			CustomerInfoExt c = new CustomerInfoExt(dr.getString(1));
			c.setTaxid(dr.getString(2));
			c.setSearchkey(dr.getString(3));
			c.setName(dr.getString(4));
			c.setCard(dr.getString(5));
			c.setTaxCustomerID(dr.getString(6));
			c.setNotes(dr.getString(7));
			c.setMaxdebt(dr.getDouble(8));
			c.setVisible(dr.getBoolean(9).booleanValue());
			c.setCurdate(dr.getTimestamp(10));
			c.setCurdebt(dr.getDouble(11));
			c.setFirstname(dr.getString(12));
			c.setLastname(dr.getString(13));
			c.setEmail(dr.getString(14));
			c.setPhone(dr.getString(15));
			c.setPhone2(dr.getString(16));
			c.setFax(dr.getString(17));
			c.setAddress(dr.getString(18));
			c.setAddress2(dr.getString(19));
			c.setPostal(dr.getString(20));
			c.setCity(dr.getString(21));
			c.setRegion(dr.getString(22));
			c.setCountry(dr.getString(23));

			return c;
		}
	}
}
