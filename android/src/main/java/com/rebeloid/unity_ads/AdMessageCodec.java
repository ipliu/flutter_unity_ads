package com.rebeloid.unity_ads;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import io.flutter.plugin.common.StandardMessageCodec;

/**
 * Encodes and decodes values by reading from a ByteBuffer and writing to a ByteArrayOutputStream.
 */
class AdMessageCodec extends StandardMessageCodec {
    // The type values below must be consistent for each platform.
    private static final byte VALUE_AD_SIZE = (byte) 128;
    private static final byte VALUE_AD_ERROR = (byte) 129;

    @NonNull Context context;

    AdMessageCodec(@NonNull Context context) {
        this.context = context;
    }

    void setContext(@NonNull Context context) {
        this.context = context;
    }

    @Override
    protected void writeValue(ByteArrayOutputStream stream, Object value) {
        if (value instanceof FlutterAdSize) {
            stream.write(VALUE_AD_SIZE);
            final FlutterAdSize adSize = (FlutterAdSize) value;
            writeValue(stream, adSize.width);
            writeValue(stream, adSize.height);
        } else if (value instanceof FlutterAd.FlutterAdError) {
            stream.write(VALUE_AD_ERROR);
            final FlutterAd.FlutterAdError error = (FlutterAd.FlutterAdError) value;
            writeValue(stream, error.code);
            writeValue(stream, error.message);
        } else {
            super.writeValue(stream, value);
        }
    }

    @Override
    protected Object readValueOfType(byte type, ByteBuffer buffer) {
        switch (type) {
            case VALUE_AD_SIZE:
                return new FlutterAdSize(
                        (Integer) readValueOfType(buffer.get(), buffer),
                        (Integer) readValueOfType(buffer.get(), buffer));
            case VALUE_AD_ERROR:
                return new FlutterAd.FlutterAdError(
                        (String) readValueOfType(buffer.get(), buffer),
                        (String) readValueOfType(buffer.get(), buffer));
            default:
                return super.readValueOfType(type, buffer);
        }
    }
}
