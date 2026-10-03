package net.golbarg.engtoper.ui.bookmark;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.text.BidiFormatter;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;

import net.golbarg.engtoper.MainActivity;
import net.golbarg.engtoper.R;
import net.golbarg.engtoper.databinding.FragmentBookmarkBinding;
import net.golbarg.engtoper.db.StudyRepository;
import net.golbarg.engtoper.models.PhraseEnglish;
import net.golbarg.engtoper.models.PhrasePersian;
import net.golbarg.engtoper.ui.DictionaryViewModel;
import net.golbarg.engtoper.ui.WordDetailBottomSheet;
import net.golbarg.engtoper.ui.dictionary.PhraseEnglishAdapter;
import net.golbarg.engtoper.ui.dictionary.PhrasePersianAdapter;
import net.golbarg.engtoper.util.AdUtil;
import net.golbarg.engtoper.util.LocaleUtil;
import net.golbarg.engtoper.util.TTSManager;
import net.golbarg.engtoper.util.UtilController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Saved words in both languages: filter, practise as flashcards, un-star with undo. */
public class BookmarkFragment extends Fragment {

    private FragmentBookmarkBinding binding;
    private DictionaryViewModel viewModel;
    private PhraseEnglishAdapter englishAdapter;
    private PhrasePersianAdapter persianAdapter;

    private List<PhraseEnglish> englishBookmarks = Collections.emptyList();
    private List<PhrasePersian> persianBookmarks = Collections.emptyList();
    private boolean showingEnglish = true;
    private boolean userPickedLanguage = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBookmarkBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(DictionaryViewModel.class);

        englishAdapter = new PhraseEnglishAdapter(
                this::openWordDetails,
                phrase -> TTSManager.getInstance(requireContext()).speak(phrase.getFromLanguage(), requireContext()),
                (phrase, position) -> unsave(phrase));
        persianAdapter = new PhrasePersianAdapter(
                this::openWordDetails,
                (phrase, position) -> unsave(phrase));
        binding.recyclerBookmark.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerBookmark.setAdapter(englishAdapter);

        setupFilter();
        binding.chipsLang.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty()) return;
            userPickedLanguage = true;
            showLanguage(ids.get(0) == R.id.chip_english);
        });
        binding.btnPractice.setOnClickListener(v -> openTab(R.id.navigation_flashcards, true));
        binding.btnExplore.setOnClickListener(v -> openTab(R.id.navigation_dictionary, false));

        viewModel.getEnglishBookmarks().observe(getViewLifecycleOwner(), list -> {
            englishBookmarks = list != null ? list : Collections.emptyList();
            render();
        });
        viewModel.getPersianBookmarks().observe(getViewLifecycleOwner(), list -> {
            persianBookmarks = list != null ? list : Collections.emptyList();
            render();
        });

        AdUtil.attachBanner(binding.adContainer, getViewLifecycleOwner(), AdUtil.Placement.SAVED_BANNER);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Words may have been starred on other screens
        viewModel.loadEnglishBookmarks();
        viewModel.loadPersianBookmarks();
    }

    private void setupFilter() {
        binding.editFilter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.btnClearFilter.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                render();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        binding.btnClearFilter.setOnClickListener(v -> binding.editFilter.setText(""));
    }

    private void showLanguage(boolean english) {
        if (english == showingEnglish) return;
        showingEnglish = english;
        binding.recyclerBookmark.setAdapter(english ? englishAdapter : persianAdapter);
        render();
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    private void render() {
        if (binding == null) return;
        int english = englishBookmarks.size();
        int persian = persianBookmarks.size();
        int total = english + persian;

        // Open on the language that has words, until the user picks one themselves
        if (!userPickedLanguage && english == 0 && persian > 0 && showingEnglish) {
            binding.chipPersian.setChecked(true);
            userPickedLanguage = false;
            return;
        }

        binding.txtCount.setText(getResources().getQuantityString(R.plurals.saved_count, total,
                LocaleUtil.number(requireContext(), total)));
        // Isolate each language name so "فارسی · 4" keeps the count after the name in either UI direction
        BidiFormatter bidi = BidiFormatter.getInstance();
        binding.chipEnglish.setText(getString(R.string.deck_with_count,
                bidi.unicodeWrap(getString(R.string.flashcard_lang_english)), english));
        binding.chipPersian.setText(getString(R.string.deck_with_count,
                bidi.unicodeWrap(getString(R.string.flashcard_lang_persian)), persian));
        binding.btnPractice.setEnabled(total > 0);

        String query = filterQuery();
        int shown;
        if (showingEnglish) {
            List<PhraseEnglish> filtered = new ArrayList<>();
            for (PhraseEnglish phrase : englishBookmarks) {
                if (matches(query, phrase.getFromLanguage(), phrase.getToLanguage())) filtered.add(phrase);
            }
            englishAdapter.submitList(filtered);
            shown = filtered.size();
        } else {
            List<PhrasePersian> filtered = new ArrayList<>();
            for (PhrasePersian phrase : persianBookmarks) {
                if (matches(query, phrase.getFromLanguage(), phrase.getToLanguage())) filtered.add(phrase);
            }
            persianAdapter.submitList(filtered);
            shown = filtered.size();
        }

        boolean nothingSaved = (showingEnglish ? english : persian) == 0;
        boolean empty = shown == 0;
        binding.recyclerBookmark.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.layoutEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
            binding.imgEmpty.setImageResource(nothingSaved ? R.drawable.ic_star_outline : R.drawable.ic_search);
            binding.txtEmptyTitle.setText(nothingSaved ? getString(R.string.deck_saved_empty_title)
                    : getString(R.string.saved_no_match, query));
            binding.txtEmptyMessage.setText(nothingSaved ? R.string.saved_empty_text : R.string.saved_no_match_hint);
            binding.btnExplore.setVisibility(nothingSaved ? View.VISIBLE : View.GONE);
        }
    }

    private String filterQuery() {
        return binding.editFilter.getText() != null ? binding.editFilter.getText().toString().trim() : "";
    }

    private static boolean matches(String query, String word, String translation) {
        if (query.isEmpty()) return true;
        String q = query.toLowerCase(Locale.ROOT);
        return (word != null && word.toLowerCase(Locale.ROOT).contains(q))
                || UtilController.removeHTMLTags(translation).toLowerCase(Locale.ROOT).contains(q);
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void unsave(PhraseEnglish phrase) {
        viewModel.setFavoriteEnglish(phrase, 0, () ->
                showUndo(() -> viewModel.setFavoriteEnglish(phrase, 1, null)));
    }

    private void unsave(PhrasePersian phrase) {
        viewModel.setFavoritePersian(phrase, 0, () ->
                showUndo(() -> viewModel.setFavoritePersian(phrase, 1, null)));
    }

    private void showUndo(Runnable undo) {
        if (binding == null) return;
        Snackbar snackbar = Snackbar.make(binding.getRoot(), R.string.bookmark_removed, Snackbar.LENGTH_LONG)
                .setAction(R.string.action_undo, v -> undo.run());
        View navBar = requireActivity().findViewById(R.id.nav_container);
        if (navBar != null && navBar.getVisibility() == View.VISIBLE) snackbar.setAnchorView(navBar);
        snackbar.show();
    }

    private void openWordDetails(PhraseEnglish phrase) {
        WordDetailBottomSheet sheet = WordDetailBottomSheet.newInstance(phrase);
        sheet.setOnBookmarkToggleListener((id, isEnglish, newState) -> viewModel.setFavoriteEnglish(phrase, newState, null));
        sheet.show(getChildFragmentManager(), "WORD_DETAIL");
    }

    private void openWordDetails(PhrasePersian phrase) {
        WordDetailBottomSheet sheet = WordDetailBottomSheet.newInstance(phrase);
        sheet.setOnBookmarkToggleListener((id, isEnglish, newState) -> viewModel.setFavoritePersian(phrase, newState, null));
        sheet.show(getChildFragmentManager(), "WORD_DETAIL");
    }

    private void openTab(int destination, boolean savedDeck) {
        if (savedDeck) viewModel.requestFlashcardDeck(StudyRepository.Deck.SAVED);
        if (requireActivity() instanceof MainActivity) ((MainActivity) requireActivity()).openTab(destination);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
