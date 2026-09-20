package com.openbravo.pos.forms;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

public class SellerSalesPermissionsTest {

	@Test
	public void sellerRoleExposesPrivilegedSalesControls() throws Exception {
		try (InputStream input = getClass().getResourceAsStream("/com/openbravo/pos/templates/Role.Seller.xml")) {
			assertTrue(input != null);
			byte[] content = new byte[input.available()];
			int offset = 0;
			while (offset < content.length) {
				offset += input.read(content, offset, content.length - offset);
			}
			String permissions = new String(content, StandardCharsets.UTF_8);

			assertTrue(permissions.contains("<class name=\"button.opendrawer\"/>"));
			assertTrue(permissions.contains("<class name=\"button.discount\"/>"));
			assertTrue(permissions.contains("<class name=\"button.discount.total\"/>"));
			assertTrue(permissions.contains("<class name=\"com.openbravo.pos.panels.JPanelCloseMoney\"/>"));
		}
	}
}
