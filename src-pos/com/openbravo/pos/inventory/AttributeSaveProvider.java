package com.openbravo.pos.inventory;

import com.openbravo.basic.BasicException;
import com.openbravo.data.loader.Datas;
import com.openbravo.data.loader.PreparedSentence;
import com.openbravo.data.loader.SerializerReadInteger;
import com.openbravo.data.loader.SerializerWriteBasicExt;
import com.openbravo.data.loader.Session;
import com.openbravo.data.user.SaveProvider;
import com.openbravo.pos.forms.AppLocal;

/**
 * Prevents deleting attribute records that are still referenced by variants.
 */
public final class AttributeSaveProvider extends SaveProvider {

	private enum RecordType {
		ATTRIBUTE, VALUE, SET
	}

	private final Session session;
	private final SaveProvider delegate;
	private final RecordType recordType;

	private AttributeSaveProvider(Session session, SaveProvider delegate, RecordType recordType) {
		super(delegate);
		this.session = session;
		this.delegate = delegate;
		this.recordType = recordType;
	}

	@Override
	public boolean canDelete() {
		return delegate.canDelete();
	}

	@Override
	public int deleteData(Object value) throws BasicException {
		Object[] record = (Object[]) value;
		if (isInUse(record)) {
			throw new BasicException(AppLocal.getIntString(messageKey()));
		}
		return delegate.deleteData(value);
	}

	@Override
	public boolean canInsert() {
		return delegate.canInsert();
	}

	@Override
	public int insertData(Object value) throws BasicException {
		return delegate.insertData(value);
	}

	@Override
	public boolean canUpdate() {
		return delegate.canUpdate();
	}

	@Override
	public int updateData(Object value) throws BasicException {
		return delegate.updateData(value);
	}

	private boolean isInUse(Object[] record) throws BasicException {
		String sql;
		Object[] values;
		switch (recordType) {
			case ATTRIBUTE :
				return count("SELECT COUNT(*) FROM ATTRIBUTEUSE WHERE ATTRIBUTE_ID = ?", new Object[]{record[0]}) > 0
						|| count("SELECT COUNT(*) FROM ATTRIBUTEINSTANCE WHERE ATTRIBUTE_ID = ?",
								new Object[]{record[0]}) > 0;
			case VALUE :
				sql = "SELECT COUNT(*) FROM ATTRIBUTEINSTANCE WHERE ATTRIBUTE_ID = ? AND VALUE = ?";
				values = new Object[]{record[1], record[2]};
				break;
			case SET :
				return count("SELECT COUNT(*) FROM PRODUCTS WHERE ATTRIBUTESET_ID = ?", new Object[]{record[0]}) > 0
						|| count("SELECT COUNT(*) FROM ATTRIBUTESETINSTANCE WHERE ATTRIBUTESET_ID = ?",
								new Object[]{record[0]}) > 0;
			default :
				throw new AssertionError(recordType);
		}

		return count(sql, values) > 0;
	}

	private int count(String sql, Object[] values) throws BasicException {
		Datas[] types = new Datas[values.length];
		int[] indexes = new int[values.length];
		for (int i = 0; i < values.length; i++) {
			types[i] = Datas.STRING;
			indexes[i] = i;
		}
		Integer result = (Integer) new PreparedSentence(session, sql, new SerializerWriteBasicExt(types, indexes),
				SerializerReadInteger.INSTANCE).find(values);
		return result == null ? 0 : result.intValue();
	}

	private String messageKey() {
		switch (recordType) {
			case ATTRIBUTE :
				return "message.cannotdeleteattributeinuse";
			case VALUE :
				return "message.cannotdeleteattributevalueinuse";
			case SET :
				return "message.cannotdeleteattributesetinuse";
			default :
				throw new AssertionError(recordType);
		}
	}

	public static SaveProvider forAttribute(Session session, SaveProvider delegate) {
		return new AttributeSaveProvider(session, delegate, RecordType.ATTRIBUTE);
	}

	public static SaveProvider forValue(Session session, SaveProvider delegate) {
		return new AttributeSaveProvider(session, delegate, RecordType.VALUE);
	}

	public static SaveProvider forSet(Session session, SaveProvider delegate) {
		return new AttributeSaveProvider(session, delegate, RecordType.SET);
	}
}
