package com.openbravo.pos.reports;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.openbravo.pos.forms.AppLocal;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class ReportsWelcomeLabelsTest {
	@Test
	void currentAndClosedPeriodsAndRollingLabelsAreConsistentAcrossLocales() {
		Locale original = Locale.getDefault();
		try {
			for (Locale locale : new Locale[]{Locale.ENGLISH, new Locale("es", "ES"), new Locale("ca", "ES")}) {
				AppLocal.setLocale(locale);
				Date asOf = date(2026, 10, 24);
				Date earlier = date(2026, 9, 15);
				for (ReportsWelcomePeriod period : new ReportsWelcomePeriod[]{ReportsWelcomePeriod.WEEK,
						ReportsWelcomePeriod.MONTH, ReportsWelcomePeriod.YEAR}) {
					Date historical = period == ReportsWelcomePeriod.YEAR ? date(2025, 10, 24) : earlier;
					assertTrue(ReportsWelcomeLabels.current(period, asOf, asOf));
					assertFalse(ReportsWelcomeLabels.current(period, historical, asOf));
					assertFalse(ReportsWelcomeLabels.title(period, historical).isBlank());
					assertFalse(ReportsWelcomeLabels.status(period, asOf, asOf).isBlank());
					assertFalse(ReportsWelcomeLabels.status(period, historical, asOf).isBlank());
					assertFalse(ReportsWelcomeLabels.comparison(period, asOf, asOf).isBlank());
					assertFalse(ReportsWelcomeLabels.comparison(period, historical, asOf).isBlank());
					assertFalse(ReportsWelcomeLabels.navigation(period, historical, asOf, -1).isBlank());
					assertFalse(ReportsWelcomeLabels.navigation(period, historical, asOf, 1).isBlank());
					assertFalse(ReportsWelcomeLabels.navigation(period, historical, asOf, 0).isBlank());
				}
				assertTrue(ReportsWelcomeLabels.current(ReportsWelcomePeriod.ROLLING_YEAR, asOf, asOf));
				assertFalse(ReportsWelcomeLabels.title(ReportsWelcomePeriod.ROLLING_YEAR, asOf).isBlank());
				assertFalse(ReportsWelcomeLabels.status(ReportsWelcomePeriod.ROLLING_YEAR, asOf, asOf).isBlank());
				assertFalse(ReportsWelcomeLabels.comparison(ReportsWelcomePeriod.ROLLING_YEAR, asOf, asOf).isBlank());
			}
		} finally {
			AppLocal.setLocale(original);
		}
	}

	@Test
	void januaryNavigationCrossesIntoPreviousYear() {
		Locale original = Locale.getDefault();
		try {
			AppLocal.setLocale(Locale.ENGLISH);
			Date january = date(2026, 1, 24);
			Date december = date(2025, 12, 31);
			assertTrue(ReportsWelcomeLabels.navigation(ReportsWelcomePeriod.MONTH, january, january, -1)
					.contains("December"));
			assertTrue(ReportsWelcomeLabels.title(ReportsWelcomePeriod.MONTH, december).contains("2025"));
			assertTrue(ReportsWelcomeLabels.navigation(ReportsWelcomePeriod.MONTH, december, january, 1)
					.contains("January"));
		} finally {
			AppLocal.setLocale(original);
		}
	}

	@Test
	void titlesStatusAndComparisonIdentifyTheSameClosedInstance() {
		Locale original = Locale.getDefault();
		try {
			AppLocal.setLocale(Locale.ENGLISH);
			Date asOf = date(2026, 10, 24);
			Date september = date(2026, 9, 30);
			Date lastWeek = date(2026, 10, 18);
			Date lastYear = date(2025, 12, 31);
			assertEquals("September 2026", ReportsWelcomeLabels.title(ReportsWelcomePeriod.MONTH, september));
			assertEquals("MONTH CLOSED", ReportsWelcomeLabels.status(ReportsWelcomePeriod.MONTH, september, asOf));
			assertTrue(ReportsWelcomeLabels.comparison(ReportsWelcomePeriod.MONTH, september, asOf)
					.contains("september 2025"));
			assertTrue(ReportsWelcomeLabels.title(ReportsWelcomePeriod.WEEK, lastWeek).contains("12–18 Oct"));
			assertEquals("WEEK CLOSED", ReportsWelcomeLabels.status(ReportsWelcomePeriod.WEEK, lastWeek, asOf));
			assertTrue(ReportsWelcomeLabels.comparison(ReportsWelcomePeriod.WEEK, lastWeek, asOf).contains("2025"));
			assertEquals("Year 2025", ReportsWelcomeLabels.title(ReportsWelcomePeriod.YEAR, lastYear));
			assertEquals("YEAR CLOSED", ReportsWelcomeLabels.status(ReportsWelcomePeriod.YEAR, lastYear, asOf));
			assertTrue(ReportsWelcomeLabels.comparison(ReportsWelcomePeriod.YEAR, lastYear, asOf).contains("2024"));
			assertTrue(ReportsWelcomeLabels.status(ReportsWelcomePeriod.MONTH, asOf, asOf).contains("24"));
		} finally {
			AppLocal.setLocale(original);
		}
	}

	@Test
	void elapsedCaptionsNameBaselineYearAndExactWeekdaysWithoutAmbiguousDayNumber() {
		Locale original = Locale.getDefault();
		try {
			Date lastClosedDay = date(2026, 10, 24);
			AppLocal.setLocale(new Locale("ca", "ES"));
			assertEquals("Comparat amb la setmana del 20 al 26 d’oct. de 2025, de dilluns a dissabte.",
					ReportsWelcomeLabels.comparison(ReportsWelcomePeriod.WEEK, lastClosedDay, lastClosedDay));
			assertTrue(ReportsWelcomeLabels.comparison(ReportsWelcomePeriod.MONTH, lastClosedDay, lastClosedDay)
					.contains("de l’1 al 24 d’oct. de 2025"));
			assertTrue(ReportsWelcomeLabels.comparison(ReportsWelcomePeriod.YEAR, lastClosedDay, lastClosedDay)
					.contains("al 24 d’oct. de 2025"));
			assertEquals("Aquesta setmana",
					ReportsWelcomeLabels.seriesCurrent(ReportsWelcomePeriod.WEEK, lastClosedDay, lastClosedDay));
			assertEquals("Mateixa setmana de 2025",
					ReportsWelcomeLabels.seriesPrevious(ReportsWelcomePeriod.WEEK, "", lastClosedDay));
			for (Locale locale : new Locale[]{Locale.ENGLISH, new Locale("es", "ES")}) {
				AppLocal.setLocale(locale);
				for (ReportsWelcomePeriod period : new ReportsWelcomePeriod[]{ReportsWelcomePeriod.WEEK,
						ReportsWelcomePeriod.MONTH, ReportsWelcomePeriod.YEAR}) {
					String caption = ReportsWelcomeLabels.comparison(period, lastClosedDay, lastClosedDay);
					assertTrue(caption.contains("2025"), caption);
					assertTrue(caption.contains("24") || period == ReportsWelcomePeriod.WEEK, caption);
					assertFalse(caption.contains("(through 25)"), caption);
				}
			}
		} finally {
			AppLocal.setLocale(original);
		}
	}

	@Test
	void compactTotalRangeKeepsDatesReadableAcrossMonthAndYearBoundaries() {
		Locale original = Locale.getDefault();
		try {
			AppLocal.setLocale(Locale.ENGLISH);
			assertEquals("10/12–18/26", ReportsWelcomeLabels.focusRange(ReportsWelcomePeriod.WEEK, date(2026, 10, 18)));
			assertEquals("12/29/25–1/4/26",
					ReportsWelcomeLabels.focusRange(ReportsWelcomePeriod.WEEK, date(2026, 1, 4)));
			assertEquals("1/1–24/26", ReportsWelcomeLabels.focusRange(ReportsWelcomePeriod.MONTH, date(2026, 1, 24)));
			assertEquals("10/24/25–10/24/26",
					ReportsWelcomeLabels.focusRange(ReportsWelcomePeriod.ROLLING_YEAR, date(2026, 10, 24)));
			AppLocal.setLocale(new Locale("ca", "ES"));
			assertEquals("12–18/10/26", ReportsWelcomeLabels.focusRange(ReportsWelcomePeriod.WEEK, date(2026, 10, 18)));
		} finally {
			AppLocal.setLocale(original);
		}
	}

	private static Date date(int year, int month, int day) {
		Calendar calendar = Calendar.getInstance();
		calendar.clear();
		calendar.set(year, month - 1, day);
		return calendar.getTime();
	}
}
