package com.openbravo.pos.forms;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class SupervisorAuthorizationTest {

	@Test
	public void onlyCanonicalSupervisorsCanAuthorize() {
		AppUser manager = new AppUser("1", "Manager", "plain:secret", null, AppUser.ROLE_MANAGER, null);
		AppUser seller = new AppUser("2", "Seller", "plain:secret", null, AppUser.ROLE_SELLER, null);

		assertTrue(SupervisorAuthorization.authenticate(manager, "secret"));
		assertFalse(SupervisorAuthorization.authenticate(manager, "wrong"));
		assertFalse(SupervisorAuthorization.authenticate(seller, "secret"));
	}

	@Test
	public void emptySupervisorPasswordUsesExistingAuthenticationRule() {
		AppUser administrator = new AppUser("0", "Administrator", null, null, AppUser.ROLE_ADMINISTRATOR, null);

		assertTrue(SupervisorAuthorization.authenticate(administrator, null));
	}
}
