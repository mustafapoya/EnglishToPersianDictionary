package net.golbarg.engtoper.ui.lookup;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import net.golbarg.engtoper.MainActivity;
import net.golbarg.engtoper.R;
import net.golbarg.engtoper.db.DictionaryRepository;
import net.golbarg.engtoper.ui.DictionaryViewModel;
import net.golbarg.engtoper.ui.WordDetailBottomSheet;
import net.golbarg.engtoper.util.AppPreferences;
import net.golbarg.engtoper.util.EnglishForms;
import net.golbarg.engtoper.util.PersianText;

/**
 * "Dictionary" in the text-selection menu and the share sheet of other apps.
 * Shows the word's detail sheet over the app the user is in; with no exact match it opens the
 * dictionary searching for the text instead.
 */
public class LookupActivity extends AppCompatActivity implements WordDetailBottomSheet.Host {

    private static final String SHEET_TAG = "LOOKUP_SHEET";
    /** Longer selections are sentences, not words: search for them in the app instead. */
    private static final int MAX_LOOKUP_LENGTH = 60;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppPreferences.getThemeMode(this));
        super.onCreate(savedInstanceState);
        // After rotation the restored sheet is still showing; nothing to look up again. If the
        // lookup hadn't finished yet, its answer went to the old activity, so look up again.
        if (savedInstanceState != null && getSupportFragmentManager().findFragmentByTag(SHEET_TAG) != null) return;

        String text = EnglishForms.clean(selectedText(getIntent()));
        if (text.isEmpty()) {
            finish();
            return;
        }
        boolean persian = PersianText.containsPersian(text);
        if (text.length() > MAX_LOOKUP_LENGTH) {
            openInDictionary(text, persian);
            return;
        }

        DictionaryRepository repository = DictionaryRepository.getInstance(this);
        if (persian) {
            repository.lookupPersian(text, phrase -> {
                if (phrase != null) show(WordDetailBottomSheet.newInstance(phrase));
                else notFound(text, true);
            });
        } else {
            repository.lookupEnglish(text, phrase -> {
                if (phrase != null) show(WordDetailBottomSheet.newInstance(phrase));
                else notFound(text, false);
            });
        }
    }

    @Nullable
    private static String selectedText(Intent intent) {
        if (intent == null) return null;
        CharSequence text = Intent.ACTION_PROCESS_TEXT.equals(intent.getAction())
                ? intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
                : intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
        return text != null ? text.toString() : null;
    }

    private void show(WordDetailBottomSheet sheet) {
        if (isFinishing() || isDestroyed() || getSupportFragmentManager().isStateSaved()) return;
        sheet.show(getSupportFragmentManager(), SHEET_TAG);
    }

    private void notFound(String text, boolean persian) {
        if (isFinishing() || isDestroyed()) return;
        // Application context: this activity finishes at once, which would cancel its own toast
        Toast.makeText(getApplicationContext(), getString(R.string.lookup_not_found, text), Toast.LENGTH_SHORT).show();
        openInDictionary(text, persian);
    }

    private void openInDictionary(String text, boolean persian) {
        startActivity(MainActivity.dictionaryIntent(this,
                persian ? DictionaryViewModel.LANG_FA : DictionaryViewModel.LANG_EN, text));
        finishQuietly();
    }

    @Override
    public void onWordSheetDismissed() {
        finishQuietly();
    }

    private void finishQuietly() {
        finish();
        overridePendingTransition(0, 0);
    }
}
