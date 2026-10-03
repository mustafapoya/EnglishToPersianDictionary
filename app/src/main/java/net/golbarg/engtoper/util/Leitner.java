package net.golbarg.engtoper.util;

import net.golbarg.engtoper.models.CardProgress;

import java.util.concurrent.TimeUnit;

/** Leitner spaced-repetition schedule: knowing a card moves it up a box, missing it resets it to box 0. */
public final class Leitner {

    public static final int MAX_BOX = 5;
    /** A card in this box or higher counts as "learned". */
    public static final int LEARNED_BOX = 3;

    private static final int[] INTERVAL_DAYS = {0, 1, 3, 7, 14, 30};

    private Leitner() {
    }

    public static int nextIntervalDays(int currentBox) {
        return INTERVAL_DAYS[Math.max(0, Math.min(currentBox + 1, MAX_BOX))];
    }

    public static CardProgress review(int wordId, String lang, int currentBox, boolean known, long now) {
        int box = known ? Math.min(currentBox + 1, MAX_BOX) : 0;
        long dueAt = now + TimeUnit.DAYS.toMillis(INTERVAL_DAYS[box]);
        return new CardProgress(wordId, lang, box, dueAt, now);
    }
}
