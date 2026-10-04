package net.golbarg.engtoper.ui.intro;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.color.MaterialColors;

import net.golbarg.engtoper.MainActivity;
import net.golbarg.engtoper.R;
import net.golbarg.engtoper.databinding.ActivityIntroBinding;
import net.golbarg.engtoper.util.AppPreferences;

import java.util.Arrays;
import java.util.List;

public class IntroActivity extends AppCompatActivity {

    /** Set when the intro is replayed from Settings: finishing returns there instead of opening a new main screen. */
    public static final String EXTRA_REPLAY = "replay";

    private ActivityIntroBinding binding;
    private List<IntroPagerAdapter.IntroPage> pages;
    private int dotSize;
    private int dotActiveWidth;
    private boolean isFinishingIntro = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ActivityIntroBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.introRoot, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        pages = Arrays.asList(
                new IntroPagerAdapter.IntroPage(R.drawable.ic_translate,
                        R.string.intro_title_1, R.string.intro_desc_1,
                        R.string.intro_chip_1a, R.string.intro_chip_1b,
                        androidx.appcompat.R.attr.colorPrimary,
                        com.google.android.material.R.attr.colorPrimaryContainer),
                new IntroPagerAdapter.IntroPage(R.drawable.ic_volume_up,
                        R.string.intro_title_2, R.string.intro_desc_2,
                        R.string.intro_chip_2a, R.string.intro_chip_2b,
                        com.google.android.material.R.attr.colorSecondary,
                        com.google.android.material.R.attr.colorSecondaryContainer),
                new IntroPagerAdapter.IntroPage(R.drawable.ic_quiz,
                        R.string.intro_title_3, R.string.intro_desc_3,
                        R.string.intro_chip_3a, R.string.intro_chip_3b,
                        com.google.android.material.R.attr.colorTertiary,
                        com.google.android.material.R.attr.colorTertiaryContainer)
        );

        binding.introPager.setAdapter(new IntroPagerAdapter(pages));
        binding.introPager.setOffscreenPageLimit(1);
        binding.introPager.setPageTransformer(this::transformPage);

        float density = getResources().getDisplayMetrics().density;
        dotSize = Math.round(8 * density);
        dotActiveWidth = Math.round(24 * density);
        buildDots();

        binding.introPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateDots(position);
                updateControls(position);
            }
        });
        updateDots(0);
        updateControls(0);

        binding.btnNext.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            int current = binding.introPager.getCurrentItem();
            if (current < pages.size() - 1) {
                binding.introPager.setCurrentItem(current + 1, true);
            } else {
                finishIntro();
            }
        });
        binding.btnSkip.setOnClickListener(v -> finishIntro());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                int current = binding.introPager.getCurrentItem();
                if (current > 0) {
                    binding.introPager.setCurrentItem(current - 1, true);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    /** Fades neighbouring pages and gives the illustration a subtle parallax. */
    private void transformPage(View page, float position) {
        float absPos = Math.abs(position);
        page.setAlpha(Math.max(0f, 1f - absPos * 0.8f));
        View illustration = page.findViewById(R.id.intro_illustration);
        if (illustration != null) {
            illustration.setTranslationX(-position * page.getWidth() * 0.25f);
            float scale = 1f - Math.min(absPos, 1f) * 0.15f;
            illustration.setScaleX(scale);
            illustration.setScaleY(scale);
        }
    }

    private void buildDots() {
        binding.layoutDots.removeAllViews();
        for (int i = 0; i < pages.size(); i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dotSize, dotSize);
            if (i > 0) params.setMarginStart(dotSize);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(R.drawable.intro_dot);
            binding.layoutDots.addView(dot);
        }
    }

    private void updateDots(int selected) {
        int activeColor = MaterialColors.getColor(binding.getRoot(), androidx.appcompat.R.attr.colorPrimary);
        int inactiveColor = MaterialColors.getColor(binding.getRoot(), R.attr.appTextTertiary);

        for (int i = 0; i < binding.layoutDots.getChildCount(); i++) {
            View dot = binding.layoutDots.getChildAt(i);
            boolean active = i == selected;
            dot.setBackgroundTintList(ColorStateList.valueOf(active ? activeColor : inactiveColor));

            ViewGroup.LayoutParams params = dot.getLayoutParams();
            int target = active ? dotActiveWidth : dotSize;
            ValueAnimator animator = ValueAnimator.ofInt(params.width, target);
            animator.setDuration(250);
            animator.addUpdateListener(a -> {
                params.width = (int) a.getAnimatedValue();
                dot.setLayoutParams(params);
            });
            animator.start();
        }
        binding.layoutDots.setContentDescription(
                getString(R.string.intro_page_indicator, selected + 1, pages.size()));
    }

    private void updateControls(int position) {
        boolean isLast = position == pages.size() - 1;
        binding.btnNext.setText(isLast ? R.string.intro_get_started : R.string.intro_next);

        binding.btnSkip.animate().cancel();
        binding.btnSkip.animate().alpha(isLast ? 0f : 1f).setDuration(200)
                .withStartAction(() -> { if (!isLast) binding.btnSkip.setVisibility(View.VISIBLE); })
                .withEndAction(() -> { if (isLast) binding.btnSkip.setVisibility(View.INVISIBLE); })
                .start();
    }

    private void finishIntro() {
        if (isFinishingIntro) return;
        isFinishingIntro = true;
        AppPreferences.setIntroCompleted(this, true);
        if (getIntent().getBooleanExtra(EXTRA_REPLAY, false)) {
            finish();
            return;
        }
        Intent main = new Intent(this, MainActivity.class);
        if (getIntent().getExtras() != null) main.putExtras(getIntent().getExtras());
        startActivity(main);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
