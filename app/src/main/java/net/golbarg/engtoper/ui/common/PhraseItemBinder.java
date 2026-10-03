package net.golbarg.engtoper.ui.common;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.google.android.material.color.MaterialColors;

import net.golbarg.engtoper.R;
import net.golbarg.engtoper.databinding.ItemPhraseBinding;
import net.golbarg.engtoper.util.TranslationParser;

import java.util.HashMap;
import java.util.Map;

/** Binds {@code item_phrase.xml} for either direction; shared by search results and bookmarks. */
public class PhraseItemBinder {
    private static final int LIST_MAX_PILLS = 6;
    private static final int LIST_MAX_SUBJECTS = 2;

    private final Map<Integer, TranslationParser.Parsed> cache = new HashMap<>();

    public TranslationParser.Parsed parsed(int id, String rawTranslation) {
        TranslationParser.Parsed parsed = cache.get(id);
        if (parsed == null) {
            parsed = TranslationParser.parse(rawTranslation);
            cache.put(id, parsed);
        }
        return parsed;
    }

    public void clearCache() {
        cache.clear();
    }

    /**
     * @param englishWord true for an English headword (Persian meanings, pronunciation available)
     */
    public void bind(ItemPhraseBinding binding, int id, String word, String rawTranslation,
                     boolean favorite, boolean englishWord) {
        Context context = binding.getRoot().getContext();
        TranslationParser.Parsed parsed = parsed(id, rawTranslation);

        binding.txtWord.setText(word);
        binding.txtWord.setTextDirection(englishWord ? View.TEXT_DIRECTION_LTR : View.TEXT_DIRECTION_RTL);
        binding.txtSummary.setText(summary(context, parsed));
        binding.txtSummary.setVisibility(binding.txtSummary.length() > 0 ? View.VISIBLE : View.GONE);
        binding.btnSpeaker.setVisibility(englishWord ? View.VISIBLE : View.GONE);

        // English headword → Persian meanings (RTL); Persian headword → English meanings (LTR)
        binding.meanings.bind(parsed, englishWord, LIST_MAX_PILLS, LIST_MAX_SUBJECTS);
        binding.meanings.setVisibility(parsed.isEmpty() ? View.GONE : View.VISIBLE);
        binding.txtNoMeaning.setVisibility(parsed.isEmpty() ? View.VISIBLE : View.GONE);

        binding.btnBookmark.setIconResource(favorite ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
        binding.btnBookmark.setIconTint(favorite
                ? ContextCompat.getColorStateList(context, R.color.bookmark_gold)
                : ColorStateList.valueOf(MaterialColors.getColor(binding.btnBookmark, R.attr.appTextTertiary)));
    }

    /** "9 meanings · 2 subjects · 4 related phrases" — only the parts that apply. */
    public static String summary(Context context, TranslationParser.Parsed parsed) {
        StringBuilder text = new StringBuilder();
        append(context, text, R.plurals.meaning_count, parsed.meanings.size());
        append(context, text, R.plurals.subject_count, parsed.subjects.size());
        append(context, text, R.plurals.related_phrases, parsed.relatedCount);
        return text.toString();
    }

    private static void append(Context context, StringBuilder text, int plural, int count) {
        if (count <= 0) return;
        if (text.length() > 0) text.append(context.getString(R.string.summary_separator));
        text.append(context.getResources().getQuantityString(plural, count, count));
    }
}
