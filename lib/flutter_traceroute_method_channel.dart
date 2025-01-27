import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'flutter_traceroute_platform_interface.dart';

/// An implementation of [FlutterTraceroutePlatform] that uses method channels.
class MethodChannelFlutterTraceroute extends FlutterTraceroutePlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('flutter_traceroute');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }

  @override
  Future<int?> traceroute(String server, int count, bool useIcmp) async {
    final result = await methodChannel.invokeMethod<int?>('traceroute',
        {'server': server, 'count': count, 'useIcmp': useIcmp}
    );
    return result;
  }

}
