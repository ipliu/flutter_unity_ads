package com.rebeloid.unity_ads;

import androidx.annotation.NonNull;

import com.unity3d.services.banners.BannerErrorInfo;
import com.unity3d.services.banners.BannerView;

class FlutterAdListener {
    protected final int adId;
    @NonNull protected final AdInstanceManager manager;

    FlutterAdListener(int adId, @NonNull AdInstanceManager manager) {
        this.adId = adId;
        this.manager = manager;
    }
}

class FlutterBannerAdListener extends FlutterAdListener implements BannerView.IListener {

    FlutterBannerAdListener(int adId, @NonNull AdInstanceManager manager) {
        super(adId, manager);
    }

    @Override
    public void onBannerLoaded(BannerView bannerAdView) {
        manager.onAdLoaded(adId);
    }

    @Override
    public void onBannerFailedToLoad(BannerView bannerAdView, BannerErrorInfo errorInfo) {
        manager.onAdFailedToLoad(adId, new FlutterAd.FlutterAdError(errorInfo));
    }

    @Override
    public void onBannerClick(BannerView bannerAdView) {
        manager.onAdClicked(adId);
    }

    @Override
    public void onBannerLeftApplication(BannerView bannerView) {
        manager.onAdLeftApplication(adId);
    }
}
