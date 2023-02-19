package org.perilouscodpiece.budbot;

import com.google.common.base.Splitter;
import lombok.extern.java.Log;
import org.glassfish.jersey.internal.guava.Lists;
import org.perilouscodpiece.budbot.actions.Choose;
import org.perilouscodpiece.budbot.actions.CoinToss;
import org.perilouscodpiece.budbot.actions.Dice;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

@Log
public class BudBot extends TelegramLongPollingBot {
    private final String telegramApiToken ;

    public BudBot(String telegramApiToken) {
        this.telegramApiToken = telegramApiToken;
    }

    @Override
    public String getBotUsername() {
        return "budholebot";
    }

    @Override
    public String getBotToken() {
        return this.telegramApiToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        var msg = update.getMessage();
        var msgText = msg.getText();
        var sender = msg.getFrom();
        log.info(sender.toString() + " says: " + msgText);
        List<String> cmdTokens = Lists.newArrayList(Splitter.on(" ").split(msgText));

        String response = "wat";
        String cmd = cmdTokens.remove(0);
        switch (cmd) {
            case "choose" -> response = Choose.between(cmdTokens);
            case "cointoss" -> response = CoinToss.tossCoin();
            case "dice" -> response = Dice.roll(cmdTokens);
            default -> log.info("Unrecognized message: " + msgText);
        }

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
