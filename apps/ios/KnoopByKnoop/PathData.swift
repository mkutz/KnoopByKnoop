import Foundation
import KnoopByKnoopCore

/// The squares of path data on the device, shared by planning and navigation.
enum PathData {
  nonisolated(unsafe) static let store: PathStore = {
    let directory = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
      .appendingPathComponent("paths")
    return PathFile_iosKt.openPathStore(directory: directory.path)
  }()
  /// Downloads wait for their answers here, and never on the main thread.
  static let queue = DispatchQueue(label: "com.knoopbyknoop.path-data", qos: .utility)
}

/// Told by the model, asked by a running download between its requests.
final class PathDataCancellation: @unchecked Sendable {
  private let lock = NSLock()
  private var cancelled = false

  func cancel() {
    lock.withLock { cancelled = true }
  }

  var isCancelled: Bool { lock.withLock { cancelled } }
}
