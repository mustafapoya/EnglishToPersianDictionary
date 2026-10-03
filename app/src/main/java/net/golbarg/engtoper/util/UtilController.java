package net.golbarg.engtoper.util;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import net.golbarg.engtoper.R;

public class UtilController {

    /** Turns line-breaking tags into newlines. */
    private static String normalizeBreaks(String trans) {
        trans = trans.replaceAll("(?i)<br\\s*/?>", "\n");
        trans = trans.replace("');", "");
        trans = trans.replaceAll("(?i)<p.*?>", "");
        trans = trans.replaceAll("(?i)</p>", "\n");
        trans = trans.replaceAll("(?i)<div.*?>", "");
        trans = trans.replaceAll("(?i)</div>", "\n");
        return trans.trim();
    }

    /** Plain text of a translation: every tag removed, entities decoded, blank lines collapsed. */
    public static String removeHTMLTags(String trans) {
        if (trans == null) return "";
        trans = normalizeBreaks(trans);
        trans = trans.replaceAll("<[^>]*>", "");
        trans = trans.replace("&nbsp;", " ")
                .replace("&quot;", "\"")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&");
        trans = trans.replaceAll("[ \\t]+\\n", "\n");
        trans = trans.replaceAll("\\n{3,}", "\n\n");
        return trans.trim();
    }

    public static void copyToClipboard(Context context, String label, String text) {
        if (context == null || text == null) return;
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
    }

    public static void shareWord(Context context, String word, String translation) {
        if (context == null) return;
        String cleanTranslation = removeHTMLTags(translation);
        String shareText = "📖 " + word + "\n\n" + cleanTranslation + "\n\n— English-Persian Dictionary";

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        sendIntent.setType("text/plain");

        Intent shareIntent = Intent.createChooser(sendIntent, context.getString(R.string.action_share));
        context.startActivity(shareIntent);
    }
}
