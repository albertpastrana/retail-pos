# Include subcategories in category filters

Captured: 2026-09-27

## Goal

Selecting a parent category in a product category filter shows products assigned
to that category and to every descendant category.

## Context

The product management category filter currently matches only the selected
category ID. The product finder also has a text category filter. Category
labels already expose the complete hierarchy, and the category relationship is
stored in `CATEGORIES.PARENTID`.

## Done when

- Selecting a category with children includes products from the parent and all
  nested subcategories.
- Selecting a leaf category still includes only that category's products.
- Clearing the category filter still shows products from every category.
- The category filtering behaviour works with the existing database engines.

## Shipped

The product management filter and product finder category search now include
all nested subcategories. The shared filter comparison builds a safe `IN`
clause, so no database-specific recursive SQL is required.
