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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.openbravo.basic.BasicException;
import com.openbravo.data.loader.SentenceList;

/**
 * Renames every category in a flat pick list to its path, "Samarretes Dona /
 * Màniga curta". Names are only unique among siblings, so "Màniga curta" on its
 * own appears three times and says nothing about which family it belongs to.
 *
 * Only the label changes: the combos select categories by ID.
 */
public class CategoryPathList implements SentenceList {

	private final SentenceList categories;

	public CategoryPathList(SentenceList categories) {
		this.categories = categories;
	}

	public List list() throws BasicException {
		return path(categories.list());
	}

	public List list(Object... params) throws BasicException {
		return path(categories.list(params));
	}

	public List list(Object params) throws BasicException {
		return path(categories.list(params));
	}

	public List listPage(int offset, int length) throws BasicException {
		return path(categories.listPage(offset, length));
	}

	public List listPage(Object params, int offset, int length) throws BasicException {
		return path(categories.listPage(params, offset, length));
	}

	private static List path(List categories) {

		Map<String, String> names = new HashMap<String, String>();
		Map<String, String> parents = new HashMap<String, String>();
		for (Object item : categories) {
			CategoryInfo category = (CategoryInfo) item;
			names.put(category.getID(), category.getName());
			parents.put(category.getID(), category.getParentID());
		}

		for (Object item : categories) {
			CategoryInfo category = (CategoryInfo) item;
			category.setName(pathOf(category.getID(), names, parents));
		}

		Collections.sort(categories, new Comparator<Object>() {
			public int compare(Object left, Object right) {
				return ((CategoryInfo) left).getName().compareToIgnoreCase(((CategoryInfo) right).getName());
			}
		});
		return categories;
	}

	private static String pathOf(String id, Map<String, String> names, Map<String, String> parents) {

		List<String> ancestors = new ArrayList<String>();
		Set<String> visited = new HashSet<String>();
		String current = id;
		while (current != null && visited.add(current) && names.containsKey(current)) {
			ancestors.add(names.get(current));
			current = parents.get(current);
		}

		String path = null;
		for (int i = ancestors.size() - 1; i >= 0; i--) {
			path = CategoryPath.of(path, ancestors.get(i));
		}
		return path == null ? "" : path;
	}
}
