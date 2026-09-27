package com.openbravo.pos.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.openbravo.basic.BasicException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProductBatchDraftTest {
	@Test
	void priceAndCategoryOnlyBecomePendingWithoutSavingTheSingleEditor() throws Exception {
		ProductBatchDraft draft = ProductBatchDraft.classify("shirt", 10.0, "old", 11.0, "new", 13.31, false);
		List<String> events = new ArrayList<>();
		assertFalse(draft.otherChanged);
		assertTrue(draft.enter(ProductBatchDraft.OtherFields.DISCARD, () -> events.add("save"),
				() -> events.add("discard editor"),
				value -> events.add("stage " + value.gross + " " + value.category)));
		assertEquals(List.of("discard editor", "stage 13.31 new"), events);
	}

	@Test
	void otherFieldsRequireResolutionAndDoNotBecomeBatchEdits() throws Exception {
		ProductBatchDraft draft = ProductBatchDraft.classify("shirt", 10.0, "old", 10.0, "old", null, true);
		List<String> events = new ArrayList<>();
		assertTrue(draft.otherChanged);
		assertFalse(draft.hasBatchChanges());
		assertFalse(draft.enter(ProductBatchDraft.OtherFields.CANCEL, () -> events.add("save"),
				() -> events.add("discard"), value -> events.add("stage")));
		assertTrue(events.isEmpty());
		assertTrue(draft.enter(ProductBatchDraft.OtherFields.SAVE, () -> events.add("save"),
				() -> events.add("discard"), value -> events.add("stage")));
		assertEquals(List.of("save"), events);
	}

	@Test
	void savingOtherFieldsKeepsTheBatchDraftForExplicitApplication() throws Exception {
		assertMixed(ProductBatchDraft.OtherFields.SAVE, "save other");
	}

	@Test
	void discardingOtherFieldsAlsoKeepsTheBatchDraft() throws Exception {
		assertMixed(ProductBatchDraft.OtherFields.DISCARD, "discard other");
	}

	@Test
	void failedSaveOfOtherFieldsDoesNotSwitchModesOrStageTheBatch() {
		ProductBatchDraft draft = ProductBatchDraft.classify("shirt", 10.0, "old", 11.0, "new", 13.31, true);
		List<String> events = new ArrayList<>();
		assertThrows(BasicException.class, () -> draft.enter(ProductBatchDraft.OtherFields.SAVE, () -> {
			throw new BasicException("save failed");
		}, () -> events.add("discard"), value -> events.add("stage")));
		assertTrue(events.isEmpty());
	}

	private void assertMixed(ProductBatchDraft.OtherFields action, String firstEvent) throws Exception {
		ProductBatchDraft draft = ProductBatchDraft.classify("shirt", 10.0, "old", 11.0, "new", 13.31, true);
		List<String> events = new ArrayList<>();
		assertTrue(draft.hasBatchChanges());
		assertTrue(draft.otherChanged);
		assertTrue(draft.enter(action, () -> events.add("save other"), () -> events.add("discard other"),
				value -> events.add("stage " + value.gross + " " + value.category)));
		assertEquals(List.of(firstEvent, "stage 13.31 new"), events);
	}
}
