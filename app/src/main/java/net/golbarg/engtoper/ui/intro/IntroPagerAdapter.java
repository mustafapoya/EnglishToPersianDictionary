package net.golbarg.engtoper.ui.intro;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.AttrRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;

import net.golbarg.engtoper.databinding.ItemIntroPageBinding;

import java.util.List;

public class IntroPagerAdapter extends RecyclerView.Adapter<IntroPagerAdapter.PageViewHolder> {

    public static class IntroPage {
        @DrawableRes final int icon;
        @StringRes final int title;
        @StringRes final int description;
        @StringRes final int chipA;
        @StringRes final int chipB;
        @AttrRes final int accentAttr;
        @AttrRes final int containerAttr;

        public IntroPage(@DrawableRes int icon, @StringRes int title, @StringRes int description,
                         @StringRes int chipA, @StringRes int chipB,
                         @AttrRes int accentAttr, @AttrRes int containerAttr) {
            this.icon = icon;
            this.title = title;
            this.description = description;
            this.chipA = chipA;
            this.chipB = chipB;
            this.accentAttr = accentAttr;
            this.containerAttr = containerAttr;
        }
    }

    private final List<IntroPage> pages;

    public IntroPagerAdapter(List<IntroPage> pages) {
        this.pages = pages;
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemIntroPageBinding binding = ItemIntroPageBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new PageViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        holder.bind(pages.get(position));
    }

    @Override
    public void onViewAttachedToWindow(@NonNull PageViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        holder.startFloating();
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull PageViewHolder holder) {
        holder.stopFloating();
        super.onViewDetachedFromWindow(holder);
    }

    @Override
    public int getItemCount() {
        return pages.size();
    }

    static class PageViewHolder extends RecyclerView.ViewHolder {
        private final ItemIntroPageBinding binding;
        private ObjectAnimator floatA;
        private ObjectAnimator floatB;

        PageViewHolder(ItemIntroPageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(IntroPage page) {
            View root = binding.getRoot();
            int accent = MaterialColors.getColor(root, page.accentAttr);
            int container = MaterialColors.getColor(root, page.containerAttr);

            binding.circleOuter.setBackgroundTintList(ColorStateList.valueOf(container));
            binding.circleInner.setBackgroundTintList(ColorStateList.valueOf(container));
            binding.imgIcon.setImageResource(page.icon);
            binding.imgIcon.setImageTintList(ColorStateList.valueOf(accent));

            binding.chipA.setText(page.chipA);
            binding.chipB.setText(page.chipB);
            binding.chipA.setTextColor(accent);

            binding.txtTitle.setText(page.title);
            binding.txtDescription.setText(page.description);
        }

        void startFloating() {
            stopFloating();
            float distance = 6 * binding.getRoot().getResources().getDisplayMetrics().density;
            floatA = createFloat(binding.chipA, -distance, 2200);
            floatB = createFloat(binding.chipB, distance, 2600);
        }

        void stopFloating() {
            if (floatA != null) floatA.cancel();
            if (floatB != null) floatB.cancel();
            binding.chipA.setTranslationY(0f);
            binding.chipB.setTranslationY(0f);
        }

        private ObjectAnimator createFloat(View view, float distance, long duration) {
            ObjectAnimator animator = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, 0f, distance);
            animator.setDuration(duration);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.setRepeatMode(ValueAnimator.REVERSE);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.start();
            return animator;
        }
    }
}
