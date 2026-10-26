# Proposal

## Why

The first full run on the Netherlands and Belgium skipped about 1,000 node network routes per country because they seemed to connect more than two knots. In 98% of them, the same number appears twice: one junction tagged on several nodes was not merged into one knot. Each skipped route is a gap in the network.

Measured on the Dutch extract (2,061 pairs of same-number knots on one route):

- 1,637 pairs were kept apart because "a route of their own" connects them. Those routes are almost always short connectors inside a junction (1,113 pairs less than 50 m apart), not ferries.
- 424 pairs were more than 100 m apart, mostly 100 to 500 m: wide junctions and parallel cycle paths. Only 4 pairs are more than 500 m apart.

## What Changes

- Nodes with the same number are one knot within 500 m (instead of 100 m).
- Only a ferry between them keeps them apart: nodes with the same number connected by a route that includes a ferry stay separate knots, so riders can choose which bank to go to.
- A connector route inside a junction no longer keeps its nodes apart.

Out of scope: numbers written with and without leading zeros (`07` and `7`), which are rare and treated as different numbers.

## Capabilities

### New Capabilities

### Modified Capabilities
- `network-data`: the rule for merging nodes with the same number into one knot

## Impact

- `pipeline`: reads the tags of route ways to recognize ferries, new merge rule
- No change to the graph file format or the apps
