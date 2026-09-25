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

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;
import javax.imageio.ImageIO;
import com.openbravo.basic.BasicException;
import com.openbravo.data.loader.*;
import com.openbravo.format.Formats;
import com.openbravo.pos.util.HiDpiIcon;
import com.openbravo.pos.util.ThumbNailBuilder;
import com.openbravo.pos.util.TillButtons;
import com.openbravo.pos.ticket.LoyaltySettings;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Icon;

/**
 *
 * @author adrianromero
 */
public class DataLogicSystem extends BeanFactoryDataSingle {

	private static final int AVATAR_SIZE = TillButtons.ICON_SOURCE_SIZE;

	// Retail POS design system tokens (design-system/tokens.json): brand,
	// success, danger, info, warning, brand-strong, border-strong, ink. Pinned
	// to their light-theme values regardless of Configuration → General's
	// Light/Dark choice: white initials text is only verified against these
	// (see tokens.json's contrast notes on brand/on-brand and ink), and the
	// dark-theme column of several of these tokens is too light for white text.
	private static final Color[] AVATAR_COLOURS = {new Color(0xb8481f), new Color(0x1f6b5c), new Color(0x8a2a22),
			new Color(0x3c6e8f), new Color(0x7a5300), new Color(0x8f3216), new Color(0x6b5f47), new Color(0x241c14)};

	protected SentenceList m_peoplevisible;
	protected SentenceFind m_peoplebycard;
	protected SerializerRead peopleread;

	private SentenceFind m_rolepermissions;
	private SentenceExec m_changepassword;
	private SentenceFind m_locationfind;

	private SentenceFind m_resourcebytes;
	private SentenceExec m_resourcebytesinsert;
	private SentenceExec m_resourcebytesupdate;
	private SentenceFind m_loyaltysettings;
	private SentenceExec m_loyaltysettingsupdate;

	protected SentenceFind m_sequencecash;
	protected SentenceFind m_activecash;
	protected SentenceExec m_insertcash;
	protected SentenceList m_closedcashlist;

	private Map<String, byte[]> resourcescache;

	/** Creates a new instance of DataLogicSystem */
	public DataLogicSystem() {
	}

	public void init(Session s) {

		final ThumbNailBuilder tnb = new ThumbNailBuilder(AVATAR_SIZE, AVATAR_SIZE);
		peopleread = new SerializerRead() {
			public Object readValues(DataRead dr) throws BasicException {
				BufferedImage image = ImageUtils.readImage(dr.getBytes(6));
				return new AppUser(dr.getString(1), dr.getString(2), dr.getString(3), dr.getString(4), dr.getString(5),
						image == null ? null : new HiDpiIcon(tnb.getThumbNail(image)));
			}
		};

		m_peoplevisible = new StaticSentence(s,
				"SELECT ID, NAME, APPPASSWORD, CARD, ROLE, IMAGE FROM PEOPLE WHERE VISIBLE = " + s.DB.TRUE()
						+ " ORDER BY SORT_ORDER, NAME, ID",
				null, peopleread);

		m_peoplebycard = new PreparedSentence(s,
				"SELECT ID, NAME, APPPASSWORD, CARD, ROLE, IMAGE FROM PEOPLE WHERE CARD = ? AND VISIBLE = "
						+ s.DB.TRUE(),
				SerializerWriteString.INSTANCE, peopleread);

		m_resourcebytes = new PreparedSentence(s, "SELECT CONTENT FROM RESOURCES WHERE NAME = ?",
				SerializerWriteString.INSTANCE, SerializerReadBytes.INSTANCE);

		Datas[] resourcedata = new Datas[]{Datas.STRING, Datas.STRING, Datas.INT, Datas.BYTES};
		m_resourcebytesinsert = new PreparedSentence(s,
				"INSERT INTO RESOURCES(ID, NAME, RESTYPE, CONTENT) VALUES (?, ?, ?, ?)",
				new SerializerWriteBasic(resourcedata));
		m_resourcebytesupdate = new PreparedSentence(s,
				"UPDATE RESOURCES SET NAME = ?, RESTYPE = ?, CONTENT = ? WHERE NAME = ?",
				new SerializerWriteBasicExt(resourcedata, new int[]{1, 2, 3, 1}));

		m_loyaltysettings = new StaticSentence(s,
				"SELECT ENABLED, NAME, ELIGIBLE_SPEND_PER_STAMP, REDEMPTION_VALUE "
						+ "FROM LOYALTY_SETTINGS WHERE ID = '0'",
				null, new SerializerReadBasic(new Datas[]{Datas.BOOLEAN, Datas.STRING, Datas.DOUBLE, Datas.DOUBLE}));
		m_loyaltysettingsupdate = new StaticSentence(s,
				"UPDATE LOYALTY_SETTINGS SET ENABLED = ?, NAME = ?, ELIGIBLE_SPEND_PER_STAMP = ?, "
						+ "REDEMPTION_VALUE = ? WHERE ID = '0'",
				new SerializerWriteBasic(new Datas[]{Datas.BOOLEAN, Datas.STRING, Datas.DOUBLE, Datas.DOUBLE}));

		m_rolepermissions = new PreparedSentence(s, "SELECT PERMISSIONS FROM ROLES WHERE ID = ?",
				SerializerWriteString.INSTANCE, SerializerReadBytes.INSTANCE);

		m_changepassword = new StaticSentence(s, "UPDATE PEOPLE SET APPPASSWORD = ? WHERE ID = ?",
				new SerializerWriteBasic(new Datas[]{Datas.STRING, Datas.STRING}));

		m_sequencecash = new StaticSentence(s, "SELECT MAX(HOSTSEQUENCE) FROM CLOSEDCASH WHERE HOST = ?",
				SerializerWriteString.INSTANCE, SerializerReadInteger.INSTANCE);
		m_activecash = new StaticSentence(s,
				"SELECT HOST, HOSTSEQUENCE, DATESTART, DATEEND FROM CLOSEDCASH WHERE MONEY = ?",
				SerializerWriteString.INSTANCE,
				new SerializerReadBasic(new Datas[]{Datas.STRING, Datas.INT, Datas.TIMESTAMP, Datas.TIMESTAMP}));
		m_insertcash = new StaticSentence(s,
				"INSERT INTO CLOSEDCASH(MONEY, HOST, HOSTSEQUENCE, DATESTART, DATEEND) " + "VALUES (?, ?, ?, ?, ?)",
				new SerializerWriteBasic(
						new Datas[]{Datas.STRING, Datas.STRING, Datas.INT, Datas.TIMESTAMP, Datas.TIMESTAMP}));

		m_closedcashlist = new StaticSentence(s,
				"SELECT CLOSEDCASH.MONEY, CLOSEDCASH.HOST, CLOSEDCASH.HOSTSEQUENCE, CLOSEDCASH.DATESTART, CLOSEDCASH.DATEEND, "
						+ "(SELECT SUM(PAYMENTS.TOTAL) FROM PAYMENTS, RECEIPTS "
						+ "WHERE PAYMENTS.RECEIPT = RECEIPTS.ID AND RECEIPTS.MONEY = CLOSEDCASH.MONEY "
						+ "AND PAYMENTS.PAYMENT NOT IN ('paperin', 'paperout')) "
						+ "FROM CLOSEDCASH WHERE CLOSEDCASH.DATEEND IS NOT NULL " + "ORDER BY CLOSEDCASH.DATEEND DESC",
				null, new SerializerReadClass(com.openbravo.pos.panels.ClosedCashInfo.class));

		m_locationfind = new StaticSentence(s, "SELECT NAME FROM LOCATIONS WHERE ID = ?",
				SerializerWriteString.INSTANCE, SerializerReadString.INSTANCE);

		resetResourcesCache();
	}

	// Staff without a photo get their initials on a colour of their own, one per
	// row, so login and seller buttons tell them apart at a glance.
	private static Icon avatarIcon(Color colour, String name) {

		BufferedImage avatar = new BufferedImage(AVATAR_SIZE, AVATAR_SIZE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = avatar.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		g.setColor(colour);
		g.fillOval(0, 0, AVATAR_SIZE, AVATAR_SIZE);

		String initials = initials(name);
		g.setColor(Color.WHITE);
		g.setFont(new Font("SansSerif", Font.BOLD, initials.length() > 1 ? AVATAR_SIZE * 2 / 5 : AVATAR_SIZE / 2));
		FontMetrics metrics = g.getFontMetrics();
		g.drawString(initials, (AVATAR_SIZE - metrics.stringWidth(initials)) / 2,
				(AVATAR_SIZE - metrics.getHeight()) / 2 + metrics.getAscent());

		g.dispose();
		return new HiDpiIcon(avatar);
	}

	private static String initials(String name) {

		if (name == null || name.trim().length() == 0) {
			return "?";
		}

		String[] nameParts = name.trim().split("\\s+");
		String initials = nameParts[0].substring(0, 1);
		if (nameParts.length > 1) {
			initials += nameParts[nameParts.length - 1].substring(0, 1);
		}
		return initials.toUpperCase(Locale.ROOT);
	}

	public final List listPeopleVisible() throws BasicException {

		List people = m_peoplevisible.list();
		for (int i = 0; i < people.size(); i++) {
			AppUser user = (AppUser) people.get(i);
			if (user.getIcon() == null) {
				user.setIcon(avatarIcon(AVATAR_COLOURS[i % AVATAR_COLOURS.length], user.getName()));
			}
		}
		return people;
	}

	public final AppUser findPeopleByCard(String card) throws BasicException {
		return (AppUser) m_peoplebycard.find(card);
	}

	public final String findRolePermissions(String sRole) {

		try {
			return Formats.BYTEA.formatValue(m_rolepermissions.find(sRole));
		} catch (BasicException e) {
			return null;
		}
	}

	public final void execChangePassword(Object[] userdata) throws BasicException {
		m_changepassword.exec(userdata);
	}

	public final void resetResourcesCache() {
		resourcescache = new HashMap<String, byte[]>();
	}

	private final byte[] getResource(String name) {

		byte[] resource;

		resource = resourcescache.get(name);

		if (resource == null) {
			// Primero trato de obtenerlo de la tabla de recursos
			try {
				resource = (byte[]) m_resourcebytes.find(name);
				resourcescache.put(name, resource);
			} catch (BasicException e) {
				resource = null;
			}
		}

		return resource;
	}

	public final void setResource(String name, int type, byte[] data) {

		Object[] value = new Object[]{UUID.randomUUID().toString(), name, new Integer(type), data};
		try {
			if (m_resourcebytesupdate.exec(value) == 0) {
				m_resourcebytesinsert.exec(value);
			}
			resourcescache.put(name, data);
		} catch (BasicException e) {
		}
	}

	public final void setResourceAsBinary(String sName, byte[] data) {
		setResource(sName, 2, data);
	}

	public final byte[] getResourceAsBinary(String sName) {
		return getResource(sName);
	}

	public final String getResourceAsText(String sName) {
		return Formats.BYTEA.formatValue(getResource(sName));
	}

	public final String getResourceAsXML(String sName) {
		return Formats.BYTEA.formatValue(getResource(sName));
	}

	public final BufferedImage getResourceAsImage(String sName) {
		try {
			byte[] img = getResource(sName); // , ".png"
			return img == null ? null : ImageIO.read(new ByteArrayInputStream(img));
		} catch (IOException e) {
			return null;
		}
	}

	public final void setResourceAsProperties(String sName, Properties p) {
		if (p == null) {
			setResource(sName, 0, null); // texto
		} else {
			try {
				ByteArrayOutputStream o = new ByteArrayOutputStream();
				p.storeToXML(o, AppLocal.APP_NAME, "UTF8");
				setResource(sName, 0, o.toByteArray()); // El texto de las propiedades
			} catch (IOException e) { // no deberia pasar nunca
			}
		}
	}

	public final LoyaltySettings getLoyaltySettings() {
		try {
			Object[] values = (Object[]) m_loyaltysettings.find();
			if (values == null) {
				return LoyaltySettings.defaults();
			}
			return new LoyaltySettings((Boolean) values[0], (String) values[1], ((Number) values[2]).doubleValue(),
					((Number) values[3]).doubleValue());
		} catch (BasicException e) {
			return LoyaltySettings.defaults();
		}
	}

	public final void setLoyaltySettings(LoyaltySettings settings) throws BasicException {
		m_loyaltysettingsupdate.exec(new Object[]{settings.isEnabled(), settings.getName(),
				settings.getEligibleSpendPerStamp(), settings.getRedemptionValue()});
	}

	public final Properties getResourceAsProperties(String sName) {

		Properties p = new Properties();
		try {
			byte[] img = getResourceAsBinary(sName);
			if (img != null) {
				p.loadFromXML(new ByteArrayInputStream(img));
			}
		} catch (IOException e) {
		}
		return p;
	}

	public final int getSequenceCash(String host) throws BasicException {
		Integer i = (Integer) m_sequencecash.find(host);
		return (i == null) ? 1 : i.intValue();
	}

	public final Object[] findActiveCash(String sActiveCashIndex) throws BasicException {
		return (Object[]) m_activecash.find(sActiveCashIndex);
	}

	public final void execInsertCash(Object[] cash) throws BasicException {
		m_insertcash.exec(cash);
	}

	public final List listClosedCash(int max) throws BasicException {
		return m_closedcashlist.listPage(0, max);
	}

	public final String findLocationName(String iLocation) throws BasicException {
		return (String) m_locationfind.find(iLocation);
	}
}
