package com.openbravo.pos.forms;

import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;

final class ConfigurationPermissions {
	private ConfigurationPermissions() {
	}

	static Set<PosixFilePermission> privateFile() {
		return EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
	}
}
