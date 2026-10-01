package com.openbravo.data.loader;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Serializable;
import java.util.ArrayList;

import com.openbravo.pos.ticket.TicketInfo;
import com.openbravo.pos.ticket.TicketLineInfo;
import com.openbravo.pos.ticket.TaxInfo;
import org.junit.jupiter.api.Test;

public class ImageUtilsTest {

	@Test
	public void readsExistingSerializedTicket() {
		TicketInfo ticket = new TicketInfo();
		ticket.setProperty("test", "value");
		ticket.addLine(new TicketLineInfo("product", 1.0, 2.0,
				new TaxInfo("tax", "Tax", "category", null, null, null, 0.21, false, 1)));

		Object result = ImageUtils.readSerializable(ImageUtils.writeSerializable(ticket));

		assertThat(result).isInstanceOf(TicketInfo.class);
		assertThat(((TicketInfo) result).getId()).isEqualTo(ticket.getId());
	}

	@Test
	public void rejectsClassesOutsideTheAllowlist() {
		Object result = ImageUtils.readSerializable(ImageUtils.writeSerializable(new DeserializationPayload()));

		assertThat(result).isNull();
	}

	@Test
	public void rejectsOversizedInput() {
		byte[] oversized = new byte[1024 * 1024 + 1];

		assertThat(ImageUtils.readSerializable(oversized)).isNull();
	}

	@Test
	public void rejectsDeeplyNestedInput() {
		Object nested = new ArrayList<>();
		for (int i = 0; i < 60; i++) {
			ArrayList<Object> next = new ArrayList<>();
			next.add(nested);
			nested = next;
		}

		assertThat(ImageUtils.readSerializable(ImageUtils.writeSerializable(nested))).isNull();
	}

	private static final class DeserializationPayload implements Serializable {
		private static final long serialVersionUID = 1L;
	}
}
