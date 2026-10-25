package com.openbravo.pos.reports;

import com.openbravo.pos.forms.AppLocal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Labels for the selected (potentially historical) period. */
final class ReportsWelcomeLabels {
	private ReportsWelcomeLabels() {
	}

	static boolean current(ReportsWelcomePeriod period, Date focus, Date asOf) {
		return period == ReportsWelcomePeriod.ROLLING_YEAR
				|| period.current(focus).getStartInclusive().equals(period.current(asOf).getStartInclusive());
	}

	static String month(Date date) {
		return new SimpleDateFormat("LLLL", Locale.getDefault()).format(date);
	}

	static String monthYear(Date date) {
		return new SimpleDateFormat("LLLL yyyy", Locale.getDefault()).format(date);
	}

	static String partialMonthLegend(Date focus) {
		Calendar previous = Calendar.getInstance();
		previous.setTime(focus);
		previous.add(Calendar.YEAR, -1);
		return AppLocal.getIntString("reports.welcome.partialMonthLegend", capitalize(monthYear(previous.getTime())),
				previous.get(Calendar.DAY_OF_MONTH));
	}

	static String title(ReportsWelcomePeriod period, Date focus) {
		Calendar start = Calendar.getInstance();
		start.setTime(period.current(focus).getStartInclusive());
		switch (period) {
			case WEEK :
				Calendar sunday = (Calendar) start.clone();
				sunday.add(Calendar.DAY_OF_MONTH, 6);
				String month = new SimpleDateFormat("MMM", Locale.getDefault()).format(sunday.getTime());
				String key = "reports.welcome.weekTitle";
				if ("ca".equals(Locale.getDefault().getLanguage()) && (month.toLowerCase(Locale.ROOT).startsWith("a")
						|| month.toLowerCase(Locale.ROOT).startsWith("o"))) {
					key += "Vowel";
				}
				return AppLocal.getIntString(key, start.get(Calendar.DAY_OF_MONTH), sunday.get(Calendar.DAY_OF_MONTH),
						month);
			case MONTH :
				return capitalize(monthYear(focus));
			case YEAR :
				return AppLocal.getIntString("reports.welcome.yearTitle", Integer.toString(start.get(Calendar.YEAR)));
			default :
				return AppLocal.getIntString("reports.welcome.rollingTitle");
		}
	}

	static String status(ReportsWelcomePeriod period, Date focus, Date asOf) {
		if (period == ReportsWelcomePeriod.ROLLING_YEAR) {
			return AppLocal.getIntString("reports.welcome.rollingStatus");
		}
		if (!current(period, focus, asOf)) {
			return AppLocal.getIntString("reports.welcome.closed." + period.name().toLowerCase(Locale.ROOT));
		}
		Calendar day = Calendar.getInstance();
		day.setTime(asOf);
		String suffix = period == ReportsWelcomePeriod.WEEK
				? new SimpleDateFormat("EEEE", Locale.getDefault()).format(asOf)
				: period == ReportsWelcomePeriod.YEAR
						? new SimpleDateFormat("d MMM", Locale.getDefault()).format(asOf)
						: Integer.toString(day.get(Calendar.DAY_OF_MONTH));
		return AppLocal.getIntString("reports.welcome.current." + period.name().toLowerCase(Locale.ROOT), suffix)
				.toUpperCase(Locale.getDefault());
	}

	static String comparison(ReportsWelcomePeriod period, Date focus, Date asOf) {
		SalesSummaryParameters baseline = period.previous(focus);
		Calendar start = Calendar.getInstance();
		start.setTime(baseline.getStartInclusive());
		Calendar end = Calendar.getInstance();
		end.setTime(baseline.getEndExclusive());
		end.add(Calendar.DAY_OF_MONTH, -1);
		if (period == ReportsWelcomePeriod.ROLLING_YEAR) {
			return AppLocal.getIntString("reports.welcome.compareRolling",
					DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault())
							.format(baseline.getStartInclusive()),
					DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault()).format(end.getTime()));
		}
		String baselineTitle = title(period, baseline.getStartInclusive());
		String language = Locale.getDefault().getLanguage();
		String baselineName = period == ReportsWelcomePeriod.MONTH && !"es".equals(language) && !"ca".equals(language)
				? baselineTitle
				: lowerFirst(baselineTitle);
		if (period == ReportsWelcomePeriod.WEEK) {
			String year = Integer.toString(start.get(Calendar.YEAR));
			if (current(period, focus, asOf)) {
				return AppLocal.getIntString("reports.welcome.compareWeekElapsed", baselineName, year,
						new SimpleDateFormat("EEEE", Locale.getDefault()).format(start.getTime()),
						new SimpleDateFormat("EEEE", Locale.getDefault()).format(end.getTime()));
			}
			return AppLocal.getIntString("reports.welcome.compareWeekClosed", baselineName, year);
		}
		if (current(period, focus, asOf)) {
			if (period == ReportsWelcomePeriod.MONTH) {
				return AppLocal.getIntString("reports.welcome.compareMonthElapsed", baselineName,
						Integer.toString(end.get(Calendar.DAY_OF_MONTH)), monthOfYear(end.getTime()));
			}
			return AppLocal.getIntString("reports.welcome.compareYearElapsed", baselineName,
					new SimpleDateFormat("MMM", Locale.getDefault()).format(start.getTime()),
					Integer.toString(end.get(Calendar.DAY_OF_MONTH)), monthOfYear(end.getTime()));
		}
		DateFormat date = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());
		return AppLocal.getIntString("reports.welcome.compareClosed", baselineName, date.format(start.getTime()),
				date.format(end.getTime()));
	}

	private static String monthOfYear(Date date) {
		String month = new SimpleDateFormat("MMM", Locale.getDefault()).format(date);
		String key = "reports.welcome.monthOfYear";
		if ("ca".equals(Locale.getDefault().getLanguage())
				&& (month.toLowerCase(Locale.ROOT).startsWith("a") || month.toLowerCase(Locale.ROOT).startsWith("o"))) {
			key += "Vowel";
		}
		return AppLocal.getIntString(key, month, new SimpleDateFormat("yyyy", Locale.getDefault()).format(date));
	}

	static String seriesCurrent(ReportsWelcomePeriod period, Date focus, Date asOf) {
		return period == ReportsWelcomePeriod.WEEK && current(period, focus, asOf)
				? AppLocal.getIntString("reports.welcome.thisWeekSeries")
				: chartPeriod(period, focus);
	}

	static String seriesPrevious(ReportsWelcomePeriod period, String periodName, Date focus) {
		if (period == ReportsWelcomePeriod.WEEK) {
			return AppLocal.getIntString("reports.welcome.previousWeekSeries",
					new SimpleDateFormat("yyyy", Locale.getDefault())
							.format(period.previous(focus).getStartInclusive()));
		}
		return periodName;
	}

	static String navigation(ReportsWelcomePeriod period, Date focus, Date asOf, int direction) {
		if (direction == 0) {
			return AppLocal.getIntString("reports.welcome.back." + period.name().toLowerCase(Locale.ROOT),
					period == ReportsWelcomePeriod.MONTH
							? month(asOf)
							: period == ReportsWelcomePeriod.YEAR ? new SimpleDateFormat("yyyy").format(asOf) : "");
		}
		Calendar adjacent = Calendar.getInstance();
		adjacent.setTime(period.current(focus).getStartInclusive());
		adjacent.add(
				period == ReportsWelcomePeriod.WEEK
						? Calendar.DAY_OF_MONTH
						: period == ReportsWelcomePeriod.MONTH ? Calendar.MONTH : Calendar.YEAR,
				period == ReportsWelcomePeriod.WEEK ? direction * 7 : direction);
		String target = period == ReportsWelcomePeriod.MONTH
				? month(adjacent.getTime())
				: period == ReportsWelcomePeriod.YEAR ? Integer.toString(adjacent.get(Calendar.YEAR)) : "";
		return AppLocal.getIntString(
				"reports.welcome." + (direction < 0 ? "prev." : "next.") + period.name().toLowerCase(Locale.ROOT),
				capitalize(target));
	}

	static String chartPeriod(ReportsWelcomePeriod period, Date focus) {
		if (period == ReportsWelcomePeriod.MONTH) {
			return monthYear(focus);
		}
		if (period == ReportsWelcomePeriod.YEAR) {
			return new SimpleDateFormat("yyyy", Locale.getDefault()).format(focus);
		}
		if (period == ReportsWelcomePeriod.ROLLING_YEAR) {
			Calendar start = Calendar.getInstance();
			start.setTime(focus);
			start.set(Calendar.DAY_OF_MONTH, 1);
			start.add(Calendar.MONTH, -11);
			return AppLocal.getIntString("reports.welcome.chartRolling", monthYear(start.getTime()), monthYear(focus));
		}
		return title(period, focus);
	}

	private static String capitalize(String text) {
		return text.isEmpty() ? text : text.substring(0, 1).toUpperCase(Locale.getDefault()) + text.substring(1);
	}

	private static String lowerFirst(String text) {
		return text.isEmpty() ? text : text.substring(0, 1).toLowerCase(Locale.getDefault()) + text.substring(1);
	}
}
