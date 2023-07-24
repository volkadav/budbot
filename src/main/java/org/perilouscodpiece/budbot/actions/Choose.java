package org.perilouscodpiece.budbot.actions;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import java.util.List;
import java.util.Random;

public class Choose {
    private static final Random rnd = new Random(System.currentTimeMillis());

    public static String between(List<String> tokens) {
        List<String> options = Splitter.on(",").splitToList(Joiner.on(" ").join(tokens));
        return options.get(rnd.nextInt(options.size())).trim();
    }
}
