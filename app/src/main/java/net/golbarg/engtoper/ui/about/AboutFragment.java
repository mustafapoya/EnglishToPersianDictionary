package net.golbarg.engtoper.ui.about;

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
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.color.MaterialColors;

import net.golbarg.engtoper.R;
import net.golbarg.engtoper.databinding.FragmentAboutBinding;
import net.golbarg.engtoper.databinding.ViewSettingRowBinding;
import net.golbarg.engtoper.databinding.ViewStatBinding;
import net.golbarg.engtoper.db.DictionaryRepository;
import net.golbarg.engtoper.util.AppLinks;
import net.golbarg.engtoper.util.LocaleUtil;

import java.time.Year;

/** About the app: version, live dictionary size, features, developer and contact links. */
public class AboutFragment extends Fragment {

    private FragmentAboutBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAboutBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
        binding.textVersion.setText(getString(R.string.about_version,
                AppLinks.versionName(requireContext()), (int) AppLinks.versionCode(requireContext())));
        binding.textCopyright.setText(getString(R.string.about_copyright, Year.now().getValue()));

        setupStats();
        setupFeatures();
        setupLinks();
    }

    private void setupStats() {
        bindStat(binding.statEnglish, R.drawable.ic_translate, R.string.about_stat_english, "…");
        bindStat(binding.statPersian, R.drawable.ic_swap_horiz, R.string.about_stat_persian, "…");
        bindStat(binding.statOffline, R.drawable.ic_check, R.string.about_stat_offline, getString(R.string.about_stat_offline_value));

        DictionaryRepository.getInstance(requireContext()).getEntryCounts(counts -> {
            if (binding == null) return;
            binding.statEnglish.textValue.setText(compact(counts[0]));
            binding.statPersian.textValue.setText(compact(counts[1]));
        });
    }

    private void bindStat(ViewStatBinding stat, @DrawableRes int icon, @StringRes int label, String value) {
        stat.icon.setImageResource(icon);
        stat.icon.setImageTintList(ColorStateList.valueOf(color(androidx.appcompat.R.attr.colorPrimary)));
        stat.textLabel.setText(label);
        stat.textValue.setText(value);
    }

    /** 146315 → "146K+" (or "۱۴۶ هزار+" in Persian); small numbers as they are. */
    private String compact(int count) {
        if (count >= 1000) return getString(R.string.about_count_thousands, LocaleUtil.number(requireContext(), count / 1000));
        return LocaleUtil.number(requireContext(), count);
    }

    private void setupFeatures() {
        int bg = com.google.android.material.R.attr.colorPrimaryContainer;
        int fg = com.google.android.material.R.attr.colorOnPrimaryContainer;
        bindInfoRow(binding.featureDictionary, R.drawable.ic_translate, bg, fg, R.string.feature_dictionary, getString(R.string.feature_dictionary_sub), null);
        bindInfoRow(binding.featurePronunciation, R.drawable.ic_volume_up, bg, fg, R.string.feature_pronunciation, getString(R.string.feature_pronunciation_sub), null);
        bindInfoRow(binding.featureFlashcards, R.drawable.ic_flashcard_outline, bg, fg, R.string.feature_flashcards, getString(R.string.feature_flashcards_sub), null);
        bindInfoRow(binding.featureQuiz, R.drawable.ic_quiz, bg, fg, R.string.feature_quiz, getString(R.string.feature_quiz_sub), null);
    }

    private void setupLinks() {
        int bg = com.google.android.material.R.attr.colorSecondaryContainer;
        int fg = com.google.android.material.R.attr.colorOnSecondaryContainer;
        bindInfoRow(binding.linkWebsite, R.drawable.ic_language, bg, fg, R.string.link_website, getString(R.string.website_label),
                v -> AppLinks.openUrl(requireContext(), getString(R.string.website_url)));
        bindInfoRow(binding.linkEmail, R.drawable.ic_email, bg, fg, R.string.link_email, getString(R.string.contact_email),
                v -> AppLinks.sendFeedback(requireContext()));
        bindInfoRow(binding.linkPlay, R.drawable.ic_star_outline, bg, fg, R.string.link_play, getString(R.string.link_play_sub),
                v -> AppLinks.openPlayStore(requireContext()));
    }

    /** Reuses the settings row: rows with an action get a chevron, plain info rows don't respond to taps. */
    private void bindInfoRow(ViewSettingRowBinding row, @DrawableRes int icon, @AttrRes int background, @AttrRes int tint,
                             @StringRes int title, String value, @Nullable View.OnClickListener onClick) {
        row.icon.setImageResource(icon);
        row.icon.setImageTintList(ColorStateList.valueOf(color(tint)));
        row.iconBg.setBackgroundTintList(ColorStateList.valueOf(color(background)));
        row.title.setText(title);
        row.value.setText(value);
        row.chevron.setVisibility(onClick != null ? View.VISIBLE : View.GONE);
        if (onClick != null) {
            row.getRoot().setOnClickListener(onClick);
        } else {
            row.getRoot().setClickable(false);
            row.getRoot().setFocusable(false);
            row.getRoot().setBackground(null);
        }
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
