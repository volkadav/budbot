package org.perilouscodpiece.budbot.actions;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
public class Karma extends PersistentCommand {
    public Karma() {
        initDB(getJdbcURL(), getExpectedTables());
    }

    @Override
    String getJdbcURL() {
        return PersistentCommand.jdbcBaseURL + "budbot_karma.db";
    }

    @Override
    Map<String, String> getExpectedTables() {
        return Map.of(
                "karma",
                """
                        create table if not exists karma(
                        entity text not null primary key,
                        value integer not null);
                    """
        );
    }

    @Override
    public String process(List<String> commandTokens) {
        final String help = "karma usage: get <entity>|<entity>++ or --|reset";
        if (commandTokens.isEmpty()) {
            return help;
        }

        String response = "";

        String firstToken = commandTokens.get(0).trim().toLowerCase();
        String secondToken = "";
        if (commandTokens.size() > 1) {
            secondToken = commandTokens.get(1).trim().toLowerCase();
        }

        switch (firstToken) {
            case "get":
                response = secondToken +
                        " karma: " +
                        executeSQL("select coalesce(value, 0) from karma where entity = ?",
                                List.of(secondToken),
                                "String");
                break;
            case "reset":
                for (String statement : List.of("delete from karma", "vacuum")) {
                    executeSQL(statement);
                }
                response = "karma reset";
                break;
            default:
                if (firstToken.endsWith("++") || firstToken.endsWith("--")) {
                    String op = firstToken.endsWith("++") ? "+" : "-";
                    String name = firstToken
                            .replace("++","")
                            .replace("--", "");

                    List<String> queries = List.of(
                            "insert into karma (entity, value) values (?, 0) on conflict(entity) do nothing;",
                            "update karma set value = value %s 1 where entity = ?".formatted(op)
                    );
                    List<List<Object>> paramsLists = List.of(
                            List.of(name),
                            List.of(name)
                    );
                    if (!executeTransaction(queries, paramsLists)) {
                        log.warn("karma update transaction failed");
                    }

                    response = executeSQL("select value from karma where entity = ?", List.of(name), "String");
                } else {
                    response = help;
                }
                break;
        }

        return response;
    }
}
