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

import java.awt.Font;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import com.openbravo.pos.config.JFrmConfig;
import com.openbravo.format.Formats;
import com.openbravo.pos.instance.InstanceQuery;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.LookAndFeel;
import net.sf.jasperreports.engine.util.JRProperties;

/**
 *
 * @author adrianromero
 */
public class StartPOS {

	private static Logger logger = Logger.getLogger("com.openbravo.pos.forms.StartPOS");

	// Staff read the till standing up and at arm's length, so every look and feel
	// font is bigger than the desktop default.
	private static final float FONT_SCALE = 1.1f;

	/** Creates a new instance of StartPOS */
	private StartPOS() {
	}

	public static boolean registerApp() {

		// vemos si existe alguna instancia
		InstanceQuery i = null;
		try {
			i = new InstanceQuery();
			i.getAppMessage().restoreWindow();
			return false;
		} catch (Exception e) {
			return true;
		}
	}

	// The JDT compiler bundled with JasperReports 3.1.4 cannot read class files
	// newer
	// than Java 5, so report expressions only compile on Java 8. Use javac where
	// the
	// running JDK offers it.
	private static void setReportCompiler() {
		try {
			Class.forName("com.sun.tools.javac.Main");
			JRProperties.setProperty("net.sf.jasperreports.compiler.class",
					"net.sf.jasperreports.engine.design.JRJdk13Compiler");
		} catch (ClassNotFoundException e) {
			logger.log(Level.WARNING, "javac not available, reports will only work on Java 8", e);
		}
	}

	// The scaled fonts go into the UIManager defaults, which win over the look and
	// feel ones, so this has to run after the look and feel is set and before any
	// component is created. Read every font first: scaling as we walk the defaults
	// can read a key that was already scaled and shrink or grow it twice.
	private static void enlargeFonts() {

		UIDefaults lafdefaults = UIManager.getLookAndFeelDefaults();
		Map<Object, Font> fonts = new HashMap<Object, Font>();

		for (Object key : Collections.list(lafdefaults.keys())) {
			Object value = lafdefaults.get(key);
			if (value instanceof Font) {
				Font font = (Font) value;
				fonts.put(key, font.deriveFont(font.getSize2D() * FONT_SCALE));
			}
		}

		for (Map.Entry<Object, Font> font : fonts.entrySet()) {
			UIManager.put(font.getKey(), new FontUIResource(font.getValue()));
		}
	}

	public static void main(final String args[]) {

		FileLogging.install();
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread thread, Throwable throwable) {
				logger.log(Level.SEVERE, "Unhandled exception thread=" + thread.getName(), throwable);
			}
		});
		logger.info("event=application_start version=" + AppLocal.APP_VERSION + " args=" + args.length);

		setReportCompiler();

		if (args.length > 0 && args[0].equals("configure")) {
			String[] newArgs = new String[args.length - 1];
			for (int i = 1, k = 0; i < args.length; i++) {
				newArgs[k++] = args[i];
			}
			JFrmConfig.main(newArgs);
			return;
		}
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {

				if (!registerApp()) {
					System.exit(1);
				}

				AppConfig config = new AppConfig(args);
				config.load();

				// set Locale.
				String slang = config.getProperty("user.language");
				String scountry = config.getProperty("user.country");
				String svariant = config.getProperty("user.variant");
				if (slang != null && !slang.equals("") && scountry != null && svariant != null) {
					Locale.setDefault(new Locale(slang, scountry, svariant));
				}

				// Set the format patterns
				Formats.setIntegerPattern(config.getProperty("format.integer"));
				Formats.setDoublePattern(config.getProperty("format.double"));
				Formats.setCurrencyPattern(config.getProperty("format.currency"));
				Formats.setPercentPattern(config.getProperty("format.percent"));
				Formats.setDatePattern(config.getProperty("format.date"));
				Formats.setTimePattern(config.getProperty("format.time"));
				Formats.setDateTimePattern(config.getProperty("format.datetime"));

				// Set the look and feel.
				try {

					Object laf = Class.forName(config.getProperty("swing.defaultlaf")).newInstance();

					if (laf instanceof LookAndFeel) {
						UIManager.setLookAndFeel((LookAndFeel) laf);
					}
				} catch (Exception e) {
					logger.log(Level.WARNING, "Cannot set look and feel", e);
				}

				enlargeFonts();

				String screenmode = config.getProperty("machine.screenmode");
				if ("fullscreen".equals(screenmode)) {
					JRootKiosk rootkiosk = new JRootKiosk();
					rootkiosk.initFrame(config);
					UpdateChecker.checkAsync(config, rootkiosk);
				} else {
					JRootFrame rootframe = new JRootFrame();
					rootframe.initFrame(config);
					UpdateChecker.checkAsync(config, rootframe);
				}
			}
		});
	}
}
