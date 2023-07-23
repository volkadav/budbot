package org.perilouscodpiece.budbot;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.internal.guava.Lists;
import org.perilouscodpiece.budbot.actions.Choose;
import org.perilouscodpiece.budbot.actions.CoinToss;
import org.perilouscodpiece.budbot.actions.Dice;
import org.perilouscodpiece.budbot.actions.Weather;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

@Slf4j
public class BudBot extends TelegramLongPollingBot {
    public BudBot(String telegramApiToken) {
        super(telegramApiToken);
    }

    @Override
    public String getBotUsername() {
        return "budholebot";
    }

    @Override
    public void onUpdateReceived(Update update) {
        var msg = update.getMessage();
        var msgText = msg.getText();
        var sender = msg.getFrom();
        log.info(sender.toString() + " says: " + msgText);
        List<String> cmdTokens = Lists.newArrayList(Splitter.on(" ").split(msgText));

        String cmd = cmdTokens.remove(0);
        log.info("cmd: {}", cmd);
        String response = switch (cmd) {
            case "choose" -> Choose.between(cmdTokens);
            case "cointoss" -> CoinToss.tossCoin();
            case "dice" -> Dice.roll(cmdTokens);
            case "weather" -> Weather.getCurrentWeather(cmdTokens.stream().reduce("", String::concat));
            default -> "Sorry, I don't understand '" + msgText + "'.";
        };
        log.info("response: {}", response);

        sendText(sender.getId(), response);
    }

    public void sendText(Long who, String what) {
        SendMessage sm = SendMessage.builder()
                .chatId(who.toString())
                .text(what).build();
        try {
            execute(sm);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}
