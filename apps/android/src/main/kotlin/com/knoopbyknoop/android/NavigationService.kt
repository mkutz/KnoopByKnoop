package com.knoopbyknoop.android

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ServiceCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.knoopbyknoop.core.Fix
import com.knoopbyknoop.core.Navigation
import com.knoopbyknoop.core.NavigationUpdate
import com.knoopbyknoop.core.Precision
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Runs the navigation while the screen is off or other apps are in use, see `knot-navigation`
 * "Navigation with the screen off": a foreground service of type location, started from the visible
 * app, so location access "while in use" is enough. It feeds the fixes to the core's [Navigation],
 * speaks the notices and shows the number to follow in an ongoing notification.
 */
class NavigationService : Service() {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
  private lateinit var locations: FusedLocationProviderClient
  private lateinit var speaker: Speaker
  private var route: NavigationRoute? = null
  private var navigation: Navigation? = null
  private var precision: Precision? = null
  private var stopping = false

  private val callback =
    object : LocationCallback() {
      override fun onLocationResult(result: LocationResult) {
        result.locations.forEach(::onLocation)
      }
    }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    locations = LocationServices.getFusedLocationProviderClient(this)
    speaker = Speaker(this)
    val manager = getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(
      // Default importance, so it is shown on the lock screen and with its Stop button; silent,
      // as the notices are spoken.
      NotificationChannel(CHANNEL, "Navigation", NotificationManager.IMPORTANCE_DEFAULT).apply {
        description = "The number to follow at the next knot while navigating"
        setSound(null, null)
        enableVibration(false)
      }
    )
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent?.action == ACTION_STOP) {
      stop()
      return START_NOT_STICKY
    }
    if (route != null) return START_NOT_STICKY
    val newRoute =
      Navigating.route
        ?: run {
          stopSelf()
          return START_NOT_STICKY
        }
    route = newRoute
    ServiceCompat.startForeground(
      this,
      NOTIFICATION_ID,
      notification("Starting navigation…"),
      ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
    )
    Navigating.update(NavigationState(navigation = null, position = null))
    // The first fix decides where navigation begins, so it is sampled precisely.
    request(Precision.FINE)
    scope.launch {
      Navigating.visible.drop(1).collect { visible ->
        navigation?.let { request(it.precision(visible)) }
      }
    }
    return START_NOT_STICKY
  }

  override fun onDestroy() {
    locations.removeLocationUpdates(callback)
    speaker.shutdown()
    scope.cancel()
    Navigating.update(null)
    super.onDestroy()
  }

  private fun onLocation(location: Location) {
    if (stopping) return
    val fix =
      Fix(location.latitude, location.longitude, location.accuracy.toDouble(), location.time)
    val visible = Navigating.visible.value
    val current = navigation
    val update =
      if (current != null) current.update(fix, visible)
      else {
        val newRoute = route ?: return
        Navigation.start(newRoute.knots, newRoute.graph, newRoute.geometry, fix, visible)
          ?: return stop()
      }
    apply(update, fix)
  }

  private fun apply(update: NavigationUpdate, fix: Fix) {
    val newNavigation = update.navigation
    navigation = newNavigation
    update.notices.forEach {
      Log.i(LOG_TAG, "notice: ${it.text}")
      speaker.speak(it.speech)
    }
    Navigating.update(NavigationState(newNavigation, fix))
    if (newNavigation.arrived) {
      // "You've arrived" is spoken before navigation ends, see `knot-navigation` "Arriving".
      stopping = true
      locations.removeLocationUpdates(callback)
      speaker.whenSilent { scope.launch { stop() } }
      return
    }
    val display = newNavigation.display
    getSystemService(NotificationManager::class.java)
      .notify(
        NOTIFICATION_ID,
        notification(if (display.offRoute) display.offRouteText else display.text),
      )
    request(update.precision)
  }

  /** Samples with [newPrecision], mapped as in the design's precision table. */
  @SuppressLint("MissingPermission") // Requested by the planner before starting the service.
  private fun request(newPrecision: Precision) {
    if (newPrecision == precision) return
    precision = newPrecision
    Log.i(LOG_TAG, "precision: $newPrecision")
    val request =
      when (newPrecision) {
        // High accuracy, but rarely: balanced power uses Wi-Fi and cell towers only and can
        // deliver no fix at all, e.g. in the countryside, and the service cannot wake up to ask
        // again without one. See the design's precision table.
        Precision.COARSE ->
          LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 30_000L).build()
        Precision.FINE -> LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3_000L).build()
        Precision.LIVE -> LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1_000L).build()
      }
    try {
      locations.requestLocationUpdates(request, callback, Looper.getMainLooper())
    } catch (exception: SecurityException) {
      stop()
    }
  }

  private fun stop() {
    stopping = true
    locations.removeLocationUpdates(callback)
    Navigating.update(null)
    ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    stopSelf()
  }

  private fun notification(title: String): Notification {
    val open =
      PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE,
      )
    val stop =
      PendingIntent.getService(
        this,
        1,
        Intent(this, NavigationService::class.java).setAction(ACTION_STOP),
        PendingIntent.FLAG_IMMUTABLE,
      )
    return Notification.Builder(this, CHANNEL)
      .setSmallIcon(R.drawable.ic_navigation)
      .setContentTitle(title)
      .setContentIntent(open)
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .setCategory(Notification.CATEGORY_NAVIGATION)
      .setVisibility(Notification.VISIBILITY_PUBLIC)
      .addAction(Notification.Action.Builder(null, "Stop", stop).build())
      .build()
  }

  companion object {
    private const val CHANNEL = "navigation"

    /**
     * Notices and precision changes are logged, to check navigation on a simulated ride and to tune
     * the precision for the battery. Read them with `adb logcat -s Navigation`.
     */
    private const val LOG_TAG = "Navigation"
    private const val NOTIFICATION_ID = 1
    private const val ACTION_STOP = "com.knoopbyknoop.android.STOP_NAVIGATION"

    /** Starts navigating [route]; call while the app is visible. */
    fun start(context: Context, route: NavigationRoute) {
      Navigating.route = route
      context.startForegroundService(Intent(context, NavigationService::class.java))
    }

    fun stop(context: Context) {
      context.startService(Intent(context, NavigationService::class.java).setAction(ACTION_STOP))
    }
  }
}
