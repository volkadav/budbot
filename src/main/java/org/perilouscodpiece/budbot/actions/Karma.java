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
                """create table if not exists karma( 
                        entity text not null primary key,
                        value integer not null);"""
        );
    }

    @Override
    public String process(List<String> commandTokens) {
        String response = "";
        if (commandTokens.isEmpty()) {
            response = "please provide a command (get <entity>/++ or -- <entity>/reset)";
        }

        return response;
    }
}
