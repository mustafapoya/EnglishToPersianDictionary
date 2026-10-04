package net.golbarg.engtoper.ads;

import android.content.Context;
import android.text.format.DateUtils;

import net.golbarg.engtoper.util.AppPreferences;

/** 24 hours without any ads, earned by watching one rewarded video ({@link AdFreeOffer}). */
public final class AdFreePass {

    public static final long DURATION_MS = 24 * 60 * 60 * 1000L;

    private AdFreePass() {
    }

    public static boolean isActive(Context context) {
        return remainingMs(context) > 0;
    }

    /** Time left on the pass; capped at a full day, so moving the clock back can't stretch it. */
    public static long remainingMs(Context context) {
        long left = AppPreferences.getAdFreeUntil(context) - System.currentTimeMillis();
        return left > 0 ? Math.min(left, DURATION_MS) : 0;
    }

    /** Starts (or restarts) a full day without ads and removes the ads on screen right away. */
    static void grant(Context context) {
        AppPreferences.setAdFreeUntil(context, System.currentTimeMillis() + DURATION_MS);
        AdUtil.refresh(context);
    }

    /** When the pass ends, e.g. "Mon 14:30", in the app language. */
    public static String formatEnd(Context context) {
        long end = System.currentTimeMillis() + remainingMs(context);
        return DateUtils.formatDateTime(context, end,
                DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_WEEKDAY);
    }
}
