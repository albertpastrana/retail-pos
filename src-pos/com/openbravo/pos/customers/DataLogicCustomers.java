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

package com.openbravo.pos.customers;

import com.openbravo.basic.BasicException;
import com.openbravo.data.loader.DataParams;
import com.openbravo.data.loader.DataRead;
import com.openbravo.data.loader.Datas;
import com.openbravo.data.loader.PreparedSentence;
import com.openbravo.data.loader.SerializerRead;
import com.openbravo.data.loader.SerializerWriteParams;
import com.openbravo.data.loader.Session;
import com.openbravo.data.loader.TableDefinition;
import com.openbravo.format.Formats;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.BeanFactoryDataSingle;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author adrianromero
 */
public class DataLogicCustomers extends BeanFactoryDataSingle {

	protected Session s;
	private TableDefinition tcustomers;

	public void init(Session s) {

		this.s = s;
		tcustomers = new TableDefinition(s, "CUSTOMERS",
				new String[]{"ID", "TAXID", "SEARCHKEY", "NAME", "NOTES", "VISIBLE", "CARD", "MAXDEBT", "CURDATE",
						"CURDEBT", "FIRSTNAME", "LASTNAME", "EMAIL", "PHONE", "PHONE2", "FAX", "ADDRESS", "ADDRESS2",
						"POSTAL", "CITY", "REGION", "COUNTRY", "TAXCATEGORY"},
				new String[]{"ID", AppLocal.getIntString("label.taxid"), AppLocal.getIntString("label.searchkey"),
						AppLocal.getIntString("label.name"), AppLocal.getIntString("label.notes"), "VISIBLE", "CARD",
						AppLocal.getIntString("label.maxdebt"), AppLocal.getIntString("label.curdate"),
						AppLocal.getIntString("label.curdebt"), AppLocal.getIntString("label.firstname"),
						AppLocal.getIntString("label.lastname"), AppLocal.getIntString("label.email"),
						AppLocal.getIntString("label.phone"), AppLocal.getIntString("label.phone2"),
						AppLocal.getIntString("label.fax"), AppLocal.getIntString("label.address"),
						AppLocal.getIntString("label.address2"), AppLocal.getIntString("label.postal"),
						AppLocal.getIntString("label.city"), AppLocal.getIntString("label.region"),
						AppLocal.getIntString("label.country"), "TAXCATEGORY"},
				new Datas[]{Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.BOOLEAN,
						Datas.STRING, Datas.DOUBLE, Datas.TIMESTAMP, Datas.DOUBLE, Datas.STRING, Datas.STRING,
						Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING,
						Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING},
				new Formats[]{Formats.STRING, Formats.STRING, Formats.STRING, Formats.STRING, Formats.STRING,
						Formats.BOOLEAN, Formats.STRING, Formats.CURRENCY, Formats.TIMESTAMP, Formats.CURRENCY,
						Formats.STRING, Formats.STRING, Formats.STRING, Formats.STRING, Formats.STRING, Formats.STRING,
						Formats.STRING, Formats.STRING, Formats.STRING, Formats.STRING, Formats.STRING, Formats.STRING,
						Formats.STRING},
				new int[]{0});

	}

	public List<CustomerInfo> searchCustomers(final String value) throws BasicException {
		final String search = value == null ? "" : value.trim();
		final String nameSearch = "%" + search + "%";
		String normalizedSearch = CustomerInfo.normalizePhone(search);
		final String phoneSearch = normalizedSearch.isEmpty() ? "!" : "%" + normalizedSearch + "%";
		return new PreparedSentence(s,
				"SELECT ID, NAME, PHONE FROM CUSTOMERS WHERE VISIBLE = " + s.DB.TRUE()
						+ " AND (NAME LIKE ? OR PHONE LIKE ?) ORDER BY NAME",
				SerializerWriteParams.INSTANCE, new SerializerRead() {
					public Object readValues(DataRead dr) throws BasicException {
						CustomerInfo c = new CustomerInfo(dr.getString(1));
						c.setName(dr.getString(2));
						c.setPhone(dr.getString(3));
						return c;
					}
				}).list(new DataParams() {
					public void writeValues() throws BasicException {
						setString(1, nameSearch);
						setString(2, phoneSearch);
					}
				});
	}

	public CustomerInfoExt createCustomer(final String name, final String phone) throws BasicException {
		final String id = UUID.randomUUID().toString();
		final String normalizedPhone = CustomerInfo.normalizePhone(phone);
		new PreparedSentence(s,
				"INSERT INTO CUSTOMERS (ID, SEARCHKEY, NAME, PHONE, VISIBLE, MAXDEBT) VALUES (?, ?, ?, ?, "
						+ s.DB.TRUE() + ", ?)",
				SerializerWriteParams.INSTANCE).exec(new DataParams() {
					public void writeValues() throws BasicException {
						setString(1, id);
						setString(2, id);
						setString(3, name.trim());
						setString(4, normalizedPhone);
						setDouble(5, 0.0);
					}
				});
		CustomerInfoExt customer = new CustomerInfoExt(id);
		customer.setName(name.trim());
		customer.setPhone(normalizedPhone);
		customer.setVisible(true);
		customer.setMaxdebt(0.0);
		return customer;
	}

	public List<CustomerInfoExt> searchCustomerSummaries(final String value) throws BasicException {
		final String search = value == null ? "" : value.trim();
		final String normalizedSearch = CustomerInfo.normalizePhone(search);
		final String phoneSearch = normalizedSearch.isEmpty() ? "!" : "%" + normalizedSearch + "%";
		return new PreparedSentence(s,
				"SELECT ID, NAME, PHONE, CURDEBT, VISIBLE FROM CUSTOMERS WHERE VISIBLE = " + s.DB.TRUE()
						+ " AND (NAME LIKE ? OR PHONE LIKE ?) ORDER BY NAME",
				SerializerWriteParams.INSTANCE, new SerializerRead() {
					public Object readValues(DataRead dr) throws BasicException {
						CustomerInfoExt c = new CustomerInfoExt(dr.getString(1));
						c.setName(dr.getString(2));
						c.setPhone(dr.getString(3));
						c.setCurdebt(dr.getDouble(4));
						c.setVisible(dr.getBoolean(5).booleanValue());
						return c;
					}
				}).list(new DataParams() {
					public void writeValues() throws BasicException {
						setString(1, "%" + search + "%");
						setString(2, phoneSearch);
					}
				});
	}

	public int updateCustomer(final CustomerInfoExt customer) throws BasicException {
		return new PreparedSentence(s, "UPDATE CUSTOMERS SET NAME = ?, PHONE = ?, VISIBLE = ? WHERE ID = ?",
				SerializerWriteParams.INSTANCE).exec(new DataParams() {
					public void writeValues() throws BasicException {
						setString(1, customer.getName());
						setString(2, CustomerInfo.normalizePhone(customer.getPhone()));
						setBoolean(3, customer.isVisible());
						setString(4, customer.getId());
					}
				});
	}

	public final TableDefinition getTableCustomers() {
		return tcustomers;
	}
}
