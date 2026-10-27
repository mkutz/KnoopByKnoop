import Foundation
import KnoopByKnoopCore

/// Downloads network files with a background `URLSession`: downloads continue when the app is in
/// the background or was closed, and wait for the connection to come back. Started downloads are
/// remembered, so they are picked up again after the app was closed. Every file is downloaded and
/// installed on its own, so files that were complete are not downloaded again.
final class NetworkDownloads: NSObject, URLSessionDownloadDelegate {

  static let sessionIdentifier = "com.knoopbyknoop.networks"
  private static let baseURL = URL(string: NetworkIndex.companion.PUBLISHED_BASE_URL)!

  /// Called on the main queue whenever a download makes progress, finishes or fails.
  var onChange: () -> Void = {}

  /// Set by the app delegate when the system woke the app for finished background downloads.
  var backgroundCompletionHandler: (() -> Void)?

  private let defaults = UserDefaults.standard
  private let downloadsDirectory = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask)[0]
    .appendingPathComponent("downloads")
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

  /// The networks with files being downloaded.
  func downloading() -> Set<IndexedNetworkType> {
    Set(pending().filter { !failedTasks.contains($0.task) }.map(\.entry.network))
  }

  /// The networks whose download failed and is not retried yet.
  func failed() -> Set<IndexedNetworkType> {
    Set(pending().filter { failedTasks.contains($0.task) }.map(\.entry.network))
  }

  /// Starts downloading `entries` that are not being downloaded yet, in their order: the first ones
  /// get the highest priority, so the cycling network comes first.
  func start(_ entries: [NetworkIndexEntry]) {
    var pending = self.pending().filter { !failedTasks.contains($0.task) }
    for (position, entry) in entries.enumerated() where !pending.contains(where: { $0.entry == entry }) {
      let task = session.downloadTask(with: Self.baseURL.appendingPathComponent(entry.file))
      task.priority = max(URLSessionTask.highPriority - Float(position) * 0.1, URLSessionTask.lowPriority)
      pending.append((task.taskIdentifier, entry))
      task.resume()
    }
    save(pending)
  }

  /// The completely downloaded files, ready to be verified and installed.
  func finished() -> [(entry: NetworkIndexEntry, file: URL)] {
    // A file only appears at its path once its download is complete, see didFinishDownloadingTo.
    pending().map { (entry: $0.entry, file: fileOf($0.entry)) }
      .filter { FileManager.default.fileExists(atPath: $0.file.path) }
  }

  /// Forgets a download and deletes its file, after it was installed or rejected.
  func remove(_ entry: NetworkIndexEntry) {
    try? FileManager.default.removeItem(at: fileOf(entry))
    save(pending().filter { $0.entry != entry })
  }

  /// Forgets failed downloads, so `start` downloads their files again.
  func forgetFailed() {
    let all = pending()
    for download in all where failedTasks.contains(download.task) {
      try? FileManager.default.removeItem(at: fileOf(download.entry))
    }
    save(all.filter { !failedTasks.contains($0.task) })
    failedTasks = []
  }

  // MARK: URLSessionDownloadDelegate

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
    downloadsDirectory.appendingPathComponent("\(entry.sha256.prefix(12))-\(entry.file)")
  }

  private func pending() -> [(task: Int, entry: NetworkIndexEntry)] {
    (defaults.dictionary(forKey: "network-downloads") as? [String: String] ?? [:]).compactMap { task, json in
      guard let identifier = Int(task), let entry = try? NetworkIndex.companion.parse(text: json).files.first
      else { return nil }
      return (identifier, entry)
    }
    .sorted { $0.task < $1.task }
  }

  private func save(_ downloads: [(task: Int, entry: NetworkIndexEntry)]) {
    let encoded = Dictionary(
      uniqueKeysWithValues: downloads.map { download in
        (String(download.task), NetworkIndex(formatVersion: 1, generatedAt: "", files: [download.entry]).toJson())
      })
    defaults.set(encoded, forKey: "network-downloads")
  }
}
