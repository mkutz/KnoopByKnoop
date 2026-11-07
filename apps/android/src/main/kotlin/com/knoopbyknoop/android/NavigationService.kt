package com.knoopbyknoop.android

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Icon
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.ServiceCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.knoopbyknoop.core.FerryArrivalState
import com.knoopbyknoop.core.Fix
import com.knoopbyknoop.core.GuidedWay
import com.knoopbyknoop.core.KnotPlate
import com.knoopbyknoop.core.KnotRole
import com.knoopbyknoop.core.Language
import com.knoopbyknoop.core.Navigation
import com.knoopbyknoop.core.NavigationUpdate
import com.knoopbyknoop.core.PathRouter
import com.knoopbyknoop.core.Precision
import com.knoopbyknoop.core.ProgressMarkKind
import com.knoopbyknoop.core.RiddenTrack
import com.knoopbyknoop.core.WayGuide
import com.knoopbyknoop.core.WaySearch
import java.time.Instant
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Runs the navigation while the screen is off or other apps are in use, see `knot-navigation`
 * "Navigation with the screen off": a foreground service of type location, started from the visible
 * app, so location access "while in use" is enough. It feeds the fixes to the core's [Navigation],
 * speaks the notices and shows the number to follow in an ongoing notification.
 */
/**
 * What the notification shows, compared to notify only on a change.
 *
 * @property knot the knot to look for: the next one, close to it the number to follow there
 * @property knotPosition its position on the route
 * @property place the rider's place on the bar, from 0 to [PROGRESS_SCALE], in steps of 1%
 * @property marks the stops and ferries ahead on the bar, each its place and color
 * @property end the number of the route's end
 */
private data class NotificationContent(
  val title: String,
  val text: String,
  val knot: String,
  val knotPosition: Int,
  val place: Int,
  val offRoute: Boolean,
  val marks: List<Pair<Int, Int>>,
  val end: String,
) {

  fun knotRole(route: NavigationRoute?): KnotRole? {
    val knots = route?.knots ?: return null
    return when {
      knotPosition == knots.lastIndex -> KnotRole.END
      knotPosition in route.stops -> KnotRole.STOP
      else -> KnotRole.PASSED
    }
  }

  /** The plate of the knot to look for, see `map-planning` "Route knots stand out". */
  fun knotPlate(route: NavigationRoute?): KnotPlate =
    KnotPlate.of(route?.knots?.getOrNull(knotPosition)?.signRegion)

  companion object {

    fun of(navigation: Navigation, fix: Fix, guidedWay: GuidedWay?): NotificationContent {
      val display = navigation.display
      val strip = navigation.progress(fix, guidedWay).strip
      // A route point has no signs to follow, see `knot-navigation` "Navigating with route points".
      val toFollow =
        display.close &&
          !display.offRoute &&
          !display.wrongWay &&
          display.follow != null &&
          display.nextRole == null &&
          display.followRole == null
      fun onBar(fraction: Double) = (fraction * PROGRESS_SCALE).roundToInt()
      val ranked =
        strip.marks.mapNotNull { mark ->
          when {
            mark.fraction <= strip.rider -> null
            mark.kind == ProgressMarkKind.FERRY && mark.state == FerryArrivalState.WARNING ->
              // Dark, not red, so it never reads like a German knot.
              Triple(0, onBar(mark.fraction), Ink.toArgb())
            mark.kind == ProgressMarkKind.STOP ->
              Triple(1, onBar(mark.fraction), mark.plate.color.toArgb())
            mark.kind == ProgressMarkKind.FERRY ->
              Triple(2, onBar(mark.fraction), android.graphics.Color.BLUE)
            else -> null
          }
        }
      return NotificationContent(
        title = if (display.offRoute) display.offRouteText else display.text,
        text = guidedWay?.view(navigation.language)?.line ?: navigation.progressLine,
        knot = if (toFollow) display.follow.orEmpty() else display.next,
        knotPosition = navigation.nextIndex + if (toFollow) 1 else 0,
        place = onBar(strip.rider) / (PROGRESS_SCALE / 100) * (PROGRESS_SCALE / 100),
        offRoute = display.offRoute || display.wrongWay,
        marks = ranked.sortedBy { it.first }.take(MAX_MARKS).map { it.second to it.third },
        end = navigation.knots.last().ref,
      )
    }
  }
}

/** The notification's bar counts in steps of this, see "Navigation with the screen off". */
private const val PROGRESS_SCALE = 1000

/** The most marks on the bar, the most important first: API 37 draws no more. */
private const val MAX_MARKS = 4

/** A dot for the rider on the notification's bar, dark off route as on the strip. */
private fun riderBitmap(offRoute: Boolean): Bitmap {
  val bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
  val canvas = Canvas(bitmap)
  val paint = Paint(Paint.ANTI_ALIAS_FLAG)
  paint.color = android.graphics.Color.WHITE
  canvas.drawCircle(24f, 24f, 22f, paint)
  paint.color = if (offRoute) Ink.toArgb() else 0xFF1E88E5.toInt()
  canvas.drawCircle(24f, 24f, 16f, paint)
  return bitmap
}

class NavigationService : Service() {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
  private lateinit var locations: FusedLocationProviderClient
  private lateinit var speaker: Speaker
  private var route: NavigationRoute? = null
  private var navigation: Navigation? = null

  /** What the notification shows now. */
  private var notified: NotificationContent? = null
  /** Kept here, not in the map, so it includes the stretches ridden with the screen off. */
  private var track = RiddenTrack()
  private var precision: Precision? = null
  private var stopping = false

  /** The way back to the route, see `knot-navigation` "Guided way back to the route". */
  private val wayGuide = WayGuide()
  private val guidedWay: GuidedWay?
    get() = wayGuide.way

  private val pathStore by lazy { PathData.store(this) }
  private val pathRouter by lazy { PathRouter(pathStore) }
  private var pathRouterVersion = -1
  private val debuggable by lazy { applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0 }

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
    createChannel()
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
      notification(getString(R.string.starting_navigation)),
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
        Navigation.start(
          newRoute.knots,
          newRoute.graph,
          newRoute.geometry,
          fix,
          visible,
          newRoute.stops,
          { ferryMoment(Instant.ofEpochMilli(it)) },
          // The texts in the app's language, the notices in the language there is a voice for, see
          // `app-languages` "Spoken notices in the language".
          Language.of(resources.configuration.locales[0].language),
          speaker.language,
        ) ?: return stop()
      }
    apply(update, fix)
  }

  /**
   * The channel of the navigation's notification, named in the app's language; created again when
   * the language changes, which renames it, see `app-languages` "Language setting".
   */
  private fun createChannel() {
    val manager = getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(
      // Default importance, so it is shown on the lock screen and with its Stop button; silent,
      // as the notices are spoken.
      NotificationChannel(
          CHANNEL,
          getString(R.string.channel_navigation),
          NotificationManager.IMPORTANCE_DEFAULT,
        )
        .apply {
          description = getString(R.string.channel_description)
          setSound(null, null)
          enableVibration(false)
        }
    )
  }

  /** Navigation follows the language at once, see `app-languages`. */
  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    createChannel()
    val current = navigation ?: return
    val locale = newConfig.locales[0]
    val language = Language.of(locale.language)
    if (language == current.language) return
    speaker.useLocale(locale)
    val fix = current.lastFix
    val newNavigation = current.inLanguage(language, speaker.language, fix)
    Log.i(LOG_TAG, "language: $language, spoken in ${speaker.language}")
    navigation = newNavigation
    publish(newNavigation, fix)
    if (!newNavigation.arrived) notifyOfChange(newNavigation, fix)
  }

  private fun apply(update: NavigationUpdate, fix: Fix) {
    val newNavigation = update.navigation
    navigation = newNavigation
    val step =
      wayGuide.onFix(
        newNavigation,
        update.notices,
        fix,
        route?.graph?.network,
        track.directionAt(fix),
        SystemClock.elapsedRealtime(),
        speaker.language,
      )
    step.search?.let(::findWay)
    step.notices.forEach {
      Log.i(LOG_TAG, "notice: ${it.text}, spoken in ${speaker.language}: ${it.speech}")
      speaker.speak(it.speech)
    }
    // The route's network sets the speed a fix may not exceed, see `knot-navigation` "Ridden
    // track".
    route?.graph?.network?.takeIf { it != track.network }?.let { track = track.copy(network = it) }
    track = track.add(fix)
    publish(newNavigation, fix)
    if (newNavigation.arrived) {
      // "You've arrived" is spoken before navigation ends, see `knot-navigation` "Arriving".
      stopping = true
      locations.removeLocationUpdates(callback)
      speaker.whenSilent { scope.launch { stop() } }
      return
    }
    notifyOfChange(newNavigation, fix)
    request(update.precision)
  }

  private fun publish(newNavigation: Navigation, fix: Fix) {
    Navigating.update(NavigationState(newNavigation, fix, track, track.directionAt(fix), guidedWay))
  }

  /** Looks for a way off the main thread, see [WayGuide]. */
  private fun findWay(search: WaySearch) {
    scope.launch {
      val found =
        withContext(Dispatchers.Default) {
          val router = pathRouter
          // Squares may have arrived or gone, see `knot-navigation` "Path data arrives on the way".
          if (pathRouterVersion != pathStore.version) {
            router.forgetSquares()
            pathRouterVersion = pathStore.version
          }
          val started = SystemClock.elapsedRealtime()
          val way =
            try {
              router.way(search.from, search.to, search.heading)
            } catch (exception: Exception) {
              Log.w(LOG_TAG, "no guided way", exception)
              null
            } catch (error: OutOfMemoryError) {
              // A long way in a dense area can take a lot, see `path-routing` "No way found".
              router.forgetSquares()
              Log.w(LOG_TAG, "no guided way: out of memory", error)
              null
            }
          val runtime = Runtime.getRuntime()
          Log.i(
            LOG_TAG,
            "guided way: ${way?.lengthMeters?.toInt()} m, ${way?.turns?.size} turns, " +
              "${SystemClock.elapsedRealtime() - started} ms, ${router.visitedSteps} steps, " +
              "${router.loadedSquares} squares, " +
              "${(runtime.totalMemory() - runtime.freeMemory()) / 1_000_000} MB used",
          )
          // Debug builds only, for a mocked ride to follow the way: this is where the rider goes.
          way
            ?.takeIf { debuggable }
            ?.let { found ->
              Log.d(
                LOG_TAG,
                "way points: " +
                  found.points.joinToString(";") { "${it.longitude},${it.latitude}" },
              )
            }
          way
        }
      val latest = navigation
      wayGuide.found(found, latest.takeUnless { stopping })
      if (stopping || latest == null || !latest.offRoute || latest.arrived) return@launch
      publish(latest, latest.lastFix)
      notifyOfChange(latest, latest.lastFix)
    }
  }

  private fun notifyOfChange(newNavigation: Navigation, fix: Fix) {
    val content = NotificationContent.of(newNavigation, fix, guidedWay)
    // Only on a change: the position comes every second while the app is visible.
    if (content != notified) {
      notified = content
      getSystemService(NotificationManager::class.java)
        .notify(NOTIFICATION_ID, notification(content.title, content.text, content))
    }
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

  /** [text] is the progress line, see `knot-navigation` "Navigation with the screen off". */
  private fun notification(
    title: String,
    text: String? = null,
    content: NotificationContent? = null,
  ): Notification {
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
      .also { builder ->
        content?.let {
          builder.setLargeIcon(
            Icon.createWithBitmap(badgeBitmap(it.knotRole(route), it.knot, it.knotPlate(route)))
          )
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) liveUpdate(builder, it)
        }
      }
      .setSmallIcon(R.drawable.ic_navigation)
      .setContentTitle(title)
      .setContentText(text)
      .setContentIntent(open)
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .setCategory(Notification.CATEGORY_NAVIGATION)
      .setVisibility(Notification.VISIBILITY_PUBLIC)
      .addAction(Notification.Action.Builder(null, getString(R.string.stop), stop).build())
      .build()
  }

  /** The route's progress, and the chip over other apps, see "Navigation with the screen off". */
  @RequiresApi(Build.VERSION_CODES.BAKLAVA)
  private fun liveUpdate(builder: Notification.Builder, content: NotificationContent) {
    // Promotion and the chip came with Android 16 QPR2, after the progress style.
    if (Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.BAKLAVA_1) {
      builder.setRequestPromotedOngoing(true).setShortCriticalText(content.knot)
    }
    builder.setStyle(
      Notification.ProgressStyle()
        .setStyledByProgress(true)
        .setProgressSegments(
          listOf(Notification.ProgressStyle.Segment(PROGRESS_SCALE).setColor(Green.toArgb()))
        )
        .setProgressPoints(
          content.marks.map { (place, color) ->
            Notification.ProgressStyle.Point(place).setColor(color)
          }
        )
        .setProgress(content.place)
        .setProgressTrackerIcon(Icon.createWithBitmap(riderBitmap(content.offRoute)))
        .setProgressEndIcon(
          Icon.createWithBitmap(
            badgeBitmap(
              KnotRole.END,
              content.end,
              KnotPlate.of(route?.knots?.lastOrNull()?.signRegion),
            )
          )
        )
    )
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
