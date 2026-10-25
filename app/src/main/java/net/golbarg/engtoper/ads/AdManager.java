package net.golbarg.engtoper.ads;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;

import net.golbarg.engtoper.ads.AdConfig.Placement;

/**
 * The single gate for ads: whether a placement may show right now, which unit ID it uses, and
 * starting the Mobile Ads SDK once. Switches and placements live in {@link AdConfig}; the only
 * part of this file that differs between apps is {@link #userIsAdFree}.
 *
 * <p>A placement shows when ads are on in {@link AdConfig}, the placement is ON with a unit ID,
 * consent allows ads ({@link AdConsent}) and the user isn't ad-free. {@link #allowedState()}
 * tells screens when that changes, so ads appear or disappear right away.</p>
 */
public final class AdManager {

    static final String TAG = "AdManager";
    /** While the user is ad-free, check again this often so a time-limited reason ends on time. */
    private static final long AD_FREE_RECHECK_MS = 60 * 1000L;

    private static final String PREFS = "ads";
    private static final String KEY_LAST_FULLSCREEN_AT = "last_fullscreen_at";

    private static final MutableLiveData<Boolean> allowed = new MutableLiveData<>(false);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final Object RECHECK_TOKEN = new Object();
    private static boolean refreshed = false;
    private static boolean sdkStarted = false;

    private AdManager() {
    }

    // ── App-specific ──────────────────────────────────────────────────────────

    /**
     * APP-SPECIFIC: reasons this user should see no ads right now (an ad-free pass, a premium
     * purchase, a Settings switch). Return false when the app has none. Call {@link #refresh}
     * whenever one of them changes.
     */
    static boolean userIsAdFree(Context context) {
        return AdFreePass.isActive(context);
    }

    // ── Gate ──────────────────────────────────────────────────────────────────

    /** Ads are on in this build ({@link AdConfig#ADS_ENABLED}). */
    public static boolean isEnabled() {
        return AdConfig.ADS_ENABLED;
    }

    /** Ads may be requested right now: on in this build, consent allows it, and the user isn't ad-free. */
    public static boolean adsAllowed(Context context) {
        return isEnabled() && AdConsent.canRequestAds(context) && !userIsAdFree(context);
    }

    /** This placement may show an ad right now. */
    public static boolean isAllowed(Context context, Placement placement) {
        return placement.on && !unitId(placement).isEmpty() && adsAllowed(context);
    }

    /**
     * Ads are off for this placement for a reason other than consent: off in this build, the
     * placement is OFF or has no unit ID, or the user is ad-free. Rewarded features then give
     * their reward without a video.
     */
    static boolean adsOffFor(Context context, Placement placement) {
        return !isEnabled() || !placement.on || unitId(placement).isEmpty() || userIsAdFree(context);
    }

    /** Whether ads may show; changes when consent is given or the user becomes (or stops being) ad-free. */
    public static LiveData<Boolean> allowedState() {
        return allowed;
    }

    /**
     * Re-evaluates {@link #adsAllowed} and tells every screen, starting the SDK the first time ads
     * are allowed. Call after consent or an ad-free reason changes; safe from any thread.
     */
    public static void refresh(Context context) {
        Context appContext = context.getApplicationContext();
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post(() -> refresh(appContext));
            return;
        }
        if (!refreshed) {
            refreshed = true;
            logConfig();
        }
        boolean now = adsAllowed(appContext);
        if (now) startSdk(appContext);
        if (!Boolean.valueOf(now).equals(allowed.getValue())) allowed.setValue(now);

        mainHandler.removeCallbacksAndMessages(RECHECK_TOKEN);
        if (isEnabled() && userIsAdFree(appContext)) {
            mainHandler.postAtTime(() -> refresh(appContext), RECHECK_TOKEN, SystemClock.uptimeMillis() + AD_FREE_RECHECK_MS);
        }
    }

    /** Makes sure ads are set up, for screens that can open without MainActivity (e.g. from a notification). */
    static void ensureStarted(Context context) {
        if (!refreshed) refresh(context);
    }

    static String unitId(Placement placement) {
        return AdConfig.USE_TEST_ADS ? placement.format.testUnitId : placement.realUnitId;
    }

    // ── Full-screen ad spacing (shared by interstitials and rewarded videos) ──

    static boolean fullscreenGapPassed(Context context) {
        long last = prefs(context).getLong(KEY_LAST_FULLSCREEN_AT, 0);
        long now = System.currentTimeMillis();
        // A clock set backwards shouldn't block ads forever
        return last == 0 || now < last || now - last >= AdConfig.FULLSCREEN_MIN_GAP_MS;
    }

    static void markFullscreenShown(Context context) {
        prefs(context).edit().putLong(KEY_LAST_FULLSCREEN_AT, System.currentTimeMillis()).apply();
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    /** Starts the Mobile Ads SDK off the main thread, once. */
    private static void startSdk(Context appContext) {
        if (sdkStarted) return;
        sdkStarted = true;
        MobileAds.setRequestConfiguration(new RequestConfiguration.Builder()
                .setMaxAdContentRating(AdConfig.MAX_CONTENT_RATING)
                .build());
        new Thread(() -> {
            try {
                MobileAds.initialize(appContext, status -> { });
            } catch (Exception e) {
                Log.w(TAG, "Mobile Ads failed to start", e);
            }
        }, "ads-init").start();
    }

    private static void logConfig() {
        if (!AdConfig.ADS_ENABLED) {
            Log.i(TAG, "Ads: off");
            return;
        }
        Log.i(TAG, AdConfig.USE_TEST_ADS ? "Ads: on, TEST units" : "Ads: on, REAL units");
        if (AdConfig.USE_TEST_ADS) return;
        for (Placement placement : Placement.values()) {
            if (placement.on && placement.realUnitId.isEmpty()) {
                Log.w(TAG, placement + " is ON but has no real unit ID in AdConfig, so it stays off");
            }
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
