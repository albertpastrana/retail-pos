package com.openbravo.pos.inventory;

import com.openbravo.basic.BasicException;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Classification of the single-product editor's unsaved values on entering
 * batch mode.
 */
final class ProductBatchDraft {
	enum OtherFields {
		SAVE, DISCARD, CANCEL
	}
	interface SaveOtherFields {
		void run() throws BasicException;
	}
	final String id;
	final Double gross;
	final String category;
	final boolean priceChanged;
	final boolean categoryChanged;
	final boolean otherChanged;

	private ProductBatchDraft(String id, Double gross, String category, boolean priceChanged, boolean categoryChanged,
			boolean otherChanged) {
		this.id = id;
		this.gross = gross;
		this.category = category;
		this.priceChanged = priceChanged;
		this.categoryChanged = categoryChanged;
		this.otherChanged = otherChanged;
	}

	static ProductBatchDraft classify(String id, Double originalNet, String originalCategory, Double editedNet,
			String editedCategory, Double gross, boolean otherChanged) {
		return new ProductBatchDraft(id, gross, editedCategory, !Objects.equals(originalNet, editedNet),
				!Objects.equals(originalCategory, editedCategory), otherChanged);
	}

	boolean hasBatchChanges() {
		return priceChanged || categoryChanged;
	}

	boolean enter(OtherFields action, SaveOtherFields save, Runnable discard, Consumer<ProductBatchDraft> stage)
			throws BasicException {
		if (action == OtherFields.CANCEL)
			return false;
		if (action == OtherFields.SAVE)
			save.run();
		else
			discard.run();
		if (hasBatchChanges())
			stage.accept(this);
		return true;
	}
}
