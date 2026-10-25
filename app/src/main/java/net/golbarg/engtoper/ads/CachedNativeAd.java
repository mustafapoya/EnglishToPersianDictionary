package net.golbarg.engtoper.ads;

import android.content.Context;
import android.os.SystemClock;

import androidx.annotation.Nullable;

import com.google.android.gms.ads.nativead.NativeAd;

import net.golbarg.engtoper.ads.AdConfig.Placement;

/**
 * One native ad kept by a ViewModel, so returning to a screen or rotating shows the same ad
 * instead of requesting a new one each time. It is replaced after {@link #MAX_AGE_MS}.
 */
public final class CachedNativeAd {

    /** Shorter than the one-hour lifetime of a native ad, and a sensible refresh interval. */
    private static final long MAX_AGE_MS = 30 * 60 * 1000L;

    private final Placement placement;
    @Nullable
    private NativeAd ad;
    private long loadedAt;
    private boolean loading;

    public CachedNativeAd(Placement placement) {
        this.placement = placement;
    }

    /** The ad if it is still fresh; otherwise null (and an old one is let go). */
    @Nullable
    public NativeAd get() {
        if (ad != null && SystemClock.elapsedRealtime() - loadedAt > MAX_AGE_MS) clear();
        return ad;
    }

    /** Loads an ad unless a fresh one is held or loading; {@code onReady} runs when one arrives. */
    public void load(Context context, Runnable onReady) {
        Context appContext = context.getApplicationContext();
        if (get() != null || loading || !AdManager.isAllowed(appContext, placement)) return;
        loading = true;
        NativeAds.load(appContext, placement, loaded -> {
            loading = false;
            if (loaded == null) return;
            if (!AdManager.isAllowed(appContext, placement)) {
                // Ads were turned off while it loaded
                loaded.destroy();
                return;
            }
            ad = loaded;
            loadedAt = SystemClock.elapsedRealtime();
            onReady.run();
        });
    }

    public void clear() {
        if (ad != null) ad.destroy();
        ad = null;
    }
}
