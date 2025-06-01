package org.perilouscodpiece.budbot.actions;

import com.google.common.base.Joiner;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
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
                        quote text not null);
                    """
        );
    }

    @Override
    public String process(List<String> commandTokens) {
        String response;
        if (commandTokens.isEmpty()) {
            response = "please provide a command (add/get n/random/status/del n)";
        } else {
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
        }

        return response;
    }

    private String addQuote(String quote) {
        return executeSQL("insert into quotes (quote) values (?)", List.of(quote));
    }

    private String getQuote(int n) {
        return executeSQL("select quote from quotes where id = ?", List.of(n), "String");
    }

    private String getRandomQuote() {
        return executeSQL("select quote from quotes order by random() limit 1", "String");
    }

    private String getQuoteDBStatus() {
        // for now, just return count
        return "quotedb size: " + executeSQL("select count(*) from quotes", Collections.emptyList(), "Integer");
    }

    private String deleteQuote(int n) {
        return executeSQL("delete from quotes where id = ?", List.of(n));
    }
}
