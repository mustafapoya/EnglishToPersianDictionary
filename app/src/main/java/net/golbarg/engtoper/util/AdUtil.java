package net.golbarg.engtoper.util;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;

import net.golbarg.engtoper.BuildConfig;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.Executors;

/**
 * The single place that shows ads.
 *
 * <ul>
 *     <li>Turn every ad on or off: {@code ads.enabled} in gradle.properties.</li>
 *     <li>Test or real ads: {@code ads.mode} in gradle.properties ({@code auto} = test ads in debug builds,
 *     real ads in release builds; {@code test} or {@code real} forces one).</li>
 *     <li>Turn one placement off: remove it from {@link #ENABLED_PLACEMENTS}.</li>
 * </ul>
 * The unit and app IDs live in app/build.gradle.
 */
public final class AdUtil {

    private static final String TAG = "AdUtil";

    /** Every spot in the app that can show an ad. */
    public enum Placement { DICTIONARY_BANNER, SAVED_BANNER }

    private static final Set<Placement> ENABLED_PLACEMENTS = EnumSet.of(
            Placement.DICTIONARY_BANNER,
            Placement.SAVED_BANNER
    );

    private static boolean initialized = false;

    private AdUtil() {
    }

    public static boolean isEnabled() {
        return BuildConfig.ADS_ENABLED;
    }

    public static boolean isTestMode() {
        return BuildConfig.ADS_TEST_MODE;
    }

    public static boolean isEnabled(Placement placement) {
        return isEnabled() && ENABLED_PLACEMENTS.contains(placement);
    }

    /** Starts the Mobile Ads SDK off the main thread; does nothing when ads are disabled. */
    public static synchronized void initialize(Context context) {
        if (!isEnabled() || initialized) return;
        initialized = true;
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

    /**
     * Loads an adaptive banner into {@code container}. The container stays hidden until an ad
     * arrives, so screens keep their full height when ads are off or nothing loads.
     */
    public static void attachBanner(FrameLayout container, LifecycleOwner owner, Placement placement) {
        container.removeAllViews();
        container.setVisibility(View.GONE);
        if (!isEnabled(placement)) return;
        initialize(container.getContext());

        AdView adView = new AdView(container.getContext());
        adView.setAdUnitId(BuildConfig.AD_BANNER_ID);
        adView.setAdSize(adaptiveSize(container.getContext()));
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                container.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                container.setVisibility(View.GONE);
                Log.d(TAG, placement + " banner not loaded: " + error.getMessage());
            }
        });
        container.addView(adView);
        owner.getLifecycle().addObserver(new BannerLifecycle(adView));
        adView.loadAd(new AdRequest.Builder().build());
    }

    /** A full-width banner sized for the current screen. */
    private static AdSize adaptiveSize(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int widthDp = (int) (metrics.widthPixels / metrics.density);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp);
    }

    /** Pauses, resumes and destroys the banner together with its screen. */
    private static class BannerLifecycle implements DefaultLifecycleObserver {
        private final AdView adView;

        BannerLifecycle(AdView adView) {
            this.adView = adView;
        }

        @Override
        public void onResume(@NonNull LifecycleOwner owner) {
            adView.resume();
        }

        @Override
        public void onPause(@NonNull LifecycleOwner owner) {
            adView.pause();
        }

        @Override
        public void onDestroy(@NonNull LifecycleOwner owner) {
            owner.getLifecycle().removeObserver(this);
            adView.destroy();
        }
    }
}
