package net.golbarg.engtoper;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.NavOptions;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.golbarg.engtoper.ads.AdConsent;
import net.golbarg.engtoper.databinding.ActivityMainBinding;
import net.golbarg.engtoper.db.StudyRepository;
import net.golbarg.engtoper.ui.DictionaryViewModel;
import net.golbarg.engtoper.ui.common.PillNavBar;
import net.golbarg.engtoper.util.AppPreferences;

public class MainActivity extends AppCompatActivity {

    // Deep links (app shortcuts, the daily reminder, "Translate" from other apps)
    public static final String EXTRA_DESTINATION = "net.golbarg.engtoper.extra.DESTINATION";
    public static final String EXTRA_LANG = "net.golbarg.engtoper.extra.LANG";
    public static final String EXTRA_QUERY = "net.golbarg.engtoper.extra.QUERY";
    public static final String DESTINATION_DICTIONARY = "dictionary";
    public static final String DESTINATION_FLASHCARDS = "flashcards";

    private static final Set<Integer> TAB_IDS = new HashSet<>(Arrays.asList(
            R.id.navigation_home, R.id.navigation_dictionary, R.id.navigation_flashcards,
            R.id.navigation_bookmark, R.id.navigation_settings));

    private ActivityMainBinding binding;
    @Nullable
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppPreferences.getThemeMode(this));
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupWindowInsets();
        setupBottomNavigation();
        AppPreferences.markFirstOpen(this);
        // Asks for ad consent where the law requires it, then lets ads start
        AdConsent.gather(this);
        if (savedInstanceState == null) handleDeepLink(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleDeepLink(intent);
    }

    /** Opens the screen a shortcut, notification or lookup asked for. */
    private void handleDeepLink(@Nullable Intent intent) {
        if (intent == null) return;
        String destination = intent.getStringExtra(EXTRA_DESTINATION);
        if (destination == null) return;
        DictionaryViewModel viewModel = new ViewModelProvider(this).get(DictionaryViewModel.class);

        if (DESTINATION_DICTIONARY.equals(destination)) {
            String lang = intent.getStringExtra(EXTRA_LANG);
            String query = intent.getStringExtra(EXTRA_QUERY);
            viewModel.requestDictionary(lang != null ? lang : viewModel.currentLang(), query, query == null);
            openTab(R.id.navigation_dictionary);
        } else if (DESTINATION_FLASHCARDS.equals(destination)) {
            viewModel.requestFlashcardDeck(StudyRepository.Deck.DUE);
            openTab(R.id.navigation_flashcards);
        }
        // Handle each link once (e.g. not again after a configuration change)
        intent.removeExtra(EXTRA_DESTINATION);
    }

    /** An intent that opens the dictionary in {@code lang}, searching {@code query} if given. */
    public static Intent dictionaryIntent(Context context, String lang, @Nullable String query) {
        return new Intent(context, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(EXTRA_DESTINATION, DESTINATION_DICTIONARY)
                .putExtra(EXTRA_LANG, lang)
                .putExtra(EXTRA_QUERY, query);
    }

    /**
     * Content is drawn edge-to-edge, so keep fragments clear of the status bar and cutouts,
     * and lift the navigation bar above the gesture/3-button bar. While the keyboard is open
     * the bottom bar is hidden so search results get the full height.
     */
    private void setupWindowInsets() {
        View navHost = binding.navHostFragmentActivityMain;
        ViewCompat.setOnApplyWindowInsetsListener(binding.container, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            boolean imeVisible = windowInsets.isVisible(WindowInsetsCompat.Type.ime());
            int imeBottom = windowInsets.getInsets(WindowInsetsCompat.Type.ime()).bottom;

            binding.navContainer.setVisibility(imeVisible ? View.GONE : View.VISIBLE);
            binding.navContainer.setPadding(bars.left, 0, bars.right, bars.bottom);
            navHost.setPadding(bars.left, bars.top, bars.right, imeVisible ? imeBottom : 0);
            return windowInsets;
        });
    }

    private void setupBottomNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_activity_main);
        if (navHostFragment == null) return;
        navController = navHostFragment.getNavController();

        binding.navView.setItems(Arrays.asList(
                new PillNavBar.Item(R.id.navigation_home, R.drawable.nav_home_selector, R.string.nav_home),
                new PillNavBar.Item(R.id.navigation_dictionary, R.drawable.ic_translate, R.string.nav_dictionary),
                new PillNavBar.Item(R.id.navigation_flashcards, R.drawable.nav_flashcard_selector, R.string.nav_flashcards),
                new PillNavBar.Item(R.id.navigation_bookmark, R.drawable.nav_bookmark_selector, R.string.nav_bookmark),
                new PillNavBar.Item(R.id.navigation_settings, R.drawable.nav_settings_selector, R.string.nav_settings)
        ));
        binding.navView.setOnItemSelectedListener(this::navigateToTab);

        // Keep the highlighted tab in sync with back presses and programmatic navigation.
        // Destinations that aren't tabs (e.g. About, opened from Settings) keep their tab highlighted.
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (TAB_IDS.contains(destination.getId())) {
                binding.navView.setSelected(destination.getId(), true);
            }
        });
    }

    /** Same behaviour as NavigationUI: each tab keeps its own back stack, restored on return. */
    private void navigateToTab(int destinationId) {
        if (navController == null) return;
        NavOptions options = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .setPopUpTo(navController.getGraph().getStartDestinationId(), false, true)
                .setEnterAnim(androidx.navigation.ui.R.animator.nav_default_enter_anim)
                .setExitAnim(androidx.navigation.ui.R.animator.nav_default_exit_anim)
                .setPopEnterAnim(androidx.navigation.ui.R.animator.nav_default_pop_enter_anim)
                .setPopExitAnim(androidx.navigation.ui.R.animator.nav_default_pop_exit_anim)
                .build();
        try {
            navController.navigate(destinationId, null, options);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    /** Switches bottom-navigation tab, exactly as if the user tapped it (keeps each tab's back stack). */
    public void openTab(int destinationId) {
        if (binding.navView.getSelectedId() == destinationId) return;
        navigateToTab(destinationId);
    }
}
