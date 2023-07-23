package org.perilouscodpiece.budbot.actions;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class Dice {
    private static final Pattern rollSpecPattern = Pattern.compile("^(\\d*)[dD](\\d+)[+\\-]?(\\d+)?");

    public static String roll(List<String> cmdTokens) {
        if (cmdTokens.size() == 1) {
            return roll(cmdTokens.get(0), false);
        } else if (cmdTokens.size() == 2) {
            return roll(cmdTokens.get(0), cmdTokens.get(1).equals("showeach"));
        } else {
            return "usage: XdY[+-Z] [showeach]";
        }
    }

    public static String roll(String rollSpec, boolean reportIndividualRolls) {
        Matcher match = rollSpecPattern.matcher(rollSpec);

        if (!match.find()) {
            return "Sorry, I don't understand \""+rollSpec+"\".";
        }

        int modifier = 0;
        try {
            int count = Integer.parseInt(match.group(1));
            int sides = Integer.parseInt(match.group(2));

            if (match.groupCount() == 3) {
                if (match.group(3).equals("-")) {
                    modifier -= Integer.parseInt(match.group(3));
                } else {
                    modifier += Integer.parseInt(match.group(3));
                }
            }

            return roll(count, sides, modifier, reportIndividualRolls);
        } catch (NumberFormatException nfe) {
            log.warn("number format exception in Dice.roll(string): " + nfe.getMessage());
            return "oops! " + nfe.getMessage();
        }
    }

    public static String roll(int number, int sides, int modifier, boolean reportIndividualRolls) {
        log.info("dice number: {} dice sides: {} total modifier: {} report each roll? {}", number, sides, modifier, reportIndividualRolls);
        Random rnd = new Random(System.currentTimeMillis());
        StringBuilder response = new StringBuilder();
        List<Integer> results = Lists.newArrayListWithCapacity(number);

        for (int i = 0; i < number; i++) {
            int roll = rnd.nextInt( sides) + 1;
            log.info("rolled a d{}, got {}", sides, roll);
            results.add(roll);
        }

        int total = results.stream().reduce(modifier, Integer::sum);

        if (reportIndividualRolls) {
            response.append(Joiner.on(", ").join(results));

            if (modifier < 0) {
                response.append(modifier);
            } else if (modifier > 0) {
                response.append(" +");
                response.append(modifier);
            }

            response.append(" = ");
        }

        response.append(total);

        return response.toString();
    }
}
