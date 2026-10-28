import Foundation
import KnoopByKnoopCore

/// The installed networks and the current route in the app's Application Support directory:
/// `networks/<file>`, `networks/installed.json` (the installed index entries) and `route.json`.
/// Files are written atomically, so they are never half-written.
final class NetworkStorage: @unchecked Sendable {

  private let directory: URL
  private let networksDirectory: URL
  private let installedFile: URL
  private let routeFile: URL
  private let cameraFile: URL

  init() {
    directory = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
    networksDirectory = directory.appendingPathComponent("networks")
    installedFile = networksDirectory.appendingPathComponent("installed.json")
    routeFile = directory.appendingPathComponent("route.json")
    cameraFile = directory.appendingPathComponent("camera.json")
    try? FileManager.default.createDirectory(at: networksDirectory, withIntermediateDirectories: true)
  }

  func installed() -> [NetworkIndexEntry] {
    guard let text = try? String(contentsOf: installedFile, encoding: .utf8) else { return [] }
    return (try? NetworkIndex.companion.parse(text: text).files) ?? []
  }

  func graphEntry(network: IndexedNetworkType) -> NetworkIndexEntry? {
    entry(network, .graph)
  }

  func graph(network: IndexedNetworkType) -> GraphFile? {
    guard let entry = entry(network, .graph), let data = try? Data(contentsOf: fileOf(entry))
    else { return nil }
    return try? GraphFile.companion.read(bytes: Data_iosKt.byteArrayOf(data: data))
  }

  /// The format version of the installed graph, or `nil` if none is installed.
  func graphFormatVersion(network: IndexedNetworkType) -> Int? {
    guard let entry = entry(network, .graph) else { return nil }
    return Self.graphFormatVersion(of: fileOf(entry))
  }

  /// The format version of the graph file at `url`, read from its first bytes only.
  static func graphFormatVersion(of url: URL) -> Int? {
    guard let handle = try? FileHandle(forReadingFrom: url) else { return nil }
    defer { try? handle.close() }
    guard let header = try? handle.read(upToCount: Int(GraphFile.companion.HEADER_SIZE)) else { return nil }
    return GraphFile.companion.formatVersion(header: Data_iosKt.byteArrayOf(data: header))?.intValue
  }

  /// The installed geometry, or `nil` if none belongs to the installed graph.
  func geometry(network: IndexedNetworkType, graphFile: GraphFile) -> NetworkGeometry? {
    guard let graphEntry = entry(network, .graph),
      let geometryEntry = entry(network, .geometry),
      let data = try? Data(contentsOf: fileOf(geometryEntry))
    else { return nil }
    return try? NetworkGeometry.companion.read(
      bytes: Data_iosKt.byteArrayOf(data: data), graph: graphFile.graph, graphSha256: graphEntry.sha256)
  }

  /// A map layer of an installed network as a GeoJSON file, built by `build` only once per
  /// installed data: building it for hundreds of thousands of points takes seconds on a phone.
  /// `withGeometry` tells whether the layer depends on the installed geometry as well as on the
  /// graph, so straight lines and courses are cached separately.
  func mapLayer(network: IndexedNetworkType, name: String, withGeometry: Bool, build: () -> String) -> URL {
    let kinds: [IndexedFileKind] = withGeometry ? [.graph, .geometry] : [.graph]
    let dataKey = kinds
      .map { entry(network, $0).map { String($0.sha256.prefix(12)) } ?? "none" }
      .joined(separator: "-")
    let prefix = "\(network.name.lowercased())-\(name)-"
    let file = networksDirectory.appendingPathComponent("\(prefix)\(dataKey).geojson")
    if FileManager.default.fileExists(atPath: file.path) { return file }
    for old in (try? FileManager.default.contentsOfDirectory(atPath: networksDirectory.path)) ?? []
    where old.hasPrefix(prefix) {
      try? FileManager.default.removeItem(at: networksDirectory.appendingPathComponent(old))
    }
    try? Data(build().utf8).write(to: file, options: .atomic)
    return file
  }

  /// Installs a verified file, replacing the installed file of the same network and kind.
  func install(_ entry: NetworkIndexEntry, data: Data) throws {
    try data.write(to: fileOf(entry), options: .atomic)
    try saveInstalled(installed().filter { $0.network != entry.network || $0.kind != entry.kind } + [entry])
  }

  /// Deletes data that is no longer installed: the networks installed per region by earlier app
  /// versions, see `network-sync`, and files replaced by files of another name. The saved route
  /// stays and is restored on the new networks.
  func removeStaleData() {
    try? FileManager.default.removeItem(at: directory.appendingPathComponent("regions"))
    let installedFiles = Set(installed().map(\.file))
    for name in (try? FileManager.default.contentsOfDirectory(atPath: networksDirectory.path)) ?? []
    where ((name.hasSuffix(".graph") || name.hasSuffix(".geometry")) && !installedFiles.contains(name))
      // Map layer caches from before lines and courses were cached separately.
      || name.contains("-connections-")
    {
      try? FileManager.default.removeItem(at: networksDirectory.appendingPathComponent(name))
    }
  }

  /// The place the map showed when the app was left last, see `faster-startup`.
  func camera() -> CameraPlace? {
    guard let data = try? Data(contentsOf: cameraFile) else { return nil }
    return try? JSONDecoder().decode(CameraPlace.self, from: data)
  }

  func saveCamera(_ place: CameraPlace) {
    guard let data = try? JSONEncoder().encode(place) else { return }
    try? data.write(to: cameraFile, options: .atomic)
  }

  func route() -> RouteSnapshot? {
    guard let text = try? String(contentsOf: routeFile, encoding: .utf8) else { return nil }
    return try? RouteSnapshot.companion.parse(text: text)
  }

  func saveRoute(_ snapshot: RouteSnapshot?) {
    if let snapshot {
      try? Data(snapshot.toJson().utf8).write(to: routeFile, options: .atomic)
    } else {
      try? FileManager.default.removeItem(at: routeFile)
    }
  }

  private func entry(_ network: IndexedNetworkType, _ kind: IndexedFileKind) -> NetworkIndexEntry? {
    installed().first { $0.network == network && $0.kind == kind }
  }

  private func fileOf(_ entry: NetworkIndexEntry) -> URL {
    networksDirectory.appendingPathComponent(entry.file)
  }

  private func saveInstalled(_ entries: [NetworkIndexEntry]) throws {
    let index = NetworkIndex(formatVersion: GraphFile.companion.FORMAT_VERSION, generatedAt: "", files: entries)
    try Data(index.toJson().utf8).write(to: installedFile, options: .atomic)
  }
}
