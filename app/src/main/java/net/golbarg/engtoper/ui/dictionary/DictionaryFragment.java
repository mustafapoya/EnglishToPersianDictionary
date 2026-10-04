package net.golbarg.engtoper.ui.dictionary;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;


import com.google.android.material.chip.Chip;

import net.golbarg.engtoper.R;
import net.golbarg.engtoper.databinding.FragmentDictionaryBinding;
import net.golbarg.engtoper.databinding.ItemRecentSearchBinding;
import net.golbarg.engtoper.models.PhraseEnglish;
import net.golbarg.engtoper.models.PhrasePersian;
import net.golbarg.engtoper.models.SearchFilter;
import net.golbarg.engtoper.models.SearchHistoryItem;
import net.golbarg.engtoper.ui.DictionaryViewModel;
import net.golbarg.engtoper.ui.WordDetailBottomSheet;
import net.golbarg.engtoper.ads.AdUtil;
import net.golbarg.engtoper.util.PersianText;
import net.golbarg.engtoper.util.TTSManager;

import java.util.ArrayList;
import java.util.List;

import static net.golbarg.engtoper.ui.DictionaryViewModel.LANG_EN;
import static net.golbarg.engtoper.ui.DictionaryViewModel.LANG_FA;

/** One search screen for both directions, switched with a segmented toggle. */
public class DictionaryFragment extends Fragment {

    private static final String STATE_QUERY_EN = "query_en";
    private static final String STATE_QUERY_FA = "query_fa";

    private FragmentDictionaryBinding binding;
    private DictionaryViewModel viewModel;
    private PhraseEnglishAdapter englishAdapter;
    private PhrasePersianAdapter persianAdapter;

    /** The query typed in each direction, restored when switching back. */
    private String queryEn = "";
    private String queryFa = "";
    private String lang = LANG_EN;
    private boolean applyingLang = false;

    private ActivityResultLauncher<Intent> speechRecognizerLauncher;

    private final OnBackPressedCallback clearSearchOnBack = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            if (binding != null) binding.editSearch.setText("");
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            queryEn = savedInstanceState.getString(STATE_QUERY_EN, "");
            queryFa = savedInstanceState.getString(STATE_QUERY_FA, "");
        }
        speechRecognizerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) return;
                    ArrayList<String> matches = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (matches != null && !matches.isEmpty()) setQuery(matches.get(0));
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDictionaryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(DictionaryViewModel.class);
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), clearSearchOnBack);

        setupRecyclerView();
        setupDirectionToggle();
        setupSearchInput();
        setupFilterChips();
        binding.btnClearHistory.setOnClickListener(v -> viewModel.clearSearchHistory(lang));
        observeViewModel();

        AdUtil.attachBanner(binding.adContainer, getViewLifecycleOwner(), AdUtil.Placement.DICTIONARY_BANNER);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        rememberCurrentQuery();
        outState.putString(STATE_QUERY_EN, queryEn);
        outState.putString(STATE_QUERY_FA, queryFa);
    }

    private boolean isEnglish() {
        return LANG_EN.equals(lang);
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private void setupRecyclerView() {
        englishAdapter = new PhraseEnglishAdapter(
                this::openWordDetails,
                phrase -> TTSManager.getInstance(requireContext()).speak(phrase.getFromLanguage(), requireContext()),
                (phrase, position) -> viewModel.toggleFavoriteEnglish(phrase, () -> englishAdapter.notifyItemChanged(position))
        );
        persianAdapter = new PhrasePersianAdapter(
                this::openWordDetails,
                (phrase, position) -> viewModel.toggleFavoritePersian(phrase, () -> persianAdapter.notifyItemChanged(position))
        );
        binding.recyclerPhrase.setLayoutManager(new LinearLayoutManager(requireContext()));
    }

    private void setupDirectionToggle() {
        binding.directionSwitch.getRoot().setOnClickListener(v -> {
            binding.directionSwitch.iconSwap.animate().rotationBy(180f).setDuration(250).start();
            v.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
            viewModel.setDictionaryLang(isEnglish() ? LANG_FA : LANG_EN);
        });
    }

    private void setupSearchInput() {
        binding.editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (applyingLang) return;
                search(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        binding.editSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_SEARCH) return false;
            viewModel.saveSearch(currentQuery(), lang);
            hideKeyboard();
            return true;
        });

        binding.btnClearSearch.setOnClickListener(v -> binding.editSearch.setText(""));
        binding.btnVoiceSearch.setOnClickListener(v -> startVoiceSearch());
        binding.btnTryContains.setOnClickListener(v -> binding.chipContains.setChecked(true));

        binding.btnSwitchHint.setOnClickListener(v -> moveQueryToOtherDirection());
        binding.btnEmptySwitch.setOnClickListener(v -> moveQueryToOtherDirection());
    }

    /** Searches the current text in the other direction (e.g. Persian typed while in English mode). */
    private void moveQueryToOtherDirection() {
        String query = currentQuery();
        String target = isEnglish() ? LANG_FA : LANG_EN;
        if (LANG_EN.equals(target)) queryEn = query;
        else queryFa = query;
        // Empty this direction's box so switching back doesn't repeat the mistake;
        // switching then restores the moved query in the other direction
        binding.editSearch.setText("");
        viewModel.setDictionaryLang(target);
    }

    private void setupFilterChips() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chip_starts_with) {
                viewModel.setSearchFilter(SearchFilter.STARTS_WITH);
            } else if (id == R.id.chip_contains) {
                viewModel.setSearchFilter(SearchFilter.CONTAINS);
            } else if (id == R.id.chip_exact) {
                viewModel.setSearchFilter(SearchFilter.EXACT);
            }
        });
    }

    // ── Direction ─────────────────────────────────────────────────────────────

    /** Re-labels the whole screen for a direction and restores the query typed in it. */
    private void applyLang(String newLang) {
        boolean changed = !newLang.equals(lang);
        if (changed) rememberCurrentQuery();
        lang = newLang;
        boolean english = isEnglish();

        applyingLang = true;
        binding.directionSwitch.labelFrom.setText(english ? R.string.flashcard_lang_english : R.string.flashcard_lang_persian);
        binding.directionSwitch.labelTo.setText(english ? R.string.flashcard_lang_persian : R.string.flashcard_lang_english);
        binding.directionSwitch.getRoot().setContentDescription(
                getString(english ? R.string.title_english : R.string.title_persian));
        binding.editSearch.setHint(english ? R.string.search_english_hint : R.string.search_persian_hint);
        binding.editSearch.setTextDirection(english ? View.TEXT_DIRECTION_LTR : View.TEXT_DIRECTION_RTL);
        binding.txtPromptTitle.setText(english ? R.string.empty_prompt_title_en : R.string.empty_prompt_title_fa);
        binding.recyclerPhrase.setAdapter(english ? englishAdapter : persianAdapter);

        String restored = english ? queryEn : queryFa;
        binding.editSearch.setText(restored);
        binding.editSearch.setSelection(restored.length());
        applyingLang = false;

        search(restored);
        viewModel.loadRecentSearches(lang);
    }

    private void rememberCurrentQuery() {
        if (binding == null) return;
        if (isEnglish()) queryEn = currentQuery();
        else queryFa = currentQuery();
    }

    // ── Search ────────────────────────────────────────────────────────────────

    private String currentQuery() {
        return binding.editSearch.getText() != null ? binding.editSearch.getText().toString().trim() : "";
    }

    private void setQuery(String query) {
        binding.editSearch.setText(query);
        binding.editSearch.setSelection(query.length());
    }

    private void search(String query) {
        binding.btnClearSearch.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
        clearSearchOnBack.setEnabled(!query.isEmpty());
        updateScriptHint(query);
        if (isEnglish()) viewModel.searchEnglish(query);
        else viewModel.searchPersian(query);
    }

    /** Offers to switch direction when the text is clearly in the other language's script. */
    private void updateScriptHint(String query) {
        boolean persian = PersianText.containsPersian(query);
        boolean latin = query.matches(".*[A-Za-z].*");
        boolean mismatch = isEnglish() ? persian && !latin : latin && !persian;
        binding.cardSwitchHint.setVisibility(mismatch ? View.VISIBLE : View.GONE);
        if (mismatch) {
            binding.txtSwitchHint.setText(isEnglish() ? R.string.script_hint_persian : R.string.script_hint_english);
        }
    }


    private void startVoiceSearch() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, isEnglish() ? "en-US" : "fa-IR");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(isEnglish() ? R.string.search_english_hint : R.string.search_persian_hint));
        try {
            speechRecognizerLauncher.launch(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Observers ─────────────────────────────────────────────────────────────

    private void observeViewModel() {
        viewModel.getDictionaryLang().observe(getViewLifecycleOwner(), this::applyLang);

        viewModel.getSearchFilter().observe(getViewLifecycleOwner(), filter -> {
            int id = filter == SearchFilter.CONTAINS ? R.id.chip_contains
                    : filter == SearchFilter.EXACT ? R.id.chip_exact
                    : R.id.chip_starts_with;
            if (binding.chipGroupFilter.getCheckedChipId() != id) binding.chipGroupFilter.check(id);
        });

        viewModel.getDictionaryRequest().observe(getViewLifecycleOwner(), request -> {
            if (request == null) return;
            viewModel.consumeDictionaryRequest();
            viewModel.setDictionaryLang(request.lang);
            if (request.query != null) setQuery(request.query);
            if (request.focusSearch) showKeyboard();
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading ->
                binding.progressLoading.setVisibility(Boolean.TRUE.equals(isLoading) ? View.VISIBLE : View.INVISIBLE));

        viewModel.getEnglishSearchResults().observe(getViewLifecycleOwner(), results -> {
            if (!isEnglish()) return;
            englishAdapter.submitList(results);
            renderResults(results == null ? 0 : results.size());
        });

        viewModel.getPersianSearchResults().observe(getViewLifecycleOwner(), results -> {
            if (isEnglish()) return;
            persianAdapter.submitList(results);
            renderResults(results == null ? 0 : results.size());
        });

        viewModel.getRecentSearches().observe(getViewLifecycleOwner(), this::renderRecentSearches);
    }

    private void renderResults(int count) {
        String query = currentQuery();
        boolean searching = !query.isEmpty();
        boolean empty = searching && count == 0;

        binding.layoutInitial.setVisibility(searching ? View.GONE : View.VISIBLE);
        binding.layoutResults.setVisibility(searching && !empty ? View.VISIBLE : View.GONE);
        binding.layoutEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);

        if (empty) {
            binding.txtEmptyMessage.setText(getString(R.string.no_results_found, query));
            binding.btnTryContains.setVisibility(binding.chipContains.isChecked() ? View.GONE : View.VISIBLE);
            loadSuggestions(query);
        } else if (searching) {
            binding.txtResultsCount.setText(getResources().getQuantityString(R.plurals.results_count, count, count));
        }
    }

    /** Shows "did you mean…?" chips; ignores the answer if the user has typed on since. */
    private void loadSuggestions(String query) {
        binding.layoutSuggestions.setVisibility(View.GONE);
        String requestLang = lang;
        viewModel.suggestSpellings(query, requestLang, suggestions -> {
            if (binding == null || !query.equals(currentQuery()) || !requestLang.equals(lang)) return;
            binding.chipsSuggestions.removeAllViews();
            for (String suggestion : suggestions) {
                Chip chip = new Chip(requireContext());
                chip.setText(suggestion);
                chip.setTextDirection(isEnglish() ? View.TEXT_DIRECTION_LTR : View.TEXT_DIRECTION_RTL);
                chip.setOnClickListener(v -> setQuery(suggestion));
                binding.chipsSuggestions.addView(chip);
            }
            binding.layoutSuggestions.setVisibility(suggestions.isEmpty() ? View.GONE : View.VISIBLE);
        });
    }

    private void renderRecentSearches(List<SearchHistoryItem> items) {
        binding.listRecent.removeAllViews();
        List<SearchHistoryItem> forLang = new ArrayList<>();
        if (items != null) {
            for (SearchHistoryItem item : items) {
                if (lang.equals(item.getLang())) forLang.add(item);
            }
        }
        boolean hasItems = !forLang.isEmpty();
        binding.layoutRecentSearchesHeader.setVisibility(hasItems ? View.VISIBLE : View.GONE);
        binding.cardRecent.setVisibility(hasItems ? View.VISIBLE : View.GONE);
        binding.layoutPrompt.setVisibility(hasItems ? View.GONE : View.VISIBLE);

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (SearchHistoryItem item : forLang) {
            ItemRecentSearchBinding row = ItemRecentSearchBinding.inflate(inflater, binding.listRecent, false);
            row.textQuery.setText(item.getQuery());
            row.textQuery.setTextDirection(isEnglish() ? View.TEXT_DIRECTION_LTR : View.TEXT_DIRECTION_RTL);
            row.textLang.setText(isEnglish() ? R.string.lang_badge_en : R.string.lang_badge_fa);
            row.imgOpen.setVisibility(View.GONE);
            row.btnRemove.setVisibility(View.VISIBLE);
            row.btnRemove.setOnClickListener(v -> viewModel.deleteSearchHistoryItem(item.getId(), item.getLang()));
            row.getRoot().setOnClickListener(v -> setQuery(item.getQuery()));
            binding.listRecent.addView(row.getRoot());
        }
    }

    // ── Word details ──────────────────────────────────────────────────────────

    private void openWordDetails(PhraseEnglish phrase) {
        viewModel.saveSearch(currentQuery(), LANG_EN);
        WordDetailBottomSheet sheet = WordDetailBottomSheet.newInstance(phrase);
        sheet.setOnBookmarkToggleListener((id, isEnglish, newState) ->
                viewModel.setFavoriteEnglish(phrase, newState, () -> englishAdapter.notifyDataSetChanged()));
        sheet.setOnReverseLookupListener((query, fromEnglish) -> reverseLookup(LANG_FA, query));
        sheet.show(getChildFragmentManager(), "WORD_DETAIL");
    }

    private void openWordDetails(PhrasePersian phrase) {
        viewModel.saveSearch(currentQuery(), LANG_FA);
        WordDetailBottomSheet sheet = WordDetailBottomSheet.newInstance(phrase);
        sheet.setOnBookmarkToggleListener((id, isEnglish, newState) ->
                viewModel.setFavoritePersian(phrase, newState, () -> persianAdapter.notifyDataSetChanged()));
        sheet.setOnReverseLookupListener((query, fromEnglish) -> reverseLookup(LANG_EN, query));
        sheet.show(getChildFragmentManager(), "WORD_DETAIL");
    }

    /** Switches direction and looks up a translation from the detail sheet. */
    private void reverseLookup(String targetLang, String query) {
        if (LANG_EN.equals(targetLang)) queryEn = query;
        else queryFa = query;
        if (targetLang.equals(lang)) {
            setQuery(query);
        } else {
            viewModel.setDictionaryLang(targetLang);
        }
    }

    // ── Keyboard ──────────────────────────────────────────────────────────────

    private void showKeyboard() {
        binding.editSearch.requestFocus();
        binding.editSearch.post(() -> {
            InputMethodManager imm = ContextCompat.getSystemService(requireContext(), InputMethodManager.class);
            if (imm != null) imm.showSoftInput(binding.editSearch, InputMethodManager.SHOW_IMPLICIT);
        });
    }

    private void hideKeyboard() {
        InputMethodManager imm = ContextCompat.getSystemService(requireContext(), InputMethodManager.class);
        if (imm != null) imm.hideSoftInputFromWindow(binding.editSearch.getWindowToken(), 0);
    }

    @Override
    public void onDestroyView() {
        rememberCurrentQuery();
        super.onDestroyView();
        binding = null;
    }
}
