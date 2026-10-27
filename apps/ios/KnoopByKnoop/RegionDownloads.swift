import Foundation
import KnoopByKnoopCore

/// Downloads a region's files with a background `URLSession`: downloads continue when the app is in
/// the background or was closed, and are retried when the connection is back. Started downloads
/// are remembered, so they are picked up again after the app was closed.
final class RegionDownloads: NSObject, URLSessionDownloadDelegate {

  static let baseURL = URL(string: "https://github.com/mkutz/knoopbyknoop/releases/download/network-data/")!
  static let sessionIdentifier = "com.knoopbyknoop.regions"

  enum Progress {
    case running(Double)
    case finished([(entry: NetworkIndexEntry, file: URL)])
    case failed
  }

  /// Called on the main queue whenever a download makes progress, finishes or fails.
  var onChange: () -> Void = {}

  /// Set by the app delegate when the system woke the app for finished background downloads.
  var backgroundCompletionHandler: (() -> Void)?

  private let defaults = UserDefaults.standard
  private let downloadsDirectory = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask)[0]
    .appendingPathComponent("downloads")
  private var receivedBytes: [Int: Int64] = [:]
  private var failedTasks: Set<Int> = []
  private lazy var session: URLSession = {
    let configuration = URLSessionConfiguration.background(withIdentifier: Self.sessionIdentifier)
    configuration.waitsForConnectivity = true
    return URLSession(configuration: configuration, delegate: self, delegateQueue: .main)
  }()

  /// Reconnects to downloads that were started before the app was closed. Downloads whose task is
  /// gone without leaving a file failed, e.g. because the user force-quit the app.
  func reconnect() {
    session.getAllTasks { tasks in
      let running = Set(tasks.map(\.taskIdentifier))
      DispatchQueue.main.async {
        for download in self.pending()
        where !running.contains(download.task) && !FileManager.default.fileExists(atPath: self.fileOf(download.entry).path) {
          self.failedTasks.insert(download.task)
        }
        self.onChange()
      }
    }
  }

  /// The regions with started downloads, finished or not.
  func regions() -> Set<String> {
    Set(pending().map(\.entry.region))
  }

  func start(_ region: RegionStatus) {
    cancel(region: region.region)
    var pending = self.pending()
    for entry in region.files {
      let task = session.downloadTask(with: Self.baseURL.appendingPathComponent(entry.file))
      pending.append((task.taskIdentifier, entry))
      task.resume()
    }
    save(pending)
  }

  func progress(region: String) -> Progress? {
    let downloads = pending().filter { $0.entry.region == region }
    guard !downloads.isEmpty else { return nil }
    if downloads.contains(where: { failedTasks.contains($0.task) }) { return .failed }
    // A file only appears at its path once its download is complete, see didFinishDownloadingTo.
    let files = downloads.map { (entry: $0.entry, file: fileOf($0.entry)) }
      .filter { FileManager.default.fileExists(atPath: $0.file.path) }
    if files.count == downloads.count { return .finished(files) }
    let finished = Set(files.map(\.entry.file))
    let total = max(downloads.map(\.entry.sizeBytes).reduce(0, +), 1)
    let received = downloads.map { finished.contains($0.entry.file) ? $0.entry.sizeBytes : receivedBytes[$0.task] ?? 0 }
      .reduce(0, +)
    return .running(Double(received) / Double(total))
  }

  /// Forgets a region's downloads and deletes their files.
  func cancel(region: String) {
    let all = pending()
    for download in all where download.entry.region == region {
      receivedBytes[download.task] = nil
      failedTasks.remove(download.task)
      try? FileManager.default.removeItem(at: fileOf(download.entry))
    }
    session.getAllTasks { tasks in
      let identifiers = Set(all.filter { $0.entry.region == region }.map(\.task))
      tasks.filter { identifiers.contains($0.taskIdentifier) }.forEach { $0.cancel() }
    }
    save(all.filter { $0.entry.region != region })
  }

  // MARK: URLSessionDownloadDelegate

  func urlSession(
    _ session: URLSession, downloadTask: URLSessionDownloadTask, didWriteData bytesWritten: Int64,
    totalBytesWritten: Int64, totalBytesExpectedToWrite: Int64
  ) {
    receivedBytes[downloadTask.taskIdentifier] = totalBytesWritten
    onChange()
  }

  func urlSession(
    _ session: URLSession, downloadTask: URLSessionDownloadTask, didFinishDownloadingTo location: URL
  ) {
    // The system deletes `location` when this method returns, so the file is moved right away.
    guard let entry = pending().first(where: { $0.task == downloadTask.taskIdentifier })?.entry,
      (downloadTask.response as? HTTPURLResponse)?.statusCode == 200
    else {
      failedTasks.insert(downloadTask.taskIdentifier)
      return
    }
    let file = fileOf(entry)
    try? FileManager.default.createDirectory(at: file.deletingLastPathComponent(), withIntermediateDirectories: true)
    try? FileManager.default.removeItem(at: file)
    do {
      try FileManager.default.moveItem(at: location, to: file)
    } catch {
      failedTasks.insert(downloadTask.taskIdentifier)
    }
  }

  func urlSession(_ session: URLSession, task: URLSessionTask, didCompleteWithError error: Error?) {
    if error != nil, (error as? URLError)?.code != .cancelled { failedTasks.insert(task.taskIdentifier) }
    onChange()
  }

  func urlSessionDidFinishEvents(forBackgroundURLSession session: URLSession) {
    backgroundCompletionHandler?()
    backgroundCompletionHandler = nil
  }

  // MARK: Remembered downloads

  private func fileOf(_ entry: NetworkIndexEntry) -> URL {
    downloadsDirectory.appendingPathComponent(entry.region).appendingPathComponent(entry.file)
  }

  private func pending() -> [(task: Int, entry: NetworkIndexEntry)] {
    (defaults.dictionary(forKey: "downloads") as? [String: String] ?? [:]).compactMap { task, json in
      guard let identifier = Int(task), let entry = try? NetworkIndex.companion.parse(text: json).files.first
      else { return nil }
      return (identifier, entry)
    }
  }

  private func save(_ downloads: [(task: Int, entry: NetworkIndexEntry)]) {
    let encoded = Dictionary(
      uniqueKeysWithValues: downloads.map { download in
        (String(download.task), NetworkIndex(formatVersion: 1, generatedAt: "", files: [download.entry]).toJson())
      })
    defaults.set(encoded, forKey: "downloads")
  }
}
