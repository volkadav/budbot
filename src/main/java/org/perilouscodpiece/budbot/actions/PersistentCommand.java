package org.perilouscodpiece.budbot.actions;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * PersistentCommand - base class for things that persist data over time (e.g. via DB)
 */
@Slf4j
public abstract class PersistentCommand {
    private Connection dbConnection;
    public static final String jdbcBaseURL = "jdbc:sqlite:";

    public void initDB(String jdbcURL, Map<String, String> expectedTables) {
        if (Strings.isNullOrEmpty(jdbcURL)) {
            throw new IllegalArgumentException("JDBC URL must not be null/empty");
        }

        try {
            if (dbConnection == null || dbConnection.isClosed()) {
                dbConnection = DriverManager.getConnection(jdbcURL);
            }

            DatabaseMetaData dbMetadata = dbConnection.getMetaData();
            for (String expectedTable : expectedTables.keySet()) {
                ResultSet rs = dbMetadata.getTables(null, null, expectedTable, null);

                boolean makeTable = true;
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    if (!Strings.isNullOrEmpty(tableName) && tableName.equals(expectedTable)) {
                        makeTable = false;
                        break;
                    }
                }

                if (makeTable) {
                    dbConnection.createStatement().execute(expectedTables.get(expectedTable));
                    log.info("Making table {} with {}", expectedTable, expectedTables.get(expectedTable));
                } else {
                    log.info("Table '{}' already exists.", expectedTable);
                }
            }
        } catch (SQLException sqle) {
            log.error("init db sql exception: " + sqle.getMessage(), sqle);
        }
    }

    public String executeSQL(String sql) {
        return executeSQL(sql, Collections.emptyList(), null);
    }

    public String executeSQL(String sql, String resultClassName) {
        return executeSQL(sql, Collections.emptyList(), resultClassName);
    }

    public String executeSQL(String sql, List<Object> params) {
        return executeSQL(sql, params, null);
    }

    public String executeSQL(String sql, List<Object> params, String resultClassName) {
        if (dbConnection == null) {
            return "no active db connection!";
        }

        String result = "";
        try {
            log.info("Executing {} with params {} and expected result class {}", sql, params, resultClassName);
            PreparedStatement stmt = prepareStatement(dbConnection, sql, params);
            stmt.execute();

            if (sql.startsWith("select")) {
                // return results flattened to string
                // this logic is also kind of dumb, but it works for now :/
                // (pulling in all of jpa/hibernate/whatever for like 4-5 queries seems excessive)

                ResultSet rs = stmt.getResultSet();
                rs.next(); // advance to first row
                if (sql.startsWith("select count")) {
                    result = Integer.toString(rs.getInt(1));
                } else if (sql.startsWith("select")) {
                    if (!Strings.isNullOrEmpty(resultClassName)) {
                        result = switch (resultClassName) {
                            case "String" -> rs.getString(1);
                            case "Integer" -> Integer.toString(rs.getInt(1));
                            default -> throw new IllegalStateException("Unexpected value: " + resultClassName);
                        };
                    } else {
                        log.warn("select call to executeSQL with null/empty resultClassName?!");
                    }
                } else {
                    log.warn("Unrecognized query: {}", sql);
                    result = "Unrecognized query!";
                }
            } else {
                // insert or delete, return success/failure
                int updateCount = stmt.getUpdateCount();
                result = (updateCount > 0) ? "success" : "failure";
                if (sql.startsWith("insert")) {
                    var idRS = dbConnection.createStatement().executeQuery("select last_insert_rowid()");
                    idRS.next();
                    result += ", new id " + idRS.getInt(1);
                }
            }
        } catch (SQLException sqle) {
            result = "DB error: " + sqle.getMessage();
            log.error(result, sqle);
        }

        return result;
    }

    private PreparedStatement prepareStatement(Connection conn, String sql, List<Object> params) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(sql);

        // Is there a prettier way to do this? Ugh.
        for (int i = 0; i < params.size(); i++) {
            Object param = params.get(i);
            if (param instanceof Integer) {
                stmt.setInt(i + 1, (Integer)param);
            } else if (param instanceof String) {
                stmt.setString(i + 1, (String)param);
            }
        }

        return stmt;
    }

    public boolean executeTransaction(List<String> queries, List<List<Object>> params) {
        if (queries.size() != params.size()) {
            log.warn("transaction arg mismatch: queries: {} params lists: {}", queries.size(), params.size());
            return false;
        }

        try (Connection txnConn = DriverManager.getConnection(dbConnection.getMetaData().getURL())) {
            txnConn.setAutoCommit(false); // sqlite driver begins txn automatically on first read/write execute()

            for (int i = 0; i < queries.size(); i++) {
                PreparedStatement stmt = prepareStatement(txnConn, queries.get(i), params.get(i));
                if (!stmt.execute()) {
                   txnConn.rollback();
                   return false;
                }
            }

            txnConn.commit();
        } catch (SQLException sqle) {
            log.warn("exception while running transaction:", sqle);
        }

        return true;
    }

    abstract String getJdbcURL();
    abstract Map<String, String> getExpectedTables();
    public abstract String process(List<String> commandTokens);
}
