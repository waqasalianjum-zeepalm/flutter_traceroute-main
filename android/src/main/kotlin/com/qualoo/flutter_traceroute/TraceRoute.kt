package com.qualoo.flutter_traceroute

import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlin.math.sqrt

object TraceRoute {

    init {
        System.loadLibrary("traceroute")
    }
    private var handler: Handler = Handler(Looper.getMainLooper())

    data class TracerouteSample(val hop: Int, val ip: String, val ms: Double)

    data class TracerouteResult(
        val id: Long,
        val avg: Double, val stdDev: Double, val best: Double, val worst: Double, val sent: Int, val lostPct: Double)

    class TracerouteStats(val key: Long, val samples: MutableList<TracerouteSample> = mutableListOf()) {
        var lastHop = 0
        fun addSample(sample: TracerouteSample) {
            samples.add(sample)
            lastHop = sample.hop
        }
        fun computeHop():Map<String,Any> {
            // Compute a record like: host, ipAddress, lostPct, sent, avg, best, worst, stdDev
            val sent = samples.size
            val validSamples = samples.filter { it.ip != "*"}.toTypedArray()
            // Nothing to compute on this hop
            if (validSamples.size == 0) {
                samples.clear()
                return mapOf(
                    "event" to "update",
                    "id" to key,
                    "hop" to lastHop,
                    "avg" to 0.0,
                    "stdDev" to 0.0,
                    "best" to 0.0,
                    "worst" to 0.0,
                    "sent" to samples.size,
                    "lostPct" to 1.0
                )
            }
            var sum = 0.0
            var best = 1000000.0
            var worst = 0.0
            for (sample in validSamples) {
                sum += sample.ms
                if (sample.ms < best) {
                    best = sample.ms
                }
                if (sample.ms > worst) {
                    worst = sample.ms
                }
            }
            val lostPct = (1.0 * (samples.size - validSamples.size)) / samples.size
            var avg = sum / validSamples.size
            var stdDev = sqrt(validSamples.map { (it.ms - avg) * (it.ms - avg) }.sum() / validSamples.size)
            var ip = validSamples[0].ip
            samples.clear()
            return mapOf(
                "event" to "update",
                "id" to key,
                "ip" to ip,
                "hop" to lastHop,
                "avg" to sum,
                "stdDev" to stdDev,
                "best" to best,
                "worst" to worst,
                "sent" to sent,
                "lostPct" to lostPct
            )
        }

    }
    val instanceMap: MutableMap<Long, TracerouteStats> = mutableMapOf()
    var _callback : ((Map<String, Any>) -> Unit) ?= null

    fun updateStats(hop: Int, ip: String, ms: Double) {
        Log.d("MyTag", "updateStats=");
    }
    // call appendResult from jni, don't confusion this method
    fun appendResult(text: String) {
        Log.d("MyTag", "appendResult=" + text);
        if (text[0] != '#') return
        val parts = text.split(", ");
        Log.d("MyTag", "parts[0]=" + parts[0])
        when (parts[0]) {
            "#start" -> {
                try {
                    val uniqueId = parts[1].toLong()
                    instanceMap[uniqueId] = TracerouteStats(uniqueId)
                    Log.i("MyTag", "started traceroute id=" + uniqueId);
                    handler.post { _callback?.invoke(mapOf("event" to "start")) }
                } catch (e: Exception) {
                    Log.i("MyTag", "failed to parse" + parts[1]);
                }
            }
            "#progress" -> {
                try {
                    // hop, IP, pingMs "#%d, %s, %.3f\n"
                    val uniqueId = parts[1].toLong()
                    val hopNumber = parts[2].toInt()
                    val ip = parts[3]
                    val pingMs = parts[4].toDouble()
                    val stats = instanceMap[uniqueId]
                    if (stats != null) {
                        if (stats.lastHop == hopNumber) {
                            Log.i("MyTag", "Adding same hop stat " + uniqueId);
                            stats.addSample(TracerouteSample(hopNumber, ip, pingMs))
                        } else {
                            val hopInfo = stats.computeHop()
                            Log.i("MyTag", "computing and pushing stats " + uniqueId);
                            handler.post { _callback?.invoke(hopInfo) }
                            stats.addSample(TracerouteSample(hopNumber, ip, pingMs))
                        }
                    } else {
                        // odd we did not find this key!
                        Log.i("MyTag", "key not found " + uniqueId);
                    }
                } catch (e: Exception) {
                    // emit error?
                    Log.i("MyTag", "cannot parse e=" + e);
                }
            }
            "#end" -> {
                try {
                    val uniqueId = parts[1].toLong()
                    Log.i("MyTag", "ended traceroute id=" + uniqueId);
                    instanceMap.remove(uniqueId)
                    handler.post { _callback?.invoke(mapOf("event" to "end")) }
                } catch (e: Exception) {
                    Log.i("MyTag", "failed to parse" + parts[1]);
                }
            }
        }

        if (text.length == 1) {
            // We are done...
            //handler.post { _callback?.invoke(stats) }
        }
    }
    fun clearResult() {
        Log.d("MyTag", "clearResult");
    }

    @Synchronized
    fun traceRouteAsync(userArgs: Array<String>, callback: (Map<String,Any>) -> Unit):Int {
        _callback = callback
        val args = arrayOf("traceroute") + userArgs
        Log.d("MyTag", "traceRouteAsync=" + args.contentToString());
        Thread({
            execute(args)
               }, "trace_route_thread").start()
        return 0
    }

    /**
     * JNI interface.
     *
     * @param args traceroute commands args
     * @return execute result code
     */
    external fun execute(args: Array<String>): Int

}