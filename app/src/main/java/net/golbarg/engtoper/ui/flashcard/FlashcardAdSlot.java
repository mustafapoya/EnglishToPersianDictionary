package net.golbarg.engtoper.ui.flashcard;

import android.content.Context;
import android.os.SystemClock;

import androidx.annotation.Nullable;

import com.google.android.gms.ads.nativead.NativeAd;

import net.golbarg.engtoper.ads.AdUtil;

/**
 * Paces native ads between flashcards: one after every {@link #CARDS_BETWEEN_ADS} answers, loaded
 * a couple of cards ahead so it is ready in time. If none has loaded when one is due, studying
 * simply carries on. Lives in the ViewModel, so the count and the ad survive rotation.
 */
final class FlashcardAdSlot {

    static final int CARDS_BETWEEN_ADS = 7;
    private static final int PRELOAD_AHEAD = 2;
    /** Native ads expire after an hour; a slightly older one isn't shown. */
    private static final long MAX_AGE_MS = 55 * 60 * 1000L;

    private final Context appContext;
    private int answersSinceAd;
    private boolean loading;
    private boolean cleared;
    @Nullable
    private NativeAd ready;
    private long readyAt;
    @Nullable
    private NativeAd showing;

    FlashcardAdSlot(Context context) {
        appContext = context.getApplicationContext();
    }

    /** Counts an answered card and starts loading when an ad will soon be due. */
    void onCardAnswered() {
        answersSinceAd++;
        if (answersSinceAd >= CARDS_BETWEEN_ADS - PRELOAD_AHEAD) preload();
    }

    /** The ad to show now, or null if none is due or ready. It stays {@link #showing()} until dismissed. */
    @Nullable
    NativeAd takeDueAd() {
        if (answersSinceAd < CARDS_BETWEEN_ADS || showing != null) return null;
        if (ready != null && SystemClock.elapsedRealtime() - readyAt > MAX_AGE_MS) {
            ready.destroy();
            ready = null;
            preload();
        }
        if (ready == null) return null;
        answersSinceAd = 0;
        showing = ready;
        ready = null;
        return showing;
    }

    @Nullable
    NativeAd showing() {
        return showing;
    }

    void dismiss() {
        if (showing == null) return;
        showing.destroy();
        showing = null;
    }

    /** Ads were turned off (e.g. an ad-free pass): let go of every ad, but keep counting. */
    void dropAds() {
        dismiss();
        if (ready != null) ready.destroy();
        ready = null;
    }

    void clear() {
        cleared = true;
        dropAds();
    }

    private void preload() {
        if (ready != null || loading || cleared || !AdUtil.isEnabled(appContext, AdUtil.Placement.FLASHCARD_NATIVE)) return;
        loading = true;
        AdUtil.loadNative(appContext, AdUtil.Placement.FLASHCARD_NATIVE, ad -> {
            loading = false;
            if (ad == null) {
                // No fill: skip this slot rather than asking again on every card
                answersSinceAd = 0;
            } else if (cleared || !AdUtil.isEnabled(appContext, AdUtil.Placement.FLASHCARD_NATIVE)) {
                ad.destroy();
            } else {
                ready = ad;
                readyAt = SystemClock.elapsedRealtime();
            }
        });
    }
}
