import Foundation
import os

/// Logs when the map, the knots and the courses are shown for the first time after the process
/// started, to check the startup times of `map-planning`. Shown in Console.app under the
/// subsystem `com.knoopbyknoop`, category `Startup`.
enum StartupMarks {

  private static let logger = Logger(subsystem: "com.knoopbyknoop", category: "Startup")
  private static let lock = NSLock()
  nonisolated(unsafe) private static var marked: Set<String> = []

  static func isMarked(_ name: String) -> Bool {
    lock.withLock { marked.contains(name) }
  }

  static func mark(_ name: String) {
    let isFirst = lock.withLock { marked.insert(name).inserted }
    guard isFirst else { return }
    let millis = Int((Date().timeIntervalSince1970 - processStart) * 1000)
    logger.notice("startup: \(name, privacy: .public) \(millis, privacy: .public) ms")
  }

  /// The start time of this process, from the kernel.
  private static let processStart: TimeInterval = {
    var info = kinfo_proc()
    var size = MemoryLayout<kinfo_proc>.stride
    var name: [Int32] = [CTL_KERN, KERN_PROC, KERN_PROC_PID, getpid()]
    guard sysctl(&name, 4, &info, &size, nil, 0) == 0 else { return Date().timeIntervalSince1970 }
    let start = info.kp_proc.p_starttime
    return TimeInterval(start.tv_sec) + TimeInterval(start.tv_usec) / 1_000_000
  }()
}
