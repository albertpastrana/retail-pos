package com.openbravo.pos.inventory;

interface CategoryNavigator {
	void setFilter(String value);

	void collapseAll();

	int getMatchCount();

	int getTotalCount();
}
