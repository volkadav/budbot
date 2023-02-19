package org.perilouscodpiece.budbot.actions;

import java.util.Random;

public class CoinToss {
    public static String tossCoin() {
        Random rnd = new Random(System.currentTimeMillis());
        return (rnd.nextDouble() <= 0.5) ? "tails" : "heads";
    }
}
