package net.golbarg.engtoper.ui.common;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import com.google.android.material.color.MaterialColors;

import net.golbarg.engtoper.R;

import java.util.List;

/**
 * Bottom navigation where only the active tab shows its label, next to the icon, inside a pill.
 * The active tab takes more width than the others; switching animates the change.
 */
public class PillNavBar extends LinearLayout {

    public static class Item {
        @IdRes
        final int id;
        @DrawableRes
        final int icon;
        @StringRes
        final int label;

        public Item(@IdRes int id, @DrawableRes int icon, @StringRes int label) {
            this.id = id;
            this.icon = icon;
            this.label = label;
        }
    }

    public interface OnItemSelectedListener {
        void onItemSelected(@IdRes int id);
    }

    private static final float ACTIVE_WEIGHT = 2.4f;
    private static final float INACTIVE_WEIGHT = 1f;
    private static final long ANIMATION_MS = 220;

    private final float density;
    private int selectedId = View.NO_ID;
    @Nullable
    private OnItemSelectedListener listener;

    public PillNavBar(@NonNull Context context) {
        this(context, null);
    }

    public PillNavBar(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setPadding(dp(8), dp(10), dp(8), dp(10));
    }

    public void setOnItemSelectedListener(@Nullable OnItemSelectedListener listener) {
        this.listener = listener;
    }

    public void setItems(List<Item> items) {
        removeAllViews();
        for (Item item : items) addView(createCell(item));
        applyStates(false);
    }

    public int getSelectedId() {
        return selectedId;
    }

    /** Highlights a tab without notifying the listener (e.g. after navigation happened elsewhere). */
    public void setSelected(@IdRes int id, boolean animate) {
        if (id == selectedId) return;
        selectedId = id;
        applyStates(animate);
    }

    // ── Cells ─────────────────────────────────────────────────────────────────

    private View createCell(Item item) {
        ImageView icon = new ImageView(getContext());
        icon.setImageResource(item.icon);
        icon.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);

        TextView label = new TextView(getContext());
        label.setText(item.label);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setMaxLines(1);
        label.setSingleLine(true);

        LinearLayout pill = new LinearLayout(getContext());
        pill.setOrientation(HORIZONTAL);
        pill.setGravity(Gravity.CENTER);
        pill.setBackgroundResource(R.drawable.bg_pill);
        pill.setPadding(dp(14), 0, dp(14), 0);
        pill.addView(icon, new LayoutParams(dp(22), dp(22)));
        LayoutParams labelParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        labelParams.setMarginStart(dp(8));
        pill.addView(label, labelParams);

        LinearLayout cell = new LinearLayout(getContext());
        cell.setId(item.id);
        cell.setGravity(Gravity.CENTER);
        cell.setClickable(true);
        cell.setFocusable(true);
        cell.setContentDescription(getContext().getString(item.label));
        cell.setBackgroundResource(R.drawable.nav_cell_ripple);
        cell.addView(pill, new LayoutParams(LayoutParams.WRAP_CONTENT, dp(44)));
        cell.setOnClickListener(v -> {
            if (item.id == selectedId) return;  // re-tapping the current tab does nothing
            v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            if (listener != null) listener.onItemSelected(item.id);
        });
        cell.setLayoutParams(new LayoutParams(0, dp(48), INACTIVE_WEIGHT));
        return cell;
    }

    private void applyStates(boolean animate) {
        if (animate) {
            TransitionManager.beginDelayedTransition(this, new AutoTransition().setDuration(ANIMATION_MS));
        }
        int activeBackground = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimaryContainer);
        int activeContent = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnPrimaryContainer);
        int inactiveContent = MaterialColors.getColor(this, R.attr.appTextSecondary);

        for (int i = 0; i < getChildCount(); i++) {
            LinearLayout cell = (LinearLayout) getChildAt(i);
            LinearLayout pill = (LinearLayout) cell.getChildAt(0);
            ImageView icon = (ImageView) pill.getChildAt(0);
            TextView label = (TextView) pill.getChildAt(1);
            boolean active = cell.getId() == selectedId;

            LayoutParams params = (LayoutParams) cell.getLayoutParams();
            params.weight = active ? ACTIVE_WEIGHT : INACTIVE_WEIGHT;
            cell.setLayoutParams(params);
            cell.setSelected(active);

            // Selected state switches the icon selectors from outline to filled
            icon.setSelected(active);
            icon.setImageTintList(ColorStateList.valueOf(active ? activeContent : inactiveContent));
            label.setTextColor(activeContent);
            label.setVisibility(active ? VISIBLE : GONE);
            pill.setBackgroundTintList(ColorStateList.valueOf(active ? activeBackground : 0x00000000));
        }
    }

    private int dp(int value) {
        return Math.round(value * density);
    }
}
