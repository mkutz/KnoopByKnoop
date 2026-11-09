import SwiftUI

/// A drawing of a doubtful junction from its encoded lines, e.g. `B0,-34 0,0|A0,0 -40,0`, see
/// `knot-navigation` "Junction picture" and the core's `JunctionPicture.encoded`: the riding
/// direction up, the way bold in green with an arrow at its end, main roads wide, other streets and
/// cycle paths as short exits, on a dark tile in light and dark appearance alike. Shared with the
/// Live Activity, which does not link the core, so it reads the text itself.
struct JunctionPictureView: View {

  let encoded: String

  /// The view in meters, as the core frames the picture.
  private static let across = 95.0
  private static let top = 32.0
  private static let bottom = -63.0

  private struct Line {
    let kind: Character
    let points: [CGPoint]
  }

  private var lines: [Line] {
    encoded.split(separator: "|").compactMap { part in
      guard let kind = part.first else { return nil }
      let points = part.dropFirst().split(separator: " ").compactMap { pair -> CGPoint? in
        let values = pair.split(separator: ",").compactMap { Double($0) }
        return values.count == 2 ? CGPoint(x: values[0], y: values[1]) : nil
      }
      return points.count >= 2 ? Line(kind: kind, points: points) : nil
    }
  }

  var body: some View {
    Canvas { context, size in
      let side = min(size.width, size.height)
      let scale = side / Self.across
      func point(_ meters: CGPoint) -> CGPoint {
        CGPoint(x: (meters.x + Self.across / 2) * scale, y: (Self.top - meters.y) * scale)
      }
      func path(_ line: Line) -> Path {
        var path = Path()
        path.addLines(line.points.map(point))
        return path
      }
      let width = side / 160
      let round = StrokeStyle(lineWidth: 1, lineCap: .round, lineJoin: .round)
      let all = lines
      // Wide roads first, the way on top, as in the sketches.
      for line in all where line.kind == "M" {
        var style = round
        style.lineWidth = 10 * width
        context.stroke(path(line), with: .color(Self.mainRoad), style: style)
      }
      for line in all where line.kind == "S" || line.kind == "C" {
        var style = round
        style.lineWidth = 3.2 * width
        context.stroke(path(line), with: .color(line.kind == "C" ? Self.cyclePath : Self.street), style: style)
      }
      for line in all where line.kind == "B" {
        var style = round
        style.lineWidth = 5 * width
        context.stroke(path(line), with: .color(Self.way.opacity(0.55)), style: style)
        let start = point(line.points[0])
        let dot = CGRect(x: start.x - 5.5 * width, y: start.y - 5.5 * width, width: 11 * width, height: 11 * width)
        context.fill(Path(ellipseIn: dot), with: .color(.white))
        context.stroke(Path(ellipseIn: dot), with: .color(Self.way), lineWidth: 2.5 * width)
      }
      for line in all where line.kind == "A" {
        var style = round
        style.lineWidth = 5.5 * width
        context.stroke(path(line), with: .color(Self.way), style: style)
        // The arrow at its end, along its last stretch.
        let end = point(line.points[line.points.count - 1])
        let before = point(line.points[line.points.count - 2])
        let angle = atan2(end.y - before.y, end.x - before.x)
        let length = 13 * width
        func at(_ distance: CGFloat, _ turn: CGFloat) -> CGPoint {
          CGPoint(x: end.x + cos(angle + turn) * distance, y: end.y + sin(angle + turn) * distance)
        }
        var head = Path()
        head.move(to: at(length * 0.45, 0))
        head.addLine(to: at(length * 0.75, .pi * 0.78))
        head.addLine(to: at(length * 0.75, -.pi * 0.78))
        head.closeSubpath()
        context.fill(head, with: .color(Self.way))
      }
    }
    .aspectRatio(1, contentMode: .fit)
    .background(Self.tile, in: RoundedRectangle(cornerRadius: 10))
    .clipShape(RoundedRectangle(cornerRadius: 10))
  }

  private static let tile = Color(red: 0x14 / 255, green: 0x1A / 255, blue: 0x1F / 255)
  private static let mainRoad = Color(red: 0x6F / 255, green: 0x7B / 255, blue: 0x86 / 255)
  private static let street = Color(red: 0x7D / 255, green: 0x8A / 255, blue: 0x95 / 255)
  private static let cyclePath = Color(red: 0x5F / 255, green: 0xB8 / 255, blue: 0xD6 / 255)
  private static let way = Color(red: 0x2F / 255, green: 0xD1 / 255, blue: 0x7A / 255)
}
