package net.golbarg.engtoper.ads;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;

import net.golbarg.engtoper.BuildConfig;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.Executors;

/**
 * The single place that decides whether and where ads show.
 *
 * <ul>
 *     <li>Turn every ad on or off: {@code ads.enabled} in gradle.properties.</li>
 *     <li>Test or real ads: {@code ads.mode} in gradle.properties ({@code auto} = test ads in debug builds,
 *     real ads in release builds; {@code test} or {@code real} forces one).</li>
 *     <li>Turn one placement off: remove it from {@link #ENABLED_PLACEMENTS}, or leave its unit ID empty
 *     in app/build.gradle.</li>
 * </ul>
 * At run time ads also stop while the user hasn't given consent where it's needed ({@link AdConsent})
 * and while an ad-free pass is active ({@link AdFreePass}). {@link #allowedState()} tells screens
 * when that changes, so banners and native ads disappear the moment ads are turned off.
 */
public final class AdUtil {

    private static final String TAG = "AdUtil";

    /** Every spot in the app that can show an ad. */
    public enum Placement {
        DICTIONARY_BANNER, SAVED_BANNER, HOME_NATIVE, FLASHCARD_NATIVE, SESSION_INTERSTITIAL, AD_FREE_REWARDED
    }

    private static final Set<Placement> ENABLED_PLACEMENTS = EnumSet.of(
            Placement.DICTIONARY_BANNER,
            Placement.SAVED_BANNER,
            Placement.HOME_NATIVE,
            Placement.FLASHCARD_NATIVE,
            Placement.SESSION_INTERSTITIAL,
            Placement.AD_FREE_REWARDED
    );

    private static final MutableLiveData<Boolean> allowed = new MutableLiveData<>(false);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final Object EXPIRY_TOKEN = new Object();
    private static boolean initialized = false;

    private AdUtil() {
    }

    public static boolean isEnabled() {
        return BuildConfig.ADS_ENABLED;
    }

    public static boolean isTestMode() {
        return BuildConfig.ADS_TEST_MODE;
    }

    /** Ads may be requested right now: on in this build, consent allows it, and no ad-free pass. */
    public static boolean adsAllowed(Context context) {
        return isEnabled() && AdConsent.canRequestAds(context) && !AdFreePass.isActive(context);
    }

    public static boolean isEnabled(Context context, Placement placement) {
        return adsAllowed(context) && ENABLED_PLACEMENTS.contains(placement) && !unitId(placement).isEmpty();
    }

    /** Whether ads may show; changes when consent is given or an ad-free pass starts or ends. */
    public static LiveData<Boolean> allowedState() {
        return allowed;
    }

    /**
     * Re-evaluates {@link #adsAllowed} and tells every screen. Call on the main thread after
     * consent or the ad-free pass changes; it also wakes itself when the pass runs out.
     */
    public static void refresh(Context context) {
        Context appContext = context.getApplicationContext();
        boolean now = adsAllowed(appContext);
        if (now) initialize(appContext);
        if (!Boolean.valueOf(now).equals(allowed.getValue())) allowed.setValue(now);

        mainHandler.removeCallbacksAndMessages(EXPIRY_TOKEN);
        long left = AdFreePass.remainingMs(appContext);
        if (left > 0) {
            mainHandler.postAtTime(() -> refresh(appContext), EXPIRY_TOKEN, SystemClock.uptimeMillis() + left + 1000);
        }
    }

    static String unitId(Placement placement) {
        switch (placement) {
            case HOME_NATIVE:
            case FLASHCARD_NATIVE:
                return BuildConfig.AD_NATIVE_ID;
            case SESSION_INTERSTITIAL:
                return BuildConfig.AD_INTERSTITIAL_ID;
            case AD_FREE_REWARDED:
                return BuildConfig.AD_REWARDED_ID;
            default:
                return BuildConfig.AD_BANNER_ID;
        }
    }

    /** Starts the Mobile Ads SDK off the main thread, once. */
    private static synchronized void initialize(Context context) {
        if (initialized) return;
        initialized = true;
        // A learning app used by all ages: keep ad content family-friendly
        MobileAds.setRequestConfiguration(new RequestConfiguration.Builder()
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_PG)
                .build());
        Context appContext = context.getApplicationContext();
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                MobileAds.initialize(appContext, status -> { });
            } catch (Exception e) {
                Log.w(TAG, "Mobile Ads failed to start", e);
            }
        });
        if (isTestMode()) Log.i(TAG, "Showing Google test ads");
    }

    // ── Banners ───────────────────────────────────────────────────────────────

    /**
     * Shows an adaptive banner in {@code container} while ads are allowed. The container stays
     * hidden until an ad arrives, while the keyboard is open (so typing keeps the full height),
     * and as soon as ads are turned off.
     */
    public static void attachBanner(FrameLayout container, LifecycleOwner owner, Placement placement) {
        container.removeAllViews();
        container.setVisibility(View.GONE);
        BannerSlot slot = new BannerSlot(container, placement);
        owner.getLifecycle().addObserver(slot);
        allowed.observe(owner, isAllowed -> slot.update(Boolean.TRUE.equals(isAllowed)));
        ViewCompat.setOnApplyWindowInsetsListener(container, (v, insets) -> {
            slot.setKeyboardOpen(insets.isVisible(WindowInsetsCompat.Type.ime()));
            return insets;
        });
    }

    /** A full-width banner sized for the current screen. */
    private static AdSize adaptiveSize(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int widthDp = (int) (metrics.widthPixels / metrics.density);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp);
    }

    /** One banner spot: loads, pauses, resumes and destroys the banner with its screen. */
    private static final class BannerSlot implements DefaultLifecycleObserver {
        private final FrameLayout container;
        private final Placement placement;
        @Nullable
        private AdView adView;
        private boolean loaded;
        private boolean keyboardOpen;

        BannerSlot(FrameLayout container, Placement placement) {
            this.container = container;
            this.placement = placement;
        }

        void update(boolean adsAllowed) {
            if (adsAllowed && isEnabled(container.getContext(), placement)) {
                if (adView == null) load();
            } else {
                remove();
            }
        }

        void setKeyboardOpen(boolean open) {
            keyboardOpen = open;
            render();
        }

        private void load() {
            AdView view = new AdView(container.getContext());
            view.setAdUnitId(unitId(placement));
            view.setAdSize(adaptiveSize(container.getContext()));
            view.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    loaded = true;
                    render();
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError error) {
                    loaded = false;
                    render();
                    Log.d(TAG, placement + " banner not loaded: " + error.getMessage());
                }
            });
            adView = view;
            container.addView(view);
            view.loadAd(new AdRequest.Builder().build());
        }

        private void remove() {
            if (adView != null) {
                container.removeView(adView);
                adView.destroy();
                adView = null;
            }
            loaded = false;
            render();
        }

        private void render() {
            container.setVisibility(loaded && !keyboardOpen ? View.VISIBLE : View.GONE);
        }

        @Override
        public void onResume(@NonNull LifecycleOwner owner) {
            if (adView != null) adView.resume();
        }

        @Override
        public void onPause(@NonNull LifecycleOwner owner) {
            if (adView != null) adView.pause();
        }

        @Override
        public void onDestroy(@NonNull LifecycleOwner owner) {
            owner.getLifecycle().removeObserver(this);
            remove();
        }
    }

    // ── Native ads ────────────────────────────────────────────────────────────

    /**
     * Loads one native ad for {@code placement}. {@code onResult} gets it on the main thread, or null
     * if none loaded; it isn't called when the placement is off. The caller must {@link NativeAd#destroy()} it.
     */
    public static void loadNative(Context context, Placement placement, Consumer<NativeAd> onResult) {
        if (!isEnabled(context, placement)) return;
        AdLoader loader = new AdLoader.Builder(context.getApplicationContext(), unitId(placement))
                .forNativeAd(onResult::accept)
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError error) {
                        Log.d(TAG, placement + " native ad not loaded: " + error.getMessage());
                        onResult.accept(null);
                    }
                })
                .withNativeAdOptions(new NativeAdOptions.Builder()
                        .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_LANDSCAPE)
                        .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                        .build())
                .build();
        loader.loadAd(new AdRequest.Builder().build());
    }
}
