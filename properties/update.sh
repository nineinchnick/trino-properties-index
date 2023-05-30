#!/usr/bin/env bash

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
cd "$SCRIPT_DIR" || exit 1

local_repo=$(mvn -B help:evaluate -Dexpression=settings.localRepository -q -DforceStdout)
cmd=(../target/trino-properties-index-*-executable.jar)
if [ "${#cmd[@]}" -eq 0 ]; then
    echo >&2 "ERROR: missing properties scanner executable, run mvn package"
    exit 2
elif [ "${#cmd[@]}" -gt 1 ]; then
    echo >&2 "ERROR: multiple properties scanner executable found, run mvn clean package"
    exit 2
fi

# This is arbitrary - it is the first version that has a health probe
version=405
artifact=io.trino:trino-server:${version}:tar.gz
while mvn -q -C dependency:get -Dtransitive=false -Dartifact=$artifact; do
    properties="$version".csv
    if [ ! -f "$properties" ]; then
        trino_server="$local_repo/io/trino/trino-server/${version}/trino-server-${version}.tar.gz"
        ${cmd[0]} "$trino_server" >"$properties"
    fi
    ((version++))
    artifact=io.trino:trino-server:${version}:tar.gz
done

# combine all properties into one file
properties=properties.csv
echo >"$properties" "version,jar,plugin,config,description,is_deprecated"
for file in ???.csv; do
    tail -n +2 "$file" >>"$properties"
done
