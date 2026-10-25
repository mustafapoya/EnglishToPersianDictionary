package net.golbarg.engtoper.ads;

import android.content.Context;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;

import net.golbarg.engtoper.ads.AdConfig.Placement;

/**
 * Native ads drawn in the app's own layouts ({@link NativeAdBinder}). Hold one in a ViewModel with
 * {@link CachedNativeAd} so rotating or coming back to a screen shows the same ad.
 */
public final class NativeAds {

    private NativeAds() {
    }

    /** Loads one ad with landscape media. See {@link #load(Context, Placement, int, Consumer)}. */
    public static void load(Context context, Placement placement, Consumer<NativeAd> onResult) {
        load(context, placement, NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_LANDSCAPE, onResult);
    }

    /**
     * Loads one native ad for {@code placement}. {@code onResult} gets it on the main thread, or null
     * if none loaded; it isn't called when the placement isn't allowed. The caller must
     * {@link NativeAd#destroy()} the ad when done with it.
     *
     * @param mediaAspectRatio one of {@code NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_*}, matching the layout
     */
    public static void load(Context context, Placement placement, int mediaAspectRatio, Consumer<NativeAd> onResult) {
        AdManager.ensureStarted(context);
        if (!AdManager.isAllowed(context, placement)) return;
        loader(context, placement, mediaAspectRatio, onResult::accept, () -> onResult.accept(null))
                .loadAd(new AdRequest.Builder().build());
    }

    /**
     * Loads up to {@code count} (1 to 5) native ads in one request, for a list or deck with several
     * ad slots. {@code onEach} gets each ad on the main thread as it arrives; failures aren't
     * reported. The caller must {@link NativeAd#destroy()} every ad when done with it.
     */
    public static void loadMany(Context context, Placement placement, int count, int mediaAspectRatio, Consumer<NativeAd> onEach) {
        AdManager.ensureStarted(context);
        if (!AdManager.isAllowed(context, placement)) return;
        loader(context, placement, mediaAspectRatio, onEach::accept, () -> { })
                .loadAds(new AdRequest.Builder().build(), Math.max(1, Math.min(5, count)));
    }

    private static AdLoader loader(Context context, Placement placement, int mediaAspectRatio,
                                   NativeAd.OnNativeAdLoadedListener onLoaded, Runnable onFailed) {
        // AdChoices goes in the top corner at the end of the line, so right-to-left screens get it on the left
        boolean rtl = context.getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
        return new AdLoader.Builder(context.getApplicationContext(), AdManager.unitId(placement))
                .forNativeAd(onLoaded)
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError error) {
                        Log.d(AdManager.TAG, placement + " not loaded: " + error.getMessage());
                        onFailed.run();
                    }
                })
                .withNativeAdOptions(new NativeAdOptions.Builder()
                        .setMediaAspectRatio(mediaAspectRatio)
                        .setAdChoicesPlacement(rtl ? NativeAdOptions.ADCHOICES_TOP_LEFT : NativeAdOptions.ADCHOICES_TOP_RIGHT)
                        .build())
                .build();
    }
}
