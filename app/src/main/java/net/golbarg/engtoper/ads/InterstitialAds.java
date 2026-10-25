package net.golbarg.engtoper.ads;

import android.app.Activity;
import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import net.golbarg.engtoper.ads.AdConfig.Placement;

import java.util.EnumMap;
import java.util.Map;

/**
 * Full-screen ads at natural breaks (after finishing or saving something, before results). Preload
 * a little before the break, then show at it. At most one full-screen ad per
 * {@link AdConfig#FULLSCREEN_MIN_GAP_MS}, across all placements. Main thread only.
 */
public final class InterstitialAds {

    /** Loaded ads expire after an hour. */
    private static final long MAX_AGE_MS = 55 * 60 * 1000L;

    private static final Map<Placement, Slot> slots = new EnumMap<>(Placement.class);

    private static final class Slot {
        @Nullable
        InterstitialAd ad;
        long loadedAt;
        boolean loading;

        boolean isFresh() {
            return ad != null && SystemClock.elapsedRealtime() - loadedAt < MAX_AGE_MS;
        }
    }

    private InterstitialAds() {
    }

    /** Loads an ad for {@code placement} ahead of the break, unless one is ready or loading. */
    public static void preload(Context context, Placement placement) {
        AdManager.ensureStarted(context);
        if (!AdManager.isAllowed(context, placement)) return;
        Slot slot = slot(placement);
        if (slot.loading || slot.isFresh()) return;
        slot.ad = null;
        slot.loading = true;
        InterstitialAd.load(context.getApplicationContext(), AdManager.unitId(placement), new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        slot.loading = false;
                        slot.ad = ad;
                        slot.loadedAt = SystemClock.elapsedRealtime();
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError error) {
                        slot.loading = false;
                        Log.d(AdManager.TAG, placement + " not loaded: " + error.getMessage());
                    }
                });
    }

    /**
     * Shows the preloaded ad if the rules allow it. {@code onDone} always runs exactly once: after
     * the ad is closed, or right away when no ad is shown. Put the next step of the flow in it.
     *
     * @return true if an ad is being shown
     */
    public static boolean show(@Nullable Activity activity, Placement placement, @Nullable Runnable onDone) {
        Runnable done = once(onDone);
        Slot slot = slots.get(placement);
        if (activity == null || activity.isFinishing() || activity.isDestroyed() || slot == null || !slot.isFresh()
                || !AdManager.isAllowed(activity, placement) || !AdManager.fullscreenGapPassed(activity)) {
            done.run();
            return false;
        }
        InterstitialAd ad = slot.ad;
        slot.ad = null;
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                done.run();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError error) {
                Log.d(AdManager.TAG, placement + " not shown: " + error.getMessage());
                done.run();
            }
        });
        AdManager.markFullscreenShown(activity);
        ad.show(activity);
        return true;
    }

    private static Slot slot(Placement placement) {
        Slot slot = slots.get(placement);
        if (slot == null) {
            slot = new Slot();
            slots.put(placement, slot);
        }
        return slot;
    }

    private static Runnable once(@Nullable Runnable action) {
        boolean[] ran = {false};
        return () -> {
            if (ran[0]) return;
            ran[0] = true;
            if (action != null) action.run();
        };
    }
}
