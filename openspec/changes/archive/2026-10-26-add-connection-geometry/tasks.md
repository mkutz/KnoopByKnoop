# Tasks

## 1. Measure first

- [x] 1.1 Keep the node sequence of each connection, simplify it with Douglas-Peucker (5 m) and measure the encoded size for the Dutch cycling and hiking networks; adjust the design if it is far off the 2–3 MB estimate

## 2. Pipeline

- [x] 2.1 `connectionOf` returns the course of the shortest path, extended to the knot positions of merged junctions; the kept connection keeps its course
- [x] 2.2 Douglas-Peucker simplification with a tolerance in meters
- [x] 2.3 `build` writes `<region>-<network>.geometry` next to the graph file; `index` lists it with kind `geometry`
- [x] 2.4 Tests: "Winding path" (simplified course stays within 5 m of the ways), "Junction tagged on several nodes", the kept course of two routes between the same knots

## 3. Core

- [x] 3.1 Geometry file writer and reader with the graph checksum check
- [x] 3.2 `kind` in `NetworkIndexEntry`, defaulting to `graph`
- [x] 3.3 `NetworkGeometry` with the course of a connection and of a route in riding direction
- [x] 3.4 Tests: round trip, "Matching files", "Mismatching or missing geometry", "Route in riding direction", "Index lists geometry", an index without `kind` still reads

## 4. Publication

- [x] 4.1 The workflow uploads geometry files and fails if the Dutch cycling geometry is 10 MB or larger ("Geometry size budget")
- [x] 4.2 Run the workflow once and check the published index lists a geometry file for every graph file

## 5. Follow-up in other changes

- [x] 5.1 Update `add-planning-screens`: download geometry with the region, draw connections and routes along their course, fall back to straight lines without matching geometry
