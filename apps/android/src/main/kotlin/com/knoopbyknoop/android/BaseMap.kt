package com.knoopbyknoop.android

import android.content.Context
import com.knoopbyknoop.core.BaseMapArea
import com.knoopbyknoop.core.BaseMapRegion
import com.knoopbyknoop.core.PathDataStatus
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import org.maplibre.android.MapLibre
import org.maplibre.android.module.http.HttpRequestUtil
import org.maplibre.android.offline.OfflineGeometryRegionDefinition
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineRegionError
import org.maplibre.android.offline.OfflineRegionStatus
import org.maplibre.geojson.MultiPolygon

/**
 * MapLibre's requests name the app and send at most [BaseMapArea.MAX_PARALLEL_REQUESTS] at a time,
 * and at most [BaseMapArea.MAX_TILES] tiles are stored, see `network-sync` "Base map requests".
 */
object BaseMapRequests {
  const val USER_AGENT = "knoopbyknoop-android (https://github.com/mkutz/knoopbyknoop)"

  private var installed = false

  /** Before the first map; again does nothing. */
  fun install(context: Context) {
    if (installed) return
    installed = true
    MapLibre.getInstance(context.applicationContext)
    val dispatcher = Dispatcher().apply { maxRequestsPerHost = BaseMapArea.MAX_PARALLEL_REQUESTS }
    HttpRequestUtil.setOkHttpClient(
      OkHttpClient.Builder()
        .dispatcher(dispatcher)
        .addInterceptor { chain ->
          chain.proceed(chain.request().newBuilder().header("User-Agent", USER_AGENT).build())
        }
        .build()
    )
    OfflineManager.getInstance(context.applicationContext)
      .setOfflineMapboxTileCountLimit(BaseMapArea.MAX_TILES.toLong())
  }
}

/**
 * The base map stored around the planned route, as MapLibre offline regions: one for the overview
 * and one for the detail, named by the route's [BaseMapArea.key], see `network-sync` "Base map
 * around the route" and "Keeping the base map".
 */
class BaseMapStore(context: Context) {
  private val context = context.applicationContext
  private val offline = OfflineManager.getInstance(this.context)

  /** The space the stored base map takes. */
  suspend fun installedBytes(): Long = regions().sumOf { status(it)?.completedResourceSize ?: 0L }

  /** Removes the stored base map, see "Deleting the base map". */
  suspend fun clear() {
    regions().forEach { delete(it) }
  }

  /**
   * Stores [area]'s map, overview first, reporting [onStatus] on the way; a map stored for another
   * route is removed once this one is in. Without network access, a missing map is
   * [PathDataStatus.UNAVAILABLE]. Above [BaseMapArea.MAX_BYTES] the detail stops where it got, see
   * "A long route".
   */
  suspend fun store(
    area: BaseMapArea,
    online: Boolean,
    onStatus: (PathDataStatus) -> Unit,
  ): PathDataStatus {
    val existing = regions()
    var stored = 0L
    for ((part, region) in listOf(OVERVIEW to area.overview, DETAIL to area.detail)) {
      val name = "${area.key} $part"
      val offlineRegion =
        existing.firstOrNull { it.metadata.decodeToString() == name }
          ?: create(region, name)
          ?: return PathDataStatus.UNAVAILABLE
      val status = status(offlineRegion)
      if (status?.isComplete == true) {
        stored += status.completedResourceSize
        continue
      }
      if (!online) return PathDataStatus.UNAVAILABLE
      onStatus(PathDataStatus.LOADING)
      stored = download(offlineRegion, stored) ?: return PathDataStatus.UNAVAILABLE
    }
    // Only the current route's map is kept.
    regions()
      .filterNot { it.metadata.decodeToString().startsWith("${area.key} ") }
      .forEach { delete(it) }
    return PathDataStatus.READY
  }

  /**
   * Downloads [region] until complete or until all stored, with [before] already stored, reaches
   * [BaseMapArea.MAX_BYTES]; returns the size stored then, or `null` on an error.
   */
  private suspend fun download(region: OfflineRegion, before: Long): Long? =
    suspendCancellableCoroutine { continuation ->
      fun finish(result: Long?) {
        region.setObserver(null)
        region.setDownloadState(OfflineRegion.STATE_INACTIVE)
        if (continuation.isActive) continuation.resume(result)
      }
      region.setObserver(
        object : OfflineRegion.OfflineRegionObserver {
          override fun onStatusChanged(status: OfflineRegionStatus) {
            val total = before + status.completedResourceSize
            if (status.isComplete || total >= BaseMapArea.MAX_BYTES) finish(total)
          }

          override fun onError(error: OfflineRegionError) {
            // MapLibre retries a failed request by itself; a lost connection restarts the
            // download from the view model.
          }

          override fun mapboxTileCountLimitExceeded(limit: Long) {
            finish(before)
          }
        }
      )
      region.setDownloadState(OfflineRegion.STATE_ACTIVE)
      continuation.invokeOnCancellation {
        region.setObserver(null)
        region.setDownloadState(OfflineRegion.STATE_INACTIVE)
      }
    }

  private suspend fun create(region: BaseMapRegion, name: String): OfflineRegion? =
    suspendCancellableCoroutine { continuation ->
      val definition =
        OfflineGeometryRegionDefinition(
          BASE_MAP_STYLE,
          MultiPolygon.fromJson(region.geoJson),
          region.minZoom.toDouble(),
          region.maxZoom.toDouble(),
          context.resources.displayMetrics.density,
        )
      offline.createOfflineRegion(
        definition,
        name.encodeToByteArray(),
        object : OfflineManager.CreateOfflineRegionCallback {
          override fun onCreate(offlineRegion: OfflineRegion) = continuation.resume(offlineRegion)

          override fun onError(error: String) = continuation.resume(null)
        },
      )
    }

  private suspend fun regions(): List<OfflineRegion> = suspendCancellableCoroutine { continuation ->
    offline.listOfflineRegions(
      object : OfflineManager.ListOfflineRegionsCallback {
        override fun onList(offlineRegions: Array<OfflineRegion>?) =
          continuation.resume(offlineRegions?.toList().orEmpty())

        override fun onError(error: String) = continuation.resume(listOf())
      }
    )
  }

  private suspend fun status(region: OfflineRegion): OfflineRegionStatus? =
    suspendCancellableCoroutine { continuation ->
      region.getStatus(
        object : OfflineRegion.OfflineRegionStatusCallback {
          override fun onStatus(status: OfflineRegionStatus?) = continuation.resume(status)

          override fun onError(error: String?) = continuation.resume(null)
        }
      )
    }

  private suspend fun delete(region: OfflineRegion) = suspendCancellableCoroutine { continuation ->
    region.delete(
      object : OfflineRegion.OfflineRegionDeleteCallback {
        override fun onDelete() = continuation.resume(Unit)

        override fun onError(error: String) = continuation.resume(Unit)
      }
    )
  }

  private companion object {
    const val OVERVIEW = "overview"
    const val DETAIL = "detail"
  }
}
