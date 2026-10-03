package net.golbarg.engtoper.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.widget.Toast;

import net.golbarg.engtoper.R;

/** Store, share, email and website links used by Settings and About. */
public final class AppLinks {

    private AppLinks() {
    }

    public static String versionName(Context context) {
        PackageInfo info = packageInfo(context);
        return info != null && info.versionName != null ? info.versionName : "";
    }

    public static long versionCode(Context context) {
        PackageInfo info = packageInfo(context);
        return info != null ? info.getLongVersionCode() : 0;
    }

    /** Opens this app's page in the Play Store app, or in the browser if the store is missing. */
    public static void openPlayStore(Context context) {
        String pkg = context.getPackageName();
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + pkg)));
        } catch (ActivityNotFoundException e) {
            openUrl(context, playStoreUrl(context));
        }
    }

    public static void shareApp(Context context) {
        String text = context.getString(R.string.share_app_text, context.getString(R.string.app_name), playStoreUrl(context));
        Intent send = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text);
        start(context, Intent.createChooser(send, context.getString(R.string.setting_share)));
    }

    public static void sendFeedback(Context context) {
        Intent email = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + context.getString(R.string.contact_email)))
                .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.feedback_subject,
                        context.getString(R.string.app_name), versionName(context)));
        start(context, email);
    }

    public static void openUrl(Context context, String url) {
        start(context, new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    private static String playStoreUrl(Context context) {
        return "https://play.google.com/store/apps/details?id=" + context.getPackageName();
    }

    private static void start(Context context, Intent intent) {
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.no_app_found, Toast.LENGTH_SHORT).show();
        }
    }

    private static PackageInfo packageInfo(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }
}
