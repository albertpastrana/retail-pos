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

package com.openbravo.pos.panels;

import java.util.Date;

import com.openbravo.basic.BasicException;
import com.openbravo.data.loader.DataRead;
import com.openbravo.data.loader.SerializableRead;
import com.openbravo.format.Formats;
import com.openbravo.pos.util.StringUtils;

public class ClosedCashInfo implements SerializableRead {

	private String money;
	private String host;
	private int sequence;
	private Date datestart;
	private Date dateend;
	private double total;

	public void readValues(DataRead dr) throws BasicException {
		money = dr.getString(1);
		host = dr.getString(2);
		sequence = dr.getInt(3).intValue();
		datestart = dr.getTimestamp(4);
		dateend = dr.getTimestamp(5);
		total = (dr.getObject(6) == null) ? 0.0 : dr.getDouble(6).doubleValue();
	}

	@Override
	public String toString() {
		// Stores that close twice a day get two rows per date, so both ends of
		// the interval carry the time.
		return "<html><table>" + "<tr><td width=\"30\">[" + sequence + "]</td>" + "<td width=\"120\">"
				+ Formats.TIMESTAMP.formatValue(datestart) + "</td>" + "<td width=\"120\">"
				+ Formats.TIMESTAMP.formatValue(dateend) + "</td>" + "<td align=\"right\" width=\"80\">"
				+ Formats.CURRENCY.formatValue(new Double(total)) + "</td>" + "<td width=\"80\">"
				+ StringUtils.encodeXML(host) + "</td></tr>" + "</table></html>";
	}

	public String getMoney() {
		return money;
	}

	public String getHost() {
		return host;
	}

	public int getSequence() {
		return sequence;
	}

	public Date getDateStart() {
		return datestart;
	}

	public Date getDateEnd() {
		return dateend;
	}
}
