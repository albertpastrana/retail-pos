package com.openbravo.pos.forms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

public class TypeSafeBeanLookupTest {

	@Test
	public void typedLookupCachesBeanAndInitializesItOnce() {
		CountingBean.reset();
		JRootApp app = new JRootApp();

		CountingBean first = app.getBean(CountingBean.class);
		CountingBean second = app.getBean(CountingBean.class);

		assertSame(first, second);
		assertEquals(1, CountingBean.initCount);
		assertEquals(1, CountingBean.beanCount);
	}

	@Test
	public void typedNameLookupRemainsAvailableForDynamicTasks() {
		CountingBean.reset();
		JRootApp app = new JRootApp();

		assertSame(app.getBean(CountingBean.class.getName(), CountingBean.class), app.getBean(CountingBean.class));
	}

	@Test
	public void initializationFailureIsReported() {
		try {
			new JRootApp().getBean(FailingBean.class);
			fail("Expected initialization failure");
		} catch (BeanFactoryException e) {
			assertEquals("initialization failed", e.getMessage());
		}
	}

	@Test
	public void invalidTypeIsReportedClearly() {
		try {
			new JRootApp().getBean(WrongTypeBean.class);
			fail("Expected invalid bean type");
		} catch (BeanFactoryException e) {
			assertTrue(e.getMessage().contains(WrongTypeBean.class.getName()));
			assertTrue(e.getMessage().contains(Object.class.getName()));
		}
	}

	public static class CountingBean implements BeanFactoryApp {
		private static int initCount;
		private static int beanCount;
		private Object bean;

		static void reset() {
			initCount = 0;
			beanCount = 0;
		}

		@Override
		public void init(AppView app) {
			initCount++;
		}

		@Override
		public Object getBean() {
			if (bean == null) {
				beanCount++;
				bean = this;
			}
			return bean;
		}
	}

	public static class FailingBean implements BeanFactoryApp {
		@Override
		public void init(AppView app) {
			throw new BeanFactoryException("initialization failed");
		}

		@Override
		public Object getBean() {
			return this;
		}
	}

	public static class WrongTypeBean implements BeanFactory {
		@Override
		public Object getBean() {
			return new Object();
		}
	}
}
