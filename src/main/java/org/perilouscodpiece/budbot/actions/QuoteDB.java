package org.perilouscodpiece.budbot.actions;

import com.google.common.base.Joiner;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.List;
import java.util.Map;

@Slf4j
public class QuoteDB extends PersistentCommand {

    public QuoteDB() {
        initDB(getJdbcURL(),getExpectedTables());
    }

    @Override
    String getJdbcURL() {
        return PersistentCommand.jdbcBaseURL + "budbot_quotes.db";
    }

    @Override
    Map<String, String> getExpectedTables() {
        return Map.of(
                "quotes",
                """
                    create table if not exists quotes(
                        id integer not null autoincrement primary key,
                        quote text not null);"""
        );
    }

    @Override
    public String process(List<String> commandTokens) {
        String response;
        if (commandTokens.isEmpty()) {
            response = "please provide a command (add/get n/random/status/del n)";
        }

        switch (commandTokens.get(0).trim().toLowerCase()) {
            case "add":
                commandTokens.remove(0);
                response = addQuote(Joiner.on(' ').join(commandTokens));
                break;
            case "get":
            case "id":
                try {
                    response = getQuote(Integer.parseInt(commandTokens.get(1)));
                } catch (NumberFormatException nfe) {
                    response = "unparseable quote number";
                } catch (ArrayIndexOutOfBoundsException aioobe) {
                    response = "please provide a quote id";
                }
                break;
            case "random":
            case "rand":
                response = getRandomQuote();
                break;
            case "status":
            case "stats":
            case "count":
                response = getQuoteDBStatus();
                break;
            case "delete":
            case "del":
            case "rm":
                try {
                    response = deleteQuote(Integer.parseInt(commandTokens.get(1)));
                } catch (NumberFormatException nfe) {
                    response = "unparseable quote number";
                } catch (ArrayIndexOutOfBoundsException aioobe) {
                    response = "please provide a quote id";
                }
                break;
            default:
                response = "Sorry, I don't recognize the command '" + commandTokens.stream().reduce("", String::concat) + "'.";
        }

        return response;
    }

    private static String addQuote(String quote) {
        return executeCommand("insert into quotes (quote) values (?)", List.of(quote));
    }

    private static String getQuote(int n) {
        return executeCommand("select quote from quotes where id = ?", List.of(n));
    }

    private static String getRandomQuote() {
        return executeCommand("select quote from quotes order by random() limit 1", List.of());
    }

    private static String getQuoteDBStatus() {
        // for now, just return count
        return "quotedb size: " + executeCommand("select count(*) from quotes", List.of());
    }

    private static String deleteQuote(int n) {
        return executeCommand("delete from quotes where id = ?", List.of(n));
    }

    private static String executeCommand(String sql, List<Object> params) {
        String result;
        // TODO it would be better to persist a connection rather than do one per-request
        try (Connection dbh = DriverManager.getConnection(JDBC_URL)) {
            ResultSet tablesRS = dbh.getMetaData().getTables(null, null,  "quotes", null);
            boolean createTable = true;
            while (tablesRS.next()) {
                if (tablesRS.getString("TABLE_NAME").equalsIgnoreCase("quotes")) {
                    createTable = false;
                    break;
                }
            }

            if (createTable) {
                dbh.createStatement().execute("""
                    create table if not exists quotes(
                        id integer primary key,
                        quote text not null);""");
            }

            PreparedStatement stmt = dbh.prepareStatement(sql);
            // Is there a prettier way to do this? Ugh.
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof Integer) {
                    stmt.setInt(i + 1, (Integer)param);
                } else if (param instanceof String) {
                    stmt.setString(i + 1, (String)param);
                }
            }
            stmt.execute();

            if (sql.startsWith("select")) {
                // return results flattened to string
                // this logic is also kind of dumb, but it works for now :/
                // (pulling in all of jpa/hibernate/whatever for like 4-5 queries seems excessive)

                ResultSet rs = stmt.getResultSet();
                rs.next(); // advance to first row
                if (sql.startsWith("select count")) {
                    result = Integer.toString(rs.getInt(1));
                } else if (sql.startsWith("select quote")) {
                    result = rs.getString(1);
                } else {
                    log.warn("Unrecognized query: {}", sql);
                    result = "Unrecognized query!";
                }
            } else {
                // insert or delete, return success/failure
                int updateCount = stmt.getUpdateCount();
                result = (updateCount > 0) ? "success" : "failure";
                if (sql.startsWith("insert")) {
                    var idRS = dbh.createStatement().executeQuery("select last_insert_rowid()");
                    idRS.next();
                    result += ", quote id " + idRS.getInt(1);
                }
            }
        } catch (SQLException sqle) {
            var errMsg = "caught SQLException: " + sqle.getMessage();
            log.warn(errMsg, sqle);
            result = errMsg;
        }

        return result;
    }
}
