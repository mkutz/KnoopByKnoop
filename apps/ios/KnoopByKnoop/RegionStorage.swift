import Foundation
import KnoopByKnoopCore

/// The installed regions and the current route in the app's Application Support directory:
/// `regions/<region>/<file>`, `regions/installed.json` (the installed index entries) and
/// `route.json`. Files are written atomically, so they are never half-written.
final class RegionStorage {

  private let regionsDirectory: URL
  private let installedFile: URL
  private let routeFile: URL

  init() {
    let directory = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
    regionsDirectory = directory.appendingPathComponent("regions")
    installedFile = regionsDirectory.appendingPathComponent("installed.json")
    routeFile = directory.appendingPathComponent("route.json")
    try? FileManager.default.createDirectory(at: regionsDirectory, withIntermediateDirectories: true)
  }

  func installed() -> [NetworkIndexEntry] {
    guard let text = try? String(contentsOf: installedFile, encoding: .utf8) else { return [] }
    return (try? NetworkIndex.companion.parse(text: text).files) ?? []
  }

  func graph(region: String, network: IndexedNetworkType) -> GraphFile? {
    guard let entry = entry(region, network, .graph), let data = try? Data(contentsOf: fileOf(entry))
    else { return nil }
    return try? GraphFile.companion.read(bytes: Data_iosKt.byteArrayOf(data: data))
  }

  /// The installed geometry, or `nil` if none belongs to the installed graph.
  func geometry(region: String, network: IndexedNetworkType, graphFile: GraphFile) -> NetworkGeometry? {
    guard let graphEntry = entry(region, network, .graph),
      let geometryEntry = entry(region, network, .geometry),
      let data = try? Data(contentsOf: fileOf(geometryEntry))
    else { return nil }
    return try? NetworkGeometry.companion.read(
      bytes: Data_iosKt.byteArrayOf(data: data), graph: graphFile.graph, graphSha256: graphEntry.sha256)
  }

  /// A map layer of an installed network as a GeoJSON file, built by `build` only once per
  /// installed data: building it for hundreds of thousands of points takes seconds on a phone.
  func mapLayer(region: String, network: IndexedNetworkType, name: String, build: () -> String) -> URL {
    let dataKey = [IndexedFileKind.graph, IndexedFileKind.geometry]
      .map { entry(region, network, $0).map { String($0.sha256.prefix(12)) } ?? "none" }
      .joined(separator: "-")
    let prefix = "\(network.name.lowercased())-\(name)-"
    let directory = regionsDirectory.appendingPathComponent(region)
    let file = directory.appendingPathComponent("\(prefix)\(dataKey).geojson")
    if FileManager.default.fileExists(atPath: file.path) { return file }
    for old in (try? FileManager.default.contentsOfDirectory(atPath: directory.path)) ?? []
    where old.hasPrefix(prefix) {
      try? FileManager.default.removeItem(at: directory.appendingPathComponent(old))
    }
    try? Data(build().utf8).write(to: file, options: .atomic)
    return file
  }

  /// Installs verified files, replacing the installed files of the same region, network and kind.
  func install(_ files: [(entry: NetworkIndexEntry, data: Data)]) throws {
    for file in files {
      try FileManager.default.createDirectory(
        at: regionsDirectory.appendingPathComponent(file.entry.region), withIntermediateDirectories: true)
      try file.data.write(to: fileOf(file.entry), options: .atomic)
    }
    let replaced = Set(files.map { key($0.entry) })
    try saveInstalled(installed().filter { !replaced.contains(key($0)) } + files.map(\.entry))
  }

  /// Removes a region's files, and the current route if it is planned on that region.
  func remove(region: String) {
    try? saveInstalled(installed().filter { $0.region != region })
    try? FileManager.default.removeItem(at: regionsDirectory.appendingPathComponent(region))
    if route()?.isFor(region: region) == true { saveRoute(nil) }
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

  private func entry(_ region: String, _ network: IndexedNetworkType, _ kind: IndexedFileKind)
    -> NetworkIndexEntry?
  {
    installed().first { $0.region == region && $0.network == network && $0.kind == kind }
  }

  private func fileOf(_ entry: NetworkIndexEntry) -> URL {
    regionsDirectory.appendingPathComponent(entry.region).appendingPathComponent(entry.file)
  }

  private func key(_ entry: NetworkIndexEntry) -> String {
    "\(entry.region)/\(entry.network.name)/\(entry.kind.name)"
  }

  private func saveInstalled(_ entries: [NetworkIndexEntry]) throws {
    let index = NetworkIndex(formatVersion: GraphFile.companion.FORMAT_VERSION, generatedAt: "", files: entries)
    try Data(index.toJson().utf8).write(to: installedFile, options: .atomic)
  }
}
