package net.golbarg.engtoper.util;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import net.golbarg.engtoper.R;

import java.util.Locale;

public class TTSManager {
    private static TTSManager instance;
    private TextToSpeech textToSpeech;
    private boolean isInitialized = false;

    private final Context appContext;

    private TTSManager(Context context) {
        appContext = context.getApplicationContext();
        textToSpeech = new TextToSpeech(appContext, status -> {
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true;
                applySettings();
            }
        });
    }

    /** Applies the accent and speech rate chosen in Settings. */
    public void applySettings() {
        if (!isInitialized || textToSpeech == null) return;
        Locale accent = AppPreferences.isBritishAccent(appContext) ? Locale.UK : Locale.US;
        int result = textToSpeech.setLanguage(accent);
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            textToSpeech.setLanguage(Locale.ENGLISH);
        }
        textToSpeech.setSpeechRate(AppPreferences.getSpeechRate(appContext));
    }

    public static synchronized TTSManager getInstance(Context context) {
        if (instance == null) {
            instance = new TTSManager(context);
        }
        return instance;
    }

    public void speak(String text, Context context) {
        if (text == null || text.trim().isEmpty()) return;

        if (isInitialized && textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.speak(text.trim(), TextToSpeech.QUEUE_FLUSH, null, "DICTIONARY_TTS_" + System.currentTimeMillis());
        } else {
            if (context != null) {
                Toast.makeText(context, R.string.tts_error, Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void stop() {
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
    }

    public void shutdown() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
            isInitialized = false;
        }
        instance = null;
    }
}
