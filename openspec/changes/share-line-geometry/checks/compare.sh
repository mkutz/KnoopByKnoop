#!/bin/zsh
# Replays the ride and round-trips the geometry with the current build; compares with the baseline.
S=${0:A:h:h}
cd /Users/mkutz/Projects/knoopbyknoop && ./gradlew -q :pipeline:installDist 2>&1 | grep "^e:"
L=pipeline/build/install/pipeline/lib
java -Xmx2g -cp $S/geo/replay2.jar:$L/'*' Replay2Kt $S/published $S/ride/fixes.txt new > $S/geo/replay2-now.txt
java -Xmx2g -cp $S/geo/roundtrip.jar:$L/'*' RoundTripKt $S/published $S/geo/now
cmp -s $S/geo/before/cycling.geometry $S/geo/now/cycling.geometry && cmp -s $S/geo/before/hiking.geometry $S/geo/now/hiking.geometry && echo "geometry bytes: same as before"
if diff -q $S/geo/replay2-before.txt $S/geo/replay2-now.txt >/dev/null; then echo "replay: identical"; else echo "replay: $(diff $S/geo/replay2-before.txt $S/geo/replay2-now.txt | grep -c '^<') lines differ"; diff $S/geo/replay2-before.txt $S/geo/replay2-now.txt | head -12; fi
