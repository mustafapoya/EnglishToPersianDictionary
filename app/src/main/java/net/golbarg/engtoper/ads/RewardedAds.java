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
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import net.golbarg.engtoper.ads.AdConfig.Placement;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Opt-in videos that give the user something. Only play one after the user tapped and was told
 * the reward. When ads are off for the app, the placement or the user, the reward is given
 * straight away without a video, so features behind a video keep working. Main thread only.
 */
public final class RewardedAds {

    /** Loaded ads expire after an hour. */
    private static final long MAX_AGE_MS = 55 * 60 * 1000L;

    /** Hears once whether a video is ready to {@link #show}. */
    public interface LoadListener {
        void onLoaded(boolean available);
    }

    private static final Map<Placement, Slot> slots = new EnumMap<>(Placement.class);

    private static final class Slot {
        @Nullable
        RewardedAd ad;
        long loadedAt;
        boolean loading;
        final List<LoadListener> listeners = new ArrayList<>();

        boolean isFresh() {
            return ad != null && SystemClock.elapsedRealtime() - loadedAt < MAX_AGE_MS;
        }
    }

    private RewardedAds() {
    }

    /** {@link #show} can give the reward right now: with a loaded video, or without one because ads are off. */
    public static boolean isReady(Context context, Placement placement) {
        return AdManager.adsOffFor(context, placement) || slot(placement).isFresh();
    }

    /**
     * Loads a video for {@code placement}. {@code listener} (optional) hears back exactly once,
     * even if a load is already running; true means {@link #show} can be called.
     */
    public static void load(Context context, Placement placement, @Nullable LoadListener listener) {
        AdManager.ensureStarted(context);
        if (isReady(context, placement)) {
            if (listener != null) listener.onLoaded(true);
            return;
        }
        if (!AdManager.isAllowed(context, placement)) {
            // e.g. consent not given yet: nothing can load now
            if (listener != null) listener.onLoaded(false);
            return;
        }
        Slot slot = slot(placement);
        if (listener != null) slot.listeners.add(listener);
        if (slot.loading) return;
        slot.ad = null;
        slot.loading = true;
        RewardedAd.load(context.getApplicationContext(), AdManager.unitId(placement), new AdRequest.Builder().build(),
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        slot.loading = false;
                        slot.ad = ad;
                        slot.loadedAt = SystemClock.elapsedRealtime();
                        notifyLoaded(slot, true);
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError error) {
                        slot.loading = false;
                        Log.d(AdManager.TAG, placement + " not loaded: " + error.getMessage());
                        notifyLoaded(slot, false);
                    }
                });
    }

    /** Forgets a listener whose screen went away before the load finished. */
    public static void cancel(LoadListener listener) {
        for (Slot slot : slots.values()) slot.listeners.remove(listener);
    }

    /**
     * Plays the video. {@code onReward} runs when the user earns the reward, then {@code onClosed}
     * when the video is closed. When ads are off both run straight away. When no video is ready,
     * only {@code onClosed} runs (no reward) and a new one starts loading.
     */
    public static void show(Activity activity, Placement placement, Runnable onReward, Runnable onClosed) {
        if (AdManager.adsOffFor(activity, placement)) {
            onReward.run();
            onClosed.run();
            return;
        }
        Slot slot = slot(placement);
        if (!slot.isFresh() || activity.isFinishing() || activity.isDestroyed()) {
            slot.ad = null;
            onClosed.run();
            load(activity, placement, null);
            return;
        }
        RewardedAd ad = slot.ad;
        slot.ad = null;
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                onClosed.run();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError error) {
                Log.d(AdManager.TAG, placement + " not shown: " + error.getMessage());
                onClosed.run();
            }
        });
        // An opted-in video still counts: no interstitial straight after it
        AdManager.markFullscreenShown(activity);
        ad.show(activity, reward -> onReward.run());
    }

    private static void notifyLoaded(Slot slot, boolean available) {
        List<LoadListener> listeners = new ArrayList<>(slot.listeners);
        slot.listeners.clear();
        for (LoadListener listener : listeners) listener.onLoaded(available);
    }

    private static Slot slot(Placement placement) {
        Slot slot = slots.get(placement);
        if (slot == null) {
            slot = new Slot();
            slots.put(placement, slot);
        }
        return slot;
    }
}
