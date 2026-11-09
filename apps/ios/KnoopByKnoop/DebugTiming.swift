import QuartzCore
import UIKit

/// Debug builds only: a timing log in the app's documents, to find what makes the app slow on a
/// real phone. Records events and every moment the main thread stalls for more than 50 ms.
/// Copied off the phone with `xcrun devicectl device copy from`.
enum DebugTiming {
  #if DEBUG
    private static let url = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
      .appendingPathComponent("timing.log")
    private static let queue = DispatchQueue(label: "debug-timing")
    private static let watcher = StallWatcher()
    private static let format: DateFormatter = {
      let format = DateFormatter()
      format.dateFormat = "HH:mm:ss.SSS"
      return format
    }()
  #endif

  /// Starts watching the main thread for stalls.
  @MainActor static func start() {
    #if DEBUG
      watcher.start()
      note("started")
    #endif
  }

  static func note(_ event: String) {
    #if DEBUG
      let line = "\(format.string(from: Date())) \(event)\n"
      queue.async {
        if let handle = try? FileHandle(forWritingTo: url) {
          handle.seekToEndOfFile()
          handle.write(Data(line.utf8))
          try? handle.close()
        } else {
          try? Data(line.utf8).write(to: url)
        }
      }
    #endif
  }
}

#if DEBUG
  /// Notes frames that came more than 50 ms after the one before.
  private final class StallWatcher: NSObject {
    private var link: CADisplayLink?
    private var last: CFTimeInterval = 0

    @MainActor func start() {
      guard link == nil else { return }
      let link = CADisplayLink(target: self, selector: #selector(tick(_:)))
      link.add(to: .main, forMode: .common)
      self.link = link
    }

    @objc private func tick(_ link: CADisplayLink) {
      if last > 0, link.timestamp - last > 0.05 {
        DebugTiming.note("main thread stalled \(Int((link.timestamp - last) * 1000)) ms")
      }
      last = link.timestamp
    }
  }
#endif
