package com.rebeloid.unity_ads;

import static com.rebeloid.unity_ads.UnityAdsConstants.PLACEMENT_ID_PARAMETER;
import static com.rebeloid.unity_ads.UnityAdsConstants.SERVER_ID_PARAMETER;

import android.app.Activity;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.rebeloid.unity_ads.privacy.PrivacyConsent;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.metadata.PlayerMetaData;

import java.util.HashMap;
import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.StandardMethodCodec;

/**
 * Unity Ads Plugin
 */
public class UnityAdsPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware {

    private static final String TAG = "UnityAdsPlugin";

    private static <T> T requireNonNull(T obj) {
        if (obj == null) {
            throw new IllegalArgumentException();
        }
        return obj;
    }

    // This is always null when not using v2 embedding.
    @Nullable private FlutterPluginBinding pluginBinding;
    @Nullable private AdInstanceManager instanceManager;
    @Nullable private AdMessageCodec adMessageCodec;

    private MethodChannel channel;
    private Context context;
    private Activity activity;
    private Map<String, MethodChannel> placementChannels;
    private BinaryMessenger binaryMessenger;
    private PrivacyConsent privacyConsent;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        pluginBinding = flutterPluginBinding;
        adMessageCodec = new AdMessageCodec(flutterPluginBinding.getApplicationContext());
        channel = new MethodChannel(
                flutterPluginBinding.getBinaryMessenger(),
                UnityAdsConstants.MAIN_CHANNEL,
                new StandardMethodCodec(adMessageCodec));
        channel.setMethodCallHandler(this);
        instanceManager = new AdInstanceManager(channel);
        context = flutterPluginBinding.getApplicationContext();
        binaryMessenger = flutterPluginBinding.getBinaryMessenger();
        placementChannels = new HashMap<>();
        privacyConsent = new PrivacyConsent();

        flutterPluginBinding.getPlatformViewRegistry()
                .registerViewFactory(UnityAdsConstants.MAIN_CHANNEL + "/ad_widget",
                        new UnityAdsViewFactory(instanceManager));
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        if (instanceManager == null || pluginBinding == null) {
            Log.e(TAG, "method call received before instanceManager initialized: " + call.method);
            return;
        }

        Map<?, ?> arguments = (Map<?, ?>) call.arguments;

        switch (call.method) {
            case UnityAdsConstants.INIT_METHOD:
                result.success(initialize(arguments));
                break;
            case UnityAdsConstants.LOAD_METHOD:
                result.success(load(arguments));
                break;
            case UnityAdsConstants.SHOW_VIDEO_METHOD:
                result.success(showVideo(arguments));
                break;
            case UnityAdsConstants.PRIVACY_CONSENT_SET_METHOD:
                result.success(privacyConsent.set(arguments));
                break;
            case "_init":
                // Internal init. This is necessary to cleanup state on hot restart.
                instanceManager.disposeAllAds();
                result.success(null);
                break;
            case "loadBannerAd":
                final FlutterBannerAd bannerAd =
                        new FlutterBannerAd(
                                requireNonNull(call.<Integer>argument("adId")),
                                instanceManager,
                                requireNonNull(call.argument("placementId")),
                                requireNonNull(call.argument("size")));
                instanceManager.trackAd(
                        bannerAd, requireNonNull(call.<Integer>argument("adId")));
                bannerAd.load();
                result.success(null);
                break;
            case "disposeAd":
                instanceManager.disposeAd(requireNonNull(call.<Integer>argument("adId")));
                result.success(null);
                break;
            case "getAdSize":
                FlutterAd ad = instanceManager.adForId(
                        requireNonNull(call.<Integer>argument("adId")));
                if (ad == null) {
                    // This was called on a dart ad container that hasn't been loaded yet.
                    result.success(null);
                } else if (ad instanceof FlutterBannerAd) {
                    result.success(((FlutterBannerAd) ad).getAdSize());
                } else {
                    result.error(
                            "unexpected_ad_type",
                            "Unexpected ad type for getAdSize: " + ad,
                            null);
                }
                break;
            default:
                result.notImplemented();
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        if (instanceManager != null) {
            instanceManager.disposeAllAds();
        }
        channel.setMethodCallHandler(null);
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        activity = binding.getActivity();
        privacyConsent.setActivity(activity);
        if (instanceManager != null) {
            instanceManager.setActivity(binding.getActivity());
        }
        if (adMessageCodec != null) {
            adMessageCodec.setContext(binding.getActivity());
        }
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        // Use the application context
        if (adMessageCodec != null && pluginBinding != null) {
            adMessageCodec.setContext(pluginBinding.getApplicationContext());
        }
        if (instanceManager != null) {
            instanceManager.setActivity(null);
        }
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        if (instanceManager != null) {
            instanceManager.setActivity(binding.getActivity());
        }
        if (adMessageCodec != null) {
            adMessageCodec.setContext(binding.getActivity());
        }
    }

    @Override
    public void onDetachedFromActivity() {
        if (adMessageCodec != null && pluginBinding != null) {
            adMessageCodec.setContext(pluginBinding.getApplicationContext());
        }
        if (instanceManager != null) {
            instanceManager.setActivity(null);
        }
    }

    private boolean initialize(Map<?, ?> args) {
        String gameId = (String) args.get(UnityAdsConstants.GAME_ID_PARAMETER);

        boolean firebaseTestMode = false;
        if (isInFirebaseTestLab()) {
            String firebaseTestLabMode = (String) args.get(UnityAdsConstants.FIREBASE_TEST_LAB_MODE_PARAMETER);
            if ("disableAds".equalsIgnoreCase(firebaseTestLabMode)) {
                return false;
            }

            firebaseTestMode = "showAdsInTestMode".equalsIgnoreCase(firebaseTestLabMode);
        }

        Boolean testMode = (Boolean) args.get(UnityAdsConstants.TEST_MODE_PARAMETER);
        if (testMode == null) {
            testMode = false;
        }

        UnityAds.initialize(context, gameId, testMode || firebaseTestMode, new UnityAdsInitializationListener(channel));
        return true;
    }

    private boolean isInFirebaseTestLab() {
        String testLabSetting = Settings.System.getString(context.getContentResolver(), "firebase.test.lab");
        return "true".equalsIgnoreCase(testLabSetting);
    }

    private boolean load(Map<?, ?> args) {
        final String placementId = (String) args.get(PLACEMENT_ID_PARAMETER);
        UnityAds.load(placementId, new UnityAdsLoadListener(placementChannels, binaryMessenger));
        return true;
    }

    private boolean showVideo(Map<?, ?> args) {
        final String placementId = (String) args.get(PLACEMENT_ID_PARAMETER);

        final String serverId = (String) args.get(SERVER_ID_PARAMETER);
        if (serverId != null) {
            PlayerMetaData playerMetaData = new PlayerMetaData(context);
            playerMetaData.setServerId(serverId);
            playerMetaData.commit();
        }
        UnityAds.show(activity, placementId, new UnityAdsShowListener(placementChannels, binaryMessenger));
        return true;
    }

}
