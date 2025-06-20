package org.perilouscodpiece.budbot;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.internal.guava.Lists;
import org.perilouscodpiece.budbot.actions.*;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class BudBot extends TelegramLongPollingBot {
    private Map<String, PersistentCommand> persistentCommandMap;

    public BudBot(String telegramApiToken) {
        super(telegramApiToken);
        initPersistentCommands();
    }

    public void initPersistentCommands() {
        // todo: automated discovery here?
        persistentCommandMap = new HashMap<>();
        persistentCommandMap.put("quoteDB", new QuoteDB());
        persistentCommandMap.put("karma", new Karma());
    }

    @Override
    public String getBotUsername() {
        return "budholebot";
    }

    @Override
    public void onUpdateReceived(Update update) {
        var msg = update.getMessage();
        if (msg == null) {
            // this seems to be what we get when someone edits a prior message
            log.debug("null message");
            return;
        }

        var msgText = msg.getText();
        if (Strings.isNullOrEmpty(msgText)) {
            log.debug("null/empty message text");
            return;
        }
        if (!msgText.startsWith("!")) {
            log.debug("message does not start with command signifier, ignoring.");
            return;
        }
        if (msgText.matches("^!{2,}")) {
            log.debug("message is probably just someone expressing surprise, not a command");
            return;
        }

        var sender = msg.getFrom();
        log.debug(sender.toString() + " sent: " + msgText);
        List<String> cmdTokens = Lists.newArrayList(Splitter.on(" ").split(msgText));

        var cmd = cmdTokens.remove(0);
        log.debug("cmd: {}", cmd);
        var response = switch (cmd) {
            case "!choose" -> Choose.between(cmdTokens);
            case "!cointoss" -> CoinToss.tossCoin();
            case "!dice", "!roll" -> Dice.roll(cmdTokens);
            case "!weather" -> Weather.getCurrentWeather(cmdTokens.stream().reduce("", String::concat));
            case "!quote" -> persistentCommandMap.get("quoteDB").process(cmdTokens);
            case "!karma" -> persistentCommandMap.get("karma").process(cmdTokens);
            default -> "Sorry, I don't understand '" + msgText + "'.";
        };
        log.debug("response: {}", response);

        sendText(msg.getChatId(), response);
    }

    public void sendText(Long who, String what) {
        var sm = SendMessage.builder()
                .chatId(who.toString())
                .text(what).build();
        try {
            execute(sm);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}
