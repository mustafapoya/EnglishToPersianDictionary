package net.golbarg.engtoper;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.SystemBarStyle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import net.golbarg.engtoper.databinding.ActivitySplashBinding;
import net.golbarg.engtoper.db.OfflineDatabaseHandler;
import net.golbarg.engtoper.ui.intro.IntroActivity;
import net.golbarg.engtoper.util.AppPreferences;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SplashScreenActivity extends AppCompatActivity {

    // Long enough for the entrance animation to finish before we leave the screen
    private static final long MIN_SPLASH_DURATION_MS = 1600;

    private ActivitySplashBinding binding;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ExecutorService executor;
    private ObjectAnimator pulseAnimator;
    private boolean isNavigated = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppPreferences.getThemeMode(this));
        EdgeToEdge.enable(this,
                SystemBarStyle.dark(Color.TRANSPARENT),
                SystemBarStyle.dark(Color.TRANSPARENT));
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.layoutBottomStatus, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setTranslationY(-bars.bottom);
            return windowInsets;
        });

        // The splash should not be dismissable mid-load
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
            }
        });

        playEntranceAnimation();

        long startTime = System.currentTimeMillis();

        // Pre-initialize database in background
        executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                OfflineDatabaseHandler dbHandler = new OfflineDatabaseHandler(getApplicationContext());
                dbHandler.openDatabase();
            } catch (Exception e) {
                e.printStackTrace();
            }

            long elapsed = System.currentTimeMillis() - startTime;
            long remaining = Math.max(0, MIN_SPLASH_DURATION_MS - elapsed);

            handler.postDelayed(this::navigateNext, remaining);
        });
    }

    private void playEntranceAnimation() {
        DecelerateInterpolator decelerate = new DecelerateInterpolator(2f);

        binding.splashGlow.animate().alpha(1f).setDuration(900).setInterpolator(decelerate).start();

        binding.splashMark.animate()
                .alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(650)
                .setInterpolator(new OvershootInterpolator(1.6f))
                .withEndAction(this::startPulse)
                .start();

        animateRing(binding.splashRingInner, 150);
        animateRing(binding.splashRingOuter, 300);

        slideUpFadeIn(binding.txtAppName, 300);
        slideUpFadeIn(binding.txtTagline, 420);

        binding.layoutBottomStatus.animate()
                .alpha(1f).setStartDelay(600).setDuration(400).setInterpolator(decelerate).start();
    }

    private void animateRing(View ring, long delay) {
        ring.setScaleX(0.7f);
        ring.setScaleY(0.7f);
        ring.animate()
                .alpha(1f).scaleX(1f).scaleY(1f)
                .setStartDelay(delay)
                .setDuration(800)
                .setInterpolator(new DecelerateInterpolator(2f))
                .start();
    }

    private void slideUpFadeIn(View view, long delay) {
        view.setTranslationY(24 * getResources().getDisplayMetrics().density);
        view.animate()
                .alpha(1f).translationY(0f)
                .setStartDelay(delay)
                .setDuration(500)
                .setInterpolator(new DecelerateInterpolator(2f))
                .start();
    }

    /** Gentle "breathing" of the outer ring while the database finishes loading. */
    private void startPulse() {
        if (isFinishing()) return;
        pulseAnimator = ObjectAnimator.ofFloat(binding.splashRingOuter, View.ALPHA, 1f, 0.35f);
        pulseAnimator.setDuration(900);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.start();
    }

    private void navigateNext() {
        if (isNavigated || isFinishing() || isDestroyed()) return;
        isNavigated = true;

        Class<?> target = AppPreferences.isIntroCompleted(this) ? MainActivity.class : IntroActivity.class;
        startActivity(new Intent(this, target));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (pulseAnimator != null) pulseAnimator.cancel();
        if (executor != null) executor.shutdown();
        super.onDestroy();
    }
}
