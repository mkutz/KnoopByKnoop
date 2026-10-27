package com.knoopbyknoop.android

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource

/** OpenStreetMap-based base map without API key, see the design of `add-planning-screens`. */
private const val BASE_MAP_STYLE = "https://tiles.openfreemap.org/styles/liberty"

/** Used when the base map cannot be loaded, e.g. offline: the network layers still show. */
private const val OFFLINE_STYLE =
  """{"version": 8, "sources": {}, "layers": [{"id": "background", "type": "background", "paint": {"background-color": "#EEF1EA"}}]}"""

/** Knots are drawn and can be tapped from this zoom level on. */
private const val KNOT_MIN_ZOOM = 10.0

/** The touch target around a tap within which the nearest knot is picked. */
private const val TOUCH_TARGET_DP = 24f

private const val GREEN = "#2E9E5B"
private const val ORANGE = "#F28C28"

/**
 * The map with the network and the route. Taps are reported with the touch target in meters at the
 * current zoom level, so the core's knot locator can pick the knot.
 */
@Composable
fun NetworkMap(
  graph: NetworkGraph,
  layers: MapLayers,
  focus: List<Knot>,
  onTap: (latitude: Double, longitude: Double, radiusMeters: Double) -> Unit,
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
          if (map.cameraPosition.zoom < KNOT_MIN_ZOOM) return@addOnMapClickListener false
          val metersPerPixel = map.projection.getMetersPerPixelAtLatitude(point.latitude)
          holder.onTap(point.latitude, point.longitude, metersPerPixel * touchTargetPixels)
          true
        }
        addOnDidFailLoadingMapListener {
          map.setStyle(Style.Builder().fromJson(OFFLINE_STYLE)) { holder.onStyle(it) }
        }
        map.setStyle(Style.Builder().fromUri(BASE_MAP_STYLE)) { holder.onStyle(it) }
      }
    }
  }
  holder.onTap = onTap
  holder.show(graph, layers, focus)

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

  AndroidView(factory = { mapView }, modifier = modifier)
}

/** Keeps the map, its style and what is shown, so updates only touch what changed. */
private class MapHolder {
  var map: MapLibreMap? = null
  var onTap: (Double, Double, Double) -> Unit = { _, _, _ -> }
  private var style: Style? = null
  private var graph: NetworkGraph? = null
  private var layers: MapLayers? = null
  private var focus: List<Knot> = listOf()
  private var shownGraph: NetworkGraph? = null
  private var shownLayers: MapLayers? = null

  fun onStyle(loaded: Style) {
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

  private fun update() {
    val style = style ?: return
    val layers = layers ?: return
    val previous = shownLayers
    if (previous == null || previous.knots !== layers.knots) {
      layers.refs.forEach { ref ->
        if (style.getImage("knot-$ref") == null) style.addImage("knot-$ref", badge(ref))
      }
      style.getSourceAs<GeoJsonSource>("knots")?.setGeoJson(layers.knots)
      style.getSourceAs<GeoJsonSource>("connections")?.setGeoJson(layers.connections)
    }
    if (previous == null || previous.route !== layers.route) {
      style.getSourceAs<GeoJsonSource>("route")?.setGeoJson(layers.route)
      style.getSourceAs<GeoJsonSource>("waypoints")?.setGeoJson(layers.waypoints)
    }
    shownLayers = layers
    if (shownGraph !== graph) {
      // A new network shows the restored route if there is one, the whole network otherwise.
      graph?.let { fitTo(if (focus.size >= 2) focus else it.knots.values) }
      shownGraph = graph
    }
  }

  private fun fitTo(knots: Collection<Knot>) {
    if (knots.size < 2) return
    val bounds =
      LatLngBounds.Builder()
        .apply { knots.forEach { include(LatLng(it.latitude, it.longitude)) } }
        .build()
    map?.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 160))
  }

  private fun addLayers(style: Style) {
    listOf("connections", "route", "waypoints", "knots").forEach {
      style.addSource(GeoJsonSource(it))
    }
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
          PropertyFactory.lineWidth(6f),
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
    style.addLayer(
      CircleLayer("waypoints", "waypoints")
        .withProperties(
          PropertyFactory.circleRadius(16f),
          PropertyFactory.circleColor(ORANGE),
          PropertyFactory.circleStrokeColor("#FFFFFF"),
          PropertyFactory.circleStrokeWidth(3f),
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
  }

  /** A round badge with the knot's number, drawn locally so numbers show without network access. */
  private fun badge(ref: String): Bitmap {
    val size = 56
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    val stroke =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor(GREEN)
        style = Paint.Style.STROKE
        strokeWidth = 5f
      }
    val text =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor(GREEN)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        textSize = if (ref.length > 2) 18f else 24f
      }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4, fill)
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4, stroke)
    canvas.drawText(ref, size / 2f, size / 2f - (text.descent() + text.ascent()) / 2, text)
    return bitmap
  }
}
