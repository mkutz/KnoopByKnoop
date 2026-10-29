package com.knoopbyknoop.android

import android.graphics.RectF
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.knoopbyknoop.core.Fix
import com.knoopbyknoop.core.Knot
import com.knoopbyknoop.core.NetworkGraph
import com.knoopbyknoop.core.PlaceHit
import com.knoopbyknoop.core.RiddenTrack
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource

/**
 * OpenStreetMap-based base maps without API key, see the design of `add-planning-screens`, light or
 * dark like the system, see `map-planning` "Light and dark appearance".
 */
private const val BASE_MAP_STYLE = "https://tiles.openfreemap.org/styles/liberty"
private const val DARK_BASE_MAP_STYLE = "https://tiles.openfreemap.org/styles/dark"

/** The offline style's background, also shown before any style is loaded. */
private const val OFFLINE_LIGHT_BACKGROUND = 0xFFEEF1EA.toInt()
private const val OFFLINE_DARK_BACKGROUND = 0xFF1E2320.toInt()

/**
 * Used when the base map cannot be loaded, e.g. offline on first start: the network layers still
 * show. Offline, MapLibre waits for the network instead of failing, so it is used after a timeout.
 */
private fun offlineStyle(dark: Boolean): String {
  val background = if (dark) OFFLINE_DARK_BACKGROUND else OFFLINE_LIGHT_BACKGROUND
  val color = "#%06X".format(background and 0xFFFFFF)
  return """{"version": 8, "sources": {}, "layers": [{"id": "background", "type": "background", "paint": {"background-color": "$color"}}]}"""
}

/** How long the base map may take to load before the offline style is used. */
private const val BASE_MAP_TIMEOUT_MILLIS = 3_000L

/**
 * How long after falling back to the offline style the base map is loaded again, see `map-planning`
 * "Slow base map": setting the offline style cancelled its loading. MapLibre shows the offline
 * style until the base map has loaded.
 */
private const val BASE_MAP_RETRY_MILLIS = 2_000L

/** The zoom level a search hit is shown at: streets and knots around it are easy to tap. */
private const val SEARCH_HIT_ZOOM = 14.0

/** The zoom when navigation begins: close enough to see the next knot and the way to it. */
private const val NAVIGATION_ZOOM = 15.0

private const val MARKER_RED = "#D93025"

/** The rider's position, in the blue maps commonly use for it. */
private const val POSITION_BLUE = "#1A73E8"

/** The ridden track, a blue next to the position's, distinct from the orange route. */
private const val TRACK_BLUE = "#1E6FD9"

/** Knots are drawn and can be tapped from this zoom level on; below it the route is a line only. */
private const val KNOT_MIN_ZOOM = 10.0

/** The touch target around a tap within which the nearest knot is picked. */
private const val TOUCH_TARGET_DP = 24f

private const val GREEN = "#2E9E5B"
private const val ORANGE = "#F28C28"

/** A place on the map, saved when the app goes to the background, see `faster-startup`. */
data class CameraPlace(val latitude: Double, val longitude: Double, val zoom: Double)

/**
 * The map with the network and the route, and buttons to zoom. Taps are reported with the touch
 * target in meters at the current zoom level, so the core's knot locator can pick the knot.
 *
 * The map is shown before a network is loaded ([graph] and [layers] `null`), at [initialPlace] if
 * there is one, see `map-planning` "Map, knots and courses one after the other".
 */
@Composable
fun NetworkMap(
  graph: NetworkGraph?,
  layers: MapLayers?,
  focus: List<Knot>,
  onTap: (latitude: Double, longitude: Double, radiusMeters: Double) -> Unit,
  online: Boolean,
  initialPlace: CameraPlace?,
  onLeave: (CameraPlace) -> Unit,
  marker: PlaceHit?,
  onMove: (CameraPlace) -> Unit,
  modifier: Modifier = Modifier,
  /** The rider's position while navigating, see `knot-navigation` "Position on the map". */
  position: Fix? = null,
  /** The track ridden while navigating, see `knot-navigation` "Ridden track". */
  track: RiddenTrack? = null,
  /** The riding direction, see `knot-navigation` "Riding direction on the map". */
  direction: Double? = null,
  /** The next knot while navigating, highlighted. */
  nextKnot: Knot? = null,
  offRoute: Boolean = false,
) {
  val context = LocalContext.current
  val touchTargetPixels = with(LocalDensity.current) { TOUCH_TARGET_DP * density }
  val holder = remember { MapHolder() }
  val dark = isSystemInDarkTheme()
  holder.dark = dark
  holder.online = online
  val mapView = remember {
    MapLibre.getInstance(context)
    // Before any style is loaded, the map shows the offline style's background, not MapLibre's.
    val options =
      MapLibreMapOptions.createFromAttributes(context)
        .foregroundLoadColor(if (dark) OFFLINE_DARK_BACKGROUND else OFFLINE_LIGHT_BACKGROUND)
    MapView(context, options).apply {
      onCreate(null)
      addOnDidFinishRenderingFrameListener { _, _, _ -> holder.markVisibleLayers(width, height) }
      getMapAsync { map ->
        holder.map = map
        map.addOnCameraIdleListener { holder.place()?.let { holder.onMove(it) } }
        // Moving or zooming the map by hand stops following the rider; the app's own camera moves
        // don't.
        map.addOnCameraMoveStartedListener { reason ->
          if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) {
            holder.following = false
          }
        }
        map.addOnMapClickListener { point ->
          if (map.cameraPosition.zoom < KNOT_MIN_ZOOM) {
            // Zoomed out, knots are not shown: a tap on the route zooms to it.
            val screenPoint = map.projection.toScreenLocation(point)
            val area =
              RectF(
                screenPoint.x - touchTargetPixels,
                screenPoint.y - touchTargetPixels,
                screenPoint.x + touchTargetPixels,
                screenPoint.y + touchTargetPixels,
              )
            if (map.queryRenderedFeatures(area, "route").isNotEmpty()) holder.zoomToFocus()
            return@addOnMapClickListener true
          }
          val metersPerPixel = map.projection.getMetersPerPixelAtLatitude(point.latitude)
          holder.onTap(point.latitude, point.longitude, metersPerPixel * touchTargetPixels)
          true
        }
        initialPlace?.let {
          map.moveCamera(
            CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), it.zoom)
          )
          holder.hasPlace = true
        }
        addOnDidFailLoadingMapListener { holder.useOfflineStyle() }
        if (online) {
          holder.loadBaseMap()
          postDelayed(
            {
              if (!holder.hasStyle) {
                holder.useOfflineStyle()
                postDelayed(
                  { if (holder.usesOfflineStyle && holder.online) holder.loadBaseMap() },
                  BASE_MAP_RETRY_MILLIS,
                )
              }
            },
            BASE_MAP_TIMEOUT_MILLIS,
          )
        } else {
          // Offline, MapLibre would wait for the base map, and nothing can be drawn without a
          // style.
          holder.useOfflineStyle()
        }
      }
    }
  }
  holder.onTap = onTap
  holder.onMove = onMove
  holder.show(graph, layers, focus)
  // A chosen search hit: the map moves there, close enough to tap knots, see `place-search`.
  LaunchedEffect(marker) { holder.showMarker(marker) }
  LaunchedEffect(position, nextKnot) {
    holder.showNavigation(position, nextKnot, offRoute, direction)
  }
  LaunchedEffect(track) { holder.showTrack(track) }
  // The offline style has no base map; it is loaded as soon as network access is back.
  LaunchedEffect(online) { if (online && holder.usesOfflineStyle) holder.loadBaseMap() }
  // Switched in place, the camera stays, see `map-planning` "Switching while the app is open".
  LaunchedEffect(dark) { holder.reloadStyleIfAppearanceChanged() }

  val lifecycle = LocalLifecycleOwner.current.lifecycle
  DisposableEffect(lifecycle) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_START -> {
          mapView.onStart()
          holder.findRider = true
        }
        Lifecycle.Event.ON_RESUME -> mapView.onResume()
        Lifecycle.Event.ON_PAUSE -> mapView.onPause()
        Lifecycle.Event.ON_STOP -> {
          holder.place()?.let(onLeave)
          mapView.onStop()
        }
        else -> {}
      }
    }
    lifecycle.addObserver(observer)
    onDispose {
      lifecycle.removeObserver(observer)
      mapView.onDestroy()
    }
  }

  Box(modifier = modifier) {
    AndroidView(factory = { mapView })
    Column(modifier = Modifier.align(Alignment.CenterEnd).padding(12.dp)) {
      listOf("+" to 1.0, "−" to -1.0).forEach { (label, delta) ->
        SmallFloatingActionButton(
          onClick = { holder.zoomBy(delta) },
          containerColor = mapOverlayColor,
          contentColor = Green,
          modifier = Modifier.padding(vertical = 4.dp),
        ) {
          Text(label, fontSize = 22.sp)
        }
      }
      // Only while navigating and after the user moved the map away from the rider.
      if (position != null && !holder.following) {
        SmallFloatingActionButton(
          onClick = holder::recenter,
          containerColor = ActionColors.green,
          contentColor = Color.White,
          modifier = Modifier.padding(top = 12.dp),
        ) {
          Icon(painterResource(R.drawable.ic_action_recenter), contentDescription = "Recenter")
        }
      }
    }
  }
}

/** Keeps the map, its style and what is shown, so updates only touch what changed. */
private class MapHolder {
  var map: MapLibreMap? = null
  var onTap: (Double, Double, Double) -> Unit = { _, _, _ -> }
  private var style: Style? = null

  /** Whether a style is loaded, the base map or the offline style. */
  val hasStyle: Boolean
    get() = style != null

  var usesOfflineStyle = false
    private set

  /** Whether the system is in dark mode; the next style loaded follows it. */
  var dark = false

  /** Whether the device has network access, to load the base map again after falling back. */
  var online = false

  /** The appearance of the loaded style, to see whether it still matches [dark]. */
  private var styleDark: Boolean? = null

  private var graph: NetworkGraph? = null
  private var layers: MapLayers? = null
  private var focus: List<Knot> = listOf()
  private var shownGraph: NetworkGraph? = null
  private var shownLayers: MapLayers? = null

  fun loadBaseMap() {
    val loadingDark = dark
    forgetStyle()
    map?.setStyle(
      Style.Builder().fromUri(if (loadingDark) DARK_BASE_MAP_STYLE else BASE_MAP_STYLE)
    ) {
      usesOfflineStyle = false
      styleDark = loadingDark
      onStyle(it)
    }
  }

  fun useOfflineStyle() {
    val loadingDark = dark
    forgetStyle()
    map?.setStyle(Style.Builder().fromJson(offlineStyle(loadingDark))) {
      usesOfflineStyle = true
      styleDark = loadingDark
      onStyle(it)
    }
  }

  /**
   * MapLibre invalidates a style as soon as the next one starts loading; until that one is loaded,
   * updates wait for [onStyle], which adds everything again.
   */
  private fun forgetStyle() {
    style = null
  }

  /** Loads the same kind of style, base map or offline, again when the appearance changed. */
  fun reloadStyleIfAppearanceChanged() {
    val loaded = styleDark ?: return
    if (loaded == dark) return
    if (usesOfflineStyle) useOfflineStyle() else loadBaseMap()
  }

  private var coursesHandedOver = false

  /**
   * Called after every rendered frame until the knots and the courses are visible: MapLibre parses
   * large layers on its own, so they show up some time after they were handed over.
   */
  fun markVisibleLayers(width: Int, height: Int) {
    val map = map ?: return
    if (StartupMarks.isMarked("courses")) return
    val screen = RectF(0f, 0f, width.toFloat(), height.toFloat())
    if (map.queryRenderedFeatures(screen, "knots").isNotEmpty()) StartupMarks.mark("knots")
    if (coursesHandedOver && map.queryRenderedFeatures(screen, "connections").isNotEmpty()) {
      StartupMarks.mark("courses")
    }
  }

  private fun onStyle(loaded: Style) {
    StartupMarks.mark("map")
    style = loaded
    shownLayers = null
    // The appearance may have changed again while this style was loading.
    if (styleDark != dark) {
      reloadStyleIfAppearanceChanged()
      return
    }
    addLayers(loaded)
    updateMarker(loaded)
    updateNavigation(loaded)
    updateTrack(loaded)
    update()
  }

  var onMove: (CameraPlace) -> Unit = {}
  private var marker: PlaceHit? = null

  fun showMarker(newMarker: PlaceHit?) {
    marker = newMarker
    style?.let { updateMarker(it) }
    if (newMarker != null) {
      map?.animateCamera(
        CameraUpdateFactory.newLatLngZoom(
          LatLng(newMarker.latitude, newMarker.longitude),
          SEARCH_HIT_ZOOM,
        )
      )
    }
  }

  private var position: Fix? = null
  private var nextKnot: Knot? = null

  /** Whether the map moves to the rider with the next position, e.g. after the app was opened. */
  var findRider = false

  /**
   * Shows the rider and the next knot; when navigation begins or the app is opened while
   * navigating, the map moves to the rider, see `knot-navigation` "Looking at the map".
   */
  /**
   * Whether the map follows the rider while navigating, see `knot-navigation` "Following the
   * rider": from the start of navigation until the user moves the map, and again after [recenter].
   */
  var following by mutableStateOf(false)

  private var direction: Double? = null

  fun showNavigation(
    newPosition: Fix?,
    newNextKnot: Knot?,
    offRoute: Boolean = false,
    newDirection: Double? = null,
  ) {
    val begins = newPosition != null && (position == null || findRider)
    if (begins) {
      findRider = false
      following = true
    }
    if (newPosition == null) following = false
    position = newPosition
    nextKnot = newNextKnot
    direction = newDirection
    style?.let { updateNavigation(it) }
    if (newPosition == null || !following) return
    val map = map ?: return
    val rider = LatLng(newPosition.latitude, newPosition.longitude)
    if (offRoute && newNextKnot != null) {
      // Off route, the rider and the next knot, so they see where the route is.
      val bounds =
        LatLngBounds.Builder()
          .include(rider)
          .include(LatLng(newNextKnot.latitude, newNextKnot.longitude))
          .build()
      map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 160))
    } else if (begins) {
      map.animateCamera(
        CameraUpdateFactory.newLatLngZoom(rider, maxOf(map.cameraPosition.zoom, NAVIGATION_ZOOM))
      )
    } else {
      map.animateCamera(CameraUpdateFactory.newLatLng(rider))
    }
  }

  /** Back to the rider at the navigation zoom, following them again. */
  fun recenter() {
    val rider = position ?: return
    following = true
    map?.animateCamera(
      CameraUpdateFactory.newLatLngZoom(LatLng(rider.latitude, rider.longitude), NAVIGATION_ZOOM)
    )
  }

  private var track: RiddenTrack? = null

  fun showTrack(newTrack: RiddenTrack?) {
    track = newTrack
    style?.let { updateTrack(it) }
  }

  private fun updateTrack(style: Style) {
    val points = track?.points.orEmpty()
    val geoJson =
      if (points.size < 2) """{"type": "FeatureCollection", "features": []}"""
      else
        points.joinToString(
          prefix = """{"type": "LineString", "coordinates": [""",
          postfix = "]}",
        ) {
          "[${it.longitude}, ${it.latitude}]"
        }
    style.getSourceAs<GeoJsonSource>("ridden-track")?.setGeoJson(geoJson)
  }

  private fun updateNavigation(style: Style) {
    fun point(latitude: Double, longitude: Double) =
      """{"type": "Point", "coordinates": [$longitude, $latitude]}"""
    val empty = """{"type": "FeatureCollection", "features": []}"""
    // With a riding direction, the position is drawn as an arrow rotated by it, see
    // `knot-navigation` "Riding direction on the map".
    style
      .getSourceAs<GeoJsonSource>("position")
      ?.setGeoJson(
        position?.let { fix ->
          val properties = direction?.let { """"direction": $it""" } ?: ""
          """{"type": "Feature", "properties": {$properties}, "geometry": ${point(fix.latitude, fix.longitude)}}"""
        } ?: empty
      )
    style
      .getSourceAs<GeoJsonSource>("next-knot")
      ?.setGeoJson(nextKnot?.let { point(it.latitude, it.longitude) } ?: empty)
  }

  private fun updateMarker(style: Style) {
    val point = marker?.let {
      """{"type": "Point", "coordinates": [${it.longitude}, ${it.latitude}]}"""
    }
    style
      .getSourceAs<GeoJsonSource>("marker")
      ?.setGeoJson(point ?: """{"type": "FeatureCollection", "features": []}""")
  }

  /** Whether the map already shows a place to keep, e.g. the one saved when the app was left. */
  var hasPlace = false

  fun place(): CameraPlace? =
    map?.cameraPosition?.let { camera ->
      camera.target?.let { CameraPlace(it.latitude, it.longitude, camera.zoom) }
    }

  fun show(newGraph: NetworkGraph?, newLayers: MapLayers?, newFocus: List<Knot>) {
    graph = newGraph
    layers = newLayers
    focus = newFocus
    update()
  }

  fun zoomBy(delta: Double) {
    map?.animateCamera(CameraUpdateFactory.zoomBy(delta))
  }

  fun zoomToFocus() {
    fitTo(focus, animated = true)
  }

  private fun update() {
    val style = style ?: return
    val layers = layers ?: return
    val previous = shownLayers
    if (
      previous == null ||
        previous.knots !== layers.knots ||
        previous.connections !== layers.connections
    ) {
      layers.refs.forEach { ref ->
        val name = badgeName(null, ref)
        if (style.getImage(name) == null) style.addImage(name, badgeBitmap(null, ref))
      }
      style.getSourceAs<GeoJsonSource>("knots")?.setGeoJson(layers.knots)
      style.getSourceAs<GeoJsonSource>("connections")?.setGeoJson(layers.connections)
      coursesHandedOver = layers.hasCourses
    }
    if (previous == null || previous.route !== layers.route) {
      layers.routeKnotList.forEach { (knot, role) ->
        val name = badgeName(role, knot.ref)
        if (style.getImage(name) == null) style.addImage(name, badgeBitmap(role, knot.ref))
      }
      style.getSourceAs<GeoJsonSource>("route")?.setGeoJson(layers.route)
      style.getSourceAs<GeoJsonSource>("route-knots")?.setGeoJson(layers.routeKnots)
    }
    shownLayers = layers
    if (shownGraph !== graph) {
      // The map keeps its place: the one saved when the app was left, or the one before switching
      // to another network. Without one, it shows the route if there is one, or the network's
      // center. Either way knots must be shown, see `map-planning`.
      val currentGraph = graph
      when {
        hasPlace -> {}
        focus.size >= 2 -> fitTo(focus, animated = false)
        currentGraph != null -> fitTo(currentGraph.knots.values, animated = false)
      }
      hasPlace = true
      map?.let {
        if (it.cameraPosition.zoom < KNOT_MIN_ZOOM)
          it.moveCamera(CameraUpdateFactory.zoomTo(KNOT_MIN_ZOOM))
      }
      shownGraph = graph
    }
  }

  private fun fitTo(knots: Collection<Knot>, animated: Boolean) {
    if (knots.size < 2) return
    val bounds =
      LatLngBounds.Builder()
        .apply { knots.forEach { include(LatLng(it.latitude, it.longitude)) } }
        .build()
    val update = CameraUpdateFactory.newLatLngBounds(bounds, 160)
    if (animated) map?.animateCamera(update) else map?.moveCamera(update)
  }

  private fun addLayers(style: Style) {
    listOf(
        "connections",
        "marker",
        "ridden-track",
        "route",
        "next-knot",
        "route-knots",
        "knots",
        "position",
      )
      .forEach {
        style.addSource(GeoJsonSource(it))
      }
    style.addImage("route-arrow", arrowBitmap())
    style.addImage(
      "rider-arrow",
      riderArrowBitmap(android.graphics.Color.parseColor(POSITION_BLUE)),
    )
    // The chosen search hit, drawn under route and knots, so it never hides a knot.
    style.addLayer(
      CircleLayer("marker", "marker")
        .withProperties(
          PropertyFactory.circleRadius(12f),
          PropertyFactory.circleColor(MARKER_RED),
          PropertyFactory.circleStrokeColor("#FFFFFF"),
          PropertyFactory.circleStrokeWidth(3f),
        )
    )
    style.addLayer(
      LineLayer("connections", "connections")
        .withProperties(
          PropertyFactory.lineColor(GREEN),
          PropertyFactory.lineWidth(2f),
          PropertyFactory.lineOpacity(0.7f),
        )
    )
    // Below the route: where both overlap the route stays visible, detours show in blue.
    style.addLayer(
      LineLayer("ridden-track", "ridden-track")
        .withProperties(
          PropertyFactory.lineColor(TRACK_BLUE),
          PropertyFactory.lineWidth(4f),
          PropertyFactory.lineOpacity(0.8f),
          PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
          PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
        )
    )
    style.addLayer(
      LineLayer("route", "route")
        .withFilter(Expression.eq(Expression.get("reachable"), true))
        .withProperties(
          PropertyFactory.lineColor(ORANGE),
          PropertyFactory.lineWidth(8f),
          PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
          PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
        )
    )
    style.addLayer(
      LineLayer("route-unreachable", "route")
        .withFilter(Expression.eq(Expression.get("reachable"), false))
        .withProperties(
          PropertyFactory.lineColor("#D32F2F"),
          PropertyFactory.lineWidth(4f),
          PropertyFactory.lineDasharray(arrayOf(2f, 2f)),
        )
    )
    // Arrows along the route show the riding direction: each leg runs from its start to its end.
    style.addLayer(
      SymbolLayer("route-direction", "route")
        .withFilter(Expression.eq(Expression.get("reachable"), true))
        .withProperties(
          PropertyFactory.symbolPlacement(Property.SYMBOL_PLACEMENT_LINE),
          PropertyFactory.symbolSpacing(48f),
          PropertyFactory.iconImage("route-arrow"),
          PropertyFactory.iconSize(0.7f),
          PropertyFactory.iconAllowOverlap(true),
          PropertyFactory.iconIgnorePlacement(true),
        )
    )
    style.addLayer(
      SymbolLayer("knots", "knots")
        .withProperties(
          PropertyFactory.iconImage(
            Expression.concat(Expression.literal("knot-"), Expression.get("ref"))
          ),
          PropertyFactory.iconAllowOverlap(true),
          PropertyFactory.iconIgnorePlacement(true),
        )
        .apply { minZoom = KNOT_MIN_ZOOM.toFloat() }
    )
    // The next knot while navigating: a ring under its badge.
    style.addLayer(
      CircleLayer("next-knot", "next-knot")
        .withProperties(
          PropertyFactory.circleRadius(26f),
          PropertyFactory.circleColor(ORANGE),
          PropertyFactory.circleOpacity(0.35f),
          PropertyFactory.circleStrokeColor(ORANGE),
          PropertyFactory.circleStrokeWidth(3f),
        )
    )
    // Route knots are drawn above the network's knots, with a badge per role.
    style.addLayer(
      SymbolLayer("route-knots", "route-knots")
        .withProperties(
          PropertyFactory.iconImage(
            Expression.concat(
              Expression.literal("route-"),
              Expression.get("role"),
              Expression.literal("-"),
              Expression.get("ref"),
            )
          ),
          PropertyFactory.iconAllowOverlap(true),
          PropertyFactory.iconIgnorePlacement(true),
        )
        .apply { minZoom = KNOT_MIN_ZOOM.toFloat() }
    )
    // The rider's position while navigating, above everything else.
    style.addLayer(
      CircleLayer("position", "position")
        .withFilter(Expression.not(Expression.has("direction")))
        .withProperties(
          PropertyFactory.circleRadius(9f),
          PropertyFactory.circleColor(POSITION_BLUE),
          PropertyFactory.circleStrokeColor("#FFFFFF"),
          PropertyFactory.circleStrokeWidth(3f),
        )
    )
    style.addLayer(
      SymbolLayer("position-arrow", "position")
        .withFilter(Expression.has("direction"))
        .withProperties(
          PropertyFactory.iconImage("rider-arrow"),
          PropertyFactory.iconRotate(Expression.get("direction")),
          PropertyFactory.iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP),
          PropertyFactory.iconSize(0.9f),
          PropertyFactory.iconAllowOverlap(true),
          PropertyFactory.iconIgnorePlacement(true),
        )
    )
  }
}
