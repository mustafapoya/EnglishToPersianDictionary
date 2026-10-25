package net.golbarg.engtoper.ads;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;

import net.golbarg.engtoper.ads.AdConfig.Placement;

/** Anchored adaptive banners in a {@code FrameLayout} slot of a screen's layout. */
public final class BannerAds {

    private BannerAds() {
    }

    /**
     * Shows a banner in {@code container} while {@code placement} is allowed. The container stays
     * hidden until an ad arrives, while the keyboard is open, and as soon as ads stop being allowed.
     * Call from onViewCreated with the fragment's view lifecycle owner (or from an activity with itself).
     */
    public static void attach(@Nullable FrameLayout container, LifecycleOwner owner, Placement placement) {
        if (container == null) return;
        AdManager.ensureStarted(container.getContext());
        container.removeAllViews();
        container.setVisibility(View.GONE);
        BannerSlot slot = new BannerSlot(container, placement);
        owner.getLifecycle().addObserver(slot);
        AdManager.allowedState().observe(owner, isAllowed -> slot.update(Boolean.TRUE.equals(isAllowed)));
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
            if (adsAllowed && AdManager.isAllowed(container.getContext(), placement)) {
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
            view.setAdUnitId(AdManager.unitId(placement));
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
                    Log.d(AdManager.TAG, placement + " not loaded: " + error.getMessage());
                }
            });
            adView = view;
            container.addView(view, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL));
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
}
