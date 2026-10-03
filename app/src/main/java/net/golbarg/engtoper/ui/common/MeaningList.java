package net.golbarg.engtoper.ui.common;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.color.MaterialColors;

import net.golbarg.engtoper.R;
import net.golbarg.engtoper.util.LocaleUtil;
import net.golbarg.engtoper.util.TranslationParser;

import java.util.ArrayList;
import java.util.List;

/** Renders meanings as a numbered list (numbers in small circles); used by the word of the day and flashcards. */
public final class MeaningList {

    private MeaningList() {
    }

    /**
     * Fills {@code container} with up to {@code max} meanings; subject meanings follow the general ones.
     *
     * @param rtl true when the meanings are Persian
     * @return how many meanings did not fit
     */
    public static int render(LinearLayout container, TranslationParser.Parsed parsed, int max, boolean rtl) {
        List<String> items = new ArrayList<>(parsed.meanings);
        for (TranslationParser.SubjectMeaning subject : parsed.subjects) {
            items.add(subject.meaning + " (" + subject.subject + ")");
        }
        int shown = Math.min(items.size(), max);

        Context context = container.getContext();
        float density = context.getResources().getDisplayMetrics().density;
        int numberSize = Math.round(26 * density);
        int verticalPadding = Math.round(7 * density);
        int onContainer = MaterialColors.getColor(container, com.google.android.material.R.attr.colorOnPrimaryContainer);
        int containerColor = MaterialColors.getColor(container, com.google.android.material.R.attr.colorPrimaryContainer);
        int primaryText = MaterialColors.getColor(container, R.attr.appTextPrimary);
        int secondaryText = MaterialColors.getColor(container, R.attr.appTextSecondary);

        container.removeAllViews();
        for (int i = 0; i < shown; i++) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setLayoutDirection(rtl ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
            row.setPadding(0, verticalPadding, 0, verticalPadding);

            TextView number = new TextView(context);
            number.setText(LocaleUtil.number(context, i + 1));
            number.setGravity(Gravity.CENTER);
            number.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            number.setTypeface(Typeface.DEFAULT_BOLD);
            number.setTextColor(onContainer);
            number.setBackgroundResource(R.drawable.bg_circle);
            number.setBackgroundTintList(ColorStateList.valueOf(containerColor));
            row.addView(number, new LinearLayout.LayoutParams(numberSize, numberSize));

            TextView meaning = new TextView(context);
            meaning.setText(items.get(i));
            meaning.setTextDirection(rtl ? View.TEXT_DIRECTION_RTL : View.TEXT_DIRECTION_LTR);
            meaning.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
            meaning.setLineSpacing(2 * density, 1f);
            meaning.setTextColor(i == 0 ? primaryText : secondaryText);
            if (i == 0) meaning.setTypeface(Typeface.DEFAULT_BOLD);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            params.setMarginStart(Math.round(12 * density));
            row.addView(meaning, params);

            container.addView(row);
        }
        return items.size() - shown;
    }
}
