import Foundation
import KnoopByKnoopCore
import MapLibre

/// MapLibre's requests name the app and send at most `BaseMapArea.maxParallelRequests` at a time,
/// and at most `BaseMapArea.maxTiles` tiles are stored, see `network-sync` "Base map requests".
enum BaseMapRequests {
  static let userAgent = "knoopbyknoop-ios (https://github.com/mkutz/knoopbyknoop)"

  /// Before the first map.
  static func install() {
    let configuration = URLSessionConfiguration.default
    configuration.httpAdditionalHeaders = ["User-Agent": userAgent]
    configuration.httpMaximumConnectionsPerHost = Int(BaseMapArea.companion.MAX_PARALLEL_REQUESTS)
    MLNNetworkConfiguration.sharedManager.sessionConfiguration = configuration
    MLNOfflineStorage.shared.setMaximumAllowedMapboxTiles(UInt64(BaseMapArea.companion.MAX_TILES))
  }
}

/// The base map stored around the planned route, as MapLibre offline packs: one for the overview and
/// one for the detail, named by the route's `BaseMapArea.key`, see `network-sync` "Base map around
/// the route" and "Keeping the base map".
@MainActor
final class BaseMapStore {
  static let shared = BaseMapStore()

  private var storage: MLNOfflineStorage { MLNOfflineStorage.shared }

  /// The space the stored base map takes.
  var installedBytes: Int64 {
    (storage.packs ?? []).reduce(0) { $0 + Int64($1.progress.countOfBytesCompleted) }
  }

  /// The space the stored base map takes, once the packs are read and know their progress.
  func readInstalledBytes() async -> Int64 {
    await waitForPacks()
    for pack in storage.packs ?? [] { await progress(of: pack) }
    return installedBytes
  }

  /// Removes the stored base map, see "Deleting the base map".
  func clear() async {
    for pack in storage.packs ?? [] { await remove(pack) }
  }

  /// Stores `area`'s map, overview first, reporting `onStatus` on the way; a map stored for another
  /// route is removed once this one is in. Without network access, a missing map is unavailable.
  /// Above `BaseMapArea.maxBytes` the detail stops where it got, see "A long route".
  func store(area: BaseMapArea, online: Bool, onStatus: (PathDataStatus) -> Void) async -> PathDataStatus {
    await waitForPacks()
    var stored: Int64 = 0
    for (part, region) in [("overview", area.overview), ("detail", area.detail)] {
      let name = "\(area.key) \(part)"
      let pack: MLNOfflinePack
      if let existing = (storage.packs ?? []).first(where: { Self.name(of: $0) == name }) {
        pack = existing
      } else if let created = await create(region, name: name) {
        pack = created
      } else {
        return .unavailable
      }
      await progress(of: pack)
      if pack.state == .complete {
        stored += Int64(pack.progress.countOfBytesCompleted)
        continue
      }
      guard online else { return .unavailable }
      onStatus(.loading)
      guard let total = await download(pack, before: stored) else { return .unavailable }
      stored = total
    }
    // Only the current route's map is kept.
    for pack in storage.packs ?? [] where !(Self.name(of: pack)?.hasPrefix("\(area.key) ") ?? false) {
      await remove(pack)
    }
    return .ready
  }

  /// A pack read from the database knows its progress only after asking.
  private func progress(of pack: MLNOfflinePack) async {
    pack.requestProgress()
    for _ in 0..<20 where pack.state == .unknown { try? await Task.sleep(for: .milliseconds(50)) }
  }

  private static func name(of pack: MLNOfflinePack) -> String? {
    String(data: pack.context, encoding: .utf8)
  }

  /// The packs are read from the database once after the start; until then `packs` is `nil`.
  private func waitForPacks() async {
    while storage.packs == nil { try? await Task.sleep(for: .milliseconds(100)) }
  }

  private func create(_ region: BaseMapRegion, name: String) async -> MLNOfflinePack? {
    guard
      let shape = try? MLNShape(data: Data(region.geoJson.utf8), encoding: String.Encoding.utf8.rawValue)
    else { return nil }
    let offlineRegion = MLNShapeOfflineRegion(
      styleURL: baseMapStyle(dark: false), shape: shape,
      fromZoomLevel: Double(region.minZoom), toZoomLevel: Double(region.maxZoom))
    return await withCheckedContinuation { continuation in
      storage.addPack(for: offlineRegion, withContext: Data(name.utf8)) { pack, error in
        continuation.resume(returning: error == nil ? pack : nil)
      }
    }
  }

  /// Downloads `pack` until complete or until all stored, with `before` already stored, reaches
  /// `BaseMapArea.maxBytes`; returns the size stored then, or `nil` when cancelled.
  private func download(_ pack: MLNOfflinePack, before: Int64) async -> Int64? {
    pack.resume()
    defer { pack.suspend() }
    while !Task.isCancelled {
      let total = before + Int64(pack.progress.countOfBytesCompleted)
      if pack.state == .complete || total >= BaseMapArea.companion.MAX_BYTES { return total }
      try? await Task.sleep(for: .milliseconds(500))
    }
    return nil
  }

  private func remove(_ pack: MLNOfflinePack) async {
    await withCheckedContinuation { continuation in
      storage.removePack(pack) { _ in continuation.resume() }
    }
  }
}
