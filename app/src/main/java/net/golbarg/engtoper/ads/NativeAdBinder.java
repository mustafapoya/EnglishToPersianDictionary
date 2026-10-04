package net.golbarg.engtoper.ads;

import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;

import net.golbarg.engtoper.R;

/**
 * Fills a native ad layout. Layouts use the shared ids {@code ad_headline}, {@code ad_body},
 * {@code ad_advertiser}, {@code ad_icon}, {@code ad_media} and {@code ad_call_to_action}.
 */
public final class NativeAdBinder {

    private NativeAdBinder() {
    }

    public static void bind(NativeAdView adView, NativeAd ad) {
        TextView headline = adView.findViewById(R.id.ad_headline);
        TextView body = adView.findViewById(R.id.ad_body);
        TextView advertiser = adView.findViewById(R.id.ad_advertiser);
        ImageView icon = adView.findViewById(R.id.ad_icon);
        MediaView media = adView.findViewById(R.id.ad_media);
        Button action = adView.findViewById(R.id.ad_call_to_action);

        adView.setHeadlineView(headline);
        adView.setBodyView(body);
        adView.setAdvertiserView(advertiser);
        adView.setIconView(icon);
        adView.setMediaView(media);
        adView.setCallToActionView(action);

        headline.setText(ad.getHeadline());
        setOptionalText(body, ad.getBody());
        setOptionalText(advertiser, ad.getAdvertiser());
        setOptionalText(action, ad.getCallToAction());
        NativeAd.Image image = ad.getIcon();
        icon.setVisibility(image == null ? View.GONE : View.VISIBLE);
        icon.setImageDrawable(image != null ? image.getDrawable() : null);
        media.setMediaContent(ad.getMediaContent());
        adView.setNativeAd(ad);
    }

    private static void setOptionalText(TextView view, @Nullable String text) {
        view.setVisibility(text == null || text.isEmpty() ? View.GONE : View.VISIBLE);
        view.setText(text);
    }
}
