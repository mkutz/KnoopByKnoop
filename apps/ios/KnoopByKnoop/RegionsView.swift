import KnoopByKnoopCore
import SwiftUI

/// The region list, see `region-management`.
struct RegionsView: View {

  @ObservedObject var model: AppModel

  var body: some View {
    NavigationStack {
      List {
        if model.catalog.otherRegionsNeedNetworkAccess {
          Text("Other regions need network access.").foregroundStyle(.secondary)
        }
        ForEach(model.catalog.regions, id: \.region) { region in
          RegionRow(region: region, progress: model.progress[region.region], model: model)
        }
      }
      .navigationTitle("Regions")
      .toolbar {
        if let session = model.session {
          Button("Map") { model.openPlanner(region: session.region) }
        }
      }
    }
  }
}

private struct RegionRow: View {

  let region: RegionStatus
  let progress: Double?
  @ObservedObject var model: AppModel

  var body: some View {
    VStack(alignment: .leading, spacing: 8) {
      Text(region.name).font(.headline)
      Text(details).font(.caption).foregroundStyle(.secondary)
      if region.state == .downloading {
        ProgressView(value: progress ?? 0)
      } else {
        HStack(spacing: 12) {
          switch region.state {
          case .available: Button("Download") { model.download(region) }.buttonStyle(.borderedProminent)
          case .updatable: Button("Update") { model.download(region) }.buttonStyle(.borderedProminent)
          default: EmptyView()
          }
          if region.state == .installed || region.state == .updatable {
            Button("Open") { model.openPlanner(region: region.region) }.buttonStyle(.bordered)
            Button("Remove", role: .destructive) { model.remove(region: region.region) }.buttonStyle(.borderless)
          }
        }
      }
    }
    .padding(.vertical, 4)
  }

  private var details: String {
    let size = String(format: "%.1f MB", Double(region.sizeBytes) / 1_000_000)
    let state =
      switch region.state {
      case .installed: " · installed"
      case .updatable: " · update available"
      default: ""
      }
    return "\(size) · data from \(region.dataDate)\(state)"
  }
}
