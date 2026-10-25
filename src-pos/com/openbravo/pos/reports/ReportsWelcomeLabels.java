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
		return new SimpleDateFormat("MMMM", Locale.getDefault()).format(date);
	}

	static String monthYear(Date date) {
		return new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(date);
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
		Calendar end = Calendar.getInstance();
		end.setTime(baseline.getEndExclusive());
		end.add(Calendar.DAY_OF_MONTH, -1);
		if (period == ReportsWelcomePeriod.ROLLING_YEAR) {
			return AppLocal.getIntString("reports.welcome.compareRolling",
					DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault())
							.format(baseline.getStartInclusive()),
					DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault()).format(end.getTime()));
		}
		String baselineName = title(period, baseline.getStartInclusive());
		if (period == ReportsWelcomePeriod.WEEK) {
			Calendar year = Calendar.getInstance();
			year.setTime(baseline.getStartInclusive());
			baselineName += " " + year.get(Calendar.YEAR);
		}
		if (current(period, focus, asOf)) {
			return AppLocal.getIntString("reports.welcome.compareElapsed", baselineName,
					end.get(Calendar.DAY_OF_MONTH));
		}
		return AppLocal.getIntString("reports.welcome.compareClosed", baselineName);
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
}
