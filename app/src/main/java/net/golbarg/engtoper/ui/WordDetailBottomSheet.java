package net.golbarg.engtoper.ui;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.color.MaterialColors;

import net.golbarg.engtoper.R;
import net.golbarg.engtoper.databinding.BottomSheetWordDetailBinding;
import net.golbarg.engtoper.models.PhraseEnglish;
import net.golbarg.engtoper.models.PhrasePersian;
import net.golbarg.engtoper.ui.common.PhraseItemBinder;
import net.golbarg.engtoper.util.TTSManager;
import net.golbarg.engtoper.util.TranslationParser;
import net.golbarg.engtoper.util.UtilController;

public class WordDetailBottomSheet extends BottomSheetDialogFragment {

    public interface OnReverseLookupListener {
        void onReverseLookup(String query, boolean fromEnglish);
    }

    public interface OnBookmarkToggleListener {
        void onBookmarkToggled(int id, boolean isEnglish, int newFavoriteState);
    }

    private BottomSheetWordDetailBinding binding;
    private String word;
    private String translation;
    private int wordId;
    private int favorite;
    private boolean isEnglish;

    private OnReverseLookupListener reverseLookupListener;
    private OnBookmarkToggleListener bookmarkToggleListener;

    public static WordDetailBottomSheet newInstance(PhraseEnglish phrase) {
        WordDetailBottomSheet sheet = new WordDetailBottomSheet();
        Bundle args = new Bundle();
        args.putInt("id", phrase.getId());
        args.putString("word", phrase.getFromLanguage());
        args.putString("translation", phrase.getToLanguage());
        args.putInt("favorite", phrase.getFavorite());
        args.putBoolean("isEnglish", true);
        sheet.setArguments(args);
        return sheet;
    }

    public static WordDetailBottomSheet newInstance(PhrasePersian phrase) {
        WordDetailBottomSheet sheet = new WordDetailBottomSheet();
        Bundle args = new Bundle();
        args.putInt("id", phrase.getId());
        args.putString("word", phrase.getFromLanguage());
        args.putString("translation", phrase.getToLanguage());
        args.putInt("favorite", phrase.getFavorite());
        args.putBoolean("isEnglish", false);
        sheet.setArguments(args);
        return sheet;
    }

    public void setOnReverseLookupListener(OnReverseLookupListener listener) {
        this.reverseLookupListener = listener;
    }

    public void setOnBookmarkToggleListener(OnBookmarkToggleListener listener) {
        this.bookmarkToggleListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetWordDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            wordId = getArguments().getInt("id");
            word = getArguments().getString("word", "");
            translation = getArguments().getString("translation", "");
            favorite = getArguments().getInt("favorite", 0);
            isEnglish = getArguments().getBoolean("isEnglish", true);
        }

        binding.detailWord.setText(word);
        binding.detailWord.setTextDirection(isEnglish ? View.TEXT_DIRECTION_LTR : View.TEXT_DIRECTION_RTL);
        TranslationParser.Parsed parsed = TranslationParser.parse(translation);
        binding.detailSummary.setText(PhraseItemBinder.summary(requireContext(), parsed));
        // An English word has Persian meanings (right-to-left) and vice versa
        binding.detailMeanings.bind(parsed, isEnglish, Integer.MAX_VALUE, Integer.MAX_VALUE);
        binding.detailMeanings.setVisibility(parsed.isEmpty() ? View.GONE : View.VISIBLE);
        binding.detailNoMeaning.setVisibility(parsed.isEmpty() ? View.VISIBLE : View.GONE);

        // Hide speaker for Persian words, show for English
        binding.detailBtnSpeaker.setVisibility(isEnglish ? View.VISIBLE : View.GONE);
        binding.detailBtnSpeaker.setOnClickListener(v -> {
            TTSManager.getInstance(requireContext()).speak(word, requireContext());
        });

        updateBookmarkButton();

        binding.detailBtnBookmark.setOnClickListener(v -> {
            favorite = (favorite == 1) ? 0 : 1;
            updateBookmarkButton();
            if (bookmarkToggleListener != null) {
                bookmarkToggleListener.onBookmarkToggled(wordId, isEnglish, favorite);
            }
        });

        binding.detailBtnCopy.setOnClickListener(v -> {
            String cleanText = word + "\n" + parsed.toPlainText(isEnglish ? "، " : ", ");
            UtilController.copyToClipboard(requireContext(), word, cleanText);
        });

        binding.detailBtnShare.setOnClickListener(v -> {
            UtilController.shareWord(requireContext(), word, parsed.toPlainText(isEnglish ? "، " : ", "));
        });

        binding.detailBtnReverse.setOnClickListener(v -> {
            dismiss();
            if (reverseLookupListener != null) {
                // Look up the first meaning in the other direction
                String firstMeaning = !parsed.meanings.isEmpty() ? parsed.meanings.get(0)
                        : !parsed.subjects.isEmpty() ? parsed.subjects.get(0).meaning : "";
                reverseLookupListener.onReverseLookup(firstMeaning, isEnglish);
            }
        });
    }

    private void updateBookmarkButton() {
        if (favorite == 1) {
            binding.detailBtnBookmark.setIconResource(R.drawable.ic_star_filled);
            binding.detailBtnBookmark.setIconTint(ContextCompat.getColorStateList(requireContext(), R.color.bookmark_gold));
        } else {
            binding.detailBtnBookmark.setIconResource(R.drawable.ic_star_outline);
            binding.detailBtnBookmark.setIconTint(ColorStateList.valueOf(MaterialColors.getColor(binding.detailBtnBookmark, R.attr.appTextSecondary)));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
