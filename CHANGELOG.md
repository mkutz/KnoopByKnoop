# Changelog

## Unreleased

- Guided all the way to and from a start, stop or end off the network: on the way there along the network, the notices say which knot the course leads to ("Next 02, then towards 06", "Towards 06, then the end"), the turn off the network is told with its street, e.g. "In 100 m, turn right into Cecilienstraße", and then each turn to the flag; from a start flag, each turn to the network and "Turn left now onto the route towards 02". The top of the panel shows the next turn on the light tile meanwhile
- No more false "Off route" with the screen off: a rough position far from the route is first checked with a precise one, and positions the iPhone delivers again are no longer counted twice, which made it switch between rough and precise positioning many times a second
- A route that passes a knot twice is no longer cut short when you ride around at that knot: coming back into its circle counts as the later visit only once you have been more than 200 m away from it
- A start, stop or end made from a tapped place shows the place's icon instead of the flag, on the map, on the strip and in the list, where it also has the place's name, e.g. "Stop · Eiscafé Riviera". It stays after a restart
- Tap a highlighted place to see its name, or what it is when it has none. While planning, "Start here", "Add stop here" and "End here" come with it and put the point at the place itself, so the way leads there; while navigating, only the name shows
- Places that matter on a ride stand out on the map from zoom 14: food and drink, supermarkets, bike shops, railway stations, and drinking water and toilets, each with its own icon on a coloured disc, with names from zoom 16. They give way to knots, sit below the route, and come with the base map, so nothing extra is downloaded. A switch in the info sheet turns them off
- Started away from the route, riding onto its course and along it now joins the route there, with the knot ahead next, instead of still leading to the first knot
- Close to a knot, the arrow on the sign plate shows which way the route goes there: straight on, slightly or sharply left or right, or back, in the app, on the lock screen and in the notification. Without the network's courses it stays a plain arrow
- The lock screen, the Dynamic Island and the Pixel's notification and status chip show the same as the top of the panel, without words: the knots, the sign plate close to a knot, the arrow and distance back to the route, and the arrival at the end after ■. Off route and riding the wrong way, the iPhone's card turns to the opposite of its usual light or dark
- The top of the panel while navigating shows no words, only knots, arrows and distances, so it looks the same in every language: far from a knot, the next knot and the distance with the knot after it small; close to it, the number to follow on a sign plate like the arrow signs at the knot. Off route, an arrow points back to the route with the distance to it, and the next knot only while you are on a path of the network; on the way to the route after starting elsewhere, the same on a light tile. The knots of the header are on the strip too, and Stop is a round button. Screen readers still read the header in words
- Knots look like their signposts: red with a white ring in North Rhine-Westphalia, dark green in the Netherlands, blue with a white ring in Belgium, from OpenStreetMap's borders. Your start, stops and end are filled plates with a small symbol at the corner (▶, flag, ■, a circular arrow for a round trip), knots the route passes are white with a coloured ring, and knots not on the route are grey. Hiking knots and knots outside these regions keep the app's green. Red no longer marks a problem: off route and riding the wrong way, the top of the panel turns dark, your dot on the strip becomes a ring with ✕, a ferry you may miss gets a clock, and the line back to the route is dark. Shows once the weekly network data includes the regions
- Bridges over your route: where the route passes under a bridge, the bridge is drawn semi-transparent across it, and where the route runs over a bridge, it gets the bridge's edges, so you see at a glance whether you go under or onto it. The ridden track stays visible on bridges. Shows once the weekly network data includes the bridges
- Routes follow the side of the road meant for your direction where the network marks its ways for one direction, e.g. a cycle path on each side or a one-way street: about 7 % of the cycling network. Shows once the weekly network data includes the directions
- The ridden track no longer zigzags where the position is unsure, e.g. between buildings or with the screen off: points come only once you have clearly moved, and single jumps to the side are left out
- iPhone: a message while planning, e.g. that your position could not be found, no longer makes the route panel disappear until the app is restarted
- A tap far from the network no longer offers to start or end there: more than 20 km from the cycling network or 5 km from the hiking network, the app says there are no knots that close instead of planning a long straight line

## 0.10.0

- Start, stop and end anywhere: tap a place on the map that is no knot and choose "Start here", "Add stop here" or "End here". The place gets a flag, and on a cycling route a way along suitable paths to the network, found on your phone from the downloaded ways; until those are in, and on a hiking route, a straight line. Navigation names it as the start, stop or end
- Android: choosing another language in the info sheet switches the texts at once, without rebuilding the screen and the map (Android 13 and later); messages also follow the chosen language on older versions
- Android: switching between light and dark in the info sheet no longer stutters; the map follows when you close the sheet
- Android, landscape: while you type in the search, the route panel slides aside, so the field and the places found use the whole width above the keyboard; a chip with the route's start, end and distance stays, and tapping it brings the panel back
- Off route on a cycling route, the app guides you back along suitable paths, also without a connection. It downloads the ways around your planned route, 1 to 6 MB for most routes, so GitHub can see the rough area of the route, but never where you are. The info sheet shows the downloaded ways with their size and a button to delete them
- Android: locking the phone while navigating with the app open keeps the navigation over the lock screen; stopping there takes holding the button, which fills up as you hold
- Android: the display no longer stays on after navigation ended, when you had left the app and come back during the ride
- iPhone: the Dynamic Island shows "At 16 follow 17" and the off-route header in full, and on the lock screen the distance and the bar use the whole width
- iPhone: the words on the lock screen and in the Dynamic Island follow the language chosen in the app
- iPhone: at the largest text sizes, the number to follow, Stop, Navigate and the search field are shown in full, and the panel is no longer clipped at its sides
- The distances on the progress strip no longer run into each other at large text sizes
- iPhone: on the lock screen, "At 94 follow 41" no longer breaks inside words at large text sizes; it takes two lines where one is too narrow
- Android: at the largest text size, the words of the "Add knots" switch and the direction off route are no longer cut or broken

## 0.9.0

- A switch in the panel decides where tapped knots go: between the others as before, or at the end, to look around and extend your route as you go
- The switch between cycling and hiking shows a bike and a hiker next to the words

## 0.8.0

- The lock screen shows what the app shows, with a bar along the route and a current distance
- No knots in view? A button shows the way to the nearest ones
- The app tells you when you ride back along the route
- Knots signposted in two places have a badge at both; routes and distances use the right one
- A stretch used both ways shows as two lines, so forks are clear
- The next knot is biggest until you are close, then the number to follow
- Arrival times follow your recent pace

## 0.7.0

- Off route, the dashed line leads back to the nearest point of the route, and you are back on route only on it, not on a path beside it
- Knots a few metres apart, and knots signposted twice like 02 in Siegburg, are passed reliably; the first knot's distance no longer shows 0 m
- The ridden track is on top of the route, and your arrow is bigger
- The map no longer flickers, the compass clears the status bar, and the off-route header is complete in every language and red in dark mode

## 0.6.0

- The apps speak German, Dutch and French: texts, spoken notices, distances and dates follow the phone's language, or the one chosen in Info

## 0.5.0

- While navigating, swipe the panel up to see the coming knots; tap one to see it on the map
- The panel always shows the distances and arrivals on the strip while navigating
- Easier to read at large text sizes
- iOS: no more zooming out to the whole region in landscape, no crash when searching offline, and System appearance works in open sheets

## 0.4.0

- Export the route as a GPX file, with the knots as waypoints, from the new ⋯ menu; Back to start moved there too
- Waypoints have a handle to reorder them and × to remove them, always visible
- A button shows your position; on the first start the app asks for location access
- The dark map shows the same details as the light one, with blue water and readable streets
- Info lets you choose the appearance: System, Light or Dark

## 0.3.1

- Navigation started away from the route leads to its start
- Off route, the app shows and tells the direction and distance to the next knot, with a line on the map

## 0.3.0

- While navigating, a strip shows where you are on the route, with distance and arrival at stops, ferries and the end
- Ferries on the strip are red when closed on arrival; tap one for its details
- The number to follow shows the knots as badges and the distance to the next knot
- The lock screen shows the distance to the next knot and the arrival
- While planning, the route shows how long it takes
- Whole kilometers from 10 km; ⓘ hidden while navigating
- iOS: ferry website links fixed

## 0.2.1

- Info is a small round ⓘ button now
- The waypoint list is more compact, and hidden while navigating
- Each ferry on the route takes one line with where it is and its status, e.g. "76 → 02 · Closed until 1 May 10:00"; tap it for the name, times, fee and website
- While navigating, each ferry ahead shows when you get there and whether it is open then, e.g. "At 18:10: closed until 1 May 10:00"

## 0.2.0

- While navigating, the app warns when a ferry on the route is closed when you get there, or closes shortly after, and the ferry list shows when you get to each ferry

## 0.1.5

- The ferries on a route show whether they are open now and when they open or close next, according to OpenStreetMap

## 0.1.4

- While navigating, the map follows you, an arrow shows your riding direction and a blue line the track you rode; a button recenters the map after you looked around
- The switch between cycling and hiking is hidden while navigating
- The display stays on while navigating with the app open
- iOS: music is back at full volume after each notice, and closing the app ends navigation

## 0.1.3

- Android: the status bar is visible again, and the search field can be tapped all over
- iOS: no location indicator after navigation has ended

## 0.1.2

- Follows the system's light or dark mode, with a dark map

## 0.1.1

- Clearer buttons for Navigate, Stop, Back to start and Clear

## 0.1.0

- Knooppunt networks for cycling and hiking in the Netherlands, Belgium and Nordrhein-Westfalen, from OpenStreetMap
- Plan a route by tapping knots
- Navigate knot by knot, by voice and on the lock screen
- Works offline once a region is downloaded
