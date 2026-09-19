package com.openbravo.pos.forms;

/**
 * Starts a fresh POS process after the previous process releases its
 * single-instance lock.
 */
public final class ApplicationRestart {
	private ApplicationRestart() {
	}

	public static void main(String[] args) throws Exception {
		Thread.sleep(1000L);
		StartPOS.main(args);
	}
}
