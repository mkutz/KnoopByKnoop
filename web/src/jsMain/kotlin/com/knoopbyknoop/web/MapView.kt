package com.knoopbyknoop.web

import com.knoopbyknoop.core.GeoBounds
import com.knoopbyknoop.core.GeoPoint
import com.knoopbyknoop.core.MapFeatures
import com.knoopbyknoop.core.MapPlace
import com.knoopbyknoop.core.PlaceKind
import com.knoopbyknoop.core.PlanningSession
import kotlin.js.json
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import org.w3c.dom.HTMLElement

/** MapLibre GL JS, bundled from npm; its worker is served next to the bundle. */
private val maplibregl: dynamic by lazy { js("require('maplibre-gl/dist/maplibre-gl.mjs')") }

private const val GREEN = "#2E9E5B"
private const val ORANGE = "#F28C28"
private const val PROBLEM_ON_LIGHT = "#263238"
private const val PROBLEM_ON_DARK = "#ECEFF1"
private const val KNOT_MIN_ZOOM = 10.0
private const val ROUTE_OFFSET = 4.0

/** The base map from OpenFreeMap, as in the apps; the dark style is the apps' own. */
private const val LIGHT_STYLE = "https://tiles.openfreemap.org/styles/liberty"

/** How far from a click a knot is still hit, in pixels. */
private const val CLICK_TOLERANCE_PIXELS = 14.0

private val OUR_SOURCES =
  listOf("connections", "route", "route-point-ways", "knots", "route-knots", "marker", "offer")

/**
 * The planner's map: the base map, the network, the route and the places, drawn as the apps draw
 * them, see `web-planner` "Networks on the map" and "Light and dark planner". Clicks go to
 * [onClick] with the place under them, if any.
 */
class MapView(
  container: HTMLElement,
  private val siteRoot: String,
  dark: Boolean,
  private val label: String,
  private val onClick:
    (
      latitude: Double,
      longitude: Double,
      radiusMeters: Double,
      place: MapPlace?,
      x: Double,
      y: Double,
    ) -> Unit,
) {

  var dark: Boolean = dark
    private set

  val map: dynamic = run {
    val library = maplibregl
    library.setWorkerUrl(
      org.w3c.dom.url
        .URL("${siteRoot}maplibre-gl-worker.js", kotlinx.browser.window.location.href)
        .href
    )
    val options: dynamic = js("({})")
    options.container = container
    options.style = styleUrl(dark)
    options.center = arrayOf(5.3, 51.6)
    options.zoom = 7
    options.attributionControl = json("compact" to true)
    options.dragRotate = false
    options.pitchWithRotate = false
    options.cooperativeGestures = false
    js("new library.Map(options)")
  }

  private var layers: Layers? = null
  private var placesShown = true
  private var placesFilters: Map<String, dynamic> = mapOf()

  /** What the map shows, kept to draw it again after a style change. */
  private class Layers(
    val knots: String,
    val connections: String,
    val route: String,
    val routePointWays: String,
    val routeKnots: String,
  )

  private var marker: GeoPoint? = null
  private var offer: GeoPoint? = null

  init {
    container.setAttribute("aria-label", label)
    val library = maplibregl
    val navigation: dynamic = js("new library.NavigationControl({ showCompass: false })")
    map.addControl(navigation, "top-right")
    map.setMissingStyleImageResolver { id: String ->
      mapImage(id, this.dark)?.let { canvas ->
        val context: dynamic = canvas.getContext("2d")
        val image = context.getImageData(0, 0, canvas.width, canvas.height)
        if (!(map.hasImage(id) as Boolean))
          map.addImage(id, image, json("pixelRatio" to PIXEL_RATIO))
      }
    }
    map.on("style.load") { _: dynamic -> addLayers() }
    map.on("click") { event: dynamic -> click(event) }
    map.on("mousemove") { event: dynamic ->
      val features: dynamic =
        map.queryRenderedFeatures(event.point, json("layers" to clickableLayers()))
      map.getCanvas().style.cursor = if ((features.length as Int) > 0) "pointer" else ""
    }
  }

  private fun styleUrl(dark: Boolean) =
    if (dark) "${siteRoot}map-styles/liberty-dark.json" else LIGHT_STYLE

  fun setDark(newDark: Boolean) {
    if (newDark == dark) return
    dark = newDark
    // Our images depend on the appearance: drawn again on demand.
    map.setStyle(styleUrl(newDark))
  }

  private fun clickableLayers(): Array<String> =
    arrayOf("knots", "route-knots").filter { map.getLayer(it) != undefined }.toTypedArray() +
      (if (placesShown) arrayOf(PlaceKind.LAYER_ID, PlaceKind.NAMED_LAYER_ID) else arrayOf())
        .filter { map.getLayer(it) != undefined }

  private fun click(event: dynamic) {
    val latitude = event.lngLat.lat as Double
    val longitude = event.lngLat.lng as Double
    val zoom = map.getZoom() as Double
    val metersPerPixel = 40_075_016.686 * cos(latitude * PI / 180) / (512 * 2.0.pow(zoom))
    val point = event.point
    val box = arrayOf(arrayOf(point.x - 8, point.y - 8), arrayOf(point.x + 8, point.y + 8))
    val place =
      if (!placesShown || map.getLayer(PlaceKind.LAYER_ID) == undefined) null
      else {
        val features: dynamic =
          map.queryRenderedFeatures(
            box,
            json(
              "layers" to
                arrayOf(PlaceKind.LAYER_ID, PlaceKind.NAMED_LAYER_ID)
                  .filter { map.getLayer(it) != undefined }
                  .toTypedArray()
            ),
          )
        if ((features.length as Int) == 0) null else placeOf(features[0])
      }
    onClick(
      latitude,
      longitude,
      CLICK_TOLERANCE_PIXELS * metersPerPixel,
      place,
      point.x as Double,
      point.y as Double,
    )
  }

  private fun placeOf(feature: dynamic): MapPlace? {
    val properties = feature.properties
    val className = properties["class"] as String?
    val subclass = properties.subclass as String?
    val kind = PlaceKind.of(className, subclass) ?: return null
    val coordinates = feature.geometry.coordinates
    val id = feature.id
    return MapPlace(
      GeoPoint(coordinates[1] as Double, coordinates[0] as Double),
      (properties.name as String?)?.takeIf { it.isNotBlank() },
      kind,
      subclass ?: "",
      if (id == undefined || id == null) null else (id as Number).toLong(),
    )
  }

  /** Draws [network]'s knots and connections, and [session]'s route. */
  fun show(network: LoadedNetwork?, session: PlanningSession?) {
    if (network == null || session == null) {
      layers = null
      OUR_SOURCES.forEach { setData(it, EMPTY) }
      return
    }
    val previous = layers
    layers =
      Layers(
        knots =
          if (previous != null && sameNetwork === network.graph) previous.knots
          else MapFeatures.knots(network.graph),
        connections =
          if (previous != null && sameGeometry === network.geometry) previous.connections
          else MapFeatures.connections(network.geometry),
        route = MapFeatures.route(session, network.geometry),
        routePointWays = MapFeatures.routePointWays(session),
        routeKnots = MapFeatures.routeKnotsLayer(session),
      )
    sameNetwork = network.graph
    sameGeometry = network.geometry
    draw()
  }

  private var sameNetwork: Any? = null
  private var sameGeometry: Any? = null

  private fun draw() {
    val current = layers ?: return
    setData("knots", current.knots)
    setData("connections", current.connections)
    setData("route", current.route)
    setData("route-point-ways", current.routePointWays)
    setData("route-knots", current.routeKnots)
    setData("marker", pointsOf(marker))
    setData("offer", pointsOf(offer))
  }

  private fun pointsOf(point: GeoPoint?) =
    if (point == null) EMPTY
    else
      """{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"Point","coordinates":[${point.longitude},${point.latitude}]},"properties":{}}]}"""

  private fun setData(source: String, geoJson: String) {
    val mapSource = map.getSource(source) ?: return
    mapSource.setData(JSON.parse<Any>(geoJson))
  }

  /** Marks the chosen search hit, see "Place search in the planner". */
  fun mark(point: GeoPoint?) {
    marker = point
    setData("marker", pointsOf(point))
    point?.let { map.flyTo(json("center" to arrayOf(it.longitude, it.latitude), "zoom" to 14)) }
  }

  /** Marks the clicked spot while its choices are open. */
  fun showOffer(point: GeoPoint?) {
    offer = point
    setData("offer", pointsOf(point))
  }

  fun fit(bounds: GeoBounds, maxZoom: Double = 15.0) {
    map.fitBounds(
      arrayOf(
        arrayOf(bounds.minLongitude, bounds.minLatitude),
        arrayOf(bounds.maxLongitude, bounds.maxLatitude),
      ),
      json("padding" to 48, "maxZoom" to maxZoom, "duration" to 0),
    )
  }

  fun setPlacesShown(shown: Boolean) {
    placesShown = shown
    updatePlaces()
  }

  private fun addLayers() {
    val problem = if (dark) PROBLEM_ON_DARK else PROBLEM_ON_LIGHT
    val casing = if (dark) PROBLEM_ON_LIGHT else "#FFFFFF"
    OUR_SOURCES.forEach {
      map.addSource(it, json("type" to "geojson", "data" to JSON.parse<Any>(EMPTY)))
    }
    fun layer(vararg pairs: Pair<String, Any?>) = map.addLayer(json(*pairs))
    layer(
      "id" to "marker",
      "type" to "circle",
      "source" to "marker",
      "paint" to
        json(
          "circle-radius" to 12,
          "circle-color" to problem,
          "circle-stroke-color" to casing,
          "circle-stroke-width" to 3,
        ),
    )
    layer(
      "id" to "connections",
      "type" to "line",
      "source" to "connections",
      "paint" to json("line-color" to GREEN, "line-width" to 2, "line-opacity" to 0.7),
    )
    layer(
      "id" to "route",
      "type" to "line",
      "source" to "route",
      "filter" to arrayOf("==", arrayOf("get", "reachable"), true),
      "layout" to json("line-cap" to "round", "line-join" to "round"),
      "paint" to json("line-color" to ORANGE, "line-width" to 5, "line-offset" to ROUTE_OFFSET),
    )
    layer(
      "id" to "route-unreachable",
      "type" to "line",
      "source" to "route",
      "filter" to arrayOf("==", arrayOf("get", "reachable"), false),
      "paint" to
        json("line-color" to problem, "line-width" to 4, "line-dasharray" to arrayOf(2, 2)),
    )
    layer(
      "id" to "route-point-ways",
      "type" to "line",
      "source" to "route-point-ways",
      "filter" to arrayOf("==", arrayOf("get", "straight"), false),
      "layout" to json("line-cap" to "round"),
      "paint" to
        json("line-color" to "#FFFFFF", "line-width" to 2.5, "line-dasharray" to arrayOf(0.1, 2)),
    )
    layer(
      "id" to "route-point-straight",
      "type" to "line",
      "source" to "route-point-ways",
      "filter" to arrayOf("==", arrayOf("get", "straight"), true),
      "paint" to
        json("line-color" to "#FFFFFF", "line-width" to 2.5, "line-dasharray" to arrayOf(3, 3)),
    )
    layer(
      "id" to "knots",
      "type" to "symbol",
      "source" to "knots",
      "minzoom" to KNOT_MIN_ZOOM,
      "layout" to
        json(
          "icon-image" to arrayOf("concat", "knot-", arrayOf("get", "ref")),
          "icon-allow-overlap" to true,
          "icon-ignore-placement" to true,
        ),
    )
    layer(
      "id" to "route-knots",
      "type" to "symbol",
      "source" to "route-knots",
      "minzoom" to KNOT_MIN_ZOOM,
      "layout" to
        json(
          "icon-image" to
            arrayOf(
              "concat",
              "route-",
              arrayOf("get", "role"),
              "-",
              arrayOf("get", "plate"),
              "-",
              arrayOf("get", "ref"),
              "-",
              arrayOf("get", "place"),
            ),
          "icon-allow-overlap" to true,
          "icon-ignore-placement" to true,
        ),
    )
    layer(
      "id" to "offer",
      "type" to "symbol",
      "source" to "offer",
      "layout" to
        json(
          "icon-image" to "offer-flag",
          "icon-size" to 1.3,
          "icon-allow-overlap" to true,
          "icon-ignore-placement" to true,
        ),
    )
    placesFilters = mapOf()
    updatePlaces()
    draw()
  }

  /**
   * Places useful on a ride, as in the apps, see `map-planning` "Places useful on a ride": the base
   * map's place layers leave our kinds out, ours draws them from zoom 14 below our layers.
   */
  private fun updatePlaces() {
    if (map.getSource("openmaptiles") == undefined) return
    if (placesFilters.isEmpty()) {
      placesFilters =
        PlaceKind.BASE_MAP_LAYERS.filter { map.getLayer(it) != undefined }
          .associateWith { map.getFilter(it) }
    }
    val notPlaces = JSON.parse<Any>(PlaceKind.notPlacesFilter)
    for ((id, filter) in placesFilters) {
      map.setFilter(
        id,
        when {
          placesShown ->
            if (filter == null || filter == undefined) notPlaces
            else arrayOf("all", filter, notPlaces)
          else -> filter
        },
      )
    }
    if (!placesShown) {
      listOf(PlaceKind.NAMED_LAYER_ID, PlaceKind.LAYER_ID).forEach {
        if (map.getLayer(it) != undefined) map.removeLayer(it)
      }
      return
    }
    if (map.getLayer(PlaceKind.LAYER_ID) != undefined) return
    fun places(id: String, named: Boolean): dynamic {
      val layout =
        json(
          "icon-image" to JSON.parse<Any>(PlaceKind.placesIcon),
          "icon-size" to 1.0,
        )
      if (named) {
        layout["text-field"] = JSON.parse<Any>(PlaceKind.placesText)
        layout["text-font"] = arrayOf("Noto Sans Regular")
        layout["text-size"] = 12
        layout["text-anchor"] = "left"
        layout["text-offset"] = arrayOf(1.1, 0)
        layout["text-max-width"] = 9
        layout["text-optional"] = true
      }
      return json(
        "id" to id,
        "type" to "symbol",
        "source" to "openmaptiles",
        "source-layer" to PlaceKind.SOURCE_LAYER,
        "filter" to JSON.parse<Any>(PlaceKind.placesFilter),
        "minzoom" to if (named) PlaceKind.NAME_ZOOM else PlaceKind.MIN_ZOOM,
        "maxzoom" to if (named) 24.0 else PlaceKind.NAME_ZOOM,
        "layout" to layout,
        "paint" to
          if (named)
            json(
              "text-color" to if (dark) "#E0E0E0" else "#37474F",
              "text-halo-color" to if (dark) "#1E2320" else "#FFFFFF",
              "text-halo-width" to 1.2,
            )
          else json(),
      )
    }
    map.addLayer(places(PlaceKind.LAYER_ID, named = false), "marker")
    map.addLayer(places(PlaceKind.NAMED_LAYER_ID, named = true), "marker")
  }

  private companion object {
    const val EMPTY = """{"type":"FeatureCollection","features":[]}"""
  }
}
