package com.openbravo.pos.forms;

/** Typed database settings used by database services. */
public final class DatabaseConfiguration {
	private final String driver;
	private final String url;
	private final String user;
	private final String password;

	private DatabaseConfiguration(String driver, String url, String user, String password) {
		this.driver = driver;
		this.url = url;
		this.user = user;
		this.password = password;
	}

	public static DatabaseConfiguration from(AppProperties properties) {
		return new DatabaseConfiguration(properties.getProperty("db.driver"), properties.getProperty("db.URL"),
				properties.getProperty("db.user"), properties.getProperty("db.password"));
	}

	public String getDriver() {
		return driver;
	}

	public String getUrl() {
		return url;
	}

	public String getUser() {
		return user;
	}

	public String getPassword() {
		return password;
	}
}
