-- Given two specific versions in a range of versions, figure out new, removed,
-- and properties with updated defaults, with the version in which the change happened.
.import properties/properties.csv properties --csv
WITH
first_source AS (
    SELECT
        name
      , default_value
    FROM properties
    WHERE version = ${source}
)
, sources AS (
    SELECT
        name
      , max(version) AS last_version
    FROM properties
    WHERE version >= ${source} AND version < ${target}
    GROUP BY name
)
, intermediate_targets AS (
    SELECT
        name
      , min(version) AS first_version
    FROM properties
    WHERE version > ${source} AND version < ${target}
    GROUP BY name
)
, targets AS (
    SELECT
        it.name
      , p.default_value
      , it.first_version
    FROM intermediate_targets it
    JOIN properties p ON (p.name, p.version) = (it.name, it.first_version)
)
, last_target AS (
    SELECT
        name
      , default_value
    FROM properties
    WHERE version = ${target}
)
, groups AS (
    SELECT
        'new-default' AS status
      , name
      , default_value
      , first_version AS version
    FROM targets
    WHERE name IN (SELECT name FROM first_source)
    AND (name, default_value) NOT IN (SELECT name, default_value FROM first_source)
    UNION ALL
    SELECT
        'new' AS status
      , name
      , default_value
      , first_version AS version
    FROM targets
    WHERE name NOT IN (SELECT name FROM first_source)
    UNION ALL
    SELECT
        'removed' AS status
      , name
      , NULL AS default_value
      , last_version AS version
    FROM sources
    WHERE name NOT IN (SELECT name FROM last_target)
)
SELECT version, status, group_concat(name, ', ') AS names
FROM groups
GROUP BY version, status
ORDER BY version, status
;
