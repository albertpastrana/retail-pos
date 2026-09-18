//    Openbravo POS is a point of sales application designed for touch screens.
//    Copyright (C) 2007-2009 Openbravo, S.L.
//    http://www.openbravo.com/product/pos
//
//    This file is part of Openbravo POS.
//
//    Openbravo POS is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    Openbravo POS is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with Openbravo POS.  If not, see <http://www.gnu.org/licenses/>.

package com.openbravo.pos.forms;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * File logs at {@code <install>/logs/pos-%g.log} ({@code dirname.path}, else
 * cwd).
 */
public final class FileLogging {

	private static final int LIMIT = 10 * 1024 * 1024;
	private static final int COUNT = 5;

	private static boolean installed;

	private FileLogging() {
	}

	public static synchronized void install() {
		if (installed) {
			return;
		}
		installed = true;

		String base = System.getProperty("dirname.path");
		if (base == null || base.isEmpty()) {
			base = System.getProperty("user.dir");
		}
		File dir = new File(base, "logs");
		if (!dir.isDirectory() && !dir.mkdirs()) {
			System.err.println("Cannot create log directory: " + dir.getAbsolutePath());
			return;
		}

		try {
			FileHandler handler = new FileHandler(new File(dir, "pos-%g.log").getPath(), LIMIT, COUNT, true);
			handler.setEncoding("UTF-8");
			handler.setFormatter(new LogFormatter());
			handler.setLevel(Level.INFO);
			Logger.getLogger("").addHandler(handler);
		} catch (IOException e) {
			System.err.println("Cannot open log file in " + dir.getAbsolutePath() + ": " + e);
		}
	}
}
