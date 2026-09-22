package com.openbravo.pos.forms;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UpdateCheckerTest {

	@Test
	void comparesReleaseCandidatesByNumber() {
		assertTrue(UpdateChecker.isNewer("3.0.0-RC3", "3.0.0-RC1"));
		assertFalse(UpdateChecker.isNewer("3.0.0-RC1", "3.0.0-RC3"));
	}

	@Test
	void stableReleaseIsNewerThanReleaseCandidate() {
		assertTrue(UpdateChecker.isNewer("3.0.0", "3.0.0-RC3"));
		assertFalse(UpdateChecker.isNewer("3.0.0-RC3", "3.0.0"));
	}

	@Test
	void comparesNumericPartsBeforePrerelease() {
		assertTrue(UpdateChecker.isNewer("3.0.1-RC1", "3.0.0-RC3"));
		assertFalse(UpdateChecker.isNewer("3.0.0-RC3", "3.0.1-RC1"));
	}
}
