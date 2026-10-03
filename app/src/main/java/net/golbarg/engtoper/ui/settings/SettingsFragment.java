package net.golbarg.engtoper.ui.settings;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.AttrRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import net.golbarg.engtoper.R;
import net.golbarg.engtoper.databinding.FragmentSettingsBinding;
import net.golbarg.engtoper.databinding.ViewSettingRowBinding;
import net.golbarg.engtoper.db.DictionaryRepository;
import net.golbarg.engtoper.db.StudyRepository;
import net.golbarg.engtoper.models.SearchFilter;
import net.golbarg.engtoper.ui.DictionaryViewModel;
import net.golbarg.engtoper.ui.intro.IntroActivity;
import net.golbarg.engtoper.util.AppLinks;
import net.golbarg.engtoper.util.AppPreferences;
import net.golbarg.engtoper.util.TTSManager;

import static net.golbarg.engtoper.ui.DictionaryViewModel.LANG_EN;
import static net.golbarg.engtoper.ui.DictionaryViewModel.LANG_FA;

/** App settings, grouped by topic, plus support links and the way into About. */
public class SettingsFragment extends Fragment {

    private static final int[] THEME_MODES = {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            AppCompatDelegate.MODE_NIGHT_NO,
            AppCompatDelegate.MODE_NIGHT_YES
    };
    private static final int[] THEME_LABELS = {R.string.theme_system, R.string.theme_light, R.string.theme_dark};

    private static final SearchFilter[] FILTERS = {SearchFilter.STARTS_WITH, SearchFilter.CONTAINS, SearchFilter.EXACT};
    private static final int[] FILTER_LABELS = {R.string.filter_starts_with, R.string.filter_contains, R.string.filter_exact};

    private static final float[] SPEECH_RATES = {
            AppPreferences.SPEECH_RATE_SLOW, AppPreferences.SPEECH_RATE_NORMAL, AppPreferences.SPEECH_RATE_FAST
    };
    private static final int[] SPEECH_LABELS = {R.string.speech_slow, R.string.speech_normal, R.string.speech_fast};

    private static final int[] DAILY_GOALS = {5, 10, 20, 30};

    /** Index 0 is "follow the system". */
    private static final String[] LANGUAGE_TAGS = {"", "en", "fa"};

    private FragmentSettingsBinding binding;
    private DictionaryViewModel dictionaryViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dictionaryViewModel = new ViewModelProvider(requireActivity()).get(DictionaryViewModel.class);

        setupRows();
        binding.textFooter.setText(getString(R.string.settings_footer,
                getString(R.string.app_name), AppLinks.versionName(requireContext())));
    }

    @Override
    public void onResume() {
        super.onResume();
        // Values can change elsewhere (e.g. the direction toggle in the dictionary)
        renderValues();
    }

    // ── Rows ──────────────────────────────────────────────────────────────────

    private void setupRows() {
        int primaryBg = com.google.android.material.R.attr.colorPrimaryContainer;
        int primaryFg = com.google.android.material.R.attr.colorOnPrimaryContainer;
        int secondaryBg = com.google.android.material.R.attr.colorSecondaryContainer;
        int secondaryFg = com.google.android.material.R.attr.colorOnSecondaryContainer;
        int tertiaryBg = com.google.android.material.R.attr.colorTertiaryContainer;
        int tertiaryFg = com.google.android.material.R.attr.colorOnTertiaryContainer;
        int neutralBg = R.attr.appSurfaceVariant;
        int neutralFg = R.attr.appTextPrimary;
        int dangerBg = com.google.android.material.R.attr.colorErrorContainer;
        int dangerFg = com.google.android.material.R.attr.colorOnErrorContainer;

        bindRow(binding.rowTheme, R.drawable.ic_palette, tertiaryBg, tertiaryFg, R.string.setting_theme, v -> chooseTheme());
        bindRow(binding.rowLanguage, R.drawable.ic_translate, tertiaryBg, tertiaryFg, R.string.setting_language, v -> chooseLanguage());

        bindRow(binding.rowDirection, R.drawable.ic_swap_horiz, primaryBg, primaryFg, R.string.setting_direction, v -> chooseDirection());
        bindRow(binding.rowFilter, R.drawable.ic_filter, primaryBg, primaryFg, R.string.setting_filter, v -> chooseFilter());

        bindRow(binding.rowSpeechRate, R.drawable.ic_speed, secondaryBg, secondaryFg, R.string.setting_speech_rate, v -> chooseSpeechRate());
        bindRow(binding.rowAccent, R.drawable.ic_language, secondaryBg, secondaryFg, R.string.setting_accent, v -> chooseAccent());

        bindRow(binding.rowDailyGoal, R.drawable.ic_target, neutralBg, neutralFg, R.string.setting_daily_goal, v -> chooseDailyGoal());
        bindRow(binding.rowMeaningFirst, R.drawable.ic_flashcard_outline, neutralBg, neutralFg, R.string.setting_meaning_first, v -> {
            AppPreferences.setFlashcardReverse(requireContext(), !AppPreferences.isFlashcardReverse(requireContext()));
            renderValues();
        });
        binding.rowMeaningFirst.toggle.setVisibility(View.VISIBLE);
        binding.rowMeaningFirst.chevron.setVisibility(View.GONE);

        bindRow(binding.rowClearHistory, R.drawable.ic_history, dangerBg, dangerFg, R.string.setting_clear_history, v -> confirmClearHistory());
        binding.rowClearHistory.value.setText(R.string.setting_clear_history_sub);
        bindRow(binding.rowResetProgress, R.drawable.ic_delete, dangerBg, dangerFg, R.string.setting_reset_progress, v -> confirmResetProgress());
        binding.rowResetProgress.value.setText(R.string.setting_reset_progress_sub);

        bindRow(binding.rowIntro, R.drawable.ic_sparkle, primaryBg, primaryFg, R.string.setting_intro, v ->
                startActivity(new Intent(requireContext(), IntroActivity.class).putExtra(IntroActivity.EXTRA_REPLAY, true)));
        binding.rowIntro.value.setText(R.string.setting_intro_sub);
        bindRow(binding.rowRate, R.drawable.ic_star_outline, primaryBg, primaryFg, R.string.setting_rate, v -> AppLinks.openPlayStore(requireContext()));
        binding.rowRate.value.setText(R.string.setting_rate_sub);
        bindRow(binding.rowShare, R.drawable.ic_share, primaryBg, primaryFg, R.string.setting_share, v -> AppLinks.shareApp(requireContext()));
        binding.rowShare.value.setText(R.string.setting_share_sub);
        bindRow(binding.rowFeedback, R.drawable.ic_email, primaryBg, primaryFg, R.string.setting_feedback, v -> AppLinks.sendFeedback(requireContext()));
        binding.rowFeedback.value.setText(R.string.contact_email);
        bindRow(binding.rowAbout, R.drawable.ic_info_outline, primaryBg, primaryFg, R.string.setting_about, v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_settings_to_about));
        binding.rowAbout.value.setText(R.string.setting_about_sub);
    }

    private void bindRow(ViewSettingRowBinding row, @DrawableRes int icon, @AttrRes int background,
                         @AttrRes int tint, @StringRes int title, View.OnClickListener onClick) {
        row.icon.setImageResource(icon);
        row.icon.setImageTintList(ColorStateList.valueOf(color(tint)));
        row.iconBg.setBackgroundTintList(ColorStateList.valueOf(color(background)));
        row.title.setText(title);
        row.getRoot().setOnClickListener(onClick);
    }

    /** Shows the current value under each configurable row. */
    private void renderValues() {
        if (binding == null) return;
        android.content.Context context = requireContext();

        binding.rowTheme.value.setText(THEME_LABELS[indexOf(THEME_MODES, AppPreferences.getThemeMode(context))]);
        binding.rowLanguage.value.setText(languageLabels()[currentLanguageIndex()]);
        binding.rowDirection.value.setText(LANG_EN.equals(dictionaryViewModel.currentLang())
                ? R.string.direction_en_fa : R.string.direction_fa_en);
        binding.rowFilter.value.setText(FILTER_LABELS[filterIndex(AppPreferences.getSearchFilter(context))]);
        binding.rowSpeechRate.value.setText(SPEECH_LABELS[speechIndex(AppPreferences.getSpeechRate(context))]);
        binding.rowAccent.value.setText(AppPreferences.isBritishAccent(context) ? R.string.accent_uk : R.string.accent_us);

        int goal = AppPreferences.getDailyGoal(context);
        binding.rowDailyGoal.value.setText(getResources().getQuantityString(R.plurals.daily_goal_value, goal, goal));

        boolean meaningFirst = AppPreferences.isFlashcardReverse(context);
        binding.rowMeaningFirst.toggle.setChecked(meaningFirst);
        binding.rowMeaningFirst.value.setText(meaningFirst ? R.string.meaning_first_on : R.string.meaning_first_off);
    }

    // ── Choices ───────────────────────────────────────────────────────────────

    private void chooseTheme() {
        int current = indexOf(THEME_MODES, AppPreferences.getThemeMode(requireContext()));
        showChoices(R.string.setting_theme, labels(THEME_LABELS), current, which -> {
            AppPreferences.setThemeMode(requireContext(), THEME_MODES[which]);
            // Recreates the activity in the new theme; this tab and its state are restored
            AppCompatDelegate.setDefaultNightMode(THEME_MODES[which]);
            renderValues();
        });
    }

    /** Each option is written in its own language so it is recognisable whatever the current one is. */
    private String[] languageLabels() {
        return new String[]{getString(R.string.language_system), getString(R.string.language_english), getString(R.string.language_persian)};
    }

    private int currentLanguageIndex() {
        LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
        if (locales.isEmpty() || locales.get(0) == null) return 0;
        return LANGUAGE_TAGS[2].equals(locales.get(0).getLanguage()) ? 2 : 1;
    }

    private void chooseLanguage() {
        showChoices(R.string.setting_language, languageLabels(), currentLanguageIndex(), which -> {
            // AppCompat stores the choice and recreates the activities in the new language (and direction)
            AppCompatDelegate.setApplicationLocales(which == 0
                    ? LocaleListCompat.getEmptyLocaleList()
                    : LocaleListCompat.forLanguageTags(LANGUAGE_TAGS[which]));
        });
    }

    private void chooseDirection() {
        int current = LANG_EN.equals(dictionaryViewModel.currentLang()) ? 0 : 1;
        String[] options = {getString(R.string.direction_en_fa), getString(R.string.direction_fa_en)};
        showChoices(R.string.setting_direction, options, current, which -> {
            dictionaryViewModel.setDictionaryLang(which == 0 ? LANG_EN : LANG_FA);
            renderValues();
        });
    }

    private void chooseFilter() {
        int current = filterIndex(AppPreferences.getSearchFilter(requireContext()));
        showChoices(R.string.setting_filter, labels(FILTER_LABELS), current, which -> {
            dictionaryViewModel.setSearchFilter(FILTERS[which]);
            renderValues();
        });
    }

    private void chooseSpeechRate() {
        int current = speechIndex(AppPreferences.getSpeechRate(requireContext()));
        showChoices(R.string.setting_speech_rate, labels(SPEECH_LABELS), current, which -> {
            AppPreferences.setSpeechRate(requireContext(), SPEECH_RATES[which]);
            previewPronunciation();
            renderValues();
        });
    }

    private void chooseAccent() {
        int current = AppPreferences.isBritishAccent(requireContext()) ? 1 : 0;
        String[] options = {getString(R.string.accent_us), getString(R.string.accent_uk)};
        showChoices(R.string.setting_accent, options, current, which -> {
            AppPreferences.setBritishAccent(requireContext(), which == 1);
            previewPronunciation();
            renderValues();
        });
    }

    private void chooseDailyGoal() {
        int goal = AppPreferences.getDailyGoal(requireContext());
        String[] options = new String[DAILY_GOALS.length];
        int current = 1;
        for (int i = 0; i < DAILY_GOALS.length; i++) {
            options[i] = getResources().getQuantityString(R.plurals.daily_goal_value, DAILY_GOALS[i], DAILY_GOALS[i]);
            if (DAILY_GOALS[i] == goal) current = i;
        }
        showChoices(R.string.setting_daily_goal, options, current, which -> {
            AppPreferences.setDailyGoal(requireContext(), DAILY_GOALS[which]);
            renderValues();
        });
    }

    /** Lets the user hear the new speed / accent right away. */
    private void previewPronunciation() {
        TTSManager tts = TTSManager.getInstance(requireContext());
        tts.applySettings();
        tts.speak(getString(R.string.pronunciation_sample), requireContext());
    }

    private interface OnChoice {
        void onChoice(int which);
    }

    private void showChoices(@StringRes int title, String[] options, int checked, OnChoice onChoice) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setSingleChoiceItems(options, checked, (dialog, which) -> {
                    dialog.dismiss();
                    if (which != checked) onChoice.onChoice(which);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    private void confirmClearHistory() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.setting_clear_history)
                .setMessage(R.string.confirm_clear_history)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.action_clear, (d, w) ->
                        DictionaryRepository.getInstance(requireContext()).clearAllSearchHistory(() -> {
                            dictionaryViewModel.loadRecentSearches(dictionaryViewModel.currentLang());
                            showMessage(R.string.history_cleared);
                        }))
                .show();
    }

    private void confirmResetProgress() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.setting_reset_progress)
                .setMessage(R.string.confirm_reset_progress)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.action_reset, (d, w) ->
                        StudyRepository.getInstance(requireContext()).resetProgress(() -> showMessage(R.string.progress_reset)))
                .show();
    }

    private void showMessage(@StringRes int message) {
        if (binding == null) return;
        Snackbar snackbar = Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT);
        View navBar = requireActivity().findViewById(R.id.nav_container);
        if (navBar != null && navBar.getVisibility() == View.VISIBLE) snackbar.setAnchorView(navBar);
        snackbar.show();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String[] labels(int[] resIds) {
        String[] labels = new String[resIds.length];
        for (int i = 0; i < resIds.length; i++) labels[i] = getString(resIds[i]);
        return labels;
    }

    private static int indexOf(int[] values, int value) {
        for (int i = 0; i < values.length; i++) if (values[i] == value) return i;
        return 0;
    }

    private static int filterIndex(SearchFilter filter) {
        for (int i = 0; i < FILTERS.length; i++) if (FILTERS[i] == filter) return i;
        return 0;
    }

    private static int speechIndex(float rate) {
        int best = 0;
        for (int i = 1; i < SPEECH_RATES.length; i++) {
            if (Math.abs(SPEECH_RATES[i] - rate) < Math.abs(SPEECH_RATES[best] - rate)) best = i;
        }
        return best;
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
