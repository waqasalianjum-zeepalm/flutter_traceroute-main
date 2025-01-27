import 'flutter_traceroute_platform_interface.dart';

class FlutterTraceroute {

Future<int?> traceroute(String server, int count, bool useIcmp) {
    return FlutterTraceroutePlatform.instance.traceroute(server, count, useIcmp);
  }

}
