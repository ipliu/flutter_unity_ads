package com.rebeloid.unity_ads;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.unity3d.services.banners.BannerView;

import io.flutter.plugin.platform.PlatformView;
import io.flutter.util.Preconditions;

/** A wrapper for {@link BannerView}. */
class FlutterBannerAd extends FlutterAd {

    private static final String TAG = "FlutterBannerAd";

    @NonNull private final AdInstanceManager manager;
    @NonNull private final String placementId;
    @NonNull private final FlutterAdSize size;
    @Nullable private BannerView bannerView;
    private boolean isImpression = false;

    /** Constructs the FlutterBannerAd. */
    public FlutterBannerAd(
            int adId,
            @NonNull AdInstanceManager manager,
            @NonNull String placementId,
            @NonNull FlutterAdSize size) {
        super(adId);
        Preconditions.checkNotNull(manager);
        Preconditions.checkNotNull(placementId);
        Preconditions.checkNotNull(size);
        this.manager = manager;
        this.placementId = placementId;
        this.size = size;
    }

    @Override
    void load() {
        if (manager.getActivity() == null) {
            Log.e(TAG, "Tried to show banner ad before activity was bound to the plugin.");
            return;
        }
        bannerView = new BannerView(manager.getActivity(), placementId, size.getAdSize());
        bannerView.setListener(new FlutterBannerAdListener(adId, manager));
        bannerView.load();
    }

    @Nullable
    @Override
    public PlatformView getPlatformView() {
        if (bannerView == null) {
            return null;
        }
        if (!isImpression) {
            isImpression = true;
            manager.onAdImpression(adId);
        }
        return new FlutterPlatformView(bannerView);
    }

    @Override
    void dispose() {
        if (bannerView != null) {
            bannerView.destroy();
            bannerView = null;
        }
    }

    @Nullable
    FlutterAdSize getAdSize() {
        if (bannerView == null || bannerView.getSize() == null) {
            return null;
        }
        return new FlutterAdSize(bannerView.getSize());
    }
}
