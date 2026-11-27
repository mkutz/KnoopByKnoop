package com.knoopbyknoop.web

import com.knoopbyknoop.core.GeoBounds
import com.knoopbyknoop.core.GeoPoint
import com.knoopbyknoop.core.IndexedNetworkType
import com.knoopbyknoop.core.Knot
import com.knoopbyknoop.core.KnotPlate
import com.knoopbyknoop.core.Language
import com.knoopbyknoop.core.MapPlace
import com.knoopbyknoop.core.MapTap
import com.knoopbyknoop.core.RouteDescription
import com.knoopbyknoop.core.RouteLink
import com.knoopbyknoop.core.WaypointRole
import com.knoopbyknoop.core.nameIn
import kotlin.js.Date
import kotlin.js.json
import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.khronos.webgl.Int8Array
import org.w3c.dom.Element
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.url.URL
import org.w3c.fetch.RequestInit
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import org.w3c.files.File
import org.w3c.files.get

/** The website's files, read with the browser's `fetch`. */
private class BrowserFetcher : Fetcher {

  override suspend fun bytes(path: String): ByteArray {
    val response = window.fetch(path).await()
    if (!response.ok) throw IllegalStateException("$path: HTTP ${response.status}")
    return Int8Array(response.arrayBuffer().await()).unsafeCast<ByteArray>()
  }

  override suspend fun range(path: String, offset: Long, size: Int): ByteArray {
    val init = RequestInit(headers = json("Range" to "bytes=$offset-${offset + size - 1}"))
    val response = window.fetch(path, init).await()
    if (response.status.toInt() != 206)
      throw IllegalStateException("$path: HTTP ${response.status}")
    val bytes = Int8Array(response.arrayBuffer().await()).unsafeCast<ByteArray>()
    if (bytes.size != size) throw IllegalStateException("$path: ${bytes.size} bytes, not $size")
    return bytes
  }
}

/** The browser's storage, see `web-planner` "Route kept in the browser". */
private class BrowserKept : Kept {
  override fun read(key: String): String? = localStorage.getItem(key)

  override fun write(key: String, value: String) = localStorage.setItem(key, value)
}

private class WebNetworkSource(private val data: PublishedData) : NetworkSource {
  override suspend fun graph(network: IndexedNetworkType): NetworkSource.Loaded {
    val (file, sha) = data.graph(network)
    return NetworkSource.Loaded(
      LoadedNetwork(
        network,
        file.graph,
        com.knoopbyknoop.core.NetworkGeometry.straightLines(file.graph),
        false,
      ),
      sha,
    )
  }

  override suspend fun courses(loaded: NetworkSource.Loaded): LoadedNetwork {
    val network = loaded.network
    val geometry = data.geometry(network.network, network.graph, loaded.graphSha256)
    return LoadedNetwork(network.network, network.graph, geometry, true)
  }

  override suspend fun <T> withSquares(load: (com.knoopbyknoop.core.PathDataOrigin) -> T): T? =
    try {
      data.withSquares(load)
    } catch (exception: Throwable) {
      null
    }
}

/** Shorthand for building the page. */
private fun element(
  tag: String,
  className: String? = null,
  text: String? = null,
  build: HTMLElement.() -> Unit = {},
): HTMLElement {
  val element = document.createElement(tag) as HTMLElement
  className?.let { element.className = it }
  text?.let { element.textContent = it }
  element.build()
  return element
}

private fun button(
  text: String,
  className: String? = null,
  onClick: () -> Unit,
): HTMLButtonElement =
  (element("button", className, text) as HTMLButtonElement).apply {
    type = "button"
    addEventListener("click", { onClick() })
  }

/**
 * The planner page, see `web-planner`: the panel beside the map, built from the planner's state
 * whenever it changes.
 */
class PlannerPage(private val root: HTMLElement) {

  private val siteRoot = root.getAttribute("data-root") ?: "/"
  private val language =
    when (root.getAttribute("data-language")) {
      "de" -> Language.DE
      "nl" -> Language.NL
      "fr" -> Language.FR
      else -> Language.EN
    }
  private val texts: Map<String, String> =
    document.getElementById("planner-texts")?.textContent?.let { text ->
      val parsed: dynamic = JSON.parse(text)
      val keys: Array<String> = js("Object.keys(parsed)")
      keys.associateWith { parsed[it] as String }
    } ?: mapOf()

  private fun text(key: String, vararg arguments: String): String {
    var result = texts[key] ?: key
    arguments.forEachIndexed { index, argument -> result = result.replace("{$index}", argument) }
    return result
  }

  private val scope = MainScope()
  private val data = PublishedData(BrowserFetcher(), "${siteRoot}data/")
  private val planner =
    Planner(
      WebNetworkSource(data),
      BrowserKept(),
      language,
      texts,
      scope,
      nowMillis = { Date.now().toLong() },
    )
  private val searcher = PlaceSearcher { url ->
    val response = window.fetch(url).await()
    if (response.ok) response.text().await() else null
  }

  private val panel = element("div", "planner-panel")
  private val searchResults = element("ul", "search-results")
  /** Built once, so redrawing the panel keeps what is typed and the focus. */
  private val searchBox =
    element("div", "search") {
      val input =
        (element("input") as HTMLInputElement).apply {
          type = "search"
          placeholder = text("search.placeholder")
          setAttribute("aria-label", text("search.placeholder"))
          addEventListener("input", { search(value) })
        }
      append(input, searchResults)
    }
  private val mapElement = element("div", "planner-map")

  /** Over the map while the network or its courses load, see `web-planner` "Loading shown". */
  private val mapStatus =
    element("p", "map-status") {
      setAttribute("role", "status")
      setAttribute("aria-live", "polite")
    }
  private val darkQuery = window.matchMedia("(prefers-color-scheme: dark)")
  private val mapView =
    MapView(mapElement, siteRoot, darkQuery.matches, text("map.label")) {
      latitude,
      longitude,
      radius,
      place,
      x,
      y ->
      clicked(latitude, longitude, radius, place)
    }

  private var searchQuery = ""
  private var searchResult: SearchResult = SearchResult.None
  private var searchJob: Job? = null
  private var card: Card? = null
  private var copied = false
  private var placesShown = true
  private var fittedNetwork: Any? = null
  private var fittedOpened = 0

  /** The clicked spot or place, with its choices. */
  private class Card(val offer: MapTap.Offer?, val place: MapPlace?)

  init {
    root.textContent = ""
    root.append(panel, mapElement)
    mapElement.append(mapStatus)
    darkQuery.addEventListener("change", { mapView.setDark(darkQuery.matches) })
    planner.onChange = { render() }
    planner.onDetails = { render() }
    enableDropping()
    planner.start()
    openFromAddress()
    render()
  }

  /** A route link forwarded here, see `web-planner` "Open a route in the planner". */
  private fun openFromAddress() {
    val hash = window.location.hash.removePrefix("#")
    if (hash.isEmpty()) return
    window.history.replaceState(null, "", window.location.pathname)
    planner.open(RouteLink.PAGE + "#" + hash)
  }

  private fun clicked(latitude: Double, longitude: Double, radius: Double, place: MapPlace?) {
    if (card != null) {
      closeCard()
      return
    }
    when (val clicked = planner.click(latitude, longitude, radius, place)) {
      is Clicked.Done -> closeCard()
      is Clicked.Offer -> {
        card = Card(clicked.offer, clicked.offer.place)
        mapView.showOffer(clicked.offer.position)
        render()
      }
      is Clicked.PlaceOnly -> {
        card = Card(null, clicked.place)
        mapView.showOffer(clicked.place.position)
        render()
      }
    }
  }

  private fun closeCard() {
    if (card == null) return
    card = null
    mapView.showOffer(null)
    render()
  }

  private fun enableDropping() {
    mapElement.addEventListener("dragover", { event -> event.preventDefault() })
    mapElement.addEventListener(
      "drop",
      { event ->
        event.preventDefault()
        val files = event.asDynamic().dataTransfer?.files
        val file = files?.get(0).unsafeCast<File?>() ?: return@addEventListener
        openFile(file)
      },
    )
  }

  private fun openFile(file: File) {
    scope.launch {
      val text = file.asDynamic().text().unsafeCast<kotlin.js.Promise<String>>().await()
      planner.open(text)
    }
  }

  private fun search(query: String) {
    searchQuery = query
    searchJob?.cancel()
    val network = planner.loaded
    if (network == null || query.trim().length < 3) {
      searchResult = SearchResult.None
      renderSearch()
      return
    }
    searchJob = scope.launch {
      delay(300)
      searchResult = SearchResult.Searching
      renderSearch()
      val center = mapView.map.getCenter()
      searchResult =
        searcher.search(
          query,
          GeoPoint(center.lat as Double, center.lng as Double),
          network,
          language,
        )
      renderSearch()
    }
  }

  private fun renderSearch() {
    searchResults.textContent = ""
    when (val result = searchResult) {
      SearchResult.None -> {}
      SearchResult.Searching ->
        searchResults.append(element("li", "note", text("search.searching")))
      SearchResult.NoHits -> searchResults.append(element("li", "note", text("search.noHits")))
      SearchResult.Unavailable ->
        searchResults.append(element("li", "note", text("search.unavailable")))
      is SearchResult.Hits ->
        result.hits.forEach { hit ->
          searchResults.append(
            element("li") {
              append(
                button("", "hit") {
                    searchResult = SearchResult.None
                    renderSearch()
                    mapView.mark(GeoPoint(hit.latitude, hit.longitude))
                  }
                  .apply {
                    append(
                      element("strong", text = hit.name),
                      element("span", text = hit.description),
                    )
                  }
              )
            }
          )
        }
    }
  }

  private fun render() {
    val network = planner.loaded
    val session = planner.session
    mapView.show(network, session)
    if (network != null && fittedNetwork !== network.graph && session != null) {
      fittedNetwork = network.graph
      val bounds = routeBounds() ?: GeoBounds.of(network.graph)
      bounds?.let { mapView.fit(it) }
    }
    if (planner.openedRoutes != fittedOpened) {
      fittedOpened = planner.openedRoutes
      routeBounds()?.let { mapView.fit(it) }
    }
    val loadingText =
      when (planner.loadingStage) {
        Planner.Loading.NETWORK -> text("data.loading")
        Planner.Loading.COURSES -> text("data.loadingCourses")
        Planner.Loading.NOTHING -> null
      }
    mapStatus.textContent = loadingText ?: ""
    mapStatus.hidden = loadingText == null
    panel.textContent = ""
    panel.append(networkSwitch(), searchBox)
    planner.dataProblem?.let { panel.append(element("p", "message problem", text("data.problem"))) }
    planner.message?.let {
      panel.append(element("p", "message", it) { setAttribute("role", "status") })
    }
    // Each part on its own, so one that fails to draw leaves the others.
    val parts: List<() -> Element?> =
      listOf({ card?.let { cardOf(it) } }, { route() }, { handOff() }, { placesSwitch() })
    for (part in parts) {
      try {
        part()?.let { panel.append(it) }
      } catch (exception: Throwable) {
        console.error("Knoop by Knoop: drawing the panel failed", exception)
      }
    }
  }

  private fun routeBounds(): GeoBounds? {
    val waypoints = planner.session?.waypoints?.takeIf { it.size >= 2 } ?: return null
    val knots =
      planner.session?.route?.let { route ->
        route.legs.flatMap { it.path?.knots ?: listOf(it.from, it.to) }
      } ?: waypoints
    return GeoBounds(
      knots.minOf { it.latitude },
      knots.minOf { it.longitude },
      knots.maxOf { it.latitude },
      knots.maxOf { it.longitude },
    )
  }

  private fun networkSwitch() =
    element("div", "network-switch") {
      setAttribute("role", "group")
      setAttribute("aria-label", text("network.label"))
      for ((network, key) in
        listOf(
          IndexedNetworkType.CYCLING to "network.cycling",
          IndexedNetworkType.HIKING to "network.hiking",
        )) {
        append(
          button(text(key), if (planner.network == network) "selected" else null) {
              planner.switchNetwork(network)
            }
            .apply { setAttribute("aria-pressed", (planner.network == network).toString()) }
        )
      }
    }

  private fun cardOf(card: Card) =
    element("section", "card") {
      setAttribute("aria-label", text("card.label"))
      card.place?.let { place ->
        val content =
          PlaceCard.of(place, planner.detailsOf(place), nowInCoveredArea(), language) { text(it) }
        append(element("h2", text = content.title))
        content.kind?.let { append(element("p", "kind", it)) }
        content.status?.let {
          append(element("p", if (it.open) "status open" else "status closed", it.text))
        }
        content.hours?.let { append(element("p", "hours", it)) }
        content.website?.let { uri ->
          append(
            element("p") {
              append(
                (element("a", text = content.websiteText ?: uri) as HTMLAnchorElement).apply {
                  href = uri
                  rel = "noopener"
                }
              )
            }
          )
        }
        content.dial?.let { uri ->
          append(
            element("p") {
              append(
                (element("a", text = content.phoneText ?: uri) as HTMLAnchorElement).apply {
                  href = uri
                }
              )
            }
          )
        }
      }
      val actions = element("div", "actions")
      card.offer?.let { offer ->
        actions.append(
          button(text("card.add"), "primary") {
            this@PlannerPage.card = null
            mapView.showOffer(null)
            planner.add(offer)
          }
        )
      }
      actions.append(button(text("card.close")) { closeCard() })
      append(actions)
    }

  private fun knotName(knot: Knot, position: Int): String {
    val session = planner.session ?: return ""
    session.labelOf(knot)?.let {
      return it
    }
    knot.routePoint?.placeName?.let {
      return it
    }
    if (knot.isRoutePoint) return text("routePoint")
    return knot.place?.nameIn(language) ?: ""
  }

  private fun route() =
    element("section", "route") {
      setAttribute("aria-label", text("route.label"))
      val session = planner.session ?: return@element
      val description = planner.description
      if (session.waypoints.isEmpty()) {
        append(element("p", "hint", text("route.empty")))
        return@element
      }
      val list = element("ol", "waypoints")
      session.waypoints.forEachIndexed { index, knot ->
        val role =
          when (session.roleOf(index)) {
            WaypointRole.START -> text("role.start")
            WaypointRole.STOP -> text("role.stop")
            WaypointRole.END -> text("role.end")
          }
        val name = knotName(knot, index)
        list.append(
          element("li") {
            setAttribute("draggable", "true")
            addEventListener(
              "dragstart",
              { event -> event.asDynamic().dataTransfer.setData("text/plain", index.toString()) },
            )
            addEventListener("dragover", { event -> event.preventDefault() })
            addEventListener(
              "drop",
              { event ->
                event.preventDefault()
                event.stopPropagation()
                val from =
                  (event.asDynamic().dataTransfer.getData("text/plain") as String).toIntOrNull()
                if (from != null) planner.move(from, index)
              },
            )
            append(
              element(
                "span",
                if (knot.isRoutePoint) "badge point" else "badge",
                knot.ref.ifEmpty { "⚑" },
              ) {
                if (!knot.isRoutePoint)
                  style.background = cssColour(KnotPlate.of(knot.signRegion).colour)
              },
              element("span", "name") {
                append(element("strong", text = role), element("span", text = name))
              },
            )
            val controls = element("span", "controls")
            if (index > 0)
              controls.append(
                button("↑", "icon") { planner.move(index, index - 1) }
                  .apply { setAttribute("aria-label", text("move.up", knot.ref.ifEmpty { name })) }
              )
            if (index < session.waypoints.lastIndex)
              controls.append(
                button("↓", "icon") { planner.move(index, index + 1) }
                  .apply {
                    setAttribute("aria-label", text("move.down", knot.ref.ifEmpty { name }))
                  }
              )
            if (session.canRemoveWaypoints)
              controls.append(
                button("✕", "icon") { planner.remove(index) }
                  .apply { setAttribute("aria-label", text("remove", knot.ref.ifEmpty { name })) }
              )
            append(controls)
          }
        )
      }
      append(list)
      when (description) {
        is RouteDescription.StartOnly -> append(element("p", "hint", text("route.startOnly")))
        is RouteDescription.Unreachable ->
          append(
            element(
              "p",
              "message problem",
              text("route.unreachable", description.legs.joinToString(", ")),
            )
          )
        is RouteDescription.Complete -> append(summary(description))
        RouteDescription.Empty -> {}
      }
      val actions = element("div", "actions")
      if (planner.canUndo) actions.append(button(text("undo")) { planner.undo() })
      if (session.canReturnToStart)
        actions.append(button(text("backToStart")) { planner.returnToStart() })
      actions.append(button(text("clear")) { planner.clear() })
      append(actions)
    }

  private fun summary(description: RouteDescription.Complete) =
    element("div", "summary") {
      append(
        element("p", "distance") {
          append(
            element("strong", text = description.distance),
            element("span", text = " · " + description.duration),
          )
        }
      )
      if (description.ferries.isNotEmpty()) {
        append(element("h3", text = text("ferries.title")))
        val now = nowInCoveredArea()
        val ferries = element("ul", "ferries")
        description.ferries.forEach { ferry ->
          ferries.append(
            element("li") {
              val status = ferry.statusAt(now)
              append(
                element("details") {
                  append(
                    element("summary") {
                      append(
                        element("strong", text = ferry.place),
                        element("span", text = " " + ferry.title),
                      )
                      append(
                        element(
                          "span",
                          if (status?.open == true) "status open" else "status",
                          " · " +
                            (status?.text ?: if (ferry.timesKnown) "" else text("ferry.noTimes")),
                        )
                      )
                    },
                    element("p", text = ferry.times),
                  )
                  ferry.fee?.let { append(element("p", text = it)) }
                  ferry.ferry.website?.let { site ->
                    append(
                      element("p") {
                        append(
                          (element("a", text = site) as HTMLAnchorElement).apply {
                            href = if ("://" in site) site else "https://$site"
                            rel = "noopener"
                          }
                        )
                      }
                    )
                  }
                  append(element("p", "note", text("ferry.osm")))
                }
              )
            }
          )
        }
        append(ferries)
      }
      append(
        element("details", "knots") {
          append(element("summary", text = text("knots.title", description.knots.size.toString())))
          append(
            element(
              "p",
              text = description.knots.joinToString(" → ") { it.knot.ref.ifEmpty { "⚑" } },
            )
          )
        }
      )
    }

  private fun handOff() =
    element("section", "hand-off") {
      setAttribute("aria-label", text("share.label"))
      append(element("h2", text = text("share.title")))
      val link = planner.link()
      val file = planner.routeFile()
      if (link != null && file != null) {
        val actions = element("div", "actions")
        actions.append(
          button(if (copied) text("share.copied") else text("share.copyLink"), "primary") {
            scope.launch {
              runCatching {
                window.navigator
                  .asDynamic()
                  .clipboard
                  .writeText(link)
                  .unsafeCast<kotlin.js.Promise<Any?>>()
                  .await()
              }
              copied = true
              render()
              delay(2_000)
              copied = false
              render()
            }
          },
          button(text("share.knoop")) { download(file.first, file.second, "text/markdown") },
        )
        planner.gpx()?.let { gpx ->
          actions.append(
            button(text("share.gpx")) { download(gpx.first, gpx.second, "application/gpx+xml") }
          )
        }
        append(actions)
        append(qrCode(link))
      } else {
        append(element("p", "hint", text("share.needsRoute")))
      }
      val input =
        (element("input") as HTMLInputElement).apply {
          type = "file"
          accept = ".knoop,text/markdown,text/plain"
          hidden = true
          addEventListener(
            "change",
            {
              files?.get(0)?.let { openFile(it) }
              value = ""
            },
          )
        }
      append(input, button(text("open")) { input.click() }, element("p", "note", text("open.drop")))
    }

  private fun qrCode(link: String): Element =
    element("figure", "qr") {
      val factory: dynamic = js("require('qrcode-generator')")
      try {
        val code = factory(0, "L")
        code.addData(link, "Byte")
        code.make()
        innerHTML =
          code.createSvgTag(json("cellSize" to 3, "margin" to 3, "scalable" to true)) as String
        firstElementChild?.setAttribute("role", "img")
        firstElementChild?.setAttribute("aria-label", text("share.qrLabel"))
        append(element("figcaption", text = text("share.qr")))
      } catch (exception: Throwable) {
        append(element("figcaption", text = text("share.qrTooLong")))
      }
    }

  private fun download(name: String, content: String, type: String) {
    val blob = Blob(arrayOf(content), BlobPropertyBag(type = "$type;charset=utf-8"))
    val url = URL.createObjectURL(blob)
    val anchor = document.createElement("a") as HTMLAnchorElement
    anchor.href = url
    anchor.download = name
    document.body?.append(anchor)
    anchor.click()
    anchor.remove()
    window.setTimeout({ URL.revokeObjectURL(url) }, 1_000)
  }

  private fun placesSwitch() =
    element("label", "places-switch") {
      val box =
        (element("input") as HTMLInputElement).apply {
          type = "checkbox"
          checked = placesShown
          addEventListener(
            "change",
            {
              placesShown = checked
              mapView.setPlacesShown(checked)
            },
          )
        }
      append(box, element("span", text = text("places.show")))
    }
}

/**
 * The planner page in the browser's language, for a route link opened without the app: the first of
 * [languages] the site has, else English, see `web-planner` "Open a route in the planner".
 */
fun plannerPathFor(languages: List<String>): String {
  val code =
    languages
      .map { it.lowercase().substringBefore('-') }
      .firstOrNull { it in setOf("en", "de", "nl", "fr") }
  return if (code == null || code == "en") "plan/" else "$code/plan/"
}
