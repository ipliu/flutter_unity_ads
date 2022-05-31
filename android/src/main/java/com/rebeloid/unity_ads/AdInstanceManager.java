package com.rebeloid.unity_ads;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

import io.flutter.plugin.common.MethodChannel;

/**
 * Maintains reference to ad instances for the {@link
 * com.rebeloid.unity_ads.UnityAdsPlugin}.
 *
 * <p>When an Ad is loaded from Dart, an equivalent ad object is created and maintained here to
 * provide access until the ad is disposed.
 */
class AdInstanceManager {
    @Nullable private Activity activity;

    @NonNull private final Map<Integer, FlutterAd> ads;
    @NonNull private final MethodChannel channel;

    /**
     * Initializes the ad instance manager. We only need a method channel to start loading ads, but an
     * activity must be present in order to attach any ads to the view hierarchy.
     */
    AdInstanceManager(@NonNull MethodChannel channel) {
        this.channel = channel;
        this.ads = new HashMap<>();
    }

    void setActivity(@Nullable Activity activity) {
        this.activity = activity;
    }

    @Nullable
    Activity getActivity() {
        return activity;
    }

    @Nullable
    FlutterAd adForId(int id) {
        return ads.get(id);
    }

    void trackAd(@NonNull FlutterAd ad, int adId) {
        if (ads.get(adId) != null) {
            throw new IllegalArgumentException(
                    String.format("Ad for following adId already exists: %d", adId));
        }
        ads.put(adId, ad);
    }

    void disposeAd(int adId) {
        if (!ads.containsKey(adId)) {
            return;
        }
        FlutterAd ad = ads.get(adId);
        if (ad != null) {
            ad.dispose();
        }
        ads.remove(adId);
    }

    void disposeAllAds() {
        for (Map.Entry<Integer, FlutterAd> entry : ads.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().dispose();
            }
        }
        ads.clear();
    }

    void onAdLoaded(int adId) {
        Map<Object, Object> arguments = new HashMap<>();
        arguments.put("adId", adId);
        arguments.put("eventName", "onAdLoaded");
        invokeOnAdEvent(arguments);
    }

    void onAdFailedToLoad(int adId, @NonNull FlutterAd.FlutterAdError error) {
        Map<Object, Object> arguments = new HashMap<>();
        arguments.put("adId", adId);
        arguments.put("eventName", "onAdFailedToLoad");
        arguments.put("adError", error);
        invokeOnAdEvent(arguments);
    }

    void onAdImpression(int id) {
        Map<Object, Object> arguments = new HashMap<>();
        arguments.put("adId", id);
        arguments.put("eventName", "onAdImpression");
        invokeOnAdEvent(arguments);
    }

    void onAdClicked(int id) {
        Map<Object, Object> arguments = new HashMap<>();
        arguments.put("adId", id);
        arguments.put("eventName", "onAdClicked");
        invokeOnAdEvent(arguments);
    }

    void onAdLeftApplication(int adId) {
        final Map<Object, Object> arguments = new HashMap<>();
        arguments.put("adId", adId);
        arguments.put("eventName", "onAdLeftApplication");
        invokeOnAdEvent(arguments);
    }

    /** Invoke the method channel using the UI thread. Otherwise the message gets silently dropped. */
    private void invokeOnAdEvent(final Map<Object, Object> arguments) {
        new Handler(Looper.getMainLooper())
                .post(() -> channel.invokeMethod("onAdEvent", arguments));
    }
}
