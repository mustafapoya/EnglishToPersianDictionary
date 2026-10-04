package net.golbarg.engtoper.ads;

import android.app.Activity;
import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import net.golbarg.engtoper.util.AppPreferences;

/**
 * A full-screen ad at a natural break: when a flashcard session ends, before its results. Kept
 * rare so it doesn't get in the way: only after a real session, at most once per
 * {@link #MIN_GAP_MS}, and never in a new user's first day.
 */
public final class SessionInterstitial {

    private static final String TAG = "SessionInterstitial";
    /** Short sessions (e.g. three due cards) never end with an ad. */
    public static final int MIN_ANSWERS = 8;
    private static final long MIN_GAP_MS = 10 * 60 * 1000L;
    private static final long NEW_USER_GRACE_MS = 24 * 60 * 60 * 1000L;
    /** Loaded ads expire after an hour. */
    private static final long MAX_AGE_MS = 55 * 60 * 1000L;

    @Nullable
    private static InterstitialAd ready;
    private static long readyAt;
    private static boolean loading;

    private SessionInterstitial() {
    }

    /** Loads an ad ahead of the end of a session, if one could be shown then. */
    public static void preload(Context context) {
        if (loading || !canShow(context)) return;
        if (ready != null && SystemClock.elapsedRealtime() - readyAt < MAX_AGE_MS) return;
        ready = null;
        loading = true;
        InterstitialAd.load(context.getApplicationContext(), AdUtil.unitId(AdUtil.Placement.SESSION_INTERSTITIAL),
                new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        loading = false;
                        ready = ad;
                        readyAt = SystemClock.elapsedRealtime();
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError error) {
                        loading = false;
                        Log.d(TAG, "Not loaded: " + error.getMessage());
                    }
                });
    }

    /** Shows the ad at the end of a session of {@code answers} cards, if the rules allow it. */
    public static void showAtSessionEnd(Activity activity, int answers) {
        if (answers < MIN_ANSWERS || ready == null || !canShow(activity)) return;
        if (SystemClock.elapsedRealtime() - readyAt >= MAX_AGE_MS) {
            ready = null;
            return;
        }
        InterstitialAd ad = ready;
        ready = null;
        AppPreferences.setLastInterstitialAt(activity, System.currentTimeMillis());
        ad.show(activity);
    }

    private static boolean canShow(Context context) {
        long now = System.currentTimeMillis();
        return AdUtil.isEnabled(context, AdUtil.Placement.SESSION_INTERSTITIAL)
                && now - AppPreferences.getFirstOpenAt(context) >= NEW_USER_GRACE_MS
                && now - AppPreferences.getLastInterstitialAt(context) >= MIN_GAP_MS;
    }
}
