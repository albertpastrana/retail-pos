package com.openbravo.pos.reports;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import org.junit.jupiter.api.Test;

public class ReportsWelcomePeriodTest {
	@Test
	public void sundayStillBelongsToTheWeekThatStartedOnMonday() {
		Date sunday = date(2026, 9, 27);
		assertEquals(LocalDate.of(2026, 9, 21), start(ReportsWelcomePeriod.WEEK.current(sunday)));
		assertEquals(LocalDate.of(2025, 9, 22), start(ReportsWelcomePeriod.WEEK.previous(sunday)));
	}

	@Test
	public void monthAndYearCompareOnlyDaysElapsedRatherThanFullLastYear() {
		Date today = date(2026, 9, 25);
		assertEquals(LocalDate.of(2025, 9, 1), start(ReportsWelcomePeriod.MONTH.previous(today)));
		assertEquals(LocalDate.of(2025, 9, 26), end(ReportsWelcomePeriod.MONTH.previous(today)));
		assertEquals(LocalDate.of(2025, 1, 1), start(ReportsWelcomePeriod.YEAR.previous(today)));
		assertEquals(LocalDate.of(2025, 9, 26), end(ReportsWelcomePeriod.YEAR.previous(today)));
	}

	@Test
	public void rollingYearComparedWithImmediatelyPrecedingTwelveMonths() {
		Date today = date(2026, 9, 25);
		assertEquals(LocalDate.of(2025, 9, 25), start(ReportsWelcomePeriod.ROLLING_YEAR.current(today)));
		assertEquals(LocalDate.of(2024, 9, 25), start(ReportsWelcomePeriod.ROLLING_YEAR.previous(today)));
		assertEquals(LocalDate.of(2025, 9, 25), end(ReportsWelcomePeriod.ROLLING_YEAR.previous(today)));
	}

	private Date date(int year, int month, int day) {
		return Date.from(LocalDate.of(year, month, day).atStartOfDay(ZoneId.systemDefault()).toInstant());
	}

	private LocalDate start(SalesSummaryParameters parameters) {
		return parameters.getStartInclusive().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
	}

	private LocalDate end(SalesSummaryParameters parameters) {
		return parameters.getEndExclusive().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
	}
}
