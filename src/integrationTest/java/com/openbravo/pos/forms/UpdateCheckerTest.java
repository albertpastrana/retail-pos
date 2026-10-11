package com.openbravo.pos.forms;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;

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

	@Test
	void buildsDetachedWindowsUpdaterCommandWithAllArguments() {
		File updater = new File("update.bat");
		File packageFile = new File("update.zip");
		File installDir = new File("retail-pos");
		List<String> command = UpdateChecker.windowsUpdaterCommand(updater, packageFile, installDir, "1234");

		assertEquals(List.of("cmd", "/d", "/c", "start", "", "/b", updater.getAbsolutePath(),
				packageFile.getAbsolutePath(), installDir.getAbsolutePath(), "1234"), command);
	}
}
