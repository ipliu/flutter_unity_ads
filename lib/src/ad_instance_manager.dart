import 'dart:collection';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'ad_containers.dart';

AdInstanceManager instanceManager = AdInstanceManager('com.rebeloid.unity_ads');

/// Maintains access to loaded [Ad] instances and handles sending/receiving
/// messages to platform code.
class AdInstanceManager {
  AdInstanceManager(String channelName)
      : channel = MethodChannel(
    channelName,
    StandardMethodCodec(AdMessageCodec()),
  ) {
    channel.setMethodCallHandler((MethodCall call) async {
      assert(call.method == 'onAdEvent');

      final int adId = call.arguments['adId'];
      final String eventName = call.arguments['eventName'];

      final Ad? ad = adFor(adId);
      if (ad != null) {
        _onAdEvent(ad, eventName, call.arguments);
      } else {
        debugPrint('$Ad with id `$adId` is not available for $eventName.');
      }
    });
  }

  int _nextAdId = 0;
  final _BiMap<int, Ad> _loadedAds = _BiMap<int, Ad>();

  /// Invokes load and dispose calls.
  final MethodChannel channel;

  void _onAdEvent(Ad ad, String eventName, Map<dynamic, dynamic> arguments) {
    if (defaultTargetPlatform == TargetPlatform.android) {
      _onAdEventAndroid(ad, eventName, arguments);
    } else {
      _onAdEventIOS(ad, eventName, arguments);
    }
  }

  void _onAdEventIOS(Ad ad, String eventName, Map<dynamic, dynamic> arguments) {

  }

  void _onAdEventAndroid(
      Ad ad, String eventName, Map<dynamic, dynamic> arguments) {
    switch (eventName) {
      case 'onAdLoaded':
        _invokeOnAdLoaded(ad, eventName);
        break;
      case 'onAdFailedToLoad':
        _invokeOnAdFailedToLoad(ad, eventName, arguments);
        break;
      case 'onAdImpression':
        _invokeOnAdImpression(ad, eventName);
        break;
      case 'onAdClicked':
        _invokeOnAdClicked(ad, eventName);
        break;
      case 'onAdLeftApplication':
        _invokeOnAdLeftApplication(ad, eventName);
        break;
      default:
        debugPrint('invalid ad event name: $eventName');
    }
  }

  void _invokeOnAdLoaded(Ad ad, String eventName) {
    if (ad is AdWithView) {
      ad.listener.onAdLoaded?.call(ad);
    } else {
      debugPrint('invalid ad: $ad, for event name: $eventName');
    }
  }

  void _invokeOnAdFailedToLoad(
      Ad ad, String eventName, Map<dynamic, dynamic> arguments) {
    if (ad is AdWithView) {
      ad.listener.onAdFailedToLoad?.call(ad, arguments['adError']);
    } else {
      debugPrint('invalid ad: $ad, for event name: $eventName');
    }
  }

  void _invokeOnAdImpression(Ad ad, String eventName) {
    if (ad is AdWithView) {
      ad.listener.onAdImpression?.call(ad);
    } else {
      debugPrint('invalid ad: $ad, for event name: $eventName');
    }
  }

  void _invokeOnAdClicked(Ad ad, String eventName) {
    if (ad is AdWithView) {
      ad.listener.onAdClicked?.call(ad);
    } else {
      debugPrint('invalid ad: $ad, for event name: $eventName');
    }
  }

  void _invokeOnAdLeftApplication(Ad ad, String eventName) {
    if (ad is AdWithView) {
      ad.listener.onAdLeftApplication?.call(ad);
    } else {
      debugPrint('invalid ad: $ad, for event name: $eventName');
    }
  }

  Future<AdSize> getAdSize(Ad ad) async {
    return (await instanceManager.channel.invokeMethod<AdSize>(
      'getAdSize',
      <dynamic, dynamic>{
        'adId': adIdFor(ad),
      },
    ))!;
  }

  /// Returns null if an invalid [adId] was passed in.
  Ad? adFor(int adId) => _loadedAds[adId];

  /// Returns null if an invalid [Ad] was passed in.
  int? adIdFor(Ad ad) => _loadedAds.inverse[ad];

  final Set<int> _mountedWidgetAdIds = <int>{};

  /// Returns true if the [adId] is already mounted in a [WidgetAd].
  bool isWidgetAdIdMounted(int adId) => _mountedWidgetAdIds.contains(adId);

  /// Indicates that [adId] is mounted in widget tree.
  void mountWidgetAdId(int adId) => _mountedWidgetAdIds.add(adId);

  /// Indicates that [adId] is unmounted from the widget tree.
  void unmountWidgetAdId(int adId) => _mountedWidgetAdIds.remove(adId);

  /// Starts loading the ad if not previously loaded.
  ///
  /// Does nothing if we have already tried to load the ad.
  Future<void> loadBannerAd(BannerAd ad) {
    if (adIdFor(ad) != null) {
      return Future<void>.value();
    }

    final int adId = _nextAdId++;
    _loadedAds[adId] = ad;
    return channel.invokeMethod<void>(
      'loadBannerAd',
      <dynamic, dynamic>{
        'adId': adId,
        'placementId': ad.placementId,
        'size': ad.size,
      },
    );
  }

  /// Free the plugin resources associated with this ad.
  ///
  /// Disposing a banner ad that's been shown removes it from the screen.
  /// Interstitial ads can't be programmatically removed from view.
  Future<void> disposeAd(Ad ad) {
    final int? adId = adIdFor(ad);
    final Ad? disposedAd = _loadedAds.remove(adId);
    if (disposedAd == null) {
      return Future<void>.value();
    }
    return channel.invokeMethod<void>(
      'disposeAd',
      <dynamic, dynamic>{
        'adId': adId,
      },
    );
  }
}

class AdMessageCodec extends StandardMessageCodec {
  // The type values below must be consistent for each platform.
  static const int _valueAdSize = 128;
  static const int _valueAdError = 129;

  @override
  void writeValue(WriteBuffer buffer, dynamic value) {
    if (value is AdSize) {
      buffer.putUint8(_valueAdSize);
      writeValue(buffer, value.width);
      writeValue(buffer, value.height);
    } else if (value is AdError) {
      buffer.putUint8(_valueAdError);
      writeValue(buffer, value.code);
      writeValue(buffer, value.message);
    } else {
      super.writeValue(buffer, value);
    }
  }

  @override
  dynamic readValueOfType(dynamic type, ReadBuffer buffer) {
    switch (type) {
      case _valueAdSize:
        num width = readValueOfType(buffer.getUint8(), buffer);
        num height = readValueOfType(buffer.getUint8(), buffer);
        return AdSize(
          width: width.toInt(),
          height: height.toInt(),
        );
      case _valueAdError:
        return AdError(
          readValueOfType(buffer.getUint8(), buffer),
          readValueOfType(buffer.getUint8(), buffer),
        );
      default:
        return super.readValueOfType(type, buffer);
    }
  }
}

class _BiMap<K extends Object, V extends Object> extends MapBase<K, V> {
  _BiMap() {
    _inverse = _BiMap<V, K>._inverse(this);
  }

  _BiMap._inverse(this._inverse);

  final Map<K, V> _map = <K, V>{};
  late _BiMap<V, K> _inverse;

  _BiMap<V, K> get inverse => _inverse;

  @override
  V? operator [](Object? key) => _map[key];

  @override
  void operator []=(K key, V value) {
    assert(!_map.containsKey(key));
    assert(!inverse.containsKey(value));
    _map[key] = value;
    inverse._map[value] = key;
  }

  @override
  void clear() {
    _map.clear();
    inverse._map.clear();
  }

  @override
  Iterable<K> get keys => _map.keys;

  @override
  V? remove(Object? key) {
    if (key == null) return null;
    final V? value = _map[key];
    inverse._map.remove(value);
    return _map.remove(key);
  }
}
