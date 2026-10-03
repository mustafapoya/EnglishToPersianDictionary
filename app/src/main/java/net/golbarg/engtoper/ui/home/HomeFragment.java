package net.golbarg.engtoper.ui.home;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.color.MaterialColors;

import net.golbarg.engtoper.MainActivity;
import net.golbarg.engtoper.R;
import net.golbarg.engtoper.databinding.FragmentHomeBinding;
import net.golbarg.engtoper.databinding.ItemRecentSearchBinding;
import net.golbarg.engtoper.databinding.ViewToolBinding;
import net.golbarg.engtoper.db.StudyRepository;
import net.golbarg.engtoper.models.PhraseEnglish;
import net.golbarg.engtoper.models.SearchHistoryItem;
import net.golbarg.engtoper.ui.DictionaryViewModel;
import net.golbarg.engtoper.ui.QuizBottomSheetDialog;
import net.golbarg.engtoper.ui.WordDetailBottomSheet;
import net.golbarg.engtoper.ui.common.MeaningList;
import net.golbarg.engtoper.ui.common.PhraseItemBinder;
import net.golbarg.engtoper.util.AppPreferences;
import net.golbarg.engtoper.util.LocaleUtil;
import net.golbarg.engtoper.util.TTSManager;
import net.golbarg.engtoper.util.TranslationParser;
import net.golbarg.engtoper.util.UtilController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Set;

import static net.golbarg.engtoper.ui.DictionaryViewModel.LANG_EN;
import static net.golbarg.engtoper.ui.DictionaryViewModel.LANG_FA;

/** Home: search entry, daily learning goal, quick tools, word of the day, words to explore, recent searches. */
public class HomeFragment extends Fragment {

    private static final int WOD_MAX_MEANINGS = 3;

    private FragmentHomeBinding binding;
    private DictionaryViewModel dictionaryViewModel;
    private HomeViewModel homeViewModel;
    private ExploreWordAdapter exploreAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dictionaryViewModel = new ViewModelProvider(requireActivity()).get(DictionaryViewModel.class);
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        setupHero();
        setupTools();
        setupExplore();
        binding.btnClearRecent.setOnClickListener(v -> homeViewModel.clearRecentSearches());
        observeViewModels();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Progress changes on other tabs (flashcards, bookmarks), so refresh whenever we come back
        homeViewModel.refresh();
    }

    // ── Hero ──────────────────────────────────────────────────────────────────

    private void setupHero() {
        binding.textDate.setText(LocaleUtil.date(requireContext(), LocalDate.now(), getString(R.string.home_date_pattern)));
        binding.textGreeting.setText(greetingRes());
        binding.searchBar.setOnClickListener(v -> openDictionary(dictionaryViewModel.currentLang(), null, true));
        binding.btnDirection.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
            dictionaryViewModel.setDictionaryLang(LANG_EN.equals(dictionaryViewModel.currentLang()) ? LANG_FA : LANG_EN);
        });
    }

    private void renderDirection(String lang) {
        boolean english = LANG_EN.equals(lang);
        binding.btnDirection.setText(english ? R.string.home_direction_en : R.string.home_direction_fa);
        binding.textSearchHint.setText(english ? R.string.search_english_hint : R.string.search_persian_hint);
        binding.textSearchHint.setTextDirection(english ? View.TEXT_DIRECTION_LTR : View.TEXT_DIRECTION_RTL);
    }

    @StringRes
    private int greetingRes() {
        int hour = LocalTime.now().getHour();
        if (hour >= 5 && hour < 12) return R.string.greeting_morning;
        if (hour >= 12 && hour < 18) return R.string.greeting_afternoon;
        return R.string.greeting_evening;
    }

    // ── Daily goal ────────────────────────────────────────────────────────────

    private void renderStats(@Nullable StudyRepository.Stats stats) {
        if (stats == null) return;
        int goal = AppPreferences.getDailyGoal(requireContext());
        int reviewed = stats.reviewedToday;

        binding.textStreakBadge.setText(getResources().getQuantityString(R.plurals.streak_days, stats.streak, stats.streak));
        binding.ringGoal.setMax(goal);
        binding.ringGoal.setProgressCompat(Math.min(reviewed, goal), true);
        binding.textGoalCount.setText(getString(R.string.goal_progress, Math.min(reviewed, goal), goal));
        binding.textMiniStats.setText(getString(R.string.home_mini_stats, stats.saved, stats.learned));

        if (reviewed == 0) {
            binding.textGoalMessage.setText(getString(R.string.goal_start, goal));
        } else if (reviewed < goal) {
            binding.textGoalMessage.setText(getResources().getQuantityString(R.plurals.goal_remaining, goal - reviewed, goal - reviewed));
        } else {
            binding.textGoalMessage.setText(R.string.goal_done);
        }

        StudyRepository.Deck deck;
        if (stats.due > 0) {
            binding.btnGoalAction.setText(getResources().getQuantityString(R.plurals.goal_review_due, stats.due, stats.due));
            deck = StudyRepository.Deck.DUE;
        } else {
            binding.btnGoalAction.setText(reviewed >= goal ? R.string.goal_keep_going : R.string.goal_start_learning);
            deck = StudyRepository.Deck.RANDOM;
        }
        binding.btnGoalAction.setOnClickListener(v -> {
            dictionaryViewModel.requestFlashcardDeck(deck);
            openTab(R.id.navigation_flashcards);
        });
    }

    /** One column per day for the last 7 days: a check when studied, today outlined. */
    private void renderWeek(@Nullable Set<LocalDate> studied) {
        LinearLayout strip = binding.weekStrip;
        strip.removeAllViews();
        LocalDate today = LocalDate.now();
        float density = getResources().getDisplayMetrics().density;
        int circleSize = Math.round(32 * density);

        for (int offset = 6; offset >= 0; offset--) {
            LocalDate day = today.minusDays(offset);
            boolean done = studied != null && studied.contains(day);
            boolean isToday = offset == 0;

            LinearLayout column = new LinearLayout(requireContext());
            column.setOrientation(LinearLayout.VERTICAL);
            column.setGravity(Gravity.CENTER_HORIZONTAL);

            TextView label = new TextView(requireContext());
            label.setText(day.getDayOfWeek().getDisplayName(TextStyle.NARROW, LocaleUtil.current(requireContext())));
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            label.setTypeface(null, isToday ? Typeface.BOLD : Typeface.NORMAL);
            label.setTextColor(color(isToday ? androidx.appcompat.R.attr.colorPrimary : R.attr.appTextTertiary));
            column.addView(label);

            FrameLayout circle = new FrameLayout(requireContext());
            LinearLayout.LayoutParams circleParams = new LinearLayout.LayoutParams(circleSize, circleSize);
            circleParams.topMargin = Math.round(6 * density);
            if (done) {
                circle.setBackgroundResource(R.drawable.bg_circle);
                circle.setBackgroundTintList(ColorStateList.valueOf(color(androidx.appcompat.R.attr.colorPrimary)));
                ImageView check = new ImageView(requireContext());
                check.setImageResource(R.drawable.ic_check);
                check.setImageTintList(ColorStateList.valueOf(color(com.google.android.material.R.attr.colorOnPrimary)));
                int iconSize = Math.round(18 * density);
                circle.addView(check, new FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER));
            } else {
                circle.setBackgroundResource(isToday ? R.drawable.bg_circle_outline : R.drawable.bg_circle);
                circle.setBackgroundTintList(ColorStateList.valueOf(
                        color(isToday ? androidx.appcompat.R.attr.colorPrimary : R.attr.appSurfaceVariant)));
                TextView number = new TextView(requireContext());
                number.setText(LocaleUtil.number(requireContext(), day.getDayOfMonth()));
                number.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                number.setTextColor(color(isToday ? androidx.appcompat.R.attr.colorPrimary : R.attr.appTextTertiary));
                circle.addView(number, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
            }
            column.addView(circle, circleParams);

            strip.addView(column, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
    }

    // ── Quick tools ───────────────────────────────────────────────────────────

    private void setupTools() {
        bindTool(binding.toolEnglish, R.drawable.ic_translate, R.string.tool_english,
                com.google.android.material.R.attr.colorPrimaryContainer,
                com.google.android.material.R.attr.colorOnPrimaryContainer,
                v -> openDictionary(LANG_EN, null, true));
        bindTool(binding.toolPersian, R.drawable.ic_swap_horiz, R.string.tool_persian,
                com.google.android.material.R.attr.colorSecondaryContainer,
                com.google.android.material.R.attr.colorOnSecondaryContainer,
                v -> openDictionary(LANG_FA, null, true));
        bindTool(binding.toolQuiz, R.drawable.ic_quiz, R.string.tool_quiz,
                com.google.android.material.R.attr.colorTertiaryContainer,
                com.google.android.material.R.attr.colorOnTertiaryContainer,
                v -> QuizBottomSheetDialog.newInstance().show(getChildFragmentManager(), "QUIZ_DIALOG"));
        bindTool(binding.toolRandom, R.drawable.ic_shuffle, R.string.tool_random,
                R.attr.appSurfaceVariant, R.attr.appTextPrimary,
                v -> openRandomWord());
    }

    private void bindTool(ViewToolBinding tool, @DrawableRes int icon, @StringRes int label,
                          @AttrRes int background, @AttrRes int tint, View.OnClickListener onClick) {
        tool.icon.setImageResource(icon);
        tool.icon.setImageTintList(ColorStateList.valueOf(color(tint)));
        tool.iconBg.setBackgroundTintList(ColorStateList.valueOf(color(background)));
        tool.label.setText(label);
        tool.getRoot().setOnClickListener(onClick);
    }

    // ── Word of the day ───────────────────────────────────────────────────────

    private void renderWordOfTheDay(@Nullable PhraseEnglish word) {
        if (word == null) return;
        TranslationParser.Parsed parsed = TranslationParser.parse(word.getToLanguage());
        binding.textWodWord.setText(word.getFromLanguage());
        binding.textWodSummary.setText(getString(R.string.wod_summary,
                getString(R.string.flashcard_lang_english), PhraseItemBinder.summary(requireContext(), parsed)));
        renderWodMeanings(parsed);
        renderWodBookmark(word);

        binding.btnWodShare.setOnClickListener(v ->
                UtilController.shareWord(requireContext(), word.getFromLanguage(), parsed.toPlainText("، ")));

        binding.btnWodListen.setOnClickListener(v ->
                TTSManager.getInstance(requireContext()).speak(word.getFromLanguage(), requireContext()));
        binding.btnWodBookmark.setOnClickListener(v ->
                dictionaryViewModel.setFavoriteEnglish(word, word.getFavorite() == 1 ? 0 : 1, () -> {
                    if (binding != null) renderWodBookmark(word);
                }));
        binding.btnWodOpen.setOnClickListener(v -> openWordDetails(word));
        binding.cardWordOfDay.setOnClickListener(v -> openWordDetails(word));
    }

    private void renderWodMeanings(TranslationParser.Parsed parsed) {
        int hidden = MeaningList.render(binding.listWodMeanings, parsed, WOD_MAX_MEANINGS, true);
        binding.textWodMore.setText(hidden > 0
                ? getResources().getQuantityString(R.plurals.wod_more_meanings, hidden, LocaleUtil.number(requireContext(), hidden))
                : "");
    }

    private void renderWodBookmark(PhraseEnglish word) {
        boolean saved = word.getFavorite() == 1;
        binding.btnWodBookmark.setIconResource(saved ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
        binding.btnWodBookmark.setIconTint(saved
                ? ContextCompat.getColorStateList(requireContext(), R.color.bookmark_gold)
                : ColorStateList.valueOf(color(R.attr.appTextTertiary)));
    }

    // ── Explore & recent ──────────────────────────────────────────────────────

    private void setupExplore() {
        exploreAdapter = new ExploreWordAdapter(this::openWordDetails);
        binding.recyclerExplore.setAdapter(exploreAdapter);
        binding.btnShuffle.setOnClickListener(v -> homeViewModel.shuffleExplore());
    }

    private void renderRecentSearches(@Nullable List<SearchHistoryItem> items) {
        binding.listRecent.removeAllViews();
        boolean hasItems = items != null && !items.isEmpty();
        binding.sectionRecent.setVisibility(hasItems ? View.VISIBLE : View.GONE);
        if (!hasItems) return;

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (SearchHistoryItem item : items) {
            ItemRecentSearchBinding row = ItemRecentSearchBinding.inflate(inflater, binding.listRecent, false);
            boolean english = LANG_EN.equals(item.getLang());
            row.textQuery.setText(item.getQuery());
            row.textQuery.setTextDirection(english ? View.TEXT_DIRECTION_LTR : View.TEXT_DIRECTION_RTL);
            row.textLang.setText(english ? R.string.lang_badge_en : R.string.lang_badge_fa);
            row.getRoot().setOnClickListener(v -> openDictionary(item.getLang(), item.getQuery(), false));
            binding.listRecent.addView(row.getRoot());
        }
    }

    // ── Observers ─────────────────────────────────────────────────────────────

    private void observeViewModels() {
        dictionaryViewModel.getDictionaryLang().observe(getViewLifecycleOwner(), this::renderDirection);
        dictionaryViewModel.getWordOfTheDay().observe(getViewLifecycleOwner(), this::renderWordOfTheDay);
        dictionaryViewModel.getBookmarksVersion().observe(getViewLifecycleOwner(), v -> homeViewModel.refreshStats());
        homeViewModel.getStats().observe(getViewLifecycleOwner(), this::renderStats);
        homeViewModel.getStudyDays().observe(getViewLifecycleOwner(), this::renderWeek);
        homeViewModel.getRecentSearches().observe(getViewLifecycleOwner(), this::renderRecentSearches);
        homeViewModel.getExploreWords().observe(getViewLifecycleOwner(), exploreAdapter::submitList);
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private void openDictionary(String lang, @Nullable String query, boolean focusSearch) {
        dictionaryViewModel.requestDictionary(lang, query, focusSearch);
        openTab(R.id.navigation_dictionary);
    }

    private void openTab(int destinationId) {
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).openTab(destinationId);
        }
    }

    private void openRandomWord() {
        dictionaryViewModel.getRandomWord(word -> {
            if (word != null && isAdded()) openWordDetails(word);
        });
    }

    private void openWordDetails(PhraseEnglish word) {
        WordDetailBottomSheet sheet = WordDetailBottomSheet.newInstance(word);
        sheet.setOnBookmarkToggleListener((id, isEnglish, newState) ->
                dictionaryViewModel.setFavoriteEnglish(word, newState, () -> {
                    PhraseEnglish wod = dictionaryViewModel.getWordOfTheDay().getValue();
                    if (binding == null || wod == null || wod.getId() != word.getId()) return;
                    wod.setFavorite(newState);
                    renderWodBookmark(wod);
                }));
        sheet.setOnReverseLookupListener((query, fromEnglish) -> openDictionary(LANG_FA, query, false));
        sheet.show(getChildFragmentManager(), "WORD_DETAIL");
    }

    private int color(@AttrRes int attr) {
        return MaterialColors.getColor(binding.getRoot(), attr);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
