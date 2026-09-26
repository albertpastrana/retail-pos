package com.openbravo.pos.admin;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

final class ResourceOriginal {
	private static final Map<String, Optional<byte[]>> CACHE = new ConcurrentHashMap<>();
	private ResourceOriginal() {
	}

	static byte[] bytes(String name, int type) {
		if (name == null || name.contains("/") || name.contains("\\"))
			return null;
		return CACHE.computeIfAbsent(type + ":" + name, key -> Optional.ofNullable(load(name, type))).orElse(null);
	}

	private static byte[] load(String name, int type) {
		String suffix = type == 0
				? (name.startsWith("Printer.") || name.startsWith("Role.") || name.startsWith("Ticket.")
						|| name.startsWith("ticketline_") ? ".xml" : ".txt")
				: type == 1 ? ".png" : "";
		try (InputStream stream = ResourceOriginal.class
				.getResourceAsStream("/com/openbravo/pos/templates/" + name + suffix)) {
			return stream == null ? null : stream.readAllBytes();
		} catch (IOException ex) {
			return null;
		}
	}

	static boolean differs(Object[] row) {
		byte[] original = bytes((String) row[1], ((Number) row[2]).intValue());
		return original != null && !Arrays.equals(original, (byte[]) row[3]);
	}
}
