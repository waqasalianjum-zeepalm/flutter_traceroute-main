import Flutter
import UIKit

public class FlutterTraceroutePlugin: NSObject, FlutterPlugin {
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "flutter_traceroute", binaryMessenger: registrar.messenger())
    let instance = FlutterTraceroutePlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

  public func doTraceRouteAsync(server: String, count: Int, useIcmp: Bool) {
    print("traceroute", server, count, useIcmp)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    switch call.method {
    case "traceroute":
        if let args = call.arguments as? [String: Any] {
            let server: String? = args["server"] as? String
            let count: Int? = args["count"] as? Int
            let useIcmp: Bool? = args["useIcmp"] as? Bool
            doTraceRouteAsync(server: server!, count: count!, useIcmp: useIcmp!)
            result("ok")
        } else {
            result(FlutterError.init(code: "BAD_ARGS",
                     message: "Invalid parameters",
                     details: nil))
        }
    default:
      result(FlutterMethodNotImplemented)
    }
  }
}
