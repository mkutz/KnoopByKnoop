package com.knoopbyknoop.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import com.knoopbyknoop.core.GeoPoint
import com.knoopbyknoop.core.PathFile
import com.knoopbyknoop.core.PathRouter
import java.io.File
import java.io.RandomAccessFile
import kotlin.concurrent.thread

/**
 * Debug builds only: measures a way on the device, see `add-offline-paths` task 2.3. Run with `adb
 * shell am broadcast -n com.knoopbyknoop/.android.PathMeasureReceiver --es from 50.79,7.20 --es to
 * 50.75,7.11` and read `adb logcat -s PathMeasure`.
 */
class PathMeasureReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    val pending = goAsync()
    val file = File(context.getExternalFilesDir(null), "cycling.paths")
    fun point(key: String) =
      intent.getStringExtra(key)!!.split(",").let { GeoPoint(it[0].toDouble(), it[1].toDouble()) }
    val from = point("from")
    val to = point("to")
    thread {
      try {
        val runtime = Runtime.getRuntime()
        fun used() = (runtime.totalMemory() - runtime.freeMemory()) / 1_000_000
        runtime.gc()
        val before = used()
        val access = RandomAccessFile(file, "r")
        val router =
          PathRouter(
            PathFile.open { offset, size ->
              access.seek(offset)
              ByteArray(size).also(access::readFully)
            }
          )
        repeat(2) { attempt ->
          val started = SystemClock.elapsedRealtime()
          val way = router.way(from, to)
          val millis = SystemClock.elapsedRealtime() - started
          Log.i(
            "PathMeasure",
            "${if (attempt == 0) "cold" else "warm"}: ${way?.lengthMeters?.toInt()} m, " +
              "$millis ms, ${router.visitedSteps} steps, ${router.loadedSquares} squares, " +
              "memory ${used() - before} MB more, ${way?.turns?.size} turns: " +
              way?.turns?.joinToString { "${it.atMeters.toInt()} ${it.direction} ${it.name}" },
          )
        }
        runtime.gc()
        Log.i("PathMeasure", "after collecting: memory ${used() - before} MB more")
        access.close()
      } finally {
        pending.finish()
      }
    }
  }
}
