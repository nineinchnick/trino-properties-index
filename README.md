# trino-properties-index

An index of all available properties in different versions of Trino

## Usage

1. Run `logs/update.sh` to get startup logs of multiple Trino versions.
1. Run `properties/update.sh` to extract properties from startup logs.
1. Run `report.sh 372 399` to list added and removed properties between versions 372 and 399, inclusive.

Example output:
```bash
version = 373
 status = removed
  names = query.max-memory-per-task, retry-attempts

version = 374
 status = new
  names = query-retry-attempts, task-retry-attempts-overall, task-retry-attempts-per-task, node-scheduler.max-absolute-full-nodes-per-query, node-scheduler.max-fraction-full-nodes-per-query, node-scheduler.allocator-type, fault-tolerant-execution-task-memory, adaptive-partial-aggregation.enabled, adaptive-partial-aggregation.min-rows, adaptive-partial-aggregation.unique-rows-ratio-threshold, optimizer.filter-conjunction-independence-factor, optimizer.join-multi-clause-independence-factor

version = 375
 status = new
  names = fault-tolerant-execution-task-memory-growth-factor, optimizer.non-estimatable-predicate-approximation.enabled
```

## TODO

1. Run Trino containers with most connectors enabled (including Accumulo, run a ZK container as a dependency) - prepare a list of catalog config files for connectors available in that version (from plugins dir)
1. Write a simple web page to list new and removed properties between two versions (with links to release notes)
1. Publish it using github pagges
1. Check if it's possible to recognize deprecated properties - use semgrep?

> Note: GitHub issues are better for tracking work, but editing 4 lines in a plain text file is still easier.
