package com.rebeloid.unity_ads;

import androidx.annotation.NonNull;

import com.unity3d.services.banners.UnityBannerSize;

class FlutterAdSize {
    @NonNull final UnityBannerSize size;
    final int width;
    final int height;

    FlutterAdSize(int width, int height) {
        this(new UnityBannerSize(width, height));
    }

    FlutterAdSize(@NonNull UnityBannerSize size) {
        this.size = size;
        this.width = size.getWidth();
        this.height = size.getHeight();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof FlutterAdSize)) {
            return false;
        }

        final FlutterAdSize that = (FlutterAdSize) o;

        if (width != that.width) {
            return false;
        }
        return height == that.height;
    }

    @Override
    public int hashCode() {
        return size.hashCode();
    }

    public UnityBannerSize getAdSize() {
        return size;
    }
}
