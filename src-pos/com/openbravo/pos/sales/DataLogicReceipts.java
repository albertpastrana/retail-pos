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

package com.openbravo.pos.sales;

import java.util.List;
import com.openbravo.basic.BasicException;
import com.openbravo.data.loader.Datas;
import com.openbravo.data.loader.PreparedSentence;
import com.openbravo.data.loader.SerializerReadBasic;
import com.openbravo.data.loader.SerializerReadClass;
import com.openbravo.data.loader.SerializerWriteBasicExt;
import com.openbravo.data.loader.SerializerWriteString;
import com.openbravo.data.loader.Session;
import com.openbravo.data.loader.StaticSentence;
import com.openbravo.data.loader.Transaction;
import com.openbravo.pos.forms.BeanFactoryDataSingle;
import com.openbravo.pos.ticket.TicketInfo;

/**
 *
 * @author adrianromero
 */
public class DataLogicReceipts extends BeanFactoryDataSingle {

	private Session s;

	/** Creates a new instance of DataLogicReceipts */
	public DataLogicReceipts() {
	}

	public void init(Session s) {
		this.s = s;
	}

	public final TicketInfo getSharedTicket(String Id) throws BasicException {

		if (Id == null) {
			return null;
		} else {
			Object[] record = (Object[]) new StaticSentence(s, "SELECT CONTENT FROM SHAREDTICKETS WHERE ID = ?",
					SerializerWriteString.INSTANCE, new SerializerReadBasic(new Datas[]{Datas.SERIALIZABLE})).find(Id);
			return record == null ? null : (TicketInfo) record[0];
		}
	}

	public final List<SharedTicketInfo> getSharedTicketList() throws BasicException {

		return (List<SharedTicketInfo>) new StaticSentence(s, "SELECT ID, NAME, HOST FROM SHAREDTICKETS ORDER BY ID",
				null, new SerializerReadClass(SharedTicketInfo.class)).list();
	}

	public final TicketInfo claimSharedTicket(final String id, final String host) throws BasicException {
		return new Transaction<TicketInfo>(s) {
			@Override
			protected TicketInfo transact() throws BasicException {
				Object[] values = new Object[]{id, host};
				Datas[] datas = new Datas[]{Datas.STRING, Datas.STRING};
				int claimed = new PreparedSentence(s,
						"UPDATE SHAREDTICKETS SET HOST = ?, PAYING = FALSE WHERE ID = ? AND (PAYING = FALSE OR HOST = ?)",
						new SerializerWriteBasicExt(datas, new int[]{1, 0, 1})).exec(values);
				return claimed == 0 ? null : getSharedTicket(id);
			}
		}.execute();
	}

	public final boolean saveSharedTicket(final String id, final TicketInfo ticket, final String host)
			throws BasicException {
		return new Transaction<Boolean>(s) {
			@Override
			protected Boolean transact() throws BasicException {
				if (updateOwnedSharedTicket(id, ticket, host, false) > 0) {
					return Boolean.TRUE;
				}
				if (sharedTicketExists(id)) {
					return Boolean.FALSE;
				}
				insertSharedTicket(id, ticket, host);
				return Boolean.TRUE;
			}
		}.execute().booleanValue();
	}

	public final boolean parkSharedTicket(final String id, final TicketInfo ticket, final String host)
			throws BasicException {
		return new Transaction<Boolean>(s) {
			@Override
			protected Boolean transact() throws BasicException {
				if (updateOwnedSharedTicket(id, ticket, host, true) > 0) {
					return Boolean.TRUE;
				}
				if (sharedTicketExists(id)) {
					return Boolean.FALSE;
				}
				insertSharedTicket(id, ticket, null);
				return Boolean.TRUE;
			}
		}.execute().booleanValue();
	}

	private int updateOwnedSharedTicket(String id, TicketInfo ticket, String host, boolean park) throws BasicException {
		Object[] values = new Object[]{id, ticket.getSharedTicketName(), ticket, host};
		Datas[] datas = new Datas[]{Datas.STRING, Datas.STRING, Datas.SERIALIZABLE, Datas.STRING};
		String sql = park
				? "UPDATE SHAREDTICKETS SET NAME = ?, CONTENT = ?, HOST = NULL WHERE ID = ? AND HOST = ? AND PAYING = FALSE"
				: "UPDATE SHAREDTICKETS SET NAME = ?, CONTENT = ? WHERE ID = ? AND HOST = ? AND PAYING = FALSE";
		return new PreparedSentence(s, sql, new SerializerWriteBasicExt(datas, new int[]{1, 2, 0, 3})).exec(values);
	}

	private boolean sharedTicketExists(String id) throws BasicException {
		Object found = new StaticSentence(s, "SELECT ID FROM SHAREDTICKETS WHERE ID = ?",
				SerializerWriteString.INSTANCE, new SerializerReadBasic(new Datas[]{Datas.STRING})).find(id);
		return found != null;
	}

	public final boolean isSharedTicketOwned(final String id, final String host) throws BasicException {
		Object[] values = new Object[]{id, host};
		Datas[] datas = new Datas[]{Datas.STRING, Datas.STRING};
		Object found = new PreparedSentence(s, "SELECT ID FROM SHAREDTICKETS WHERE ID = ? AND HOST = ?",
				new SerializerWriteBasicExt(datas, new int[]{0, 1}), new SerializerReadBasic(new Datas[]{Datas.STRING}))
				.find(values);
		return found != null;
	}

	public final boolean beginSharedTicketPayment(final String id, final String host) throws BasicException {
		Object[] values = new Object[]{id, host};
		Datas[] datas = new Datas[]{Datas.STRING, Datas.STRING};
		return new PreparedSentence(s,
				"UPDATE SHAREDTICKETS SET PAYING = TRUE WHERE ID = ? AND HOST = ? AND PAYING = FALSE",
				new SerializerWriteBasicExt(datas, new int[]{0, 1})).exec(values) > 0;
	}

	public final void cancelSharedTicketPayment(final String id, final String host) throws BasicException {
		Object[] values = new Object[]{id, host};
		Datas[] datas = new Datas[]{Datas.STRING, Datas.STRING};
		new PreparedSentence(s, "UPDATE SHAREDTICKETS SET PAYING = FALSE WHERE ID = ? AND HOST = ?",
				new SerializerWriteBasicExt(datas, new int[]{0, 1})).exec(values);
	}

	private void insertSharedTicket(final String id, final TicketInfo ticket, final String host) throws BasicException {

		Object[] values = new Object[]{id, ticket.getSharedTicketName(), ticket, host};
		Datas[] datas = new Datas[]{Datas.STRING, Datas.STRING, Datas.SERIALIZABLE, Datas.STRING};

		new PreparedSentence(s, "INSERT INTO SHAREDTICKETS (ID, NAME, CONTENT, HOST) VALUES (?, ?, ?, ?)",
				new SerializerWriteBasicExt(datas, new int[]{0, 1, 2, 3})).exec(values);
	}

	public final boolean deleteSharedTicket(final String id, final String host) throws BasicException {

		Object[] values = new Object[]{id, host};
		Datas[] datas = new Datas[]{Datas.STRING, Datas.STRING};
		return new PreparedSentence(s, "DELETE FROM SHAREDTICKETS WHERE ID = ? AND HOST = ?",
				new SerializerWriteBasicExt(datas, new int[]{0, 1})).exec(values) > 0;
	}
}
