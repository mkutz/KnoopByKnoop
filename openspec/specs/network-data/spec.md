# network-data Specification

## Purpose
Provides the apps with up-to-date knooppunt network graphs from OpenStreetMap, small enough to download per region and route on offline.

## Requirements

### Requirement: Extract node networks from OpenStreetMap
The pipeline SHALL extract every knot (a node with `rcn_ref` or `rwn_ref` in a `network:type=node_network`) and every node-to-node route of the cycling and hiking networks from an OpenStreetMap extract.

#### Scenario: Cycling and hiking are kept apart
- **WHEN** the extract contains a cycling and a hiking network in the same area
- **THEN** the pipeline writes a separate graph for each network type

#### Scenario: Route lengths
- **WHEN** a node-to-node route is extracted
- **THEN** its length in meters is computed from the geometry of its ways

### Requirement: One knot per signposted junction
Nodes of the same network with the same number that belong to one junction SHALL become one knot. Knots with the same number that are separate places SHALL stay separate knots. Only a ferry between two nodes makes them separate places. The graph SHALL keep the position of every node a knot is tagged on.

#### Scenario: Junction tagged on several nodes
- **WHEN** knot 30 is tagged on two nodes within 500 m of each other, e.g. on both sides of a dual carriageway or around a roundabout, and no ferry lies between them
- **THEN** they become one knot 30, and routes arriving at either node meet there

#### Scenario: Connector inside a junction
- **WHEN** a short route of its own connects two nodes tagged 30 of the same junction, without a ferry
- **THEN** they still become one knot 30

#### Scenario: Equal numbers on both banks of a river
- **WHEN** knot 30 is tagged on both banks of a river and a route including a ferry connects the two
- **THEN** they stay two knots 30, connected by that route, so riders can choose which one to go to

#### Scenario: Positions of a merged knot
- **WHEN** knot 02 is tagged on two nodes 195 m apart and they become one knot
- **THEN** the graph file has the positions of both nodes for knot 02

### Requirement: Compact versioned graph files
Each graph file SHALL state its format version, its network type, the area it covers, the date of the OSM data and the ODbL attribution.

#### Scenario: Unsupported format version
- **WHEN** an app reads a graph file with a format version it does not support
- **THEN** it rejects the file with a clear error and keeps the previously installed data

#### Scenario: Size budget
- **WHEN** the graph for the cycling network of the whole covered area is written
- **THEN** the file is smaller than 5 MB

#### Scenario: Apps without the positions of merged knots
- **WHEN** an app released before graph files had the positions of all nodes of a merged knot reads such a file
- **THEN** it installs and reads it as before, each knot at one position

#### Scenario: Graph files without the positions of merged knots
- **WHEN** an app reads a graph file written before it had the positions of all nodes of a merged knot
- **THEN** it reads it as before, each knot with one position

### Requirement: Weekly publication
The network data SHALL be rebuilt and published at least once a week, together with an index of all published files, their network types, sizes, checksums and data dates.

#### Scenario: Failed rebuild keeps old data
- **WHEN** the weekly rebuild fails, including when only one of the extracts cannot be downloaded or read
- **THEN** the previously published files and index stay available unchanged

### Requirement: Checked before publication
Before the weekly network data is published, every file SHALL be read back as the apps read it, each network SHALL have at least a fixed number of knots and connections, no network SHALL lose more than 5 % of its knots or connections against the published data unless the maintainer allows it for that run, and fixed knot pairs SHALL still be routable. If any check fails, nothing SHALL be published.

#### Scenario: File the apps would reject
- **WHEN** a geometry file of the rebuild does not belong to its graph file, or a square of the path data does not match its checksum
- **THEN** the publication fails, and the previously published files and index stay available unchanged

#### Scenario: Too few knots
- **WHEN** a rebuild from a truncated extract has 12,000 cycling knots, below the floor of 18,000
- **THEN** the publication fails, and the previously published data stays

#### Scenario: Sudden drop
- **WHEN** the published hiking network has 50,000 knots and the rebuild has 46,000, a loss of 8 %
- **THEN** the publication fails, and the previously published data stays

#### Scenario: Small change
- **WHEN** the published cycling network has 20,000 knots and the rebuild has 19,500, a loss of 2.5 %, and every other check passes
- **THEN** the rebuild is published

#### Scenario: Deliberate drop
- **WHEN** the maintainer starts a rebuild by hand allowing a drop, and the rebuild loses 8 % of the hiking knots but passes the floors, the read-back and the known routes
- **THEN** the rebuild is published

#### Scenario: Known route lost
- **WHEN** a rebuild has no cycling route between the fixed knot pair from the Netherlands to Belgium, or only one more than twice as long as the straight line between them
- **THEN** the publication fails, and the previously published data stays

#### Scenario: Nothing published yet
- **WHEN** the published data cannot be fetched or read, e.g. on the first publication or after a format version change
- **THEN** the drop is not checked, the other checks still apply, and the run says so in its log

### Requirement: Offline use after download
An app SHALL be able to plan and navigate on an installed network without network access.

#### Scenario: Airplane mode
- **WHEN** the cycling network has been installed and the device has no network access
- **THEN** cycling routes can still be planned anywhere in the covered area

### Requirement: Course of every connection
For every connection, the published data SHALL include the course of the path its length is measured on, accurate to within 5 m of the OpenStreetMap ways. Where a connection reaches a merged knot at another node than the knot's position, its course SHALL continue to the knot's position along the network's ways between the two, and only where no such way exists in a straight line.

#### Scenario: Winding path
- **WHEN** a connection follows a winding forest path
- **THEN** its course follows the path's curves within 5 m, not a straight line between the knots

#### Scenario: Route in riding direction
- **WHEN** a route goes from knot 12 via 7 to 45
- **THEN** its course starts at 12, passes 7 and ends at 45 as one continuous line, regardless of the direction in which each connection was stored

#### Scenario: Junction tagged on several nodes
- **WHEN** a connection ends at a knot that was merged from several nodes
- **THEN** its course still ends at that knot's position

#### Scenario: Along the ways to the knot's position
- **WHEN** a connection reaches knot 02 at its node near the L 16, 195 m from the knot's position, and a way of the network leads from there to the knot's position
- **THEN** its course continues along that way to the knot's position, not straight across the road

#### Scenario: No way between the nodes
- **WHEN** no way of the network joins the node a connection reaches and the knot's position
- **THEN** its course continues in a straight line to the knot's position, as before

### Requirement: Geometry belongs to one graph
A geometry file SHALL belong to exactly one graph file. Geometry SHALL only be used together with the graph file it was built with.

#### Scenario: Matching files
- **WHEN** an app has the graph file and the geometry file of the same build
- **THEN** connections and routes are drawn along their actual course

#### Scenario: Mismatching or missing geometry
- **WHEN** an app has a graph file but no geometry file of the same build, e.g. because only the graph was updated
- **THEN** routing works unchanged, and connections are drawn as straight lines until a matching geometry file is installed

### Requirement: Published geometry files
The weekly publication SHALL include one geometry file per network type, listed in the index with its size, checksum and data date like the graph files.

#### Scenario: Index lists geometry
- **WHEN** an app reads the index
- **THEN** it finds for every graph file the matching geometry file, with size and checksum to verify the download

#### Scenario: Geometry size budget
- **WHEN** the geometry for the cycling network of the whole covered area is written
- **THEN** the file is smaller than 10 MB

### Requirement: One graph per network type
The pipeline SHALL build one graph and one geometry file per network type from all extracts of the covered area together. Elements that appear in more than one extract SHALL be used once.

#### Scenario: Covered area
- **WHEN** the weekly publication is built
- **THEN** it contains one cycling and one hiking graph covering the Netherlands, Belgium and Nordrhein-Westfalen together

#### Scenario: Knot on a border
- **WHEN** a knot lies in the overlap of two extracts
- **THEN** the graph contains it once, with the connections from both sides

#### Scenario: Data date of several extracts
- **WHEN** the extracts have different data dates
- **THEN** the graph states the oldest of them

### Requirement: Routes across borders
Connections that cross the border between two extracts SHALL be part of the graph, so routes can be planned across borders like any other route.

#### Scenario: Connection cut by an extract border
- **WHEN** a node-to-node route crosses the border between the Dutch and the Belgian extract, so that neither extract alone contains all of its ways
- **THEN** the graph contains the connection with its full length and course

#### Scenario: Route from the Netherlands into Belgium
- **WHEN** the user plans a cycling route from a knot in Zeeland to a knot in Flanders
- **THEN** the route crosses the border along the signposted network

### Requirement: Ferry details
For every connection that includes a ferry, the published graph SHALL carry the details OpenStreetMap has about the ferry: its name, operator, operating times, fee and website. Details missing in OpenStreetMap SHALL be left out, not guessed.

#### Scenario: Ferry with times and fee
- **WHEN** a ferry way of a connection has `name`, `opening_hours` and `charge` in OpenStreetMap
- **THEN** the graph carries that name, those operating times and that fee for the connection

#### Scenario: Details on the ferry relation
- **WHEN** a ferry's operating times are only tagged on its route relation, not on its way
- **THEN** the graph still carries them

#### Scenario: Ferry without details
- **WHEN** a ferry has no name, times or fee in OpenStreetMap
- **THEN** the connection is still marked as a ferry, without details

#### Scenario: Older graph files
- **WHEN** an app reads a graph file written before ferry details were published
- **THEN** it reads it as before, without ferry details

### Requirement: Courses to each signpost
Where the ways of a connection's route reach one of its knots at several of the nodes the knot is tagged on, the published data SHALL also include a course to each of those nodes, along the route's own ways and not through another node of that knot. The connection's course and length SHALL stay as they are. Apps that do not know these courses SHALL still read the file.

#### Scenario: Two branches of a route
- **WHEN** the route 02-06 has one branch to the node of 02 by the river and one to its node near the L 16
- **THEN** the data has a course from 06 to each of the two nodes, and the connection keeps its shortest course and length

#### Scenario: Only through another node
- **WHEN** a route reaches a knot's second node only by passing its first
- **THEN** the data has no course of its own to the second node

#### Scenario: One node
- **WHEN** a route reaches a knot at only one of its nodes
- **THEN** the data has no further course for that connection

#### Scenario: Released apps
- **WHEN** an app of version 0.7.0 reads a geometry file with these courses
- **THEN** it reads the connections' courses as before, and ignores the others

#### Scenario: Geometry size budget
- **WHEN** the geometry for the cycling network of the whole covered area is written with these courses
- **THEN** the file is still smaller than 10 MB

### Requirement: Published path data
The weekly publication SHALL include one file with the ways needed for routing bicycles and for drawing what surrounds them, for the whole covered area, built from the same extracts as the networks and split into squares that can be downloaded on their own.

#### Scenario: Ways across borders
- **WHEN** a cycle path crosses from Germany into the Netherlands
- **THEN** the squares on both sides connect, and a way can follow it across the border

#### Scenario: Roads that can't be ridden
- **WHEN** a motorway passes through a square
- **THEN** it is in the file marked as not rideable, so it can be drawn but not routed on

#### Scenario: Failed path build
- **WHEN** building the path data fails
- **THEN** the previously published files and index stay available unchanged

#### Scenario: Path data size budget
- **WHEN** the path file of the whole covered area is written
- **THEN** it is smaller than 150 MB, or the publication fails

### Requirement: Path data in the index
The index SHALL describe the path file in an optional entry of its own, with the data date and what is needed to verify it. Apps that don't know this entry SHALL be able to read the index as before.

#### Scenario: An earlier app reads the new index
- **WHEN** a released app that doesn't know path data reads an index with this entry
- **THEN** it installs the graphs and geometry as before

#### Scenario: An index without path data
- **WHEN** an app reads an index without this entry
- **THEN** it works as without path data, and off route works as it does without a guided way

### Requirement: Versioned path format
The path file SHALL state its format version, so an app can tell that it can't read it.

#### Scenario: Unsupported path format
- **WHEN** the path file has a format version the app doesn't support
- **THEN** the app doesn't use it, navigation works without a guided way, and the graphs and geometry are not affected

### Requirement: Course in each riding direction
Where a node network route marks its ways for one direction only, the data SHALL give each connection the course of each riding direction: a way marked `forward` SHALL be used only in its own direction, a way marked `backward` only against it, and an unmarked way in both. Where the course back is not the course there reversed, the data SHALL include it.

#### Scenario: A cycle path on each side
- **WHEN** the route from 28 to 02 at the Sieg bridge in Siegburg-Zange is mapped with a `forward` way on one side of the road and a `backward` way on the other
- **THEN** the course from 28 to 02 runs on the `forward` way, and the course from 02 to 28 on the `backward` way

#### Scenario: One-way stretch
- **WHEN** a route runs through a one-way street as a `forward` member and the other direction is mapped through a parallel street as a `backward` member
- **THEN** each direction's course uses its own street

#### Scenario: No marks
- **WHEN** no way of a route is marked for one direction
- **THEN** the connection has one course, used reversed for the other direction, as before

#### Scenario: Marks that leave no way
- **WHEN** a route's ways are all marked `forward` against the direction from its first knot to its second
- **THEN** the connection still has a course in that direction, the other direction's course reversed

#### Scenario: Courses to each signpost in each direction
- **WHEN** knot 02 in Siegburg-Zange has two signposts, and the route from 06 to 02 is mapped with separate ways for each direction near 02
- **THEN** each course to a signpost of 02 also has its course back where that differs, and the route from 06 to 02 follows the ways for that direction

#### Scenario: Bridges on the course back
- **WHEN** the course back runs over a bridge the course there does not use
- **THEN** the course back has its own bridge stretch there

#### Scenario: Released apps
- **WHEN** an app of version 0.10.0 reads a geometry file with courses back
- **THEN** it reads the courses, the courses to each signpost and the bridge stretches as before, and ignores the courses back

#### Scenario: Geometry size budget
- **WHEN** the geometry for the cycling network of the whole covered area is written with courses back
- **THEN** the file is still smaller than 10 MB

### Requirement: Bridge stretches of courses
For every course in the published data, including the courses to each signpost, the data SHALL include the stretches of the course that run on a bridge. A bridge is an OpenStreetMap way with a `bridge` tag other than `no`. Each stretch SHALL start and end within 5 m of where the bridge starts and ends along the course. Apps that do not know these stretches SHALL still read the file.

#### Scenario: Course over a bridge
- **WHEN** the connection 28-29 crosses a road on a bridge
- **THEN** its course has one bridge stretch, from where the bridge starts to where it ends

#### Scenario: Course under a bridge
- **WHEN** a connection passes under a railway bridge on an untagged path
- **THEN** its course has no bridge stretch there

#### Scenario: Several ways of one bridge
- **WHEN** a bridge is mapped as two consecutive ways with a `bridge` tag
- **THEN** the course has one bridge stretch covering both

#### Scenario: Bridge tagged no
- **WHEN** a way of a course has `bridge=no`
- **THEN** the course has no bridge stretch there

#### Scenario: Stretch kept through simplification
- **WHEN** the course of a connection is simplified to within 5 m and a bridge starts at a point the simplification would drop
- **THEN** the bridge stretch still starts within 5 m of where the bridge starts

#### Scenario: Signpost course over a bridge
- **WHEN** the course to one of the signposts of a knot runs over a bridge
- **THEN** that course has its own bridge stretch

#### Scenario: Released apps
- **WHEN** an app of version 0.9.0 reads a geometry file with bridge stretches
- **THEN** it reads the courses and the courses to each signpost as before, and ignores the bridge stretches

#### Scenario: Geometry size budget
- **WHEN** the geometry for the cycling network of the whole covered area is written with bridge stretches
- **THEN** the file is still smaller than 10 MB

### Requirement: Sign region of each knot
Each cycling knot in a graph file SHALL carry the region whose signposts it follows, so the apps can show its badge like its signs:
- the Netherlands for a knot inside the Dutch national border
- Belgium for a knot inside the Belgian national border
- North Rhine-Westphalia for a knot inside that state's border

The borders SHALL come from OpenStreetMap's administrative boundaries in the extracts, at build time. Other cycling knots and all hiking knots SHALL carry no region. If a border cannot be read completely from the extracts, the build SHALL fail rather than write wrong regions. Apps released before this requirement SHALL read such graph files as before, and apps SHALL read graph files without regions with no region for any knot.

#### Scenario: Knot in Limburg
- **WHEN** the pipeline builds the cycling graph and knot 95 lies in Roermond
- **THEN** knot 95 carries the region Netherlands

#### Scenario: Knot in Flanders
- **WHEN** knot 150 lies in Haspengouw
- **THEN** it carries the region Belgium

#### Scenario: Knot in North Rhine-Westphalia
- **WHEN** knot 60 lies in Lünen
- **THEN** it carries the region North Rhine-Westphalia

#### Scenario: Knot in another German state
- **WHEN** the North Rhine-Westphalia extract contains a knot just across the border in Rhineland-Palatinate
- **THEN** that knot carries no region

#### Scenario: Knot near a border
- **WHEN** a knot lies 50 m on the Belgian side of the Dutch-Belgian border and is contained in both the Dutch and the Belgian extract
- **THEN** it carries the region Belgium

#### Scenario: Hiking knots
- **WHEN** the pipeline builds the hiking graph
- **THEN** no hiking knot carries a region

#### Scenario: Incomplete border
- **WHEN** the Belgian national border cannot be closed from the ways in the extracts
- **THEN** the build fails and names the incomplete border

#### Scenario: Apps without sign regions
- **WHEN** an app released before graph files had sign regions reads such a file
- **THEN** it installs and reads it as before

#### Scenario: Graph files without sign regions
- **WHEN** an app reads a graph file written before it had sign regions
- **THEN** it reads every knot without a region

### Requirement: Published place details
The weekly publication SHALL include one file with the details of every highlighted place (see `map-planning` "Places useful on a ride") in the covered area: its website, its phone number and its opening hours as OpenStreetMap has them. It SHALL be built from the same extracts as the networks, identify each place by its OpenStreetMap object, so the app can find the details of a place tapped on the base map, and be split into squares that can be downloaded on their own. Places with none of them SHALL be left out. Details missing in OpenStreetMap SHALL be left out, not guessed.

#### Scenario: Campsite with contacts
- **WHEN** a campsite node has `website` and `phone` in OpenStreetMap
- **THEN** the file carries that website and that phone number for that node

#### Scenario: Contact keys
- **WHEN** a hotel has `contact:website` and `contact:phone` instead of `website` and `phone`
- **THEN** the file carries them as its website and phone number

#### Scenario: Place drawn from a building
- **WHEN** a restaurant is mapped as a building outline (a way) with its tags on the way
- **THEN** the file carries its details for that way, and the app finds them when its icon is tapped

#### Scenario: Café with opening hours only
- **WHEN** a café has `opening_hours` but no website and no phone number in OpenStreetMap
- **THEN** the file carries its opening hours as tagged

#### Scenario: Place without contacts
- **WHEN** a drinking water point has no website, no phone number and no opening hours
- **THEN** it is not in the file

#### Scenario: Places that are not highlighted
- **WHEN** a pharmacy or a holiday home has a website
- **THEN** it is not in the file

#### Scenario: Failed places build
- **WHEN** building the place details fails
- **THEN** the previously published place details and the index stay available unchanged, and the networks and the path data are still published

#### Scenario: Place details size budget
- **WHEN** the place details of the whole covered area are written
- **THEN** the file is smaller than 10 MB, or the place details are not published

### Requirement: Place details in the index
The index SHALL describe the place details file in an optional entry of its own, with the data date and what is needed to verify it. Apps that don't know this entry SHALL be able to read the index as before.

#### Scenario: An earlier app reads the index with place details
- **WHEN** a released app that doesn't know place details reads an index with this entry
- **THEN** it installs the graphs, the geometry and the path data as before

#### Scenario: An index without place details
- **WHEN** an app reads an index without this entry
- **THEN** it works as before, and place cards show no contacts

### Requirement: Versioned place details format
The place details file SHALL state its format version, so an app can tell that it can't read it.

#### Scenario: Unsupported place details format
- **WHEN** the place details file has a format version the app doesn't support
- **THEN** the app doesn't use it, place cards show no contacts, and the networks and the path data are not affected

### Requirement: Place of each knot
Each knot in a graph file SHALL carry the place it lies in: a village, town or city of OpenStreetMap, inside its mapped area or close to its point by the place's size. Its name is the place's `name`, as on its signpost, plus its names in English, German, Dutch and French where mapped. Hamlets, suburbs, quarters and districts SHALL NOT count, and a knot in no such place SHALL carry none. Apps released before SHALL read such files as before; files without places give no place for any knot.

#### Scenario: Knot in a town
- **WHEN** the pipeline builds the cycling graph and knot 06 lies at the edge of Lohmar, a town
- **THEN** knot 06 carries the place Lohmar

#### Scenario: Knot in the open
- **WHEN** a knot lies in fields 3 km from the nearest village
- **THEN** it carries no place

#### Scenario: Only a hamlet nearby
- **WHEN** a knot lies beside a hamlet, and the nearest village is far away
- **THEN** it carries no place

#### Scenario: Names in other languages
- **WHEN** a knot lies in Liège, mapped with `name` Liège, `name:de` Lüttich and `name:nl` Luik
- **THEN** the knot's place has the name Liège and those names in German and Dutch

#### Scenario: Graph files without places
- **WHEN** an app reads a graph file written before knots had places
- **THEN** it reads it as before, with no place for any knot

### Requirement: Borders between knots
Each connection in a graph file SHALL carry where along its course it crosses a border of a country, a Dutch or Belgian province, Brussels or a German state, and what lies beyond, named in English, German, Dutch and French, from OpenStreetMap's administrative boundaries. A border that cannot be read completely from the extracts SHALL fail the build rather than give wrong crossings. Apps released before SHALL read such files as before.

#### Scenario: Crossing into Belgium
- **WHEN** the connection from a knot in Dutch Limburg to one in Belgian Limburg crosses the national border 1.2 km after its start
- **THEN** it carries a crossing at 1.2 km into Belgium and the Belgian province of Limburg

#### Scenario: Crossing a province border
- **WHEN** a connection crosses from Gelderland into Overijssel
- **THEN** it carries a crossing into Overijssel only, as the country stays the same

#### Scenario: Within one province
- **WHEN** a connection runs inside North Brabant
- **THEN** it carries no crossing

#### Scenario: Beyond the covered area
- **WHEN** a connection leaves North Rhine-Westphalia into Lower Saxony, whose border the extracts don't hold
- **THEN** it carries no crossing there

#### Scenario: Incomplete border
- **WHEN** a province's boundary relation misses ways in the extracts
- **THEN** the build fails and names the province

#### Scenario: Graph files without borders
- **WHEN** an app reads a graph file written before connections had borders
- **THEN** it reads it as before, with no crossings
