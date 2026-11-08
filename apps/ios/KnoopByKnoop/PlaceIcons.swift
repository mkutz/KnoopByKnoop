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
  static func symbol(_ kind: PlaceKind) -> String {
    switch kind {
    case .food: return "fork.knife"
    case .supermarket: return "cart.fill"
    case .bicycle: return "bicycle"
    case .railway: return "lightrail.fill"
    default: return "drop.fill"
    }
  }

  /// The disc's colour of `kind`.
  static func fill(_ kind: PlaceKind) -> UIColor {
    let colour = UInt32(truncatingIfNeeded: kind.colour)
    return UIColor(
      red: CGFloat((colour >> 16) & 0xFF) / 255, green: CGFloat((colour >> 8) & 0xFF) / 255,
      blue: CGFloat(colour & 0xFF) / 255, alpha: 1)
  }

  /// The name of a kind of place in the app's language, for a place without a name of its own.
  static func kindName(_ kindKey: String) -> String {
    switch kindKey {
    case "food": String(localized: "Food and drink", bundle: AppLanguage.bundle)
    case "supermarket": String(localized: "Supermarket", bundle: AppLanguage.bundle)
    case "bicycle": String(localized: "Bicycle shop", bundle: AppLanguage.bundle)
    case "railway": String(localized: "Railway station", bundle: AppLanguage.bundle)
    case "toilets": String(localized: "Toilets", bundle: AppLanguage.bundle)
    default: String(localized: "Drinking water", bundle: AppLanguage.bundle)
    }
  }

  static func image(_ kind: PlaceKind) -> UIImage {
    let size = disc + 2 * edge
    let fill = fill(kind)
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
