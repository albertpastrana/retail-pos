package com.openbravo.pos.reports;

import com.openbravo.pos.forms.AppLocal;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** The four fixed periods shown by the reports landing screen. */
enum ReportsWelcomePeriod {
	WEEK("reports.welcome.week"), MONTH("reports.welcome.month"), YEAR("reports.welcome.year"), ROLLING_YEAR(
			"reports.welcome.rollingyear");

	private final String labelKey;

	ReportsWelcomePeriod(String labelKey) {
		this.labelKey = labelKey;
	}

	String getLabel() {
		return AppLocal.getIntString(labelKey);
	}

	SalesSummaryParameters current() {
		return current(new Date());
	}

	SalesSummaryParameters previous() {
		return previous(new Date());
	}

	SalesSummaryParameters current(Date date) {
		Calendar today = Calendar.getInstance();
		today.setTime(date);
		Calendar start = startOfDay(today);
		Calendar end = (Calendar) start.clone();
		end.add(Calendar.DAY_OF_MONTH, 1);
		switch (this) {
			case WEEK :
				start.add(Calendar.DAY_OF_MONTH, -dayOfWeekFromMonday(today));
				break;
			case MONTH :
				start.set(Calendar.DAY_OF_MONTH, 1);
				break;
			case YEAR :
				start.set(Calendar.DAY_OF_YEAR, 1);
				break;
			case ROLLING_YEAR :
				start.add(Calendar.MONTH, -12);
				break;
			default :
				throw new IllegalStateException("Unknown dashboard period");
		}
		return new SalesSummaryParameters(start.getTime(), end.getTime());
	}

	SalesSummaryParameters previous(Date date) {
		SalesSummaryParameters current = current(date);
		Calendar start = Calendar.getInstance();
		start.setTime(current.getStartInclusive());
		Calendar end = Calendar.getInstance();
		end.setTime(current.getEndExclusive());
		if (this == ROLLING_YEAR) {
			end.setTime(current.getStartInclusive());
			start.add(Calendar.MONTH, -12);
		} else if (this == WEEK) {
			start.add(Calendar.DAY_OF_MONTH, -364);
			end.add(Calendar.DAY_OF_MONTH, -364);
		} else {
			start.add(Calendar.YEAR, -1);
			end.add(Calendar.YEAR, -1);
		}
		return new SalesSummaryParameters(start.getTime(), end.getTime());
	}

	String rangeLabel(Date date) {
		DateFormat day = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());
		SalesSummaryParameters range = current(date);
		Calendar end = Calendar.getInstance();
		end.setTime(range.getEndExclusive());
		end.add(Calendar.DAY_OF_MONTH, -1);
		String endLabel = day.format(end.getTime());
		Calendar today = Calendar.getInstance();
		today.set(Calendar.HOUR_OF_DAY, 0);
		today.set(Calendar.MINUTE, 0);
		today.set(Calendar.SECOND, 0);
		today.set(Calendar.MILLISECOND, 0);
		if (end.getTime().equals(today.getTime())) {
			endLabel = AppLocal.getIntString("reports.welcome.today");
		}
		return day.format(range.getStartInclusive()) + " – " + endLabel;
	}

	String baselineLabel() {
		return AppLocal.getIntString(this == ROLLING_YEAR
				? "reports.welcome.previous12"
				: this == YEAR ? "reports.welcome.previousSpan" : "reports.welcome.previousDays");
	}

	private static Calendar startOfDay(Calendar source) {
		Calendar result = (Calendar) source.clone();
		result.set(Calendar.HOUR_OF_DAY, 0);
		result.set(Calendar.MINUTE, 0);
		result.set(Calendar.SECOND, 0);
		result.set(Calendar.MILLISECOND, 0);
		return result;
	}

	private static int dayOfWeekFromMonday(Calendar date) {
		int day = date.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY;
		return day < 0 ? day + 7 : day;
	}
}
