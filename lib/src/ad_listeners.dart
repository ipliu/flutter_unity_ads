import 'package:meta/meta.dart';

import 'ad_containers.dart';

/// The callback type to handle an event occurring for an [Ad].
typedef AdEventCallback = void Function(Ad ad);

/// The callback type to handle an error loading an [Ad].
typedef AdLoadErrorCallback = void Function(Ad ad, AdError error);

/// Shared event callbacks used in Native and Banner ads.
abstract class AdWithViewListener {
  /// Default constructor for [AdWithViewListener], meant to be used by subclasses.
  @protected
  const AdWithViewListener({
    this.onAdLoaded,
    this.onAdFailedToLoad,
    this.onAdImpression,
    this.onAdClicked,
    this.onAdLeftApplication,
  });

  /// Called when an ad is successfully received.
  final AdEventCallback? onAdLoaded;

  /// Called when an ad request failed.
  final AdLoadErrorCallback? onAdFailedToLoad;

  /// Called when an impression occurs on the ad.
  final AdEventCallback? onAdImpression;

  /// Called when the ad is clicked.
  final AdEventCallback? onAdClicked;

  /// Called when the user leaves the app before ad experience is completed,
  /// such as opening the Store page for the ad.
  final AdEventCallback? onAdLeftApplication;
}

/// A listener for receiving notifications for the lifecycle of a [BannerAd].
class BannerAdListener extends AdWithViewListener {
  /// Constructs a [BannerAdListener] that notifies for the provided event callbacks.
  ///
  /// Typically you will override [onAdLoaded] and [onAdFailedToLoad]:
  /// ```dart
  /// BannerAdListener(
  ///   onAdLoaded: (ad) {
  ///     // Ad successfully loaded - display an AdWidget with the banner ad.
  ///   },
  ///   onAdFailedToLoad: (ad, error) {
  ///     // Ad failed to load - log the error and dispose the ad.
  ///   },
  ///   ...
  /// )
  /// ```
  const BannerAdListener({
    AdEventCallback? onAdLoaded,
    AdLoadErrorCallback? onAdFailedToLoad,
    AdEventCallback? onAdImpression,
    AdEventCallback? onAdClicked,
    AdEventCallback? onAdLeftApplication,
  }) : super(
    onAdLoaded: onAdLoaded,
    onAdFailedToLoad: onAdFailedToLoad,
    onAdImpression: onAdImpression,
    onAdClicked: onAdClicked,
    onAdLeftApplication: onAdLeftApplication,
  );
}