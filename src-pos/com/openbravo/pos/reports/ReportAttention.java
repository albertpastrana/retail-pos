package com.openbravo.pos.reports;

import java.awt.Color;

/** A factual, actionable notice linking to an existing report. */
final class ReportAttention {
	final String label;
	final String task;
	final Color color;

	ReportAttention(String label, String task, Color color) {
		this.label = label;
		this.task = task;
		this.color = color;
	}
}
