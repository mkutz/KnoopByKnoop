package com.knoopbyknoop.android

import android.graphics.RectF
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.knoopbyknoop.core.Knot
import com.knoopbyknoop.core.NetworkGraph
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource

/** OpenStreetMap-based base map without API key, see the design of `add-planning-screens`. */
private const val BASE_MAP_STYLE = "https://tiles.openfreemap.org/styles/liberty"

/**
 * Used when the base map cannot be loaded, e.g. offline on first start: the network layers still
 * show. Offline, MapLibre waits for the network instead of failing, so it is used after a timeout.
 */
private const val OFFLINE_STYLE =
  """{"version": 8, "sources": {}, "layers": [{"id": "background", "type": "background", "paint": {"background-color": "#EEF1EA"}}]}"""

/** How long the base map may take to load before the offline style is used. */
private const val BASE_MAP_TIMEOUT_MILLIS = 3_000L

/** Knots are drawn and can be tapped from this zoom level on; below it the route is a line only. */
private const val KNOT_MIN_ZOOM = 10.0

/** The touch target around a tap within which the nearest knot is picked. */
private const val TOUCH_TARGET_DP = 24f

private const val GREEN = "#2E9E5B"
private const val ORANGE = "#F28C28"

/**
 * The map with the network and the route, and buttons to zoom. Taps are reported with the touch
 * target in meters at the current zoom level, so the core's knot locator can pick the knot.
 */
@Composable
fun NetworkMap(
  graph: NetworkGraph,
  layers: MapLayers,
  focus: List<Knot>,
  onTap: (latitude: Double, longitude: Double, radiusMeters: Double) -> Unit,
  online: Boolean,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val touchTargetPixels = with(LocalDensity.current) { TOUCH_TARGET_DP * density }
  val holder = remember { MapHolder() }
  val mapView = remember {
    MapLibre.getInstance(context)
    MapView(context).apply {
      onCreate(null)
      getMapAsync { map ->
        holder.map = map
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
        addOnDidFailLoadingMapListener { holder.useOfflineStyle() }
        holder.loadBaseMap()
        postDelayed({ if (!holder.hasStyle) holder.useOfflineStyle() }, BASE_MAP_TIMEOUT_MILLIS)
      }
    }
  }
  holder.onTap = onTap
  holder.show(graph, layers, focus)
  // The offline style has no base map; it is loaded as soon as network access is back.
  LaunchedEffect(online) { if (online && holder.usesOfflineStyle) holder.loadBaseMap() }

  val lifecycle = LocalLifecycleOwner.current.lifecycle
  DisposableEffect(lifecycle) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_START -> mapView.onStart()
        Lifecycle.Event.ON_RESUME -> mapView.onResume()
        Lifecycle.Event.ON_PAUSE -> mapView.onPause()
        Lifecycle.Event.ON_STOP -> mapView.onStop()
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
          containerColor = Color.White,
          contentColor = Green,
          modifier = Modifier.padding(vertical = 4.dp),
        ) {
          Text(label, fontSize = 22.sp)
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

  private var graph: NetworkGraph? = null
  private var layers: MapLayers? = null
  private var focus: List<Knot> = listOf()
  private var shownGraph: NetworkGraph? = null
  private var shownLayers: MapLayers? = null

  fun loadBaseMap() {
    map?.setStyle(Style.Builder().fromUri(BASE_MAP_STYLE)) {
      usesOfflineStyle = false
      onStyle(it)
    }
  }

  fun useOfflineStyle() {
    map?.setStyle(Style.Builder().fromJson(OFFLINE_STYLE)) {
      usesOfflineStyle = true
      onStyle(it)
    }
  }

  private fun onStyle(loaded: Style) {
    style = loaded
    shownLayers = null
    addLayers(loaded)
    update()
  }

  fun show(newGraph: NetworkGraph, newLayers: MapLayers, newFocus: List<Knot>) {
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
    if (previous == null || previous.knots !== layers.knots) {
      layers.refs.forEach { ref ->
        val name = badgeName(null, ref)
        if (style.getImage(name) == null) style.addImage(name, badgeBitmap(null, ref))
      }
      style.getSourceAs<GeoJsonSource>("knots")?.setGeoJson(layers.knots)
      style.getSourceAs<GeoJsonSource>("connections")?.setGeoJson(layers.connections)
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
      // A new network shows the restored route if there is one, the whole network otherwise.
      graph?.let { fitTo(if (focus.size >= 2) focus else it.knots.values, animated = false) }
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
    listOf("connections", "route", "route-knots", "knots").forEach {
      style.addSource(GeoJsonSource(it))
    }
    style.addImage("route-arrow", arrowBitmap())
    style.addLayer(
      LineLayer("connections", "connections")
        .withProperties(
          PropertyFactory.lineColor(GREEN),
          PropertyFactory.lineWidth(2f),
          PropertyFactory.lineOpacity(0.7f),
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
  }
}
