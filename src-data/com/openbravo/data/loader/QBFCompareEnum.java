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

package com.openbravo.data.loader;

/**
 *
 * @author adrian
 */
public abstract class QBFCompareEnum {

	public final static QBFCompareEnum COMP_NONE = new QBFCompareEnum(0, "qbf.none") {
		public String getExpression(String sField, String sSQLValue) {
			return null;
		}
	};
	public final static QBFCompareEnum COMP_ISNULL = new QBFCompareEnum(1, "qbf.null") {
		public String getExpression(String sField, String sSQLValue) {
			return sField + " IS NULL";
		}
	};
	public final static QBFCompareEnum COMP_ISNOTNULL = new QBFCompareEnum(2, "qbf.notnull") {
		public String getExpression(String sField, String sSQLValue) {
			return sField + " IS NOT NULL";
		}
	};
	public final static QBFCompareEnum COMP_RE = new QBFCompareEnum(3, "qbf.re") {
		public String getExpression(String sField, String sSQLValue) {
			return sField + " LIKE " + sSQLValue;
		}
	};
	public final static QBFCompareEnum COMP_BLOOKUP = new QBFCompareEnum(9, "qbf.blookup") {
		public String getExpression(String sField, String sSQLValue) {
			String sCodes = zeroPaddedList(sSQLValue);
			return sField + " IN (" + sCodes + ")"
					+ " OR EXISTS (SELECT 1 FROM BARCODE_TABLE WHERE BARCODE_TABLE.PID = PRODUCTS.ID AND BARCODE_TABLE.CODE IN ("
					+ sCodes + "))";
		}
	};
	public final static QBFCompareEnum COMP_EQUALS = new QBFCompareEnum(3, "qbf.equals") {
		public String getExpression(String sField, String sSQLValue) {
			return sField + " = " + sSQLValue;
		}
	};
	public final static QBFCompareEnum COMP_IN = new QBFCompareEnum(3, "qbf.in") {
		public String getExpression(String sField, String sSQLValue) {
			if (sSQLValue == null || "NULL".equals(sSQLValue) || sSQLValue.length() < 2) {
				return null;
			}
			String[] values = sSQLValue.substring(1, sSQLValue.length() - 1).split(",", -1);
			StringBuilder sqlValues = new StringBuilder();
			for (String value : values) {
				if (sqlValues.length() > 0) {
					sqlValues.append(", ");
				}
				sqlValues.append(DataWriteUtils.getSQLValue(value));
			}
			return sField + " IN (" + sqlValues + ")";
		}
	};
	public final static QBFCompareEnum COMP_DISTINCT = new QBFCompareEnum(4, "qbf.distinct") {
		public String getExpression(String sField, String sSQLValue) {
			return sField + " <> " + sSQLValue;
		}
	};
	public final static QBFCompareEnum COMP_GREATER = new QBFCompareEnum(5, "qbf.greater") {
		public String getExpression(String sField, String sSQLValue) {
			return sField + " > " + sSQLValue;
		}
	};
	public final static QBFCompareEnum COMP_LESS = new QBFCompareEnum(6, "qbf.less") {
		public String getExpression(String sField, String sSQLValue) {
			return sField + " < " + sSQLValue;
		}
	};
	public final static QBFCompareEnum COMP_GREATEROREQUALS = new QBFCompareEnum(7, "qbf.greaterequals") {
		public String getExpression(String sField, String sSQLValue) {
			return sField + " >= " + sSQLValue;
		}
	};
	public final static QBFCompareEnum COMP_LESSOREQUALS = new QBFCompareEnum(8, "qbf.lessequals") {
		public String getExpression(String sField, String sSQLValue) {
			return sField + " <= " + sSQLValue;
		}
	};
	public final static QBFCompareEnum COMP_STARTSWITH = new QBFCompareEnum(12, "qbf.startswith") {
		public String getExpression(String sField, String sSQLValue) {
			String sPattern = wrapLike(sSQLValue, false, true);
			return sPattern == null ? null : likeIgnoreCase(sField, sPattern);
		}
	};
	public final static QBFCompareEnum COMP_CONTAINS = new QBFCompareEnum(13, "qbf.contains") {
		public String getExpression(String sField, String sSQLValue) {
			String sPattern = wrapLike(sSQLValue, true, true);
			return sPattern == null ? null : likeIgnoreCase(sField, sPattern);
		}
	};

	// LIKE is case-sensitive in Derby and PostgreSQL, so a search for
	// cheese would never find Cheese. UPPER on both sides works on every engine.
	private static String likeIgnoreCase(String sField, String sSQLPattern) {
		return "UPPER(" + sField + ") LIKE UPPER(" + sSQLPattern + ")";
	}

	// sSQLValue is already a quoted SQL literal, e.g. 'foo'.
	private static String wrapLike(String sSQLValue, boolean prefix, boolean suffix) {
		if (sSQLValue == null || "NULL".equals(sSQLValue) || sSQLValue.length() < 2) {
			return null;
		}
		String sInner = sSQLValue.substring(1, sSQLValue.length() - 1);
		if (sInner.length() == 0) {
			return null;
		}
		return "'" + (prefix ? "%" : "") + sInner + (suffix ? "%" : "") + "'";
	}

	// A code scanned as EAN-13 carries up to two leading zeros that the stored
	// code does not, so a barcode lookup has to try the padded forms too.
	private static String zeroPaddedList(String sSQLValue) {
		String sEscaped = sSQLValue.substring(1, sSQLValue.length() - 1);
		return sSQLValue + ", '0" + sEscaped + "', '00" + sEscaped + "'";
	}

	private int m_iValue;
	private String m_sKey;

	private QBFCompareEnum(int iValue, String sKey) {
		m_iValue = iValue;
		m_sKey = sKey;
	}

	public int getCompareInt() {
		return m_iValue;
	}

	public String toString() {
		return LocalRes.getIntString(m_sKey);
	}

	public abstract String getExpression(String sField, String sSQLValue);
}
