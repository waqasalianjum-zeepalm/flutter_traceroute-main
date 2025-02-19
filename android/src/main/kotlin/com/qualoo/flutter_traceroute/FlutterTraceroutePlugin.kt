package com.qualoo.flutter_traceroute

import androidx.annotation.NonNull

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.EventChannel

import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import com.qualoo.flutter_traceroute.TraceRoute
import android.util.Log

/** FlutterTraceroutePlugin */
class FlutterTraceroutePlugin: FlutterPlugin, MethodCallHandler {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var channel : MethodChannel
  private lateinit var eventChannel : EventChannel
  private var eventSink : EventChannel.EventSink? = null

  override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_traceroute")
    channel.setMethodCallHandler(this)

    eventChannel = EventChannel(flutterPluginBinding.binaryMessenger, "flutter_traceroute_events")
    eventChannel.setStreamHandler(object : EventChannel.StreamHandler {
      override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        eventSink = events!!
      }

      override fun onCancel(arguments: Any?) {
        eventSink = null
      }
    })
  }

  override fun onMethodCall(call: MethodCall, result: Result) {
    if (call.method == "traceroute") {
      val server:String? = call.argument<String>("server");
      val count:Int? = call.argument<Int>("count");
      val useIcmp:Boolean? = call.argument<Boolean>("useIcmp");
      Log.d("MyTag", "receiving arg=" + server + " " + count.toString() + " icmp=" + useIcmp);
      if (server != null && count != null && useIcmp != null) {
        doTraceRouteAsync(server, count, useIcmp);
        result.success(0);
      } else {
        result.error("-1", "server needed", null);
      }
    } else {
      result.notImplemented()
    }
  }

  override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

  private fun doTraceRouteAsync(host: String, count: Int, useIcmp: Boolean) {
    val maxTtl = 20
    val maxSimultaneous = 64
    val params = arrayOf("-m", maxTtl.toString(), "-N", maxSimultaneous.toString(), "-q", count.toString(), if (useIcmp) "-I" else "", host)
    Log.d("MyTag", "doTraceRouteAsync=" + params);

    val result = TraceRoute.traceRouteAsync(params) { output ->
      Log.d("MyTag", "traceroute async res=" + output)
      val outputWithServer = output.toMutableMap()
      outputWithServer["host"] = host
      eventSink?.success(outputWithServer)
    }
  }
}
