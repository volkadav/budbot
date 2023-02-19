package org.perilouscodpiece.budbot;

import com.google.common.base.Strings;
import lombok.extern.java.Log;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

@Log
public class Main {
    public static void main(String[] args) {
        // load api key
        String telegramApiKey = "";
        try (InputStream tokIn = Main.class.getClassLoader().getResourceAsStream("apitoken");
            BufferedReader bufin = new BufferedReader(new InputStreamReader(tokIn))) {
            telegramApiKey = bufin.readLine().trim();
        } catch (IOException ioe) {
            System.err.println("Received exception reading api token: " + ioe.getMessage());
            System.exit(1);
        }
        if (Strings.isNullOrEmpty(telegramApiKey)) {
            log.severe("No api key retrieved from jar file! Exiting!");
            System.exit(1);
        }
        log.info("Using api key from jar file: " + telegramApiKey);

        try {
            // register bot
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new BudBot(telegramApiKey));

        } catch (TelegramApiException tae) {
            System.err.println("Received exception registering budbot: " + tae.getMessage());
            System.exit(1);
        }
        log.info("BudBot registered");
        log.info("BudBot initialization complete");
    }
}
