package com.rebeloid.unity_ads;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.unity3d.services.banners.BannerErrorInfo;

import java.util.Objects;

import io.flutter.plugin.platform.PlatformView;

abstract class FlutterAd {

    protected final int adId;

    FlutterAd(int adId) {
        this.adId = adId;
    }

    /** Wrapper for {@link BannerErrorInfo}. */
    static class FlutterAdError {
        @NonNull final String code;
        @NonNull final String message;

        FlutterAdError(@NonNull BannerErrorInfo error) {
            code = error.errorCode.name();
            message = error.errorMessage;
        }

        FlutterAdError(@NonNull String code, @NonNull String message) {
            this.code = code;
            this.message = message;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            } else if (!(object instanceof FlutterAdError)) {
                return false;
            }

            final FlutterAdError that = (FlutterAdError) object;

            if (!code.equals(that.code)) {
                return false;
            }
            return message.equals(that.message);
        }

        @Override
        public int hashCode() {
            return Objects.hash(code, message);
        }
    }

    abstract void load();

    /**
     * Gets the PlatformView for the ad. Default behavior is to return null. Should be overridden by
     * ads with platform views, such as banner and native ads.
     */
    @Nullable
    PlatformView getPlatformView() {
        return null;
    }

    /**
     * Invoked when dispose() is called on the corresponding Flutter ad object. This perform any
     * necessary cleanup.
     */
    abstract void dispose();
}
