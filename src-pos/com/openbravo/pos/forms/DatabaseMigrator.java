package com.openbravo.pos.forms;

import java.util.HashMap;
import java.util.Map;

import org.flywaydb.core.Flyway;

final class DatabaseMigrator {

    private static final String BINARY_TYPE = "binary_type";
    private static final String BOOLEAN_TYPE = "boolean_type";
    private static final String TRUE_VALUE = "true_value";
    private static final String FALSE_VALUE = "false_value";
    private static final String TICKET_NUMBER_OBJECTS = "ticket_number_objects";

    private DatabaseMigrator() {
    }

    static void migrate(String url, String user, String password) {
        Map<String, String> placeholders = placeholdersFor(url);
        placeholders.put("app_id", AppLocal.APP_ID);
        placeholders.put("app_name", AppLocal.APP_NAME);
        placeholders.put("app_version", AppLocal.APP_VERSION);

        Flyway.configure()
                .dataSource(url, user, password)
                .locations("classpath:db/migration")
                .placeholders(placeholders)
                .load()
                .migrate();
    }

    private static Map<String, String> placeholdersFor(String url) {
        Map<String, String> placeholders = new HashMap<String, String>();

        if (url.startsWith("jdbc:derby:")) {
            placeholders.put(BINARY_TYPE, "BLOB");
            placeholders.put(BOOLEAN_TYPE, "SMALLINT");
            placeholders.put(TRUE_VALUE, "1");
            placeholders.put(FALSE_VALUE, "0");
            placeholders.put(TICKET_NUMBER_OBJECTS,
                    "CREATE TABLE TICKETSNUM (ID INTEGER GENERATED ALWAYS AS IDENTITY (START WITH 1));"
                    + "INSERT INTO TICKETSNUM VALUES (DEFAULT);"
                    + "CREATE TABLE TICKETSNUM_REFUND (ID INTEGER GENERATED ALWAYS AS IDENTITY (START WITH 1));"
                    + "INSERT INTO TICKETSNUM_REFUND VALUES (DEFAULT);"
                    + "CREATE TABLE TICKETSNUM_PAYMENT (ID INTEGER GENERATED ALWAYS AS IDENTITY (START WITH 1));"
                    + "INSERT INTO TICKETSNUM_PAYMENT VALUES (DEFAULT)");
        } else if (url.startsWith("jdbc:mysql:")) {
            placeholders.put(BINARY_TYPE, "MEDIUMBLOB");
            placeholders.put(BOOLEAN_TYPE, "BIT");
            placeholders.put(TRUE_VALUE, "TRUE");
            placeholders.put(FALSE_VALUE, "FALSE");
            placeholders.put(TICKET_NUMBER_OBJECTS,
                    "CREATE TABLE TICKETSNUM (ID INTEGER NOT NULL);"
                    + "INSERT INTO TICKETSNUM VALUES (1);"
                    + "CREATE TABLE TICKETSNUM_REFUND (ID INTEGER NOT NULL);"
                    + "INSERT INTO TICKETSNUM_REFUND VALUES (1);"
                    + "CREATE TABLE TICKETSNUM_PAYMENT (ID INTEGER NOT NULL);"
                    + "INSERT INTO TICKETSNUM_PAYMENT VALUES (1)");
        } else if (url.startsWith("jdbc:postgresql:")) {
            placeholders.put(BINARY_TYPE, "BYTEA");
            placeholders.put(BOOLEAN_TYPE, "BOOLEAN");
            placeholders.put(TRUE_VALUE, "TRUE");
            placeholders.put(FALSE_VALUE, "FALSE");
            placeholders.put(TICKET_NUMBER_OBJECTS,
                    "CREATE SEQUENCE TICKETSNUM START WITH 1;"
                    + "CREATE SEQUENCE TICKETSNUM_REFUND START WITH 1;"
                    + "CREATE SEQUENCE TICKETSNUM_PAYMENT START WITH 1");
        } else {
            throw new IllegalArgumentException("Flyway migrations support Derby, MySQL and PostgreSQL only: " + url);
        }

        return placeholders;
    }
}
