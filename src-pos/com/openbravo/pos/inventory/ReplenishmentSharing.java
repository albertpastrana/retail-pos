package com.openbravo.pos.inventory;

import com.openbravo.basic.BasicException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Keep the send confirmation and successful file write as the only
 * status-changing boundaries.
 */
public final class ReplenishmentSharing {
	private ReplenishmentSharing() {
	}

	public static void afterWhatsApp(DataLogicReplenishment data, List<ReplenishmentEntry> included, String user,
			boolean confirmed) throws BasicException {
		if (confirmed)
			data.markExportedPending(included, user);
	}

	public static void saveTextFile(DataLogicReplenishment data, List<ReplenishmentEntry> included, String user,
			Path target, String message) throws IOException, BasicException {
		Files.writeString(target, message, StandardCharsets.UTF_8);
		data.markExportedPending(included, user);
	}
}
