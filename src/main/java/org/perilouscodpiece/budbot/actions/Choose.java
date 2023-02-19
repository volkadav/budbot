package org.perilouscodpiece.budbot.actions;

import com.google.common.base.Splitter;

import java.util.List;
import java.util.Random;

public class Choose {
    public static String between(List<String> options) {
        Random rnd = new Random(System.currentTimeMillis());
        int which = rnd.nextInt(options.size());
        String chosen = options.get(which);

        if (chosen.contains(",")) {
            List<String> suboptions = Splitter.on(",").splitToList(chosen);
            which = rnd.nextInt(suboptions.size());
            chosen = suboptions.get(which);
        }

        return chosen;
    }
}
