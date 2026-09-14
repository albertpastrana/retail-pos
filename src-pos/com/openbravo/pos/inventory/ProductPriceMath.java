package com.openbravo.pos.inventory;

import com.openbravo.basic.BasicException;
import com.openbravo.format.Formats;
import com.openbravo.pos.forms.AppLocal;

/**
 * The numbers behind a product price: factory cost, VAT, the till price, and
 * the two kinds of margin the shop uses. The editor and the till dialog both go
 * through here so they cannot disagree.
 */
public final class ProductPriceMath {

	private ProductPriceMath() {
	}

	public static Double parseCurrency(String value) {
		try {
			return (Double) Formats.CURRENCY.parseValue(value);
		} catch (BasicException e) {
			return null;
		}
	}

	public static Double parsePositiveCurrency(String value, boolean emptyIsZero) {
		if (value == null || value.trim().isEmpty()) {
			return emptyIsZero ? Double.valueOf(0.0) : null;
		}
		Double parsed = parseCurrency(value);
		return parsed == null || parsed.doubleValue() < 0.0 ? null : parsed;
	}

	public static Double parsePercent(String value) {
		try {
			return (Double) Formats.PERCENT.parseValue(value);
		} catch (BasicException e) {
			return null;
		}
	}

	public static String formatCurrency(Object value) {
		return Formats.CURRENCY.formatValue(value);
	}

	public static String formatPercent(Double value) {
		return value == null ? null : Formats.PERCENT.formatValue(value);
	}

	public static String secondaryLabel(TaxRegime regime) {
		return AppLocal.getIntString(
				regime == TaxRegime.EQUIVALENCE_SURCHARGE ? "label.prodpriceeconomic" : "label.prodpricesell");
	}

	public static Double secondary(TaxRegime regime, Double factory, Double netSell, Double taxRate) {
		if (regime == TaxRegime.NORMAL) {
			return netSell;
		}
		if (factory == null || taxRate == null) {
			return null;
		}
		return Double
				.valueOf(PriceRuleService.calculateEconomicCost(factory.doubleValue(), taxRate.doubleValue(), regime));
	}

	public static Double grossFromNet(Double net, double taxRate) {
		if (net == null) {
			return null;
		}
		return Double.valueOf(net.doubleValue() * (1.0 + taxRate));
	}

	public static Double netFromGross(Double gross, double taxRate) {
		if (gross == null) {
			return null;
		}
		return Double.valueOf(gross.doubleValue() / (1.0 + taxRate));
	}

	public static Double markup(Double cost, Double sell) {
		if (cost == null || sell == null || cost.doubleValue() == 0.0) {
			return null;
		}
		return Double.valueOf(sell.doubleValue() / cost.doubleValue() - 1.0);
	}

	public static Double netFromMarkup(Double buy, Double margin) {
		if (buy == null || margin == null) {
			return null;
		}
		return Double.valueOf(buy.doubleValue() * (1.0 + margin.doubleValue()));
	}

	public static Double netFromGrossMarkup(Double grossCost, Double marginTax, double taxRate) {
		if (grossCost == null || marginTax == null) {
			return null;
		}
		return Double.valueOf(grossCost.doubleValue() * (1.0 + marginTax.doubleValue()) / (1.0 + taxRate));
	}

	public static Double grossCostBasis(Double factory, Double taxRate, TaxRegime regime) {
		if (factory == null || taxRate == null) {
			return null;
		}
		return Double.valueOf(
				PriceRuleService.calculateGrossCostBasis(factory.doubleValue(), taxRate.doubleValue(), regime));
	}

	public static Double commercialMargin(Double factory, Double gross, double taxRate, TaxRegime regime) {
		if (factory == null || gross == null || factory.doubleValue() <= 0.0 || gross.doubleValue() <= 0.0) {
			return null;
		}
		double cost = PriceRuleService.calculateGrossCostBasis(factory.doubleValue(), taxRate, regime);
		return Double.valueOf((gross.doubleValue() - cost) / gross.doubleValue());
	}

	public static Double grossFromCommercialMargin(Double factory, double commercialMargin, double taxRate,
			TaxRegime regime) {
		if (factory == null || factory.doubleValue() <= 0.0 || commercialMargin >= 1.0) {
			return null;
		}
		double cost = PriceRuleService.calculateGrossCostBasis(factory.doubleValue(), taxRate, regime);
		return Double.valueOf(cost / (1.0 - commercialMargin));
	}

	public static Double offerNetFromFactory(Double factory, double taxRate, PriceRule rule, TaxRegime regime) {
		if (factory == null || factory.doubleValue() <= 0.0 || rule == null) {
			return null;
		}
		double gross = PriceRuleService.calculateGross(factory.doubleValue(), taxRate, rule, regime);
		return Double.valueOf(gross / (1.0 + taxRate));
	}

	public static Double offerGrossFromFactory(Double factory, double taxRate, PriceRule rule, TaxRegime regime) {
		if (factory == null || factory.doubleValue() <= 0.0 || rule == null) {
			return null;
		}
		return Double.valueOf(PriceRuleService.calculateGross(factory.doubleValue(), taxRate, rule, regime));
	}
}
