# Changelog

## Unreleased

- Share a route from the panel: Share offers "Route for Knoop by Knoop", a small file that lists the knots in riding order like a hand-written knot list, and the GPX export for other apps. Open such a file from Files, mail, a messenger or AirDrop and the app shows the route, ready to ride; Undo brings back the one you had. Text after a stop's number in the file, like "lunch at the Fährhaus", shows in the waypoint list
- Android: Navigate and Stop are Android's own floating button now, a rounded square in the same place on the panel's edge; Stop has left the navigation header, which gets the room for the numbers. Over the lock screen, the button to hold for stopping is in that place too
- Android: on slow phones the app could stay on "Loading the cycling knot network…" with an empty panel after starting, until it was started again
- Android: typing fast into the search no longer loses letters

## 0.16.0

- A tapped knot is added where it fits best: as a stop beside the route, or as the new end beyond it. A short note then offers to make it the start or the end instead. The "Add knots: Between | At end" switch is gone
- Undo in the panel takes back the last change to the route, and each further Undo the one before: tapped knots and places, removed or moved waypoints, Back to start, even Clear. The last 50 changes are kept, also after closing the app
- A tapped spot or a highlighted place (restaurant, campsite, …) offers "Add to route", which adds it where it fits, like a tapped knot, with the same note to change it
- The map's buttons stand in groups on glass (search; position and nearest knots; zoom) right above the panel and move with it. Their icons and the gear for Settings at the top right are in the text colour, so they show in dark mode too. Navigate is a round button, on Android floating on the panel's edge. Back to start, Export GPX and Clear sit in a row below the route's knots instead of a menu. The note after a tap and the place card are in the same glass
- The note after a tap stays open after choosing start, stop or end, so a mistap can be put right; it goes away 6 seconds after the last choice, with the next tap, or at once on Navigate. So do the other planning tools: an open place card, a spot still being added, the search and its marker, and a map move for "Show my position" or "Nearest knots" not yet made
- Before there is a route, the panel shows "Plan a ride" with a slot for the start and one for the end, filled with the knot's badge and place as you tap, a short hint, and buttons to search a place or show the nearest knots
- Whether the map and the paths and streets around a route are saved for offline use shows as two symbols at the end of the panel's action row, with a spinner, a checkmark or a warning mark, instead of two lines of text, so the panel no longer jumps. Tap a symbol to read what it means for the ride. On narrow phones, Back to start shows its icon only. The info sheet's "Ways for guidance" is now "Paths and streets"
- Navigate reacts at once: it shows a spinner while it finds your position, uses a position the phone found in the last minute, and no longer waits for a precise one, which indoors could take up to 10 seconds

## 0.15.0

- Navigate asks first when a ferry on the route will be closed by the time you get there, or closes shortly after: ride around it, with how much longer the way around is, or keep the ferry. With several closed ferries it asks about one at a time, and the map shows each ferry's way around while asking. Riding around counts only for that ride: after stopping or arriving, the plan is back as you made it
- Starting on the route nearby counts only for that ride too: after stopping or arriving, the plan starts at its own start again, and the next Navigate asks again
- The waypoint list and the coming knots while navigating name the village, town or city each knot lies in, and the coming knots show where the route crosses into another country, province or state
- With the phone locked while navigating, the screen lights up with each spoken notice and shows it, silently. On the iPhone the app asks once for permission to show notifications

## 0.14.0

- The map around a planned route is stored on the phone, so streets, villages and rivers are shown along it without a connection too, in the light and the dark map. The panel says when it is loaded, and the info sheet shows its size and can delete it. The info sheet scrolls now
- Started away from the route's start but close to the route, Navigate asks whether to ride to the start or to start on the route nearby. Starting nearby begins the plan where the route is closest, before its first stop. On the way to the start, navigation says "To the start" and no longer joins the route by itself, which could lead along a course against the route and then call it the wrong way
- Navigation started before the network's courses had loaded takes them as soon as they are there, instead of going on without them for the whole ride, which meant no "Off route" and distances to the knots' main positions
- Off route, the map now zooms in on a tricky junction on the way back, as it does on the route; before, it kept showing the rider and the end of the way back
- Android 12 and older: navigation's notification and voice follow the language chosen in the app, not the phone's, also when the phone's language changes on the way

## 0.13.0

- Campsites and hotels or guest houses are highlighted on the map with their own icons, a tent and a bed, next to food, supermarkets and the others
- Tapping a highlighted place shows what OpenStreetMap knows about it: whether it is open now and until when, with its opening hours, its website and its phone number. Tap the website to open it, or the number to call it. The details are downloaded around planned routes, also hiking routes, so they are there offline on the way, and for a place tapped while planning. The info sheet shows their size and can delete them

## 0.12.0

- Starting beside a start flag off the network, the way to the network now follows the planned way: a way found from a first, still imprecise position is looked for again as soon as the position is precise, instead of staying, e.g. a longer way around the block. The same holds off route
- A guided way that begins with a turn says it right away, e.g. "Turn left now", once, also when you ride on without taking it
- Android: when newer network data needs a newer app version, the info sheet asks to update the app; before, the request disappeared again right away. On both phones it now stays until the data can be read, also after closing the app
- iPhone: a broken network download is tried again by itself after 30 s, then after longer and longer waits, instead of only at the next start
- Damaged path data no longer ends the app while it looks for a way along paths; the straight line stays instead
- On a way back to the route, a drawing of a tricky junction ahead shows below the header with what to do, e.g. "In 90 m, keep left at the fork", from about 150 m before it; on the Pixel's lock screen the sentence
- Off the route, the way back leads to where it saves the most, e.g. on to where the route goes next instead of back to where you left it
- At the largest text sizes, the navigation header keeps its badges and arrows in one row with the distance below it, and on a way to the route the turn's distance is no longer cut short on the iPhone
- iPhone: at the largest text sizes, the map's buttons work on the first tap instead of pulling the panel down, and the panel's top, the route's distance or the search field, is no longer cut off
- Android: the place search is a magnifier button with the map's other buttons; tapping it opens the search at the top of the panel, with the places found below it. No search field covers the map any more, and there is none while navigating
- The places found are named in the app's language, in German, English or French, whatever language you search in; in Dutch as named on the spot, e.g. "Köln", as the search service has no Dutch names
- iPhone: typing in the place search no longer redraws the map and the route with each letter, so the letters show without delay

## 0.11.1

- Ways along paths are found within moments again: in 0.11.0 they could take a minute or more on the iPhone, so the way from a start flag stayed a straight line and there was no guidance along it, and guided ways back to the route came late
- On a guided way, the top of the panel stays one row of the same height: the turn and its distance with the street below, the distance left and the next knot, instead of growing to five lines and pushing the map up
- On the way from or to a start, stop or end off the network, the map draws the guided way only where it differs from the route, instead of a second line beside it
- iPhone: the place search is a magnifier button with the map's other buttons; tapping it opens the search at the top of the panel, with the places found below it. No search field floats over the map any more, so it hides none of its buttons, and the map's buttons stay above the panel at any height. A picked place far away is jumped to, with "Showing …" on the map until it is drawn

## 0.11.0

- Android: over the lock screen, the stop button is round and as small as the one in the app; hold it until the ring around it closes to stop, and a short tap shows "Hold to stop"
- While navigating, the map's zoom follows what lies ahead: from 200 m before a knot it zooms in on the knot and the route after it, after passing a knot it zooms out for about 10 s to show the next one, and before a tricky stretch with sharp turns close together it zooms in on it. Otherwise it stays at the usual zoom. Moving the map by hand still stops following, and recenter brings back the zoom for what lies ahead
- Guided all the way to and from a start, stop or end off the network: on the way there along the network, the notices say which knot the course leads to ("Next 02, then towards 06", "Towards 06, then the end"), the turn off the network is told with its street, e.g. "In 100 m, turn right into Cecilienstraße", and then each turn to the flag; from a start flag, each turn to the network and "Turn left now onto the route towards 02". The top of the panel shows the next turn on the light tile meanwhile
- No more false "Off route" with the screen off: a rough position far from the route is first checked with a precise one, and positions the iPhone delivers again are no longer counted twice, which made it switch between rough and precise positioning many times a second
- A route that passes a knot twice is no longer cut short when you ride around at that knot: coming back into its circle counts as the later visit only once you have been more than 200 m away from it
- A start, stop or end made from a tapped place shows the place's icon instead of the flag, on the map, on the strip and in the list, where it also has the place's name, e.g. "Stop · Eiscafé Riviera". It stays after a restart
- Tap a highlighted place to see its name, or what it is when it has none. While planning, "Start here", "Add stop here" and "End here" come with it and put the point at the place itself, so the way leads there; while navigating, only the name shows
- Places that matter on a ride stand out on the map from zoom 14: food and drink, supermarkets, bike shops, railway stations, and drinking water and toilets, each with its own icon on a coloured disc, with names from zoom 16. They give way to knots, sit below the route, and come with the base map, so nothing extra is downloaded. A switch in the info sheet turns them off
- Started away from the route, riding onto its course and along it now joins the route there, with the knot ahead next, instead of still leading to the first knot
- Close to a knot, the arrow on the sign plate shows which way the route goes there: straight on, slightly or sharply left or right, or back, in the app, on the lock screen and in the notification. Without the network's courses it stays a plain arrow
- The lock screen, the Dynamic Island and the Pixel's notification and status chip show the same as the top of the panel, without words (the Dynamic Island only the most important part: the number to follow, the next knot or the arrow, with the distance): the knots, the sign plate close to a knot, the arrow and distance back to the route, and the arrival at the end after ■. Off route and riding the wrong way, the iPhone's card turns to the opposite of its usual light or dark
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
