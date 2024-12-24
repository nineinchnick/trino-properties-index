# trino-properties-index

An index of all available properties in different versions of Trino

## Usage
1. Install correct Java version (see `.java-version`) and make sure the `JAVA_HOME` environmental variable is set correctly.
2. Run `./mvnw package` to build the project. 
3. Run `properties/update.sh` to extract properties.
4. Run `report.sh -s 372 -t 399` to list added and removed properties between versions 372 (source) and 399 (target), inclusive.

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
