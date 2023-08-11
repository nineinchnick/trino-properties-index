-- Given two specific versions in a range of versions, figure out new, removed,
-- and properties with updated defaults, with the version in which the change happened.
.import properties/properties.csv raw_properties --csv
WITH
properties AS (
    SELECT
      -- available columns: version,plugin,jar,config,description,is_deprecated
        plugin AS connector
      , config AS name
      , version
      , NULL AS default_value
    FROM raw_properties
)
, first_source AS (
    SELECT
        connector
      , name
      , default_value
    FROM properties
    WHERE version = ${SOURCE}
)
, sources AS (
    SELECT
        connector
      , name
      , max(version) AS last_version
    FROM properties
    WHERE version >= ${SOURCE} AND version < ${TARGET}
    GROUP BY connector, name
)
, intermediate_targets AS (
    SELECT
        connector
      , name
      , min(version) AS first_version
    FROM properties
    WHERE version > ${SOURCE} AND version <= ${TARGET}
    GROUP BY connector, name
)
, targets AS (
    SELECT
        it.connector
      , it.name
      , p.default_value
      , it.first_version
    FROM intermediate_targets it
    JOIN properties p ON (p.connector, p.name, p.version) = (it.connector, it.name, it.first_version)
)
, last_target AS (
    SELECT
        connector
      , name
      , default_value
    FROM properties
    WHERE version = ${TARGET}
)
, groups AS (
    SELECT
        'new-default' AS status
      , connector
      , name
      , default_value
      , first_version AS version
    FROM targets
    WHERE (connector, name) IN (SELECT connector, name FROM first_source)
    AND (connector, name, default_value) NOT IN (SELECT connector, name, default_value FROM first_source)
    UNION ALL
    SELECT
        'new' AS status
      , connector
      , name
      , default_value
      , first_version AS version
    FROM targets
    WHERE (connector, name) NOT IN (SELECT connector, name FROM first_source)
    UNION ALL
    SELECT
        'removed' AS status
      , connector
      , name
      , NULL AS default_value
      , last_version AS version
    FROM sources
    WHERE (connector, name) NOT IN (SELECT connector, name FROM last_target)
)
SELECT status, version, group_concat(connector || ':' || name, ', ') AS names
FROM groups
GROUP BY status, version
ORDER BY status, version
;
