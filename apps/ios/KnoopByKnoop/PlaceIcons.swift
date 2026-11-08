import KnoopByKnoopCore
import UIKit

/// The icons of highlighted places on the map, see `map-planning` "Places useful on a ride": a
/// white glyph on a disc in the kind's colour with a white edge.
enum PlaceIcons {

  /// The disc's diameter in points, a little smaller than a knot badge's plate.
  private static let disc: CGFloat = 21
  /// The white edge around the disc, so it stands out on the light and the dark map.
  private static let edge: CGFloat = 1.5
  /// The glyph's size within the disc.
  private static let glyph: CGFloat = 12

  /// The SF Symbol of each kind, see `design.md` of `highlight-places-on-map`.
  private static func symbol(_ kind: PlaceKind) -> String {
    switch kind {
    case .food: return "fork.knife"
    case .supermarket: return "cart.fill"
    case .bicycle: return "bicycle"
    case .railway: return "lightrail.fill"
    default: return "drop.fill"
    }
  }

  static func image(_ kind: PlaceKind) -> UIImage {
    let size = disc + 2 * edge
    let colour = UInt32(truncatingIfNeeded: kind.colour)
    let fill = UIColor(
      red: CGFloat((colour >> 16) & 0xFF) / 255, green: CGFloat((colour >> 8) & 0xFF) / 255,
      blue: CGFloat(colour & 0xFF) / 255, alpha: 1)
    return UIGraphicsImageRenderer(size: CGSize(width: size, height: size)).image { context in
      UIColor.white.setFill()
      context.cgContext.fillEllipse(in: CGRect(x: 0, y: 0, width: size, height: size))
      fill.setFill()
      context.cgContext.fillEllipse(in: CGRect(x: edge, y: edge, width: disc, height: disc))
      let configuration = UIImage.SymbolConfiguration(pointSize: glyph, weight: .semibold)
      if let symbol = UIImage(systemName: symbol(kind), withConfiguration: configuration)?
        .withTintColor(.white, renderingMode: .alwaysOriginal)
      {
        let scale = min(glyph / symbol.size.width, glyph / symbol.size.height, 1)
        let width = symbol.size.width * scale
        let height = symbol.size.height * scale
        symbol.draw(in: CGRect(x: (size - width) / 2, y: (size - height) / 2, width: width, height: height))
      }
    }
  }
}
