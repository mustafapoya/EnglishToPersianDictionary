package net.golbarg.engtoper.ui.common;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.color.MaterialColors;

import net.golbarg.engtoper.R;
import net.golbarg.engtoper.util.TranslationParser;

import java.util.List;

/**
 * Shows a parsed translation: general meanings as wrapping pills (the first one emphasised),
 * then subject-specific meanings as "label · meaning" rows. Child views are reused across binds,
 * so it is cheap inside RecyclerView rows.
 */
public class MeaningsView extends LinearLayout {

    /** Pill colours: {@link #STYLE_SURFACE} for normal cards, {@link #STYLE_HERO} on the blue gradient. */
    public static final int STYLE_SURFACE = 0;
    public static final int STYLE_HERO = 1;

    private final PillFlowLayout pills;
    private final LinearLayout subjectRows;
    private final int style;
    private final float density;

    public MeaningsView(@NonNull Context context) {
        this(context, null);
    }

    public MeaningsView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        density = getResources().getDisplayMetrics().density;

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MeaningsView);
        style = a.getInt(R.styleable.MeaningsView_meaningsStyle, STYLE_SURFACE);
        a.recycle();

        pills = new PillFlowLayout(context, dp(6), dp(6));
        addView(pills, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        subjectRows = new LinearLayout(context);
        subjectRows.setOrientation(VERTICAL);
        LayoutParams rowsParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        addView(subjectRows, rowsParams);
    }

    /**
     * @param rtl         true when the meanings are Persian (pills flow right-to-left)
     * @param maxPills    pills to show before a "+N" pill; {@code Integer.MAX_VALUE} for all
     * @param maxSubjects subject rows to show; {@code Integer.MAX_VALUE} for all
     */
    public void bind(TranslationParser.Parsed parsed, boolean rtl, int maxPills, int maxSubjects) {
        int direction = rtl ? LAYOUT_DIRECTION_RTL : LAYOUT_DIRECTION_LTR;
        pills.setLayoutDirection(direction);
        subjectRows.setLayoutDirection(direction);

        bindPills(parsed.meanings, rtl, maxPills);
        bindSubjects(parsed.subjects, rtl, maxSubjects);
    }

    // ── Pills ─────────────────────────────────────────────────────────────────

    private void bindPills(List<String> meanings, boolean rtl, int maxPills) {
        int shown = Math.min(meanings.size(), maxPills);
        int hidden = meanings.size() - shown;
        int needed = shown + (hidden > 0 ? 1 : 0);

        ensureChildren(pills, needed, this::createPill);
        for (int i = 0; i < pills.getChildCount(); i++) {
            TextView pill = (TextView) pills.getChildAt(i);
            if (i >= needed) {
                pill.setVisibility(GONE);
                continue;
            }
            pill.setVisibility(VISIBLE);
            pill.setTextDirection(rtl ? TEXT_DIRECTION_RTL : TEXT_DIRECTION_LTR);
            if (i < shown) {
                stylePill(pill, i == 0 ? PillKind.PRIMARY : PillKind.NORMAL);
                pill.setText(meanings.get(i));
            } else {
                // "+N" is UI text, not a meaning: lay it out in the app language's direction
                pill.setTextDirection(TEXT_DIRECTION_LOCALE);
                stylePill(pill, PillKind.MORE);
                pill.setText(getContext().getString(R.string.meanings_more, hidden));
            }
        }
        pills.setVisibility(needed > 0 ? VISIBLE : GONE);
    }

    private enum PillKind { PRIMARY, NORMAL, MORE }

    private TextView createPill() {
        TextView pill = new TextView(getContext());
        pill.setBackgroundResource(R.drawable.bg_pill);
        pill.setPadding(dp(12), dp(5), dp(12), dp(6));
        pill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        pill.setMaxLines(2);
        pill.setGravity(Gravity.CENTER_VERTICAL);
        return pill;
    }

    private void stylePill(TextView pill, PillKind kind) {
        int background;
        int text;
        if (style == STYLE_HERO) {
            background = ContextCompat.getColor(getContext(), kind == PillKind.PRIMARY ? R.color.hero_on : R.color.hero_button);
            text = ContextCompat.getColor(getContext(), kind == PillKind.PRIMARY ? R.color.hero_end : R.color.hero_on);
        } else if (kind == PillKind.PRIMARY) {
            background = color(com.google.android.material.R.attr.colorPrimaryContainer);
            text = color(com.google.android.material.R.attr.colorOnPrimaryContainer);
        } else if (kind == PillKind.MORE) {
            background = color(R.attr.appBackgroundColor);
            text = color(androidx.appcompat.R.attr.colorPrimary);
        } else {
            background = color(R.attr.appSurfaceVariant);
            text = color(R.attr.appTextPrimary);
        }
        pill.setBackgroundTintList(ColorStateList.valueOf(background));
        pill.setTextColor(text);
        pill.setTypeface(null, kind == PillKind.NORMAL ? Typeface.NORMAL : Typeface.BOLD);
    }

    // ── Subject rows ──────────────────────────────────────────────────────────

    private void bindSubjects(List<TranslationParser.SubjectMeaning> subjects, boolean rtl, int maxSubjects) {
        int shown = Math.min(subjects.size(), maxSubjects);
        int hidden = subjects.size() - shown;
        int needed = shown + (hidden > 0 ? 1 : 0);

        ensureChildren(subjectRows, needed, this::createSubjectRow);
        for (int i = 0; i < subjectRows.getChildCount(); i++) {
            LinearLayout row = (LinearLayout) subjectRows.getChildAt(i);
            if (i >= needed) {
                row.setVisibility(GONE);
                continue;
            }
            row.setVisibility(VISIBLE);
            TextView label = (TextView) row.getChildAt(0);
            TextView meaning = (TextView) row.getChildAt(1);
            meaning.setTextDirection(rtl ? TEXT_DIRECTION_RTL : TEXT_DIRECTION_LTR);
            meaning.setTextAlignment(TEXT_ALIGNMENT_GRAVITY);
            if (i < shown) {
                label.setVisibility(VISIBLE);
                label.setText(subjects.get(i).subject);
                meaning.setText(subjects.get(i).meaning);
                meaning.setTextColor(color(style == STYLE_HERO ? 0 : R.attr.appTextSecondary));
            } else {
                label.setVisibility(GONE);
                meaning.setTextDirection(TEXT_DIRECTION_LOCALE);
                // UI text in the app language, aligned with the rows above it
                meaning.setTextAlignment(TEXT_ALIGNMENT_VIEW_END);
                meaning.setText(getContext().getResources().getQuantityString(R.plurals.subjects_more, hidden, hidden));
                meaning.setTextColor(color(androidx.appcompat.R.attr.colorPrimary));
            }
        }
        subjectRows.setVisibility(needed > 0 ? VISIBLE : GONE);
        ((LayoutParams) subjectRows.getLayoutParams()).topMargin =
                needed > 0 && pills.getVisibility() == VISIBLE ? dp(10) : 0;
    }

    private LinearLayout createSubjectRow() {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.TOP);
        row.setPadding(0, dp(3), 0, dp(3));

        TextView label = new TextView(getContext());
        label.setBackgroundResource(R.drawable.bg_pill);
        label.setBackgroundTintList(ColorStateList.valueOf(color(com.google.android.material.R.attr.colorTertiaryContainer)));
        label.setTextColor(color(com.google.android.material.R.attr.colorOnTertiaryContainer));
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        label.setTypeface(null, Typeface.BOLD);
        label.setPadding(dp(8), dp(2), dp(8), dp(3));
        label.setMaxLines(1);
        LayoutParams labelParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        labelParams.setMarginEnd(dp(8));
        labelParams.topMargin = dp(1);
        row.addView(label, labelParams);

        TextView meaning = new TextView(getContext());
        meaning.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        meaning.setLineSpacing(dp(2), 1f);
        row.addView(meaning, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private interface Factory {
        View create();
    }

    private static void ensureChildren(android.view.ViewGroup parent, int count, Factory factory) {
        while (parent.getChildCount() < count) parent.addView(factory.create());
    }

    private int color(int attr) {
        if (attr == 0) return ContextCompat.getColor(getContext(), R.color.hero_on_muted);
        return MaterialColors.getColor(this, attr);
    }

    private int dp(int value) {
        return Math.round(value * density);
    }
}
