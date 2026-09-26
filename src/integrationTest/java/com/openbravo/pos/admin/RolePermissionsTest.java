package com.openbravo.pos.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Locale;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.data.user.DirtyManager;
import org.junit.jupiter.api.Test;

class RolePermissionsTest {
	@Test
	void catalogueCoversAllShippedRolesAndRoundTripsTheirKeys() throws Exception {
		assertThat(RolePermissions.catalogue()).hasSizeGreaterThan(50).doesNotHaveDuplicates();
		for (String name : List.of("Administrator", "Manager", "Seller", "Employee", "Guest")) {
			try (InputStream input = getClass()
					.getResourceAsStream("/com/openbravo/pos/templates/Role." + name + ".xml")) {
				String xml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
				Set<String> keys = RolePermissions.keys(xml);
				assertThat(RolePermissions.catalogue()).containsAll(keys);
				assertThat(RolePermissions.keys(RolePermissions.update(xml, keys)))
						.containsExactlyInAnyOrderElementsOf(keys);
			}
		}
	}

	@Test
	void editingKnownKeysPreservesUnknownKeysAndOtherXmlNodes() throws Exception {
		String xml = "<?xml version=\"1.0\"?><permissions><!-- retained -->"
				+ "<class name=\"sales.Total\"/><class name=\"custom.Unknown\"/>"
				+ "<other extra=\"yes\"/></permissions>";
		Set<String> selected = new LinkedHashSet<>(List.of("sales.EditLines"));
		String updated = RolePermissions.update(xml, selected);
		assertThat(RolePermissions.keys(updated)).containsExactlyInAnyOrder("sales.EditLines", "custom.Unknown");
		assertThat(updated).contains("retained", "extra=\"yes\"");
	}

	@Test
	void externalEntitiesAreRejected() {
		assertThatThrownBy(() -> RolePermissions.keys("<!DOCTYPE permissions [<!ENTITY x SYSTEM 'file:///etc/passwd'>]>"
				+ "<permissions><class name='&x;'/></permissions>")).isInstanceOf(Exception.class);
	}

	@Test
	void openingAndSavingAnUnchangedRoleKeepsItsStoredXmlBytes() throws Exception {
		try (InputStream input = getClass()
				.getResourceAsStream("/com/openbravo/pos/templates/Role.Administrator.xml")) {
			byte[] xml = input.readAllBytes();
			RolesView view = new RolesView(new DirtyManager());
			view.writeValueEdit(new Object[]{"0", "Administrator", xml});
			assertThat((byte[]) ((Object[]) view.createValue())[2]).isEqualTo(xml);
		}
	}

	@Test
	void everyCataloguedPermissionHasALabelInAllSupportedLocales() {
		Locale previous = Locale.getDefault();
		try {
			for (Locale locale : List.of(Locale.ENGLISH, new Locale("es"), new Locale("ca"))) {
				AppLocal.setLocale(locale);
				assertThat(AppLocal.getIntString("Admin.AccessCard")).isEqualTo(switch (locale.getLanguage()) {
					case "es" -> "Tarjeta de acceso";
					case "ca" -> "Targeta d'accés";
					default -> "Access card";
				});
				for (String key : RolePermissions.catalogue()) {
					assertThat(RolePermissions.label(key)).as(key + " in " + locale).isNotBlank()
							.doesNotStartWith("** ");
				}
			}
		} finally {
			AppLocal.setLocale(previous);
		}
	}
}
