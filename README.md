# trino-properties-index

An index of all available properties in different releases of Trino.

## Usage

Install correct Java version (see `.java-version`) and make sure the `JAVA_HOME` 
environmental variable is set correctly.

Run `./mvnw package` to build the project. 

### Search the property index

The tool uses [sqlite](https://www.sqlite.org/) to create an in-memory database
out of the Trino properties files stored locally in the [properties](properties)
folder. Using an SQL query, it can search through all the Trino properties changes
between any two Trino releases.

Run `report.sh -s 372 -t 375` to list added and removed properties between releases
372 (source) and 375 (target), inclusive.

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

### Update the property index 

In case that the local properties index of the project does not include the properties 
corresponding to a particular Trino release, run `properties/update.sh` to retrieve and
save locally those properties.

This operation may require retrieving several versions of the 
`io.trino:trino-server:${version}:tar.gz` artifacts to the local Maven repository of the
machine on which the tool is being used and may require multiple minutes to complete.
Note that the current project is equipped with an [Update](.github/workflows/update.yaml)
Github workflow specifically tailored to take care of this maintenance task over GitHub
runners, by utilizing their fast internet connection, and ephemeral storage.

Run `properties/update.sh -s 466 -t 468` to create/update locally in the properties local
index the properties corresponding to the Trino releases 466 (source) and 468 (target),
inclusive.

Run `properties/update.sh -s 466` to create/update locally in the properties local index
the properties corresponding to the Trino releases 466 (source) and the latest available
Trino release in the Maven central repository.

