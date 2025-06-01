package org.perilouscodpiece.budbot.actions;

import java.util.List;
import java.util.Map;

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
                        executeSQL("select value from karma where entity = ?",
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
                if (secondToken.endsWith("++") || secondToken.endsWith("--")) {
                    String op = secondToken.endsWith("++") ? "+" : "-";
                    executeSQL("""
                            begin transaction;
                            update karma set value = (select value from karma where entity = ?) %s 1
                                where entity = ?;
                            commit;
                            """.formatted(op), List.of(secondToken, secondToken));
                } else {
                    response = help;
                }
                break;
        }

        return response;
    }
}
