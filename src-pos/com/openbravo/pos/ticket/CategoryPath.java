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

package com.openbravo.pos.ticket;

/**
 * Joins a category name to its parent's, "Samarretes Dona / Màniga curta".
 *
 * Category names are only unique among siblings, so a name on its own does not
 * identify the category. Report expressions call {@link #of(String, String)}
 * with the parent name left-joined in SQL; concatenating there instead would
 * need a different operator on every engine the till supports.
 */
public final class CategoryPath {

	public static final String SEPARATOR = " / ";

	private CategoryPath() {
	}

	public static String of(String parent, String name) {
		return parent == null || parent.isEmpty() ? name : parent + SEPARATOR + name;
	}
}
