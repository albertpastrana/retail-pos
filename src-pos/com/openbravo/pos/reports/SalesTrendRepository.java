package com.openbravo.pos.reports;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormatSymbols;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Locale;

/**
 * Reads payment totals per receipt and buckets them without database-specific
 * date SQL.
 */
final class SalesTrendRepository {
	private static final String SALES = "SELECT R.DATENEW, COALESCE(P.TOTAL, 0) FROM RECEIPTS R "
			+ "JOIN TICKETS T ON T.ID = R.ID AND T.TICKETTYPE IN (0, 1) "
			+ "LEFT JOIN (SELECT RECEIPT, SUM(TOTAL) AS TOTAL FROM PAYMENTS GROUP BY RECEIPT) P ON P.RECEIPT = R.ID "
			+ "WHERE R.DATENEW >= ? AND R.DATENEW < ?";

	SalesTrend load(Connection connection, ReportsWelcomePeriod period, Date now) throws SQLException {
		LocalDate today = now.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		LocalDate start;
		int size;
		boolean monthly = period == ReportsWelcomePeriod.YEAR || period == ReportsWelcomePeriod.ROLLING_YEAR;
		switch (period) {
			case WEEK :
				start = today.minusDays(today.getDayOfWeek().getValue() - 1);
				size = 7;
				break;
			case MONTH :
				start = today.withDayOfMonth(1);
				size = today.lengthOfMonth();
				break;
			case YEAR :
				start = today.withDayOfYear(1);
				size = 12;
				break;
			case ROLLING_YEAR :
				start = today.withDayOfMonth(1).minusMonths(11);
				size = 12;
				break;
			default :
				throw new IllegalStateException("Unknown chart period");
		}
		String[] labels = new String[size];
		String[] weekdays = new DateFormatSymbols(Locale.getDefault()).getShortWeekdays();
		String[] months = new DateFormatSymbols(Locale.getDefault()).getShortMonths();
		for (int i = 0; i < size; i++) {
			LocalDate bucket = monthly ? start.plusMonths(i) : start.plusDays(i);
			labels[i] = monthly
					? months[bucket.getMonthValue() - 1]
					: period == ReportsWelcomePeriod.WEEK
							? weekdays[bucket.getDayOfWeek().getValue() % 7 + 1]
							: Integer.toString(bucket.getDayOfMonth());
		}
		SalesTrend trend = new SalesTrend(labels);
		LocalDate previousStart = period == ReportsWelcomePeriod.WEEK ? start.minusDays(364) : start.minusYears(1);
		LocalDate previousEnd = monthly
				? previousStart.plusMonths(size)
				: period == ReportsWelcomePeriod.WEEK ? previousStart.plusDays(7) : previousStart.plusMonths(1);
		read(connection, previousStart, previousEnd, previousStart, monthly, trend.previous);
		read(connection, start, today.plusDays(1), start, monthly, trend.current);
		for (int i = 0; i < size; i++) {
			trend.reached[i] = !(monthly ? start.plusMonths(i).isAfter(today) : start.plusDays(i).isAfter(today));
		}
		return trend;
	}

	private void read(Connection connection, LocalDate from, LocalDate until, LocalDate first, boolean monthly,
			double[] amounts) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(SALES)) {
			statement.setTimestamp(1, timestamp(from));
			statement.setTimestamp(2, timestamp(until));
			try (ResultSet result = statement.executeQuery()) {
				while (result.next()) {
					LocalDate date = result.getTimestamp(1).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
					int index = (int) (monthly
							? ChronoUnit.MONTHS.between(YearMonth.from(first), YearMonth.from(date))
							: ChronoUnit.DAYS.between(first, date));
					if (index >= 0 && index < amounts.length) {
						amounts[index] += result.getDouble(2);
					}
				}
			}
		}
	}

	private Timestamp timestamp(LocalDate day) {
		return Timestamp.from(day.atStartOfDay(ZoneId.systemDefault()).toInstant());
	}
}
