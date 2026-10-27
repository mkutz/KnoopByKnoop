package com.knoopbyknoop.android

import android.os.Process
import android.os.SystemClock
import android.util.Log

/**
 * Logs when the map, the knots and the courses are shown for the first time after the process
 * started, to check the startup times of `map-planning`. Read them with `adb logcat -s Startup`.
 */
object StartupMarks {

  private val marked = mutableSetOf<String>()

  fun isMarked(name: String): Boolean = name in marked

  fun mark(name: String) {
    if (!marked.add(name)) return
    val millis = SystemClock.elapsedRealtime() - Process.getStartElapsedRealtime()
    Log.i("Startup", "startup: $name $millis ms")
  }
}
