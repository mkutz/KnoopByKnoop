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
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.ServiceCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.knoopbyknoop.core.BarMarkKind
import com.knoopbyknoop.core.Fix
import com.knoopbyknoop.core.GuidedWay
import com.knoopbyknoop.core.HeaderBadge
import com.knoopbyknoop.core.HeaderRow
import com.knoopbyknoop.core.KnotPlate
import com.knoopbyknoop.core.KnotRole
import com.knoopbyknoop.core.Language
import com.knoopbyknoop.core.Navigation
import com.knoopbyknoop.core.NavigationRoute
import com.knoopbyknoop.core.NavigationSession
import com.knoopbyknoop.core.NavigationState
import com.knoopbyknoop.core.NetworkGeometry
import com.knoopbyknoop.core.Precision
import com.knoopbyknoop.core.SessionEnd
import com.knoopbyknoop.core.SessionStep
import com.knoopbyknoop.core.TurnDirection
import com.knoopbyknoop.core.WayFinder
import com.knoopbyknoop.core.WayGuide
import com.knoopbyknoop.core.WaySearch
import com.knoopbyknoop.core.barMarks
import com.knoopbyknoop.core.outside
import java.time.Instant
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Runs the navigation while the screen is off or other apps are in use, see `knot-navigation`
 * "Navigation with the screen off": a foreground service of type location, started from the visible
 * app, so location access "while in use" is enough. It feeds the fixes to the core's [Navigation],
 * speaks the notices and shows the number to follow in an ongoing notification.
 */
/**
 * The notification's large icon, see `knot-navigation` "Navigation with the screen off": the knot
 * to look for, on its sign plate close to it, or a symbol on a disc off route, on the way to the
 * route and riding the wrong way.
 */
private sealed class LargeIcon {
  data class Knot(val badge: HeaderBadge, val onPlate: Boolean, val turn: TurnDirection? = null) :
    LargeIcon()

  data class Symbol(@DrawableRes val icon: Int, val dark: Boolean) : LargeIcon()
}

/**
 * What the notification shows, compared to notify only on a change.
 *
 * @property title numbers, arrow characters and distances, the same in every language
 * @property text the second line: the distance close to a knot and the end arrival
 * @property chip the status chip's text, at most 7 characters
 * @property place the rider's place on the bar, from 0 to [PROGRESS_SCALE], in steps of 1%
 * @property marks the stops and ferries ahead on the bar, each its place and color
 * @property end the number of the route's end
 */
private data class NotificationContent(
  val title: String,
  val text: String,
  val chip: String,
  @DrawableRes val smallIcon: Int,
  val largeIcon: LargeIcon,
  val place: Int,
  val offRoute: Boolean,
  val marks: List<Pair<Int, Int>>,
  val end: String,
) {

  companion object {

    fun of(
      navigation: Navigation,
      fix: Fix,
      guidedWay: GuidedWay?,
      direction: Double?,
      geometry: NetworkGeometry?,
    ): NotificationContent {
      val strip = navigation.progress(fix, guidedWay).strip
      val row = navigation.header(fix, direction, guidedWay, geometry)
      val outside = row.outside(navigation.endArrival(fix, guidedWay))
      fun onBar(fraction: Double) = (fraction * PROGRESS_SCALE).roundToInt()
      val marks =
        strip.barMarks(MAX_MARKS).map { mark ->
          onBar(mark.fraction) to
            when (mark.kind) {
              // Dark, not red, so it never reads like a German knot.
              BarMarkKind.CLOSED_FERRY -> Ink.toArgb()
              BarMarkKind.STOP -> mark.mark.plate.color.toArgb()
              BarMarkKind.FERRY -> android.graphics.Color.BLUE
            }
        }
      val (smallIcon, largeIcon) =
        when (row) {
          is HeaderRow.Close ->
            R.drawable.ic_navigation to LargeIcon.Knot(row.follow, true, row.turn)
          is HeaderRow.Far -> R.drawable.ic_navigation to LargeIcon.Knot(row.next, false)
          is HeaderRow.End -> R.drawable.ic_navigation to LargeIcon.Knot(row.next, false)
          is HeaderRow.WrongWay ->
            R.drawable.ic_u_turn to LargeIcon.Symbol(R.drawable.ic_u_turn, dark = true)
          is HeaderRow.ToRoute ->
            R.drawable.ic_to_route to LargeIcon.Symbol(R.drawable.ic_to_route, dark = row.offRoute)
        }
      return NotificationContent(
        title = outside.title,
        // At a doubtful junction close ahead, its sentence, as the notification has no room for its
        // picture, see `knot-navigation` "Text on the Android lock screen".
        text = (row as? HeaderRow.ToRoute)?.guided?.pictureSentence ?: outside.line,
        chip = outside.chip,
        smallIcon = smallIcon,
        largeIcon = largeIcon,
        place = onBar(strip.rider) / (PROGRESS_SCALE / 100) * (PROGRESS_SCALE / 100),
        offRoute = navigation.offRoute || navigation.wrongWay,
        marks = marks,
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

  /** The navigation running, see [NavigationSession]; `null` once stopped. */
  private var session: NavigationSession? = null

  /** What the notification shows now. */
  private var notified: NotificationContent? = null
  private var precision: Precision? = null

  private val wayFinder by lazy { WayFinder(PathData.store(this)) }
  private val debuggable by lazy { applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0 }

  private val callback =
    object : LocationCallback() {
      override fun onLocationResult(result: LocationResult) {
        result.locations.forEach(::onLocation)
      }
    }

  override fun onBind(intent: Intent?): IBinder? = null

  /** A text in the app's language, also before Android 13, see [AppLocale]. */
  private fun text(id: Int): String = AppLocale.context(this).getString(id)

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
    session =
      NavigationSession(
        newRoute,
        { ferryMoment(Instant.ofEpochMilli(it)) },
        // The texts in the app's language, the notices in the language there is a voice for, see
        // `app-languages` "Spoken notices in the language".
        Language.of(AppLocale.of(this).language),
        speaker.language,
      )
    ServiceCompat.startForeground(
      this,
      NOTIFICATION_ID,
      notification(text(R.string.starting_navigation)),
      ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
    )
    session?.let { Navigating.update(it.state) }
    // The first fix decides where navigation begins, so it is sampled precisely.
    request(Precision.FINE)
    scope.launch {
      Navigating.visible.drop(1).collect { visible ->
        session?.state?.navigation?.let { request(it.precision(visible)) }
      }
    }
    // Courses loaded after the start, see `knot-navigation` "Courses loaded after the start".
    scope.launch {
      Navigating.lateCourses.filterNotNull().collect { geometry ->
        session?.let { act(it.withCourses(geometry)) }
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
    val current = session ?: return
    val fix =
      Fix(location.latitude, location.longitude, location.accuracy.toDouble(), location.time)
    act(current.onFix(fix, Navigating.visible.value, SystemClock.elapsedRealtime()))
  }

  /** Does what [step] says, see [NavigationSession]. */
  private fun act(step: SessionStep) {
    step.search?.let(::findWay)
    step.notices.forEach {
      Log.i(LOG_TAG, "notice: ${it.text}, spoken in ${speaker.language}: ${it.speech}")
      speaker.speak(it.speech)
    }
    step.state?.let(Navigating::update)
    when (step.end) {
      SessionEnd.NOW -> stop()
      SessionEnd.AFTER_SPEECH -> {
        // "You've arrived" is spoken before navigation ends, see `knot-navigation` "Arriving".
        locations.removeLocationUpdates(callback)
        speaker.whenSilent { scope.launch { stop() } }
      }
      SessionEnd.NONE -> {
        step.state?.let(::notifyOfChange)
        step.precision?.let(::request)
      }
    }
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
          text(R.string.channel_navigation),
          NotificationManager.IMPORTANCE_DEFAULT,
        )
        .apply {
          description = text(R.string.channel_description)
          setSound(null, null)
          enableVibration(false)
        }
    )
  }

  /**
   * Navigation follows the language at once, see `app-languages`. The chosen one, not the phone's:
   * before Android 13 a change of the phone's language reaches the service also when the rider
   * chose another in the app.
   */
  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    useLocale(AppLocale.of(this))
  }

  /**
   * Writes and speaks in [locale] from now on, see `app-languages` "Language change while
   * navigating". Does nothing for the language already in use.
   */
  private fun useLocale(locale: Locale) {
    createChannel()
    val current = session ?: return
    val language = Language.of(locale.language)
    if (language == current.language) return
    speaker.useLocale(locale)
    Log.i(LOG_TAG, "language: $language, spoken in ${speaker.language}")
    act(current.inLanguage(language, speaker.language))
  }

  /** Looks for a way off the main thread, see [WayGuide]. */
  private fun findWay(search: WaySearch) {
    scope.launch {
      val found =
        withContext(Dispatchers.Default) {
          val router = wayFinder
          val started = SystemClock.elapsedRealtime()
          // Other failures give no way already, see `path-routing` "Broken path data".
          val way =
            try {
              router.way(search)
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
      session?.let { act(it.onWay(found)) }
    }
  }

  private fun notifyOfChange(state: NavigationState) {
    val navigation = state.navigation ?: return
    val fix = state.position ?: return
    val content =
      NotificationContent.of(navigation, fix, state.guidedWay, state.direction, route?.geometry)
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
    session = null
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
          val large =
            when (val icon = it.largeIcon) {
              is LargeIcon.Knot ->
                if (icon.onPlate)
                  signPlateBitmap(
                    icon.badge,
                    icon.turn?.degrees?.toFloat(),
                    if (icon.turn == TurnDirection.BACK) getDrawable(R.drawable.ic_u_turn)
                    else null,
                  )
                else
                  badgeBitmap(
                    icon.badge.role,
                    icon.badge.ref,
                    icon.badge.plate,
                    filled = icon.badge.filled,
                  )
              is LargeIcon.Symbol -> symbolDiscBitmap(this, icon.icon, icon.dark)
            }
          builder.setLargeIcon(Icon.createWithBitmap(large))
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) liveUpdate(builder, it)
        }
      }
      .setSmallIcon(content?.smallIcon ?: R.drawable.ic_navigation)
      .setContentTitle(title)
      .setContentText(text)
      .setContentIntent(open)
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .setCategory(Notification.CATEGORY_NAVIGATION)
      .setVisibility(Notification.VISIBILITY_PUBLIC)
      .addAction(Notification.Action.Builder(null, text(R.string.stop), stop).build())
      .build()
  }

  /** The route's progress, and the chip over other apps, see "Navigation with the screen off". */
  @RequiresApi(Build.VERSION_CODES.BAKLAVA)
  private fun liveUpdate(builder: Notification.Builder, content: NotificationContent) {
    // Promotion and the chip came with Android 16 QPR2, after the progress style.
    if (Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.BAKLAVA_1) {
      builder.setRequestPromotedOngoing(true).setShortCriticalText(content.chip)
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
