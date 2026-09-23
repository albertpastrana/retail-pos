package com.openbravo.pos.forms;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.openbravo.basic.BasicException;
import com.openbravo.data.loader.Datas;
import com.openbravo.data.loader.PreparedSentence;
import com.openbravo.data.loader.SerializerWriteBasicExt;
import com.openbravo.data.loader.Session;
import com.openbravo.data.model.Column;
import com.openbravo.data.model.Field;
import com.openbravo.data.model.PrimaryKey;
import com.openbravo.data.model.Row;
import com.openbravo.data.model.Table;
import com.openbravo.data.user.SaveProvider;
import com.openbravo.format.Formats;
import com.openbravo.pos.forms.DatabaseMigrator;
import com.openbravo.pos.inventory.AttributeSaveProvider;

public class AttributeManagementPersistenceTest {

	@TempDir
	Path temp;

	@Test
	public void rejectsDeletingRecordsUsedByVariants() throws Exception {
		String url = "jdbc:derby:" + temp.resolve("attribute-management") + ";create=true";
		DatabaseMigrator.migrate(url, null, null);
		Session session = new Session(url, null, null);
		try {
			SaveProvider attributes = AttributeSaveProvider.forAttribute(session,
					provider(session,
							new Row(new Field("ID", Datas.STRING, Formats.STRING),
									new Field("NAME", Datas.STRING, Formats.STRING)),
							new Table("ATTRIBUTE", new PrimaryKey("ID"), new Column("NAME"))));
			attributes.insertData(new Object[]{"attribute", "Colour"});

			execute(session, "INSERT INTO ATTRIBUTESET (ID, NAME) VALUES (?, ?)", new Object[]{"set", "T-shirt"},
					new Datas[]{Datas.STRING, Datas.STRING});
			execute(session, "INSERT INTO ATTRIBUTEUSE (ID, ATTRIBUTESET_ID, ATTRIBUTE_ID, LINENO) VALUES (?, ?, ?, ?)",
					new Object[]{"use", "set", "attribute", 1},
					new Datas[]{Datas.STRING, Datas.STRING, Datas.STRING, Datas.INT});

			assertThrows(BasicException.class, () -> attributes.deleteData(new Object[]{"attribute", "Colour"}));

			SaveProvider values = AttributeSaveProvider.forValue(session,
					provider(session,
							new Row(new Field("ID", Datas.STRING, Formats.STRING),
									new Field("ATTRIBUTE_ID", Datas.STRING, Formats.STRING),
									new Field("VALUE", Datas.STRING, Formats.STRING)),
							new Table("ATTRIBUTEVALUE", new PrimaryKey("ID"), new Column("ATTRIBUTE_ID"),
									new Column("VALUE"))));
			values.insertData(new Object[]{"value", "attribute", "Red"});
			execute(session, "INSERT INTO ATTRIBUTESETINSTANCE (ID, ATTRIBUTESET_ID, DESCRIPTION) VALUES (?, ?, ?)",
					new Object[]{"instance", "set", "Red"}, new Datas[]{Datas.STRING, Datas.STRING, Datas.STRING});
			execute(session,
					"INSERT INTO ATTRIBUTEINSTANCE (ID, ATTRIBUTESETINSTANCE_ID, ATTRIBUTE_ID, VALUE) VALUES (?, ?, ?, ?)",
					new Object[]{"attribute-instance", "instance", "attribute", "Red"},
					new Datas[]{Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING});

			assertThrows(BasicException.class, () -> values.deleteData(new Object[]{"value", "attribute", "Red"}));

			SaveProvider sets = AttributeSaveProvider.forSet(session,
					provider(session,
							new Row(new Field("ID", Datas.STRING, Formats.STRING),
									new Field("NAME", Datas.STRING, Formats.STRING)),
							new Table("ATTRIBUTESET", new PrimaryKey("ID"), new Column("NAME"))));
			assertThrows(BasicException.class, () -> sets.deleteData(new Object[]{"set", "T-shirt"}));
		} finally {
			session.close();
		}
	}

	private SaveProvider provider(Session session, Row row, Table table) {
		return row.getSaveProvider(session, table);
	}

	private void execute(Session session, String sql, Object[] values, Datas[] types) throws BasicException {
		int[] indexes = new int[values.length];
		for (int i = 0; i < indexes.length; i++) {
			indexes[i] = i;
		}
		new PreparedSentence(session, sql, new SerializerWriteBasicExt(types, indexes)).exec(values);
	}
}
